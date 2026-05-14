package app.keystone.admin.customize.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validator.run(null));

        assertTrue(exception.getMessage().contains("token.secret"));
        assertTrue(exception.getMessage().contains("TOKEN_SECRET"));
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

    @Test
    void run_shouldReportAllMissingKeyloProductionSettings() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("spring.datasource.dynamic.druid.stat-view-servlet.enabled", "false");
        KeyloProperties keyloProperties = new KeyloProperties();
        keyloProperties.setEnabled(true);
        keyloProperties.setIssuerUri("");
        keyloProperties.setJwkSetUri("");
        keyloProperties.setAudiences(List.of());
        keyloProperties.setAudience("");
        keyloProperties.setCredentialVerifyUrl("");
        KeyloUserProvisioningProperties provisioningProperties = new KeyloUserProvisioningProperties();
        provisioningProperties.setEnabled(true);
        provisioningProperties.setCreateUserUrl("");
        provisioningProperties.setAdminTokenUrl("");
        provisioningProperties.setAdminClientId("");
        provisioningProperties.setAdminClientSecret("");
        ProductionSecurityPropertiesValidator validator = new ProductionSecurityPropertiesValidator(
            environment, keyloProperties, provisioningProperties);
        setField(validator, "tokenSecret", "0123456789abcdef0123456789abcdef");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validator.run(null));

        String message = exception.getMessage();
        assertTrue(message.contains("KEYLO_ISSUER_URI"));
        assertTrue(message.contains("keystone.auth.keylo.issuer-uri"));
        assertTrue(message.contains("KEYLO_JWK_SET_URI"));
        assertTrue(message.contains("KEYLO_AUDIENCES"));
        assertTrue(message.contains("KEYLO_CREDENTIAL_VERIFY_URL"));
        assertTrue(message.contains("KEYLO_CREATE_USER_URL"));
        assertTrue(message.contains("KEYLO_ADMIN_TOKEN_URL"));
        assertTrue(message.contains("KEYLO_ADMIN_CLIENT_ID"));
        assertTrue(message.contains("KEYLO_ADMIN_CLIENT_SECRET"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
