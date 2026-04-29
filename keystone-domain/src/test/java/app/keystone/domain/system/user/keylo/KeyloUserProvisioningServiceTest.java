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
        properties.setCreateUserUrl("http://keylo.local/api/users");
        properties.setAuthHeaderValue("");

        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void provisionUser_shouldThrowProvisionFailed_whenHttpStatusNot2xx() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        properties.setTimeoutMillis(12000);
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/api/users")).thenReturn(request);
            when(request.header("Authorization", "Bearer keylo-admin-token")).thenReturn(request);
            when(request.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(request);
            when(request.timeout(12000)).thenReturn(request);
            when(request.execute()).thenReturn(response);
            when(response.getStatus()).thenReturn(500);
            when(response.body()).thenReturn("{\"message\":\"failed\"}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldThrowSubjectMissing_whenSubjectFieldAbsent() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/api/users")).thenReturn(request);
            when(request.header("Authorization", "Bearer keylo-admin-token")).thenReturn(request);
            when(request.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(request);
            when(request.timeout(10000)).thenReturn(request);
            when(request.execute()).thenReturn(response);
            when(response.getStatus()).thenReturn(200);
            when(response.body()).thenReturn("{\"data\":{\"user\":{\"username\":\"u-1\"}}}");

            ApiException exception = assertThrows(ApiException.class, () -> service.provisionUser(buildCommand()));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
        }
    }

    @Test
    void provisionUser_shouldReturnSubject_whenProvisioningSucceededWithUserNode() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/api/users")).thenReturn(request);
            when(request.header("Authorization", "Bearer keylo-admin-token")).thenReturn(request);
            when(request.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(request);
            when(request.timeout(10000)).thenReturn(request);
            when(request.execute()).thenReturn(response);
            when(response.getStatus()).thenReturn(201);
            when(response.body()).thenReturn("{\"data\":{\"user\":{\"id\":\"sub-1001\"}}}");

            String subject = service.provisionUser(buildCommand());

            assertEquals("sub-1001", subject);
        }
    }

    @Test
    void provisionUser_shouldReturnSubject_whenProvisioningSucceededWithDataNode() {
        KeyloUserProvisioningProperties properties = buildDefaultProperties();
        KeyloUserProvisioningService service = new KeyloUserProvisioningService(properties);

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);

        try (MockedStatic<HttpRequest> httpRequestMocked = mockStatic(HttpRequest.class)) {
            httpRequestMocked.when(() -> HttpRequest.post("http://keylo.local/api/users")).thenReturn(request);
            when(request.header("Authorization", "Bearer keylo-admin-token")).thenReturn(request);
            when(request.body(anyString(), eq(ContentType.JSON.getValue()))).thenReturn(request);
            when(request.timeout(10000)).thenReturn(request);
            when(request.execute()).thenReturn(response);
            when(response.getStatus()).thenReturn(201);
            when(response.body()).thenReturn("{\"data\":{\"id\":\"sub-2002\"}}");

            String subject = service.provisionUser(buildCommand());

            assertEquals("sub-2002", subject);
        }
    }

    private KeyloUserProvisioningProperties buildDefaultProperties() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setEnabled(true);
        properties.setCreateUserUrl("http://keylo.local/api/users");
        properties.setAuthHeaderName("Authorization");
        properties.setAuthHeaderValue("Bearer keylo-admin-token");
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
