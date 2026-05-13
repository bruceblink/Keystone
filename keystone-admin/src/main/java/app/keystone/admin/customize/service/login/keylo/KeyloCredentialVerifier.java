package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyloCredentialVerifier {

    private final KeyloProperties keyloProperties;

    public KeyloTokenIdentity verify(String username, String password) {
        if (StringUtils.isBlank(keyloProperties.getCredentialVerifyUrl())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        String meUrl = resolveCredentialMeUrl();
        if (StringUtils.isBlank(meUrl)) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(keyloProperties.getCredentialUsernameField(), username);
        body.put(keyloProperties.getCredentialPasswordField(), password);

        try {
            Map<String, String> tokenHeaders = new HashMap<>();
            if (StringUtils.isNotBlank(keyloProperties.getCredentialAuthHeaderValue())) {
                tokenHeaders.put(keyloProperties.getCredentialAuthHeaderName(), keyloProperties.getCredentialAuthHeaderValue());
            }
            HttpResponse<String> tokenResponse = sendPostJson(
                keyloProperties.getCredentialVerifyUrl(),
                JacksonUtil.to(body),
                tokenHeaders
            );
            if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "HTTP " + tokenResponse.statusCode());
            }

            String tokenResponseBody = tokenResponse.body();
            String accessToken = JacksonUtil.getAsString(tokenResponseBody, "access_token");
            if (StringUtils.isBlank(accessToken)) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "access_token missing");
            }

            HttpResponse<String> meResponse = sendGet(
                meUrl,
                Map.of("Authorization", "Bearer " + accessToken)
            );
            if (meResponse.statusCode() < 200 || meResponse.statusCode() >= 300) {
                throw new ApiException(ErrorCode.Business.LOGIN_ERROR, "HTTP " + meResponse.statusCode());
            }

            String meResponseBody = meResponse.body();
            String subject = JacksonUtil.getAsString(meResponseBody, keyloProperties.getSubjectClaim());
            if (StringUtils.isBlank(subject)) {
                log.error("Keylo credential verify succeeded but subject missing, response={}", meResponseBody);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            String userId = JacksonUtil.getAsString(meResponseBody, keyloProperties.getUserIdClaim());
            JsonNode expiresInNode = JacksonUtil.getAsJsonObject(tokenResponseBody, "expires_in");
            Long expiresIn = expiresInNode == null || expiresInNode.isNull() ? null : JacksonUtil.getAsLong(tokenResponseBody, "expires_in");
            return new KeyloTokenIdentity(
                subject,
                userId,
                accessToken,
                null,
                expiresIn,
                JacksonUtil.getAsString(tokenResponseBody, "token_type")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_ERROR, e.getMessage());
        }
    }

    private HttpResponse<String> sendPostJson(String url, String jsonBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(10000))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendGet(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(10000))
            .GET();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String resolveCredentialMeUrl() {
        if (StringUtils.isNotBlank(keyloProperties.getCredentialMeUrl())) {
            return keyloProperties.getCredentialMeUrl();
        }
        if (StringUtils.endsWithIgnoreCase(keyloProperties.getCredentialVerifyUrl(), "/token")) {
            return StringUtils.removeEndIgnoreCase(keyloProperties.getCredentialVerifyUrl(), "/token") + "/me";
        }
        return null;
    }
}
