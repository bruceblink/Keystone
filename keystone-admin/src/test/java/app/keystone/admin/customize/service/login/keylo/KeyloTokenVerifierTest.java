package app.keystone.admin.customize.service.login.keylo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

class KeyloTokenVerifierTest {

    private final KeyloProperties keyloProperties = new KeyloProperties();
    private final KeyloTokenVerifier keyloTokenVerifier = new KeyloTokenVerifier(keyloProperties);

    @Test
    void verify_shouldThrowConfigMissing_whenIssuerAndJwkBothEmpty() {
        keyloProperties.setIssuerUri(null);
        keyloProperties.setJwkSetUri(null);

        ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("mock-token"));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING, exception.getErrorCode());
    }

    @Test
    void verify_shouldThrowInvalidToken_whenTokenMalformed() {
        keyloProperties.setJwkSetUri("http://localhost/.well-known/jwks.json");

        ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("not-a-jwt"));

        assertEquals(ErrorCode.Client.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void verify_shouldThrowSubjectMissing_whenSubjectClaimEmpty() {
        keyloProperties.setIssuerUri("http://localhost/mock-issuer");
        keyloProperties.setJwkSetUri(null);
        keyloProperties.setSubjectClaim("sub");

        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode("mock-token")).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn("");

        try (MockedStatic<JwtDecoders> jwtDecodersMocked = mockStatic(JwtDecoders.class)) {
            jwtDecodersMocked.when(() -> JwtDecoders.fromIssuerLocation("http://localhost/mock-issuer"))
                .thenReturn(jwtDecoder);

            ApiException exception = assertThrows(ApiException.class, () -> keyloTokenVerifier.verify("mock-token"));

            assertEquals(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING, exception.getErrorCode());
        }
    }
}
