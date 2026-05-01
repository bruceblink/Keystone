package app.keystone.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 数据库密码加密工具。
 * 用法: java DataSourcePasswordEncryptor <encryptKey> <plainPassword>
 */
public class DataSourcePasswordEncryptor {

    private DataSourcePasswordEncryptor() {
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: DataSourcePasswordEncryptor <encryptKey> <plainPassword>");
            return;
        }

        String encryptKey = args[0];
        String plainPassword = args[1];

        String encrypted = encryptAesBase64(buildAesKey(encryptKey), plainPassword);
        System.out.println("ENC(" + encrypted + ")");
    }

    private static String encryptAesBase64(byte[] key, String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt datasource password failed", e);
        }
    }

    private static byte[] buildAesKey(String encryptKey) {
        byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[16];
        int copyLength = Math.min(keyBytes.length, aesKey.length);
        System.arraycopy(keyBytes, 0, aesKey, 0, copyLength);
        return aesKey;
    }
}
