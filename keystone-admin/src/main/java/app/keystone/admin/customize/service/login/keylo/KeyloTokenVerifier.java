package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keylo token 校验器
 */
@Component
@RequiredArgsConstructor
public class KeyloTokenVerifier {

    private final KeyloProperties keyloProperties;

    public KeyloPrincipal verify(String accessToken) {
        try {
            Jwt jwt = buildJwtDecoder().decode(accessToken);
            String subjectClaim = StringUtils.hasText(keyloProperties.getSubjectClaim())
                ? keyloProperties.getSubjectClaim() : "sub";
            String subject = jwt.getClaimAsString(subjectClaim);
            if (!StringUtils.hasText(subject)) {
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            return new KeyloPrincipal(subject, accessToken, null, null, null);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Client.INVALID_TOKEN);
        }
    }

    private JwtDecoder buildJwtDecoder() {
        if (StringUtils.hasText(keyloProperties.getJwkSetUri())) {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(keyloProperties.getJwkSetUri()).build();
            jwtDecoder.setJwtValidator(buildValidator());
            return jwtDecoder;
        }

        if (StringUtils.hasText(keyloProperties.getIssuerUri())) {
            JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(keyloProperties.getIssuerUri());
            if (jwtDecoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
                nimbusJwtDecoder.setJwtValidator(buildValidator());
            }
            return jwtDecoder;
        }

        throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
    }

    private OAuth2TokenValidator<Jwt> buildValidator() {
        OAuth2TokenValidator<Jwt> defaultValidator = StringUtils.hasText(keyloProperties.getIssuerUri())
            ? JwtValidators.createDefaultWithIssuer(keyloProperties.getIssuerUri())
            : JwtValidators.createDefault();

        if (!StringUtils.hasText(keyloProperties.getAudience())) {
            return defaultValidator;
        }

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(keyloProperties.getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required audience is missing", null));
        };
        return new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
    }
}
