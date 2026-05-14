package app.keystone.infrastructure.config.datasource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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

    private static final String PROPERTY_SOURCE_NAME = "KeystoneDataSourcePasswordDecrypt";
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final String SECRET_V1_PREFIX = "secret:v1:aes-256-gcm";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;

    private static final Pattern DATA_SOURCE_PASSWORD_KEY = Pattern.compile(
        "^spring\\.datasource(\\.dynamic\\.datasource\\.[^.]+)?\\.password$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty(
            "keystone.datasource.password-encryption.enabled",
            Boolean.class,
            false
        );
        if (!enabled) {
            return;
        }

        String encryptKey = environment.getProperty("keystone.datasource.password-encryption.encrypt-key");
        if (encryptKey == null || encryptKey.trim().isEmpty()) {
            encryptKey = environment.getProperty("KEYSTONE_DATASOURCE_ENCRYPT_KEY");
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
                if (encryptKey == null || encryptKey.trim().isEmpty()) {
                    throw new IllegalStateException(
                        "Database password is encrypted but encrypt key is missing: keystone.datasource.password-encryption.encrypt-key"
                    );
                }

                try {
                    String decrypted = decryptValue(value, encryptKey);
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
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith(SECRET_V1_PREFIX) || (trimmed.startsWith(ENC_PREFIX) && trimmed.endsWith(ENC_SUFFIX));
    }

    private static String decryptValue(String value, String encryptKey) throws Exception {
        String trimmed = value.trim();
        if (trimmed.startsWith(SECRET_V1_PREFIX)) {
            return decryptSecretV1(encryptKey, trimmed);
        }

        String encryptedBody = trimmed.substring(ENC_PREFIX.length(), trimmed.length() - ENC_SUFFIX.length());
        return decryptAesBase64(buildLegacyAesKey(encryptKey), encryptedBody);
    }

    private static String decryptSecretV1(String encryptKey, String encryptedValue) throws Exception {
        String[] parts = encryptedValue.split(":");
        if (parts.length != 5
            || !"secret".equals(parts[0])
            || !"v1".equals(parts[1])
            || !"aes-256-gcm".equals(parts[2])) {
            throw new IllegalArgumentException(
                "Database password must use format secret:v1:aes-256-gcm:<nonce_base64>:<ciphertext_base64>");
        }

        byte[] nonce = Base64.getDecoder().decode(parts[3]);
        if (nonce.length != GCM_NONCE_BYTES) {
            throw new IllegalArgumentException("AES-GCM nonce must be 12 bytes");
        }

        byte[] ciphertextAndTag = Base64.getDecoder().decode(parts[4]);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decodeSecretKey(encryptKey), "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] decrypted = cipher.doFinal(ciphertextAndTag);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String decryptAesBase64(byte[] key, String encryptedBase64) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] decodeSecretKey(String encryptKey) {
        String trimmed = encryptKey.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to raw 32-byte key support.
        }

        byte[] raw = trimmed.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }

        throw new IllegalArgumentException("encrypt key must be 32 bytes or standard base64 for 32 bytes");
    }

    private static byte[] buildLegacyAesKey(String encryptKey) {
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
