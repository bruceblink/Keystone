package app.keystone.admin.config;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.constant.Constants.UploadSubDir;
import cn.hutool.extra.spring.SpringUtil;
import java.io.File;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KeystoneConfigTest.TestConfig.class)
@TestPropertySource(properties = {
    "keystone.name=Keystone",
    "keystone.version=1.8.0",
    "keystone.copyrightYear=2022",
    "keystone.demoEnabled=false",
    "keystone.fileBaseDir=D:/keystone",
    "keystone.addressEnabled=false",
    "keystone.captchaType=math"
})
public class KeystoneConfigTest {

    @Resource
    private KeystoneConfig config;

    @Test
    public void testConfig() {
        String fileBaseDir = "D:/keystone/profile";
        String actualFileBaseDir;

        try (MockedStatic<SpringUtil> springUtilMockedStatic = Mockito.mockStatic(SpringUtil.class)) {
            springUtilMockedStatic.when(() -> SpringUtil.getBean(KeystoneConfig.class)).thenReturn(config);
            actualFileBaseDir = KeystoneConfig.getFileBaseDir();

            Assertions.assertFalse(KeystoneConfig.isDemoEnabled());
            Assertions.assertFalse(KeystoneConfig.isAddressEnabled());
            Assertions.assertEquals("math", KeystoneConfig.getCaptchaType());
        }

        Assertions.assertEquals("Keystone", config.getName());
        Assertions.assertEquals("1.8.0", config.getVersion());
        Assertions.assertEquals("2022", config.getCopyrightYear());
        Assertions.assertEquals(normalizePath(fileBaseDir), normalizePath(actualFileBaseDir));
        Assertions.assertEquals(normalizePath(fileBaseDir + "/import"),
            normalizePath(actualFileBaseDir + File.separator + UploadSubDir.IMPORT_PATH));
        Assertions.assertEquals(normalizePath(fileBaseDir + "/avatar"),
            normalizePath(actualFileBaseDir + File.separator + UploadSubDir.AVATAR_PATH));
        Assertions.assertEquals(normalizePath(fileBaseDir + "/download"),
            normalizePath(actualFileBaseDir + File.separator + UploadSubDir.DOWNLOAD_PATH));
        Assertions.assertEquals(normalizePath(fileBaseDir + "/upload"),
            normalizePath(actualFileBaseDir + File.separator + UploadSubDir.UPLOAD_PATH));
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    @Configuration
    @EnableConfigurationProperties(KeystoneConfig.class)
    static class TestConfig {
    }
}
