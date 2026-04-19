package app.keystone.infrastructure.security;

import cn.hutool.crypto.SecureUtil;
import java.nio.charset.StandardCharsets;

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

        String encrypted = SecureUtil.aes(buildAesKey(encryptKey)).encryptBase64(plainPassword);
        System.out.println("ENC(" + encrypted + ")");
    }

    private static byte[] buildAesKey(String encryptKey) {
        byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[16];
        int copyLength = Math.min(keyBytes.length, aesKey.length);
        System.arraycopy(keyBytes, 0, aesKey, 0, copyLength);
        return aesKey;
    }
}
