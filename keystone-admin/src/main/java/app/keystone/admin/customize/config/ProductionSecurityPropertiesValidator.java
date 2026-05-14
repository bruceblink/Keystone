package app.keystone.admin.customize.config;

import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast when production starts with unsafe placeholder security settings.
 */
@Component
@RequiredArgsConstructor
public class ProductionSecurityPropertiesValidator implements ApplicationRunner {

    private static final String DEFAULT_TOKEN_SECRET = "sdhfkjshBN6rr32df38";
    private static final String DOCKER_PLACEHOLDER_TOKEN_SECRET = "change-me-please-update-token-secret-32bytes";
    private static final String DEFAULT_KEYLO_ADMIN_CLIENT_SECRET = "replace-with-strong-admin-secret";

    private final Environment environment;

    private final KeyloProperties keyloProperties;

    private final KeyloUserProvisioningProperties keyloProvisioningProperties;

    @Value("${token.secret:}")
    private String tokenSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateTokenSecret(errors);
        validateDruidPassword(errors);
        validateKeylo(errors);
        validateKeyloProvisioning(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production security configuration: " + String.join("; ", errors));
        }
    }

    private boolean isProdProfile() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    private void validateTokenSecret(List<String> errors) {
        if (!StringUtils.hasText(tokenSecret)) {
            errors.add("TOKEN_SECRET must be configured");
            return;
        }
        if (DEFAULT_TOKEN_SECRET.equals(tokenSecret) || DOCKER_PLACEHOLDER_TOKEN_SECRET.equals(tokenSecret)) {
            errors.add("TOKEN_SECRET must not use the default development or docker placeholder secret");
        }
        if (tokenSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            errors.add("TOKEN_SECRET must be at least 32 bytes");
        }
    }

    private void validateDruidPassword(List<String> errors) {
        if (!isDruidEnabled()) {
            return;
        }
        String druidPassword = firstText(
            environment.getProperty("spring.datasource.druid.statViewServlet.login-password"),
            environment.getProperty("spring.datasource.dynamic.druid.stat-view-servlet.login-password"),
            environment.getProperty("spring.datasource.druid.stat-view-servlet.login-password")
        );
        if (!StringUtils.hasText(druidPassword)) {
            errors.add("DRUID_PASSWORD must be configured when Druid is enabled");
        }
    }

    private boolean isDruidEnabled() {
        return environment.getProperty("spring.datasource.druid.statViewServlet.enabled", Boolean.class,
            environment.getProperty("spring.datasource.dynamic.druid.stat-view-servlet.enabled", Boolean.class,
                environment.getProperty("spring.datasource.druid.stat-view-servlet.enabled", Boolean.class, true)));
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void validateKeylo(List<String> errors) {
        if (!keyloProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(keyloProperties.getIssuerUri())) {
            errors.add("KEYLO_ISSUER_URI must be configured when Keylo auth is enabled");
        }
        if (!StringUtils.hasText(keyloProperties.getJwkSetUri())) {
            errors.add("KEYLO_JWK_SET_URI must be configured when Keylo auth is enabled");
        }
        if (trustedAudiences().isEmpty()) {
            errors.add("KEYLO_AUDIENCES must contain at least one trusted audience when Keylo auth is enabled");
        }
        if (!StringUtils.hasText(keyloProperties.getCredentialVerifyUrl())) {
            errors.add("KEYLO_CREDENTIAL_VERIFY_URL must be configured when Keylo auth is enabled");
        }
    }

    private List<String> trustedAudiences() {
        List<String> trustedAudiences = new ArrayList<>();
        if (keyloProperties.getAudiences() != null) {
            trustedAudiences.addAll(keyloProperties.getAudiences().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
        }
        if (trustedAudiences.isEmpty() && StringUtils.hasText(keyloProperties.getAudience())) {
            trustedAudiences.add(keyloProperties.getAudience().trim());
        }
        return trustedAudiences;
    }

    private void validateKeyloProvisioning(List<String> errors) {
        if (!keyloProvisioningProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(keyloProvisioningProperties.getAdminClientId())) {
            errors.add("KEYLO_ADMIN_CLIENT_ID must be configured when Keylo provisioning is enabled");
        }
        String adminClientSecret = keyloProvisioningProperties.getAdminClientSecret();
        if (!StringUtils.hasText(adminClientSecret)) {
            errors.add("KEYLO_ADMIN_CLIENT_SECRET must be configured when Keylo provisioning is enabled");
        } else if (DEFAULT_KEYLO_ADMIN_CLIENT_SECRET.equals(adminClientSecret)) {
            errors.add("KEYLO_ADMIN_CLIENT_SECRET must not use the default placeholder");
        }
    }
}
