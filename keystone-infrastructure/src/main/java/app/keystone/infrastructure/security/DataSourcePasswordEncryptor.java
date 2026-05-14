package app.keystone.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Database password encryption utility.
 *
 * <p>Usage: java DataSourcePasswordEncryptor &lt;encryptKey&gt; &lt;plainPassword&gt;</p>
 */
public class DataSourcePasswordEncryptor {

    private static final String SECRET_V1_PREFIX = "secret:v1:aes-256-gcm";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DataSourcePasswordEncryptor() {
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: DataSourcePasswordEncryptor <encryptKey> <plainPassword>");
            return;
        }

        System.out.println(encryptSecretV1(args[0], args[1]));
    }

    private static String encryptSecretV1(String encryptKey, String plainText) {
        try {
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(decodeSecretKey(encryptKey), "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return SECRET_V1_PREFIX + ":"
                + Base64.getEncoder().encodeToString(nonce) + ":"
                + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt datasource password failed", e);
        }
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
}
