package app.keystone.domain.system.user.keylo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
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

        if (StrUtil.isBlank(properties.getCreateUserUrl()) || StrUtil.isBlank(properties.getAuthHeaderValue())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        Map<String, Object> body = new HashMap<>();
        body.put(properties.getUsernameField(), command.getUsername());
        body.put(properties.getNicknameField(), command.getNickname());
        body.put(properties.getEmailField(), command.getEmail());
        body.put(properties.getPhoneField(), command.getPhoneNumber());

        try {
            String response = HttpRequest.post(properties.getCreateUserUrl())
                .header(properties.getAuthHeaderName(), properties.getAuthHeaderValue())
                .body(JacksonUtil.to(body), ContentType.JSON.getValue())
                .timeout(10000)
                .execute()
                .body();

            String subject = JacksonUtil.getAsString(response, properties.getSubjectField());
            if (StrUtil.isBlank(subject)) {
                log.error("Keylo user provisioning succeeded but subject missing, response={}", response);
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            return subject;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, e.getMessage());
        }
    }
}
