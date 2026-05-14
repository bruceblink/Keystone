package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.time.Duration;
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
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keylo token verifier.
 */
@Component
@RequiredArgsConstructor
public class KeyloTokenVerifier {

    private final KeyloProperties keyloProperties;

    private volatile JwtDecoder jwtDecoder;

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
        JwtDecoder cachedJwtDecoder = jwtDecoder;
        if (cachedJwtDecoder != null) {
            return cachedJwtDecoder;
        }

        synchronized (this) {
            if (jwtDecoder == null) {
                jwtDecoder = createJwtDecoder();
            }
            return jwtDecoder;
        }
    }

    private JwtDecoder createJwtDecoder() {
        if (!StringUtils.hasText(keyloProperties.getIssuerUri())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }

        if (StringUtils.hasText(keyloProperties.getJwkSetUri())) {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(keyloProperties.getJwkSetUri()).build();
            jwtDecoder.setJwtValidator(buildValidator());
            return jwtDecoder;
        }

        JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(keyloProperties.getIssuerUri());
        if (jwtDecoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
            nimbusJwtDecoder.setJwtValidator(buildValidator());
        }
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> buildValidator() {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds()));
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(keyloProperties.getIssuerUri());
        OAuth2TokenValidator<Jwt> defaultValidator = new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator);

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

    private long clockSkewSeconds() {
        Integer clockSkewSeconds = keyloProperties.getClockSkewSeconds();
        return clockSkewSeconds == null || clockSkewSeconds < 0 ? 60L : clockSkewSeconds;
    }

    private List<String> trustedAudiences() {
        List<String> trustedAudiences = new ArrayList<>();
        if (keyloProperties.getAudiences() != null) {
            trustedAudiences.addAll(keyloProperties.getAudiences().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
        }
        return trustedAudiences;
    }
}
