package app.keystone.domain.system.user.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.user.command.AddUserCommand;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyloUserProvisioningServiceTest {

    @Test
    void provisionUser_shouldReturnNull_whenProvisioningDisabled() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(false);

        KeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        String subject = service.provisionUser(buildCommand());

        assertNull(subject);
    }

    @Test
    void provisionUser_shouldThrowConfigMissing_whenRequiredConfigMissing() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(true);
        properties.setCreateUserUrl("http://keylo.local/v1/admin/users");
        properties.setAdminTokenUrl("http://keylo.local/v1/admin/token");
        properties.setAdminClientId("cli-admin-root");
        properties.setAdminClientSecret("");

        KeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(401, "{}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenMissing() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"token_type\":\"Bearer\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(500, "{\"message\":\"failed\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserReturnsBusinessError() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"error\":\"invalid_token\",\"message\":\"Invalid token\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowSubjectMissing_whenSubjectFieldAbsent() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"data\":{\"user\":{\"username\":\"u-1\"}}}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldReturnSubject_whenProvisioningSucceededWithDataNode() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        TestableKeyloUserProvisioningService service = new TestableKeyloUserProvisioningService(properties);
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(201, "{\"data\":{\"id\":\"sub-2002\"}}"));

        String subject = service.provisionUser(buildCommand());

        assertEquals("sub-2002", subject);
    }

    private HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private KeyloUserProvisioningProperties buildDefaultProperties() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(true);
        properties.setCreateUserUrl("http://keylo.local/v1/admin/users");
        properties.setAdminTokenUrl("http://keylo.local/v1/admin/token");
        properties.setAdminClientId("cli-admin-root");
        properties.setAdminClientSecret("strong-secret");
        properties.setAuthHeaderName("Authorization");
        properties.setSubjectField("id");
        properties.setUsernameField("username");
        properties.setNicknameField("nickname");
        properties.setEmailField("email");
        properties.setPhoneField("phoneNumber");
        properties.setPasswordField("password");
        return properties;
    }

    private AddUserCommand buildCommand() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("keystone-user");
        command.setNickname("Keystone User");
        command.setEmail("user@test.com");
        command.setPhoneNumber("13800000000");
        command.setPassword("Keylo#123456");
        return command;
    }

    private static class TestableKeyloUserProvisioningService extends KeyloUserProvisioningService {
        private final Deque<HttpResponse<String>> responses = new ArrayDeque<>();

        private TestableKeyloUserProvisioningService(KeyloUserProvisioningProperties properties) {
            super(properties);
        }

        void enqueue(HttpResponse<String> response) {
            responses.addLast(response);
        }

        @Override
        protected HttpResponse<String> sendPostJson(String url, String jsonBody, Map<String, String> headers, int timeoutMillis) {
            HttpResponse<String> response = responses.pollFirst();
            if (response == null) {
                throw new IllegalStateException("No mocked response for " + url);
            }
            return response;
        }
    }
}
