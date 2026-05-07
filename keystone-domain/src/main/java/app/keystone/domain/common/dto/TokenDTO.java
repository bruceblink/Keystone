package app.keystone.domain.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author valarchie
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {

    private String token;

    private CurrentLoginUserDTO currentUser;

    private String keyloAccessToken;

    private String keyloRefreshToken;

    private Long keyloExpiresIn;

    private String keyloTokenType;

}
