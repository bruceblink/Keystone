package app.keystone.admin.customize.service.login.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.keystone.common.utils.jackson.JacksonUtil;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyloCredentialVerifierTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void verify_shouldReturnNullExpiresIn_whenTokenResponseDoesNotContainExpiresIn() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();

        server.createContext("/token", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBody = "{\"access_token\":\"keylo-access-token\",\"token_type\":\"Bearer\"}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.createContext("/me", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] responseBody = "{\"uid\":\"sub-001\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        KeyloProperties properties = new KeyloProperties();
        properties.setCredentialVerifyUrl(baseUrl + "/token");
        properties.setCredentialMeUrl(baseUrl + "/me");
        properties.setCredentialUsernameField("username");
        properties.setCredentialPasswordField("password");
        properties.setSubjectClaim("uid");

        KeyloCredentialVerifier verifier = new KeyloCredentialVerifier(properties);

        KeyloPrincipal principal = verifier.verify("admin", "plain-password");

        assertEquals("sub-001", principal.getSubject());
        assertEquals("keylo-access-token", principal.getAccessToken());
        assertNull(principal.getRefreshToken());
        assertNull(principal.getExpiresIn());
        assertEquals("Bearer", principal.getTokenType());
        assertEquals("Bearer keylo-access-token", authorizationHeader.get());
        assertEquals("admin", JacksonUtil.getAsString(requestBody.get(), "username"));
        assertEquals("plain-password", JacksonUtil.getAsString(requestBody.get(), "password"));
    }
}
