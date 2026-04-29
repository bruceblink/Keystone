package app.keystone.admin.customize.service.login.keylo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyloCredentialVerifier {

    private final KeyloProperties keyloProperties;

    public KeyloPrincipal verify(String username, String password) {
        if (StrUtil.isBlank(keyloProperties.getCredentialVerifyUrl())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        String meUrl = resolveCredentialMeUrl();
        if (StrUtil.isBlank(meUrl)) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(keyloProperties.getCredentialUsernameField(), username);
        body.put(keyloProperties.getCredentialPasswordField(), password);

        try {
            HttpRequest tokenRequest = HttpRequest.post(keyloProperties.getCredentialVerifyUrl())
                .body(JacksonUtil.to(body), ContentType.JSON.getValue())
                .timeout(10000);
            if (StrUtil.isNotBlank(keyloProperties.getCredentialAuthHeaderValue())) {
                tokenRequest.header(keyloProperties.getCredentialAuthHeaderName(), keyloProperties.getCredentialAuthHeaderValue());
            }
            HttpResponse tokenResponse = tokenRequest.execute();
            if (tokenResponse.getStatus() < 200 || tokenResponse.getStatus() >= 300) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "HTTP " + tokenResponse.getStatus());
            }

            String tokenResponseBody = tokenResponse.body();
            String accessToken = JacksonUtil.getAsString(tokenResponseBody, "access_token");
            if (StrUtil.isBlank(accessToken)) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "access_token missing");
            }

            HttpResponse meResponse = HttpRequest.get(meUrl)
                .header("Authorization", "Bearer " + accessToken)
                .timeout(10000)
                .execute();
            if (meResponse.getStatus() < 200 || meResponse.getStatus() >= 300) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "HTTP " + meResponse.getStatus());
            }

            String meResponseBody = meResponse.body();
            String subject = JacksonUtil.getAsString(meResponseBody, keyloProperties.getSubjectClaim());
            if (StrUtil.isBlank(subject)) {
                log.error("Keylo credential verify succeeded but subject missing, response={}", meResponseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            return new KeyloPrincipal(subject);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_ERROR, e.getMessage());
        }
    }

    private String resolveCredentialMeUrl() {
        if (StrUtil.isNotBlank(keyloProperties.getCredentialMeUrl())) {
            return keyloProperties.getCredentialMeUrl();
        }
        if (StrUtil.endWithIgnoreCase(keyloProperties.getCredentialVerifyUrl(), "/token")) {
            return StrUtil.removeSuffixIgnoreCase(keyloProperties.getCredentialVerifyUrl(), "/token") + "/me";
        }
        return null;
    }
}
