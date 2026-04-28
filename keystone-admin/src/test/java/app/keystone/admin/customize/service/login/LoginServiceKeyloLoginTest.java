package app.keystone.admin.customize.service.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.admin.customize.service.login.command.KeyloLoginCommand;
import app.keystone.admin.customize.service.login.command.LoginCommand;
import app.keystone.admin.customize.service.login.keylo.KeyloCredentialVerifier;
import app.keystone.admin.customize.service.login.keylo.KeyloPrincipal;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.enums.common.UserStatusEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.i18n.MessageUtils;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.domain.system.dept.db.SysDeptService;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.thread.ThreadPoolManager;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;

class LoginServiceKeyloLoginTest {

    private final TokenService tokenService = mock(TokenService.class);
    private final RedisCacheService redisCache = mock(RedisCacheService.class);
    private final SysConfigService sysConfigService = mock(SysConfigService.class);
    private final SysDeptService sysDeptService = mock(SysDeptService.class);
    private final LocalCacheService localCache = spy(new LocalCacheService(sysConfigService, sysDeptService));
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final SysUserService userService = mock(SysUserService.class);
    private final UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
    private final KeyloTokenVerifier keyloTokenVerifier = mock(KeyloTokenVerifier.class);
    private final KeyloCredentialVerifier keyloCredentialVerifier = mock(KeyloCredentialVerifier.class);
    private final KeyloProperties keyloProperties = mock(KeyloProperties.class);

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = spy(new LoginService(
            tokenService,
            redisCache,
            localCache,
            authenticationManager,
            userService,
            userDetailsService,
            keyloTokenVerifier,
            keyloCredentialVerifier,
            keyloProperties
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_shouldUseKeyloCredential_whenAuthModeMixedAndKeyloEnabled() throws Exception {
        setAuthMode(loginService, "mixed");
        when(keyloProperties.isEnabled()).thenReturn(true);
        when(sysConfigService.getConfigValueByKey("sys.account.captchaOnOff")).thenReturn("false");

        LoginCommand command = new LoginCommand();
        command.setUsername("admin");
        command.setPassword("plain-password");

        doReturn("plain-password").when(loginService).decryptPassword("plain-password");

        when(keyloCredentialVerifier.verify("admin", "plain-password")).thenReturn(new KeyloPrincipal("sub-001"));

        SysUserEntity mappedUser = new SysUserEntity();
        mappedUser.setUserId(1L);
        mappedUser.setUsername("admin");
        mappedUser.setStatus(UserStatusEnum.NORMAL.getValue());
        when(userService.getUserByExternalSubject("sub-001")).thenReturn(mappedUser);

        SystemLoginUser loginUser = new SystemLoginUser(1L, true, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        when(userDetailsService.buildLoginUser(mappedUser)).thenReturn(loginUser);
        when(tokenService.createTokenAndPutUserInCache(loginUser)).thenReturn("keystone-token");

        SysUserEntity cachedUser = new SysUserEntity() {
            @Override
            public boolean updateById() {
                return true;
            }
        };
        cachedUser.setUserId(1L);

        @SuppressWarnings("unchecked")
        RedisCacheTemplate<SysUserEntity> userCache = mock(RedisCacheTemplate.class);
        redisCache.userCache = userCache;
        when(userCache.getObjectById(1L)).thenReturn(cachedUser);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        try (MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMocked = mockStatic(ThreadPoolManager.class)) {
            servletHolderUtilMocked.when(ServletHolderUtil::getRequest).thenReturn(request);
            threadPoolManagerMocked.when(() -> ThreadPoolManager.execute(any(Runnable.class))).thenAnswer(invocation -> null);

            String token = loginService.login(command);

            assertEquals("keystone-token", token);
            verify(keyloCredentialVerifier, times(1)).verify("admin", "plain-password");
            verify(authenticationManager, never()).authenticate(any());
        }
    }

    @Test
    void login_shouldUseLocalAuth_whenAuthModeLocal() throws Exception {
        setAuthMode(loginService, "local");
        when(keyloProperties.isEnabled()).thenReturn(true);
        when(sysConfigService.getConfigValueByKey("sys.account.captchaOnOff")).thenReturn("false");

        LoginCommand command = new LoginCommand();
        command.setUsername("admin");
        command.setPassword("plain-password");

        doReturn("plain-password").when(loginService).decryptPassword("plain-password");

        SystemLoginUser loginUser = new SystemLoginUser(1L, true, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenService.createTokenAndPutUserInCache(loginUser)).thenReturn("local-token");

        SysUserEntity cachedUser = new SysUserEntity() {
            @Override
            public boolean updateById() {
                return true;
            }
        };
        cachedUser.setUserId(1L);

        @SuppressWarnings("unchecked")
        RedisCacheTemplate<SysUserEntity> userCache = mock(RedisCacheTemplate.class);
        redisCache.userCache = userCache;
        when(userCache.getObjectById(1L)).thenReturn(cachedUser);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        try (MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMocked = mockStatic(ThreadPoolManager.class)) {
            servletHolderUtilMocked.when(ServletHolderUtil::getRequest).thenReturn(request);
            threadPoolManagerMocked.when(() -> ThreadPoolManager.execute(any(Runnable.class))).thenAnswer(invocation -> null);

            String token = loginService.login(command);

            assertEquals("local-token", token);
            verify(authenticationManager, times(1)).authenticate(any());
            verify(keyloCredentialVerifier, never()).verify(any(), any());
        }
    }

    @Test
    void login_shouldThrowKeyloDisabled_whenAuthModeKeyloOnlyButKeyloDisabled() throws Exception {
        setAuthMode(loginService, "keylo-only");
        when(keyloProperties.isEnabled()).thenReturn(false);
        when(sysConfigService.getConfigValueByKey("sys.account.captchaOnOff")).thenReturn("false");

        LoginCommand command = new LoginCommand();
        command.setUsername("admin");
        command.setPassword("plain-password");

        ApiException exception = assertThrows(ApiException.class, () -> loginService.login(command));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_DISABLED, exception.getErrorCode());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void keyloLogin_shouldReturnToken_whenSubjectMappedAndUserEnabled() throws Exception {
        setAuthMode(loginService, "mixed");
        when(keyloProperties.isEnabled()).thenReturn(true);

        KeyloLoginCommand command = new KeyloLoginCommand();
        command.setAccessToken("mock-token");

        when(keyloTokenVerifier.verify("mock-token")).thenReturn(new KeyloPrincipal("sub-001"));

        SysUserEntity mappedUser = new SysUserEntity();
        mappedUser.setUserId(1L);
        mappedUser.setUsername("admin");
        mappedUser.setStatus(UserStatusEnum.NORMAL.getValue());
        when(userService.getUserByExternalSubject("sub-001")).thenReturn(mappedUser);

        SystemLoginUser loginUser = new SystemLoginUser(1L, true, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        when(userDetailsService.buildLoginUser(mappedUser)).thenReturn(loginUser);
        when(tokenService.createTokenAndPutUserInCache(loginUser)).thenReturn("keystone-token");

        SysUserEntity cachedUser = new SysUserEntity() {
            @Override
            public boolean updateById() {
                return true;
            }
        };
        cachedUser.setUserId(1L);

        @SuppressWarnings("unchecked")
        RedisCacheTemplate<SysUserEntity> userCache = mock(RedisCacheTemplate.class);
        redisCache.userCache = userCache;
        when(userCache.getObjectById(1L)).thenReturn(cachedUser);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        try (MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMocked = mockStatic(ThreadPoolManager.class)) {
            servletHolderUtilMocked.when(ServletHolderUtil::getRequest).thenReturn(request);
            threadPoolManagerMocked.when(() -> ThreadPoolManager.execute(any(Runnable.class))).thenAnswer(invocation -> null);

            String token = loginService.keyloLogin(command);

            assertEquals("keystone-token", token);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(loginUser, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            verify(tokenService, times(1)).createTokenAndPutUserInCache(loginUser);
        }
    }

    @Test
    void keyloLogin_shouldThrow_whenAuthModeIsLocal() throws Exception {
        setAuthMode(loginService, "local");
        when(keyloProperties.isEnabled()).thenReturn(true);

        KeyloLoginCommand command = new KeyloLoginCommand();
        command.setAccessToken("mock-token");

        ApiException exception = assertThrows(ApiException.class, () -> loginService.keyloLogin(command));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_DISABLED, exception.getErrorCode());
    }


    @Test
    void keyloLogin_shouldThrow_whenAccessTokenBlank() throws Exception {
        setAuthMode(loginService, "mixed");
        when(keyloProperties.isEnabled()).thenReturn(true);

        KeyloLoginCommand command = new KeyloLoginCommand();
        command.setAccessToken(" ");

        ApiException exception = assertThrows(ApiException.class, () -> loginService.keyloLogin(command));

        assertEquals(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, exception.getErrorCode());
    }

    @Test
    void keyloLogin_shouldThrowUserNonExist_whenSubjectNotMapped() throws Exception {
        setAuthMode(loginService, "mixed");
        when(keyloProperties.isEnabled()).thenReturn(true);

        KeyloLoginCommand command = new KeyloLoginCommand();
        command.setAccessToken("mock-token");

        when(keyloTokenVerifier.verify("mock-token")).thenReturn(new KeyloPrincipal("sub-404"));
        when(userService.getUserByExternalSubject("sub-404")).thenReturn(null);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        try (MockedStatic<MessageUtils> messageUtilsMocked = mockStatic(MessageUtils.class);
             MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMocked = mockStatic(ThreadPoolManager.class)) {
            messageUtilsMocked.when(() -> MessageUtils.message("Business.USER_NON_EXIST", "sub-404")).thenReturn("mock-message");
            servletHolderUtilMocked.when(ServletHolderUtil::getRequest).thenReturn(request);
            threadPoolManagerMocked.when(() -> ThreadPoolManager.execute(any(Runnable.class))).thenAnswer(invocation -> null);

            ApiException exception = assertThrows(ApiException.class, () -> loginService.keyloLogin(command));
            assertEquals(ErrorCode.Business.USER_NON_EXIST, exception.getErrorCode());
        }
    }

    @Test
    void keyloLogin_shouldThrowUserDisabled_whenMappedUserDisabled() throws Exception {
        setAuthMode(loginService, "mixed");
        when(keyloProperties.isEnabled()).thenReturn(true);

        KeyloLoginCommand command = new KeyloLoginCommand();
        command.setAccessToken("mock-token");

        when(keyloTokenVerifier.verify("mock-token")).thenReturn(new KeyloPrincipal("sub-002"));

        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setUsername("disabled-user");
        userEntity.setStatus(UserStatusEnum.DISABLED.getValue());
        when(userService.getUserByExternalSubject("sub-002")).thenReturn(userEntity);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        try (MockedStatic<MessageUtils> messageUtilsMocked = mockStatic(MessageUtils.class);
             MockedStatic<ServletHolderUtil> servletHolderUtilMocked = mockStatic(ServletHolderUtil.class);
             MockedStatic<ThreadPoolManager> threadPoolManagerMocked = mockStatic(ThreadPoolManager.class)) {
            messageUtilsMocked.when(() -> MessageUtils.message("Business.USER_IS_DISABLE", "disabled-user")).thenReturn("mock-message");
            servletHolderUtilMocked.when(ServletHolderUtil::getRequest).thenReturn(request);
            threadPoolManagerMocked.when(() -> ThreadPoolManager.execute(any(Runnable.class))).thenAnswer(invocation -> null);

            ApiException exception = assertThrows(ApiException.class, () -> loginService.keyloLogin(command));
            assertEquals(ErrorCode.Business.USER_IS_DISABLE, exception.getErrorCode());
        }
    }

    private void setAuthMode(LoginService target, String authMode) throws Exception {
        Field field = LoginService.class.getDeclaredField("authMode");
        field.setAccessible(true);
        field.set(target, authMode);
    }
}
