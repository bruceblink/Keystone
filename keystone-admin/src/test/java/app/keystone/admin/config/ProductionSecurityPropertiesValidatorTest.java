package app.keystone.admin.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.keystone.admin.customize.config.ProductionSecurityPropertiesValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityPropertiesValidatorTest {

    @Test
    void shouldSkipValidationOutsideProdProfile() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("token.secret", "sdhfkjshBN6rr32df38");
        environment.setActiveProfiles("dev");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void shouldRejectWeakTokenSecretInProdProfile() {
        MockEnvironment environment = secureProdEnvironment()
            .withProperty("token.secret", "changeme-please-update-in-production");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void shouldRejectSampleRsaKeyInProdProfile() {
        MockEnvironment environment = secureProdEnvironment()
            .withProperty("keystone.rsaPrivateKey", "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8-sample");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void shouldRejectSampleKeyloAdminSecretWhenProvisioningEnabledInProdProfile() {
        MockEnvironment environment = secureProdEnvironment()
            .withProperty("keystone.auth.keylo.provisioning.enabled", "true")
            .withProperty("keystone.auth.keylo.provisioning.admin-client-secret", "replace-with-strong-admin-secret");

        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void shouldAllowStrongSecretsInProdProfile() {
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(secureProdEnvironment());

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    private MockEnvironment secureProdEnvironment() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("token.secret", "a-production-token-secret-with-enough-entropy")
            .withProperty("keystone.rsaPrivateKey", "production-rsa-private-key")
            .withProperty("keystone.auth.keylo.provisioning.enabled", "false");
        environment.setActiveProfiles("prod");
        return environment;
    }
}
