package app.keystone.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keystone application.yaml中的prefix = "keystone.frontend"配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "keystone.frontend")
public class KeystoneFrontendConfig {

    /**
     * 前端访问地址
     */
    private String url;
}

