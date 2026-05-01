package app.keystone.api.controller.common;

import app.keystone.api.customize.service.JwtTokenService;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调度日志操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
@AllArgsConstructor
public class LoginController extends BaseController {

    private final JwtTokenService jwtTokenService;

    /**
     * 访问首页，提示语
     */
    @PostMapping("/app/{appId}/login")
    public ResponseDTO<String> appLogin() {
        String token = jwtTokenService.generateToken(Map.of("token", "user1"));
        return ResponseDTO.ok(token);
    }




}
