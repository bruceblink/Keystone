package app.keystone.domain.system.user.keylo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keystone.auth.keylo.provisioning")
public class KeyloUserProvisioningProperties {

    private boolean enabled = false;

    private String createUserUrl;

    private String authHeaderName = "Authorization";

    private String authHeaderValue;

    private String subjectField = "id";

    private String usernameField = "username";

    private String nicknameField = "nickname";

    private String emailField = "email";

    private String phoneField = "phoneNumber";

    private String passwordField = "password";

    private int timeoutMillis = 10000;
}
