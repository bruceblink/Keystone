package com.agileboot.admin.controller.common;

import com.agileboot.infrastructure.annotations.ratelimit.RateLimit;
import com.agileboot.infrastructure.annotations.ratelimit.RateLimitKey;
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
     * 访问首页，提示语
     */
    @Operation(summary = "首页")
    @GetMapping("/")
    @RateLimit(key = RateLimitKey.TEST_KEY, time = 10, maxCount = 5, cacheType = RateLimit.CacheType.Map,
            limitType = RateLimit.LimitType.GLOBAL)
    public String index() {
        return "redirect:" + frontend.getUrl();
    }
}

