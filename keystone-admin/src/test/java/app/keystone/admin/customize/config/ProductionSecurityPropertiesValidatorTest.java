package app.keystone.admin.customize.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningProperties;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityPropertiesValidatorTest {

    @Test
    void run_shouldFailFast_whenProdUsesUnsafeDefaults() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        KeyloProperties keyloProperties = new KeyloProperties();
        keyloProperties.setEnabled(false);
        KeyloUserProvisioningProperties provisioningProperties = new KeyloUserProvisioningProperties();
        provisioningProperties.setEnabled(false);
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(
            environment, keyloProperties, provisioningProperties);
        setField(validator, "tokenSecret", "sdhfkjshBN6rr32df38");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void run_shouldPass_whenProdSecurityPropertiesAreConfigured() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.dynamic.druid.stat-view-servlet.login-password", "strong-druid-password");
        KeyloProperties keyloProperties = new KeyloProperties();
        keyloProperties.setEnabled(true);
        keyloProperties.setIssuerUri("http://keylo");
        keyloProperties.setJwkSetUri("http://keylo/.well-known/jwks.json");
        keyloProperties.setAudiences(List.of("admin-backend"));
        keyloProperties.setCredentialVerifyUrl("http://keylo/v1/auth/token");
        KeyloUserProvisioningProperties provisioningProperties = new KeyloUserProvisioningProperties();
        provisioningProperties.setEnabled(false);
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(
            environment, keyloProperties, provisioningProperties);
        setField(validator, "tokenSecret", "0123456789abcdef0123456789abcdef");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void run_shouldSkipValidation_whenProfileIsNotProd() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(
            environment, new KeyloProperties(), new KeyloUserProvisioningProperties());
        setField(validator, "tokenSecret", "short");

        assertDoesNotThrow(() -> validator.run(null));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
