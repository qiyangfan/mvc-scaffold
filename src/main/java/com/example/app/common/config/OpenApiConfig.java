package com.example.app.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置：启动后访问 /swagger-ui.html 查看与调试接口。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI scaffoldOpenApi() {
        return new OpenAPI().info(new Info()
                .title("mvc-scaffold API")
                .version("1.0.0")
                .description("增强 MVC 架构脚手架（Controller-Service-Manager-Mapper）示例接口"));
    }
}
