package com.example.school_management.commons.filter;

import com.example.school_management.TestcontainersConfiguration;
import com.example.school_management.commons.configs.RateLimitingConfig;
import com.example.school_management.dev.DevFixtureLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Percent-encoded spellings of an endpoint path, sent to the real Tomcat server (MockMvc never
 * decodes a request URI). Tomcat and Spring MVC decode unreserved characters, so
 * /api/auth/%6Cogin and /api/%61uth/login are routed to the login endpoint; they must share its
 * bucket and its classification. Encoded slashes and NUL are refused by Tomcat before routing.
 *
 * <p>The loopback peer is a trusted proxy here, so X-Forwarded-For gives each test its own client.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.trusted-proxies=127.0.0.1")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "fixtures"})
class RateLimitingEncodedPathIntegrationTest {

    private static final long AUTH_CAPACITY = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();
    private static final long REFRESH_CAPACITY = RateLimitingConfig.REFRESH_CONFIGURATION.getBandwidths()[0].getCapacity();

    private static final List<String> LOGIN_SPELLINGS = List.of(
            "/api/auth/login",
            "/api/auth/%6Cogin",
            "/api/auth/%6cogin",
            "/api/auth/l%6Fgin",
            "/api/%61uth/login",
            "/%61pi/auth/login");

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Test
    void everyEncodedSpellingOfLoginReachesLoginAndSharesOneAuthBucket() throws Exception {
        String address = "198.51.100.61";
        long remaining = AUTH_CAPACITY;
        for (String path : LOGIN_SPELLINGS) {
            HttpResponse<String> response = post(path, address, validLogin());

            // Routed to the login endpoint: valid credentials are answered with an access token.
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(response.body()).as(path).contains("accessToken");
            remaining--;
            assertThat(response.headers().firstValue("X-Rate-Limit-Remaining")).as(path).hasValue(String.valueOf(remaining));
        }
    }

    @Test
    void encodedSpellingsCannotContinuePastAnExhaustedLoginBucket() throws Exception {
        String address = "198.51.100.62";
        for (int i = 0; i < AUTH_CAPACITY; i++) {
            assertThat(post("/api/auth/login", address, wrongPassword()).statusCode()).isEqualTo(401);
        }

        for (String path : LOGIN_SPELLINGS) {
            assertThat(post(path, address, validLogin()).statusCode()).as(path).isEqualTo(429);
        }
    }

    @Test
    void anEncodedRefreshPathUsesTheRefreshBucket() throws Exception {
        String address = "198.51.100.63";
        String body = "{\"refreshToken\":\"not-a-token\"}";

        HttpResponse<String> plain = post("/api/auth/refresh", address, body);
        HttpResponse<String> encoded = post("/api/auth/%72efresh", address, body);

        assertThat(plain.statusCode()).isEqualTo(401);
        assertThat(encoded.statusCode()).isEqualTo(401);
        assertThat(plain.headers().firstValue("X-Rate-Limit-Remaining")).hasValue(String.valueOf(REFRESH_CAPACITY - 1));
        assertThat(encoded.headers().firstValue("X-Rate-Limit-Remaining")).hasValue(String.valueOf(REFRESH_CAPACITY - 2));
    }

    @Test
    void encodedSlashesAndNulAreRejectedBeforeRouting() throws Exception {
        for (String path : List.of("/api/auth%2Flogin", "/api/auth/login%2F", "/api/auth/login%00")) {
            HttpResponse<String> response = post(path, "198.51.100.64", validLogin());

            assertThat(response.statusCode()).as(path).isEqualTo(400);
            assertThat(response.body()).as(path).doesNotContain("accessToken");
        }
    }

    private HttpResponse<String> post(String path, String forwardedFor, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String validLogin() {
        return credentials(DevFixtureLoader.PASSWORD);
    }

    private static String wrongPassword() {
        return credentials("not-the-password");
    }

    private static String credentials(String password) {
        return "{\"email\":\"" + DevFixtureLoader.TEACHER_EMAIL + "\",\"password\":\"" + password + "\"}";
    }
}
