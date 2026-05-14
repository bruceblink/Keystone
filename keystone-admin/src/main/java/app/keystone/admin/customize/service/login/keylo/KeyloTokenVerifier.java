package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.util.ArrayList;
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

    public KeyloTokenIdentity verify(String accessToken) {
        try {
            Jwt jwt = buildJwtDecoder().decode(accessToken);
            String subjectClaim = StringUtils.hasText(keyloProperties.getSubjectClaim())
                ? keyloProperties.getSubjectClaim() : "sub";
            String subject = jwt.getClaimAsString(subjectClaim);
            if (!StringUtils.hasText(subject)) {
                throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
            }
            String userIdClaim = StringUtils.hasText(keyloProperties.getUserIdClaim())
                ? keyloProperties.getUserIdClaim() : "uid";
            String userId = jwt.getClaimAsString(userIdClaim);
            String tokenType = jwt.getClaimAsString("token_type");
            Long expiresIn = jwt.getExpiresAt() == null
                ? null
                : Math.max(0L, (jwt.getExpiresAt().toEpochMilli() - System.currentTimeMillis()) / 1000L);
            return new KeyloTokenIdentity(subject, userId, accessToken, null, expiresIn, tokenType);
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

        List<String> trustedAudiences = trustedAudiences();
        if (trustedAudiences.isEmpty()) {
            return defaultValidator;
        }

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.stream().anyMatch(trustedAudiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The required audience is missing", null));
        };
        return new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
    }

    private List<String> trustedAudiences() {
        List<String> trustedAudiences = new ArrayList<>();
        if (keyloProperties.getAudiences() != null) {
            trustedAudiences.addAll(keyloProperties.getAudiences().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
        }
        if (trustedAudiences.isEmpty() && StringUtils.hasText(keyloProperties.getAudience())) {
            trustedAudiences.add(keyloProperties.getAudience().trim());
        }
        return trustedAudiences;
    }
}
