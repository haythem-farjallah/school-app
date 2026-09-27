package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * STOMP CONNECT authentication, SUBSCRIBE authorization and revocation of connected sessions, using
 * real tokens issued by JwtTokenProvider and accounts loaded from PostgreSQL.
 */
@IntegrationTest
class WebSocketSecurityInterceptorIntegrationTest {

    private static final MessageChannel CHANNEL = (message, timeout) -> true;

    @Autowired
    WebSocketSecurityInterceptor interceptor;

    @Autowired
    WebSocketDeliveryInterceptor deliveryInterceptor;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    String jwtSecret;

    /* ---------------- CONNECT ---------------- */

    @Test
    void connectWithoutTokenIsRejected() {
        assertConnectRejected(null);
    }

    @Test
    void connectWithMalformedTokenIsRejected() {
        assertConnectRejected("Bearer not-a-jwt");
        assertConnectRejected("Basic " + accessToken(DevFixtureLoader.STUDENT_EMAIL));
    }

    @Test
    void connectWithRefreshTokenIsRejected() {
        BaseUser user = userDetailsService.findBaseUserByEmail(DevFixtureLoader.STUDENT_EMAIL);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                userDetailsService.toUserDetails(user), user.getTokenVersion());

        assertConnectRejected("Bearer " + refreshToken);
    }

    @Test
    void connectWithExpiredAccessTokenIsRejected() {
        long now = System.currentTimeMillis();
        String expired = Jwts.builder()
                .setSubject(DevFixtureLoader.STUDENT_EMAIL)
                .claim("tokenType", "ACCESS")
                .claim("tokenVersion", 0)
                .setIssuedAt(new Date(now - 120_000))
                .setExpiration(new Date(now - 60_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertConnectRejected("Bearer " + expired);
    }

    @Test
    void connectWithValidAccessTokenAuthenticatesTheAccount() {
        String email = student();
        BaseUser user = userRepository.findByEmail(email).orElseThrow();

        Authentication authentication = connect("Bearer " + accessToken(email));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new WebSocketPrincipal(user.getId(), user.getTokenVersion()));
        assertThat(authentication.getName()).isEqualTo(user.getId().toString());
        assertThat(authentication.getPrincipal().toString()).doesNotContain(email);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_STUDENT")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    void connectForDeletedAccountIsRejected() {
        String email = student();
        String token = accessToken(email);
        setStatus(email, Status.DELETED);

        assertConnectRejected("Bearer " + token);
    }

    @Test
    void connectForSuspendedAccountIsRejected() {
        String email = student();
        String token = accessToken(email);
        setStatus(email, Status.SUSPENDED);

        assertConnectRejected("Bearer " + token);
    }

    @Test
    void connectWithRevokedTokenVersionIsRejected() {
        String email = student();
        String token = accessToken(email);
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.saveAndFlush(user);

        assertConnectRejected("Bearer " + token);
    }

    @Test
    void connectForAccountThatMustChangeItsPasswordIsRejected() {
        String email = student();
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordChangeRequired(true);
        userRepository.saveAndFlush(user);

        assertConnectRejected("Bearer " + accessToken(email));
    }

    /* ---------------- SUBSCRIBE ---------------- */

    @Test
    void adminSubscriptions() {
        Authentication admin = connect("Bearer " + accessToken(DevFixtureLoader.ADMIN_EMAIL));
        String ownQueue = personalQueue(admin);

        assertSubscribeAllowed(admin, "/topic/system-alerts");
        assertSubscribeAllowed(admin, "/topic/admin-feeds");
        assertSubscribeAllowed(admin, "/topic/notifications/admin");
        assertSubscribeAllowed(admin, ownQueue);

        assertSubscribeDenied(admin, "/topic/notifications/student");
        assertSubscribeDenied(admin, otherPersonalQueue(admin));
    }

    @Test
    void studentSubscriptions() {
        Authentication student = connect("Bearer " + accessToken(DevFixtureLoader.STUDENT_EMAIL));

        assertSubscribeAllowed(student, "/topic/system-alerts");
        assertSubscribeAllowed(student, "/topic/notifications/student");
        assertSubscribeAllowed(student, personalQueue(student));

        assertSubscribeDenied(student, "/topic/admin-feeds");
        assertSubscribeDenied(student, "/topic/notifications/admin");
        assertSubscribeDenied(student, "/topic/notifications/parent");
        assertSubscribeDenied(student, otherPersonalQueue(student));
    }

    @Test
    void teacherCannotSubscribeToAnotherRoleTopic() {
        Authentication teacher = connect("Bearer " + accessToken(DevFixtureLoader.TEACHER_EMAIL));

        assertSubscribeAllowed(teacher, "/topic/notifications/teacher");
        assertSubscribeDenied(teacher, "/topic/notifications/admin");
    }

    @Test
    void unknownDestinationsAreDenied() {
        Authentication admin = connect("Bearer " + accessToken(DevFixtureLoader.ADMIN_EMAIL));
        long id = ((WebSocketPrincipal) admin.getPrincipal()).accountId();

        assertSubscribeDenied(admin, "/topic/test");
        assertSubscribeDenied(admin, "/topic/notifications");
        assertSubscribeDenied(admin, "/topic/notifications/ADMIN");
        assertSubscribeDenied(admin, "/topic/announcements");
        assertSubscribeDenied(admin, "/topic/**");
        assertSubscribeDenied(admin, "/user/" + id + "/notifications");
        assertSubscribeDenied(admin, "/queue/user/" + id + "/notifications/extra");
        assertSubscribeDenied(admin, null);
    }

    @Test
    void unauthenticatedSubscribeIsDenied() {
        assertThatThrownBy(() -> send(subscribe(null, "/topic/system-alerts")))
                .isInstanceOf(MessagingException.class)
                .hasMessage(WebSocketSecurityInterceptor.AUTHENTICATION_FAILED);
    }

    @Test
    void unauthenticatedSendIsDenied() {
        Message<byte[]> sendFrame = frame(StompCommand.SEND, accessor -> accessor.setDestination("/app/test"));

        assertThatThrownBy(() -> send(sendFrame))
                .isInstanceOf(MessagingException.class)
                .hasMessage(WebSocketSecurityInterceptor.AUTHENTICATION_FAILED);
    }

    @Test
    void authenticatedSendIsDenied() {
        Authentication admin = connect("Bearer " + accessToken(DevFixtureLoader.ADMIN_EMAIL));

        for (String destination : new String[] {"/app/test", "/app/admin/broadcast", "/topic/system-alerts"}) {
            Message<byte[]> sendFrame = frame(StompCommand.SEND, accessor -> {
                accessor.setUser(admin);
                accessor.setDestination(destination);
            });

            assertThatThrownBy(() -> send(sendFrame))
                    .as("send to %s", destination)
                    .isInstanceOf(MessagingException.class)
                    .hasMessage(WebSocketSecurityInterceptor.ACCESS_DENIED);
        }
    }

    @Test
    void authenticatedSessionFramesOtherThanSendStillPass() {
        Authentication student = connect("Bearer " + accessToken(DevFixtureLoader.STUDENT_EMAIL));

        for (StompCommand command : new StompCommand[] {StompCommand.UNSUBSCRIBE, StompCommand.DISCONNECT}) {
            Message<byte[]> sessionFrame = frame(command, accessor -> {
                accessor.setUser(student);
                accessor.setSubscriptionId("sub-0");
            });

            assertThatCode(() -> send(sessionFrame)).as("%s", command).doesNotThrowAnyException();
        }
    }

    /* ---------------- revocation after CONNECT ---------------- */

    @Test
    void suspendedAfterConnect() {
        assertRevokedAfterConnect(email -> setStatus(email, Status.SUSPENDED));
    }

    @Test
    void deletedAfterConnect() {
        assertRevokedAfterConnect(email -> setStatus(email, Status.DELETED));
    }

    @Test
    void tokenVersionRevokedAfterConnect() {
        assertRevokedAfterConnect(email -> {
            BaseUser user = userRepository.findByEmail(email).orElseThrow();
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.saveAndFlush(user);
        });
    }

    @Test
    void passwordChangeRequiredAfterConnect() {
        assertRevokedAfterConnect(email -> {
            BaseUser user = userRepository.findByEmail(email).orElseThrow();
            user.setPasswordChangeRequired(true);
            userRepository.saveAndFlush(user);
        });
    }

    @Test
    void protocolFramesToAnyConnectedSessionPass() {
        String email = student();
        String sessionId = "session-" + UUID.randomUUID();
        connect("Bearer " + accessToken(email), sessionId);
        setStatus(email, Status.SUSPENDED);

        for (SimpMessageType type : new SimpMessageType[] {
                SimpMessageType.CONNECT_ACK, SimpMessageType.HEARTBEAT, SimpMessageType.DISCONNECT_ACK}) {
            SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
            accessor.setSessionId(sessionId);
            Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThat(deliveryInterceptor.preSend(frame, CHANNEL)).as("%s", type).isSameAs(frame);
        }
    }

    @Test
    void messagesToUnknownOrClosedSessionsAreSuppressed() {
        String sessionId = "session-" + UUID.randomUUID();
        assertThat(deliver(sessionId)).isNull();
        assertThat(deliver(null)).isNull();

        Authentication student = connect("Bearer " + accessToken(student()), sessionId);
        assertThat(deliver(sessionId)).isNotNull();

        send(frame(StompCommand.DISCONNECT, sessionId, accessor -> accessor.setUser(student)));
        assertThat(deliver(sessionId)).isNull();
    }

    /**
     * A session that was valid at CONNECT: frames and deliveries pass until the account changes, after
     * which every later frame but DISCONNECT is refused generically and no message is delivered.
     */
    private void assertRevokedAfterConnect(Consumer<String> revoke) {
        String email = student();
        String sessionId = "session-" + UUID.randomUUID();
        Authentication user = connect("Bearer " + accessToken(email), sessionId);
        String ownQueue = personalQueue(user);

        assertSubscribeAllowed(user, ownQueue);
        assertThat(deliver(sessionId)).as("delivery before revocation").isNotNull();

        revoke.accept(email);

        assertThat(deliver(sessionId)).as("delivery after revocation").isNull();
        for (StompCommand command : new StompCommand[] {
                StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE, StompCommand.SEND}) {
            Message<byte[]> sessionFrame = frame(command, sessionId, accessor -> {
                accessor.setUser(user);
                accessor.setSubscriptionId("sub-0");
                accessor.setDestination(ownQueue);
            });

            assertThatThrownBy(() -> send(sessionFrame))
                    .as("%s", command)
                    .isInstanceOf(MessagingException.class)
                    .hasMessage(WebSocketSecurityInterceptor.AUTHENTICATION_FAILED);
        }
        assertThatCode(() -> send(frame(StompCommand.DISCONNECT, sessionId, accessor -> accessor.setUser(user))))
                .doesNotThrowAnyException();
    }

    /** A broker MESSAGE addressed to the session, as the simple broker hands it to the outbound channel. */
    private Message<?> deliver(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId("sub-0");
        accessor.setDestination("/topic/system-alerts");
        return deliveryInterceptor.preSend(
                MessageBuilder.createMessage("{}".getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders()),
                CHANNEL);
    }

    /* ---------------- helpers ---------------- */

    private Authentication connect(String authorization) {
        return connect(authorization, "session-" + UUID.randomUUID());
    }

    private Authentication connect(String authorization, String sessionId) {
        Message<byte[]> connectFrame = frame(StompCommand.CONNECT, sessionId, accessor -> {
            if (authorization != null) {
                accessor.setNativeHeader("Authorization", authorization);
            }
        });
        Message<?> sent = send(connectFrame);
        Principal user = MessageHeaderAccessor.getAccessor(sent, StompHeaderAccessor.class).getUser();
        assertThat(user).isInstanceOf(Authentication.class);
        return (Authentication) user;
    }

    private void assertConnectRejected(String authorization) {
        assertThatThrownBy(() -> connect(authorization))
                .isInstanceOf(MessagingException.class)
                .hasMessage(WebSocketSecurityInterceptor.AUTHENTICATION_FAILED);
    }

    private void assertSubscribeAllowed(Authentication user, String destination) {
        assertThatCode(() -> send(subscribe(user, destination)))
                .as("subscribe to %s", destination)
                .doesNotThrowAnyException();
    }

    private void assertSubscribeDenied(Authentication user, String destination) {
        assertThatThrownBy(() -> send(subscribe(user, destination)))
                .as("subscribe to %s", destination)
                .isInstanceOf(MessagingException.class)
                .hasMessage(WebSocketSecurityInterceptor.ACCESS_DENIED);
    }

    private static Message<byte[]> subscribe(Authentication user, String destination) {
        return frame(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setUser(user);
            accessor.setSubscriptionId("sub-0");
            if (destination != null) {
                accessor.setDestination(destination);
            }
        });
    }

    private Message<?> send(Message<byte[]> message) {
        return interceptor.preSend(message, CHANNEL);
    }

    private static Message<byte[]> frame(StompCommand command, Consumer<StompHeaderAccessor> headers) {
        return frame(command, "session-" + UUID.randomUUID(), headers);
    }

    private static Message<byte[]> frame(StompCommand command, String sessionId,
                                         Consumer<StompHeaderAccessor> headers) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        headers.accept(accessor);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static String personalQueue(Authentication user) {
        return "/queue/user/" + ((WebSocketPrincipal) user.getPrincipal()).accountId() + "/notifications";
    }

    private static String otherPersonalQueue(Authentication user) {
        return "/queue/user/" + (((WebSocketPrincipal) user.getPrincipal()).accountId() + 1) + "/notifications";
    }

    private String accessToken(String email) {
        BaseUser user = userDetailsService.findBaseUserByEmail(email);
        return jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }

    private String student() {
        String email = "websocket-" + UUID.randomUUID() + "@accounts.school.test";
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail(email);
        student.setFirstName("Web");
        student.setLastName("Socket");
        student.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        student.setStatus(Status.ACTIVE);
        student.setPasswordChangeRequired(false);
        student.setIsEmailVerified(true);
        studentRepository.saveAndFlush(student);
        return email;
    }

    private void setStatus(String email, Status status) {
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(status);
        userRepository.saveAndFlush(user);
    }
}
