package app.keystone.admin.customize.config;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityPropertiesValidator implements ApplicationRunner {

    private static final String DEFAULT_TOKEN_SECRET = "sdhfkjshBN6rr32df38";

    private static final String DEFAULT_RSA_PRIVATE_KEY_PREFIX = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8";

    private static final String DEFAULT_KEYLO_ADMIN_SECRET = "replace-with-strong-admin-secret";

    private static final List<String> WEAK_TOKEN_SECRETS = List.of(
        DEFAULT_TOKEN_SECRET,
        "changeme-please-update-in-production"
    );

    private final Environment environment;

    public ProductionSecurityPropertiesValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        validateTokenSecret();
        validateRsaPrivateKey();
        validateKeyloProvisioningSecret();
    }

    private boolean isProdProfileActive() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private void validateTokenSecret() {
        String tokenSecret = environment.getProperty("token.secret");
        if (StringUtils.isBlank(tokenSecret) || WEAK_TOKEN_SECRETS.contains(tokenSecret)) {
            throw new IllegalStateException("Unsafe production configuration: token.secret must be set to a strong secret.");
        }
    }

    private void validateRsaPrivateKey() {
        String rsaPrivateKey = environment.getProperty("keystone.rsaPrivateKey");
        if (StringUtils.isBlank(rsaPrivateKey) || StringUtils.startsWith(rsaPrivateKey, DEFAULT_RSA_PRIVATE_KEY_PREFIX)) {
            throw new IllegalStateException("Unsafe production configuration: keystone.rsaPrivateKey must not use the bundled sample key.");
        }
    }

    private void validateKeyloProvisioningSecret() {
        boolean provisioningEnabled = environment.getProperty(
            "keystone.auth.keylo.provisioning.enabled", Boolean.class, false);
        if (!provisioningEnabled) {
            return;
        }

        String adminSecret = environment.getProperty("keystone.auth.keylo.provisioning.admin-client-secret");
        if (StringUtils.isBlank(adminSecret) || DEFAULT_KEYLO_ADMIN_SECRET.equals(adminSecret)) {
            throw new IllegalStateException(
                "Unsafe production configuration: Keylo provisioning admin-client-secret must be set to a strong secret.");
        }
    }
}
