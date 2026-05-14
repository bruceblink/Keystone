package app.keystone.infrastructure.config.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class EncryptedDataSourcePasswordEnvironmentPostProcessorTest {

    @Test
    void decryptsSecretV1DataSourcePassword() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "keystone.datasource.password-encryption.enabled", "true",
            "keystone.datasource.password-encryption.encrypt-key", "oN06GRBVOr2G8lFxKisSmnONozK0Ru8z9Og2q7Bsbww=",
            "spring.datasource.dynamic.datasource.master.password",
            "secret:v1:aes-256-gcm:Mq+2ogeNoFKYIQYe:ZXSfffeenjQZTYXIRWJpH2xaBZgiRBAiuv4qVpzIZMrZy/B/rw=="
        )));

        new EncryptedDataSourcePasswordEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("python-rust-db-secret",
            environment.getProperty("spring.datasource.dynamic.datasource.master.password"));
    }
}
