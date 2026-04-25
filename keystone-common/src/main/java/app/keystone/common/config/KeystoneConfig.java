package app.keystone.common.config;

import cn.hutool.extra.spring.SpringUtil;
import app.keystone.common.constant.Constants;
import java.io.File;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 * @author valarchie
 */
@Component
@ConfigurationProperties(prefix = "keystone")
public class KeystoneConfig {

    /**
     * 项目名称
     */
    private String name;

    /**
     * 版本
     */
    private String version;

    /**
     * 版权年份
     */
    private String copyrightYear;

    /**
     * 实例演示开关
     */
    private boolean demoEnabled;

    /**
     * 上传路径
     */
    private String fileBaseDir;

    /**
     * 获取地址开关
     */
    private boolean addressEnabled;

    /**
     * 验证码类型
     */
    private String captchaType;

    /**
     * rsa private key  静态属性的注入！！ set方法一定不能是static 方法
     */
    private String rsaPrivateKey;

    private String apiPrefix;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public void setFileBaseDir(String fileBaseDir) {
        this.fileBaseDir = fileBaseDir;
    }

    public void setApiPrefix(String apiDocsPathPrefix) {
        this.apiPrefix = apiDocsPathPrefix;
    }

    public void setAddressEnabled(boolean addressEnabled) {
        this.addressEnabled = addressEnabled;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public void setRsaPrivateKey(String rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public void setDemoEnabled(boolean demoEnabled) {
        this.demoEnabled = demoEnabled;
    }

    public static String getFileBaseDir() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.fileBaseDir + File.separator + Constants.RESOURCE_PREFIX;
    }

    public static String getApiPrefix() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.apiPrefix;
    }

    public static boolean isAddressEnabled() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.addressEnabled;
    }

    public static String getCaptchaType() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.captchaType;
    }

    public static String getRsaPrivateKey() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.rsaPrivateKey;
    }

    public static boolean isDemoEnabled() {
        KeystoneConfig config = SpringUtil.getBean(KeystoneConfig.class);
        return config.demoEnabled;
    }

}
