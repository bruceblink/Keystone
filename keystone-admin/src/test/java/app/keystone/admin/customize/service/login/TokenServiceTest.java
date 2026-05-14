package app.keystone.admin.customize.service.login;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import app.keystone.common.constant.Constants.Token;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void createTokenAndPutUserInCache_shouldIncludeStandardJwtClaims() throws Exception {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
        redisCacheService.loginUserCache = loginUserCache;
        TokenService tokenService = new TokenService(redisCacheService);
        setField(tokenService, "secret", "0123456789abcdef0123456789abcdef");
        setField(tokenService, "expirationSeconds", 1800L);

        SystemLoginUser loginUser = new SystemLoginUser(1L, false, "admin", "pwd", RoleInfo.EMPTY_ROLE, 1L);
        String token = tokenService.createTokenAndPutUserInCache(loginUser);

        Claims claims = Jwts.parser()
            .verifyWith(signingKey("0123456789abcdef0123456789abcdef"))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.get(Token.LOGIN_USER_KEY));
    }

    private SecretKey signingKey(String secret) {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 64);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
