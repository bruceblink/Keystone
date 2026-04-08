package com.agileboot.admin.config;


import com.agileboot.common.config.AgileBootConfig;
import com.agileboot.common.constant.Constants.UploadSubDir;
import java.io.File;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AgileBootConfigTest.TestConfig.class)
@TestPropertySource(properties = {
    "agileboot.name=AgileBoot",
    "agileboot.version=1.8.0",
    "agileboot.copyrightYear=2022",
    "agileboot.demoEnabled=false",
    "agileboot.fileBaseDir=D:/agileboot",
    "agileboot.addressEnabled=false",
    "agileboot.captchaType=math"
})
public class AgileBootConfigTest {

    @Resource
    private AgileBootConfig config;

    @Test
    public void testConfig() {
        String fileBaseDir = "D:/agileboot/profile";
        String actualFileBaseDir = AgileBootConfig.getFileBaseDir();

        Assertions.assertEquals("AgileBoot", config.getName());
        Assertions.assertEquals("1.8.0", config.getVersion());
        Assertions.assertEquals("2022", config.getCopyrightYear());
        Assertions.assertFalse(config.isDemoEnabled());
        Assertions.assertEquals(normalizePath(fileBaseDir), normalizePath(actualFileBaseDir));
        Assertions.assertFalse(AgileBootConfig.isAddressEnabled());
        Assertions.assertEquals("math", AgileBootConfig.getCaptchaType());
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
    @EnableConfigurationProperties(AgileBootConfig.class)
    static class TestConfig {
    }

}
