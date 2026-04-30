package app.keystone.domain.system.user.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.user.command.AddUserCommand;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class KeyloUserProvisioningServiceTest {

    @Test
    void provisionUser_shouldReturnNull_whenProvisioningDisabled() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(false);

        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

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

        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(401);

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenAdminTokenMissing() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(200);
            when(tokenResponse.body()).thenReturn("{\"token_type\":\"Bearer\"}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);
        HttpRequest createRequest = mock(HttpRequest.class);
        HttpResponse createResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/users")).thenReturn(createRequest);

            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(200);
            when(tokenResponse.body()).thenReturn("{\"access_token\":\"admin-token\"}");

            when(createRequest.header("Authorization", "Bearer admin-token")).thenReturn(createRequest);
            when(createRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(createRequest);
            when(createRequest.timeout(10000)).thenReturn(createRequest);
            when(createRequest.execute()).thenReturn(createResponse);
            when(createResponse.getStatus()).thenReturn(500);
            when(createResponse.body()).thenReturn("{\"message\":\"failed\"}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenCreateUserReturnsBusinessError() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);
        HttpRequest createRequest = mock(HttpRequest.class);
        HttpResponse createResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/users")).thenReturn(createRequest);

            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(200);
            when(tokenResponse.body()).thenReturn("{\"access_token\":\"admin-token\"}");

            when(createRequest.header("Authorization", "Bearer admin-token")).thenReturn(createRequest);
            when(createRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(createRequest);
            when(createRequest.timeout(10000)).thenReturn(createRequest);
            when(createRequest.execute()).thenReturn(createResponse);
            when(createResponse.getStatus()).thenReturn(200);
            when(createResponse.body()).thenReturn("{\"error\":\"invalid_token\",\"message\":\"Invalid token\"}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldThrowSubjectMissing_whenSubjectFieldAbsent() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);
        HttpRequest createRequest = mock(HttpRequest.class);
        HttpResponse createResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/users")).thenReturn(createRequest);

            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(200);
            when(tokenResponse.body()).thenReturn("{\"access_token\":\"admin-token\"}");

            when(createRequest.header("Authorization", "Bearer admin-token")).thenReturn(createRequest);
            when(createRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(createRequest);
            when(createRequest.timeout(10000)).thenReturn(createRequest);
            when(createRequest.execute()).thenReturn(createResponse);
            when(createResponse.getStatus()).thenReturn(200);
            when(createResponse.body()).thenReturn("{\"data\":{\"user\":{\"username\":\"u-1\"}}}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldReturnSubject_whenProvisioningSucceededWithDataNode() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest tokenRequest = mock(HttpRequest.class);
        HttpResponse tokenResponse = mock(HttpResponse.class);
        HttpRequest createRequest = mock(HttpRequest.class);
        HttpResponse createResponse = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/token")).thenReturn(tokenRequest);
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/v1/admin/users")).thenReturn(createRequest);

            when(tokenRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(tokenRequest);
            when(tokenRequest.timeout(10000)).thenReturn(tokenRequest);
            when(tokenRequest.execute()).thenReturn(tokenResponse);
            when(tokenResponse.getStatus()).thenReturn(200);
            when(tokenResponse.body()).thenReturn("{\"access_token\":\"admin-token\"}");

            when(createRequest.header("Authorization", "Bearer admin-token")).thenReturn(createRequest);
            when(createRequest.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(createRequest);
            when(createRequest.timeout(10000)).thenReturn(createRequest);
            when(createRequest.execute()).thenReturn(createResponse);
            when(createResponse.getStatus()).thenReturn(201);
            when(createResponse.body()).thenReturn("{\"data\":{\"id\":\"sub-2002\"}}");

            String subject = service.provisionUser(buildCommand());

            assertEquals("sub-2002", subject);
        }
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
}
