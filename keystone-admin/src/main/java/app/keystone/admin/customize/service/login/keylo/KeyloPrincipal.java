package app.keystone.admin.customize.service.login.keylo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Keylo 主体信息
 */
@Data
@AllArgsConstructor
public class KeyloPrincipal {

    private String subject;
}
