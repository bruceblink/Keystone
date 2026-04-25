package app.keystone.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author valarchie
 * SpringDoc API文档相关配置
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI keystoneApi() {
        return new OpenAPI()
            .info(new Info().title("Keystone 后台管理系统")
                .description("Keystone API")
                .version("v3.2.0")
                .license(new License().name("MIT 3.0").url("https://github.com/bruceblink/Keystone")))
            .externalDocs(new ExternalDocumentation()
                .description("Keystone 后台管理系统接口文档")
                .url("https://juejin.cn/column/7159946528827080734"));
    }

}
