package app.keystone.admin.customize.service.login.keylo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
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
        if (!StrUtil.isNotBlank(keyloProperties.getCredentialVerifyUrl())
            || !StrUtil.isNotBlank(keyloProperties.getCredentialAuthHeaderValue())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(keyloProperties.getCredentialUsernameField(), username);
        body.put(keyloProperties.getCredentialPasswordField(), password);

        try {
            String response = HttpRequest.post(keyloProperties.getCredentialVerifyUrl())
                .header(keyloProperties.getCredentialAuthHeaderName(), keyloProperties.getCredentialAuthHeaderValue())
                .body(JacksonUtil.to(body), ContentType.JSON.getValue())
                .timeout(10000)
                .execute()
                .body();

            String subject = JacksonUtil.getAsString(response, keyloProperties.getSubjectClaim());
            if (!StrUtil.isNotBlank(subject)) {
                log.error("Keylo credential verify succeeded but subject missing, response={}", response);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            return new KeyloPrincipal(subject);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_ERROR, e.getMessage());
        }
    }
}
