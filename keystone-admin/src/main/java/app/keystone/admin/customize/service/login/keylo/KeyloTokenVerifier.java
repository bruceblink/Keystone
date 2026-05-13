package app.keystone.admin.customize.service.login.keylo;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
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
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator(resolveClockSkew()));

        if (StringUtils.hasText(keyloProperties.getIssuerUri())) {
            validators.add(new JwtIssuerValidator(keyloProperties.getIssuerUri()));
        }

        if (StringUtils.hasText(keyloProperties.getAudience())) {
            validators.add(new JwtAudienceValidator(keyloProperties.getAudience()));
        }

        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    private Duration resolveClockSkew() {
        Integer clockSkewSeconds = keyloProperties.getClockSkewSeconds();
        return Duration.ofSeconds(clockSkewSeconds == null ? 60 : Math.max(0, clockSkewSeconds));
    }
}
