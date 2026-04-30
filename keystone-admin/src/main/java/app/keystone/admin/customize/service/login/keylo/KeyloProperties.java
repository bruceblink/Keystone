package app.keystone.admin.customize.service.login.keylo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keylo 鉴权配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "keystone.auth.keylo")
public class KeyloProperties {

    private boolean enabled = true;

    private String issuerUri;

    private String jwkSetUri;

    private String subjectClaim = "uid";

    private Integer clockSkewSeconds = 60;

    private String audience;

    private String credentialVerifyUrl;

    private String credentialMeUrl;

    private String credentialAuthHeaderName = "Authorization";

    private String credentialAuthHeaderValue;

    private String credentialUsernameField = "username";

    private String credentialPasswordField = "password";
}
