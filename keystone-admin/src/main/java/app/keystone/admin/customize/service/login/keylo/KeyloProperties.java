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

    private String subjectClaim = "sub";

    private Integer clockSkewSeconds = 60;

    private String audience;
}
