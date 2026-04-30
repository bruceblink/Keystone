package app.keystone.domain.system.user.keylo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.user.command.AddUserCommand;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyloUserProvisioningService {

    private final KeyloUserProvisioningProperties properties;

    public String provisionUser(AddUserCommand command) {
        if (!properties.isEnabled()) {
            return null;
        }

        if (StrUtil.isBlank(properties.getCreateUserUrl())
            || StrUtil.isBlank(properties.getAdminTokenUrl())
            || StrUtil.isBlank(properties.getAdminClientId())
            || StrUtil.isBlank(properties.getAdminClientSecret())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(properties.getUsernameField(), command.getUsername());
        body.put(properties.getNicknameField(), command.getNickname());
        body.put(properties.getEmailField(), command.getEmail());
        body.put(properties.getPhoneField(), command.getPhoneNumber());
        body.put(properties.getPasswordField(), command.getPassword());

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse response = HttpRequest.post(properties.getCreateUserUrl())
                .header(properties.getAuthHeaderName(), "Bearer " + adminAccessToken)
                .body(JacksonUtil.to(body), ContentType.JSON.getValue())
                .timeout(properties.getTimeoutMillis())
                .execute();

            int statusCode = response.getStatus();
            String responseBody = response.body();
            if (statusCode < 200 || statusCode >= 300) {
                log.error("Keylo user provisioning failed, status={}, response={}", statusCode, responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "HTTP " + statusCode);
            }

            String error = JacksonUtil.getAsString(responseBody, "error");
            if (StrUtil.isNotBlank(error)) {
                String message = JacksonUtil.getAsString(responseBody, "message");
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, StrUtil.blankToDefault(message, error));
            }

            String subject = extractSubject(responseBody);
            if (StrUtil.isBlank(subject)) {
                log.error("Keylo user provisioning succeeded but subject missing, response={}", responseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            return subject;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, e.getMessage());
        }
    }

    private String getAdminAccessToken() {
        Map<String, Object> tokenBody = new HashMap<>();
        tokenBody.put("client_id", properties.getAdminClientId());
        tokenBody.put("client_secret", properties.getAdminClientSecret());

        HttpResponse tokenResponse = HttpRequest.post(properties.getAdminTokenUrl())
            .body(JacksonUtil.to(tokenBody), ContentType.JSON.getValue())
            .timeout(properties.getTimeoutMillis())
            .execute();

        if (tokenResponse.getStatus() < 200 || tokenResponse.getStatus() >= 300) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "admin token HTTP " + tokenResponse.getStatus());
        }

        String tokenResponseBody = tokenResponse.body();
        String adminAccessToken = JacksonUtil.getAsString(tokenResponseBody, "access_token");
        if (StrUtil.isBlank(adminAccessToken)) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "admin access_token missing");
        }
        return adminAccessToken;
    }

    private String extractSubject(String responseBody) {
        String dataNode = JacksonUtil.getAsString(responseBody, "data");
        String userNode = JacksonUtil.getAsString(dataNode, "user");

        String subject = JacksonUtil.getAsString(userNode, properties.getSubjectField());
        if (StrUtil.isNotBlank(subject)) {
            return subject;
        }

        subject = JacksonUtil.getAsString(dataNode, properties.getSubjectField());
        if (StrUtil.isNotBlank(subject)) {
            return subject;
        }

        return JacksonUtil.getAsString(responseBody, properties.getSubjectField());
    }
}
