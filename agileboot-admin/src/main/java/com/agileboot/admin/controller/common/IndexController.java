package com.agileboot.admin.controller.common;

import com.agileboot.infrastructure.config.AgileBootFrontendConfig;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Data
@Controller
public class IndexController {

    private final AgileBootFrontendConfig frontend;

    /**
     * 访问"/"首页，直接跳转到前端页面
     */
    @Operation(summary = "首页")
    @GetMapping("/")
    public String index() {
        return "redirect:" + frontend.getUrl();
    }
}

