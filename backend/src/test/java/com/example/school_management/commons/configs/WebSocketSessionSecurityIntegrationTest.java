package com.example.school_management.commons.configs;

import com.example.school_management.TestcontainersConfiguration;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A real STOMP client against the running server: the account authenticated on CONNECT is kept for
 * the rest of the session, refused frames (including every client SEND) are answered with a generic
 * error, and the handshake only accepts the configured browser origins.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "fixtures"})
class WebSocketSessionSecurityIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @LocalServerPort
    int port;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    SimpUserRegistry userRegistry;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    @Test
    void authenticatedSessionReceivesItsOwnNotifications() throws Exception {
        BaseUser student = userDetailsService.findBaseUserByEmail(DevFixtureLoader.STUDENT_EMAIL);
        String queue = "/queue/user/" + student.getId() + "/notifications";
        StompSession session = connect(ALLOWED_ORIGIN, "Bearer " + accessToken(student), new ErrorCapture());

        CompletableFuture<Object> received = new CompletableFuture<>();
        session.subscribe(queue, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete(payload);
            }
        });
        awaitSubscription(student.getId(), queue);

        messagingTemplate.convertAndSend(queue, Map.of("title", "Absence recorded"));

        assertThat(received.get(10, TimeUnit.SECONDS)).isEqualTo(Map.of("title", "Absence recorded"));
        session.disconnect();
    }

    @Test
    void subscribingToAnotherAccountsQueueIsRefused() throws Exception {
        BaseUser student = userDetailsService.findBaseUserByEmail(DevFixtureLoader.STUDENT_EMAIL);
        ErrorCapture errors = new ErrorCapture();
        StompSession session = connect(ALLOWED_ORIGIN, "Bearer " + accessToken(student), errors);

        session.subscribe("/queue/user/" + (student.getId() + 1) + "/notifications", new StompSessionHandlerAdapter() {});

        assertThat(errors.message.get(10, TimeUnit.SECONDS)).isEqualTo(WebSocketSecurityInterceptor.ACCESS_DENIED);
    }

    @Test
    void sendFromAuthenticatedSessionIsRefused() throws Exception {
        BaseUser admin = userDetailsService.findBaseUserByEmail(DevFixtureLoader.ADMIN_EMAIL);
        ErrorCapture errors = new ErrorCapture();
        StompSession session = connect(ALLOWED_ORIGIN, "Bearer " + accessToken(admin), errors);

        session.send("/app/test", Map.of("message", "hello"));

        assertThat(errors.message.get(10, TimeUnit.SECONDS)).isEqualTo(WebSocketSecurityInterceptor.ACCESS_DENIED);
    }

    @Test
    void connectWithoutTokenIsRefused() throws Exception {
        ErrorCapture errors = new ErrorCapture();
        CompletableFuture<StompSession> session = connectAsync(ALLOWED_ORIGIN, null, errors);

        assertThat(errors.message.get(10, TimeUnit.SECONDS))
                .isEqualTo(WebSocketSecurityInterceptor.AUTHENTICATION_FAILED);
        assertThatThrownBy(() -> session.get(2, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    @Test
    void handshakeFromUnknownOriginIsRefused() {
        BaseUser student = userDetailsService.findBaseUserByEmail(DevFixtureLoader.STUDENT_EMAIL);
        CompletableFuture<StompSession> session =
                connectAsync("http://evil.example", "Bearer " + accessToken(student), new ErrorCapture());

        assertThatThrownBy(() -> session.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
    }

    /** The server registers the subscription under the account id it authenticated on CONNECT. */
    private void awaitSubscription(long accountId, String destination) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (userRegistry.findSubscriptions(subscription ->
                destination.equals(subscription.getDestination())
                        && subscription.getSession().getUser().getName().equals(Long.toString(accountId))).isEmpty()) {
            assertThat(System.currentTimeMillis()).as("subscription registered").isLessThan(deadline);
            Thread.sleep(50);
        }
    }

    private StompSession connect(String origin, String authorization, ErrorCapture errors) throws Exception {
        return connectAsync(origin, authorization, errors).get(10, TimeUnit.SECONDS);
    }

    private CompletableFuture<StompSession> connectAsync(String origin, String authorization, ErrorCapture errors) {
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin(origin);
        StompHeaders connectHeaders = new StompHeaders();
        if (authorization != null) {
            connectHeaders.add("Authorization", authorization);
        }
        return stompClient.connectAsync(
                "ws://localhost:" + port + "/ws-native", handshakeHeaders, connectHeaders, errors);
    }

    private String accessToken(BaseUser user) {
        return jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }

    /** Records the message header of the STOMP ERROR frame the server answers a refused frame with. */
    private static class ErrorCapture extends StompSessionHandlerAdapter {
        final CompletableFuture<String> message = new CompletableFuture<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            message.complete(headers.getFirst("message"));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                    byte[] payload, Throwable exception) {
            message.completeExceptionally(exception);
        }
    }
}
