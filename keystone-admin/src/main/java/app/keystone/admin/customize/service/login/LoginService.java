package app.keystone.admin.customize.service.login;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.admin.customize.service.login.command.KeyloLoginCommand;
import app.keystone.admin.customize.service.login.command.LoginCommand;
import app.keystone.admin.customize.service.login.dto.CaptchaDTO;
import app.keystone.admin.customize.service.login.dto.ConfigDTO;
import app.keystone.admin.customize.service.login.keylo.KeyloCredentialVerifier;
import app.keystone.admin.customize.service.login.keylo.KeyloPrincipal;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.constant.Constants.Captcha;
import app.keystone.common.enums.common.ConfigKeyEnum;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.enums.common.UserStatusEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.i18n.MessageUtils;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.MapCache;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.thread.ThreadPoolManager;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import com.google.code.kaptcha.Producer;
import jakarta.annotation.Resource;
import java.awt.image.BufferedImage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StringUtils;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginService {

    private final TokenService tokenService;

    private final RedisCacheService redisCache;

    private final LocalCacheService localCache;

    private final AuthenticationManager authenticationManager;

    private final SysUserService userService;

    private final UserDetailsServiceImpl userDetailsService;

    private final KeyloTokenVerifier keyloTokenVerifier;

    private final KeyloCredentialVerifier keyloCredentialVerifier;

    private final KeyloProperties keyloProperties;

    @Value("${keystone.auth.mode:mixed}")
    private String authMode;

    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    /**
     * 登录验证
     *
     * @param loginCommand 登录参数
     * @return 结果
     */
    public String login(LoginCommand loginCommand) {
        if (isCaptchaOn()) {
            validateCaptcha(loginCommand.getUsername(), loginCommand.getCaptchaCode(), loginCommand.getCaptchaCodeKey());
        }

        if (!"local".equalsIgnoreCase(authMode) && keyloProperties.isEnabled()) {
            return loginByKeyloCredential(loginCommand);
        }

        if ("keylo-only".equalsIgnoreCase(authMode)) {
            throw new ApiException(Business.LOGIN_KEYLO_DISABLED);
        }

        Authentication authentication;
        String decryptPassword = decryptPassword(loginCommand.getPassword());
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginCommand.getUsername(), decryptPassword));
        } catch (BadCredentialsException e) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginCommand.getUsername(), LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.LOGIN_WRONG_USER_PASSWORD")));
            throw new ApiException(e, ErrorCode.Business.LOGIN_WRONG_USER_PASSWORD);
        } catch (Exception e) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginCommand.getUsername(), LoginStatusEnum.LOGIN_FAIL, e.getMessage()));
            throw new ApiException(e, Business.LOGIN_ERROR, e.getMessage());
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SystemLoginUser loginUser = (SystemLoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser);
        return tokenService.createTokenAndPutUserInCache(loginUser);
    }

    public String keyloLogin(KeyloLoginCommand keyloLoginCommand) {
        if ("local".equalsIgnoreCase(authMode) || !keyloProperties.isEnabled()) {
            throw new ApiException(Business.LOGIN_KEYLO_DISABLED);
        }
        if (keyloLoginCommand == null || !StringUtils.hasText(keyloLoginCommand.getAccessToken())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "accessToken is required");
        }

        KeyloPrincipal keyloPrincipal = keyloTokenVerifier.verify(keyloLoginCommand.getAccessToken());
        return buildTokenByKeyloSubject(keyloPrincipal.getSubject());
    }

    private String loginByKeyloCredential(LoginCommand loginCommand) {
        if (loginCommand == null || !StringUtils.hasText(loginCommand.getUsername()) || !StringUtils.hasText(loginCommand.getPassword())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "username and password are required");
        }

        String password = decryptPassword(loginCommand.getPassword());
        KeyloPrincipal keyloPrincipal = keyloCredentialVerifier.verify(loginCommand.getUsername(), password);
        return buildTokenByKeyloSubject(keyloPrincipal.getSubject());
    }

    private String buildTokenByKeyloSubject(String subject) {
        SysUserEntity userEntity = userService.getUserByExternalSubject(subject);
        if (userEntity == null) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(subject, LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.USER_NON_EXIST", subject)));
            throw new ApiException(ErrorCode.Business.USER_NON_EXIST, subject);
        }
        if (!Objects.equals(UserStatusEnum.NORMAL.getValue(), userEntity.getStatus())) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(userEntity.getUsername(), LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.USER_IS_DISABLE", userEntity.getUsername())));
            throw new ApiException(ErrorCode.Business.USER_IS_DISABLE, userEntity.getUsername());
        }

        SystemLoginUser loginUser = userDetailsService.buildLoginUser(userEntity);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        recordLoginInfo(loginUser);
        return tokenService.createTokenAndPutUserInCache(loginUser);
    }

    /**
     * 获取验证码 data
     *
     * @return {@link ConfigDTO}
     */
    public ConfigDTO getConfig() {
        ConfigDTO configDTO = new ConfigDTO();

        boolean isCaptchaOn = isCaptchaOn();
        configDTO.setIsCaptchaOn(isCaptchaOn);
        configDTO.setDictionary(MapCache.dictionaryCache());
        return configDTO;
    }

    /**
     * 获取验证码 data
     *
     * @return 验证码
     */
    public CaptchaDTO generateCaptchaImg() {
        CaptchaDTO captchaDTO = new CaptchaDTO();

        boolean isCaptchaOn = isCaptchaOn();
        captchaDTO.setIsCaptchaOn(isCaptchaOn);

        if (isCaptchaOn) {
            String expression;
            String answer = null;
            BufferedImage image = null;

            String captchaType = KeystoneConfig.getCaptchaType();
            if (Captcha.MATH_TYPE.equals(captchaType)) {
                String capText = captchaProducerMath.createText();
                String[] expressionAndAnswer = capText.split("@");
                expression = expressionAndAnswer[0];
                answer = expressionAndAnswer[1];
                image = captchaProducerMath.createImage(expression);
            }

            if (Captcha.CHAR_TYPE.equals(captchaType)) {
                expression = answer = captchaProducer.createText();
                image = captchaProducer.createImage(expression);
            }

            if (image == null) {
                throw new ApiException(ErrorCode.Internal.LOGIN_CAPTCHA_GENERATE_FAIL);
            }

            String imgKey = IdUtil.simpleUUID();

            redisCache.captchaCache.set(imgKey, answer);
            FastByteArrayOutputStream os = new FastByteArrayOutputStream();
            ImgUtil.writeJpg(image, os);

            captchaDTO.setCaptchaCodeKey(imgKey);
            captchaDTO.setCaptchaCodeImg(Base64.encode(os.toByteArray()));

        }

        return captchaDTO;
    }


    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param captchaCode 验证码
     * @param captchaCodeKey 验证码对应的缓存key
     */
    public void validateCaptcha(String username, String captchaCode, String captchaCodeKey) {
        String captcha = redisCache.captchaCache.getObjectById(captchaCodeKey);
        redisCache.captchaCache.delete(captchaCodeKey);
        if (captcha == null) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(username, LoginStatusEnum.LOGIN_FAIL,
                ErrorCode.Business.LOGIN_CAPTCHA_CODE_EXPIRE.message()));
            throw new ApiException(ErrorCode.Business.LOGIN_CAPTCHA_CODE_EXPIRE);
        }
        if (!captchaCode.equalsIgnoreCase(captcha)) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(username, LoginStatusEnum.LOGIN_FAIL,
                ErrorCode.Business.LOGIN_CAPTCHA_CODE_WRONG.message()));
            throw new ApiException(ErrorCode.Business.LOGIN_CAPTCHA_CODE_WRONG);
        }
    }

    /**
     * 记录登录信息
     * @param loginUser 登录用户
     */
    public void recordLoginInfo(SystemLoginUser loginUser) {
        ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginUser.getUsername(), LoginStatusEnum.LOGIN_SUCCESS,
            LoginStatusEnum.LOGIN_SUCCESS.description()));

        SysUserEntity entity = redisCache.userCache.getObjectById(loginUser.getUserId());

        entity.setLoginIp(ServletHolderUtil.getRequest().getRemoteAddr());
        entity.setLoginDate(DateUtil.date());
        entity.updateById();
    }

    public String decryptPassword(String originalPassword) {
        byte[] decryptBytes = SecureUtil.rsa(KeystoneConfig.getRsaPrivateKey(), null)
            .decrypt(Base64.decode(originalPassword), KeyType.PrivateKey);

        return StrUtil.str(decryptBytes, CharsetUtil.CHARSET_UTF_8);
    }

    private boolean isCaptchaOn() {
        return Convert.toBool(localCache.configCache.get(ConfigKeyEnum.CAPTCHA.getValue()));
    }
}
