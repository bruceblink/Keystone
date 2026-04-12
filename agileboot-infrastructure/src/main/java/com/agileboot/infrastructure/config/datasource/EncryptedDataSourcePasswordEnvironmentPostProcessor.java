package com.agileboot.infrastructure.config.datasource;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * 支持数据库密码配置使用 ENC(...) 的启动期自动解密。
 */
public class EncryptedDataSourcePasswordEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "agilebootDataSourcePasswordDecrypt";
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    private static final Pattern DATA_SOURCE_PASSWORD_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty(
            "agileboot.datasource.password-encryption.enabled",
            Boolean.class,
            false
        );
        if (!enabled) {
            return;
        }

        String encryptKey = environment.getProperty("agileboot.datasource.password-encryption.encrypt-key");
        if (StrUtil.isBlank(encryptKey)) {
            encryptKey = environment.getProperty("AGILEBOOT_DATASOURCE_ENCRYPT_KEY");
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> decryptedMap = new LinkedHashMap<>();
        boolean foundEncryptedPassword = false;

        for (PropertySource<?> source : propertySources) {
            if (!(source instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
                continue;
            }
            for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                if (!DATA_SOURCE_PASSWORD_KEY.matcher(propertyName).matches()) {
                    continue;
                }

                String value = environment.getProperty(propertyName);
                if (!isEncryptedValue(value)) {
                    continue;
                }

                foundEncryptedPassword = true;
                if (StrUtil.isBlank(encryptKey)) {
                    throw new IllegalStateException(
                        "Database password is encrypted but encrypt key is missing: agileboot.datasource.password-encryption.encrypt-key"
                    );
                }

                String encryptedBody = value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length());
                try {
                    String decrypted = SecureUtil.aes(
                        buildAesKey(encryptKey)
                    ).decryptStr(encryptedBody);
                    decryptedMap.put(propertyName, decrypted);
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to decrypt datasource password for property: " + propertyName, ex);
                }
            }
        }

        if (foundEncryptedPassword && !decryptedMap.isEmpty()) {
            propertySources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, decryptedMap));
        }
    }

    private static boolean isEncryptedValue(String value) {
        return StrUtil.isNotBlank(value) && value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    private static byte[] buildAesKey(String encryptKey) {
        byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[16];
        int copyLength = Math.min(keyBytes.length, aesKey.length);
        System.arraycopy(keyBytes, 0, aesKey, 0, copyLength);
        return aesKey;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
