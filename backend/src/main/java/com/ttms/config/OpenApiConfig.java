package com.ttms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI (Swagger UI) 配置
 * 访问地址: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ttmsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TTMS 影院管理系统 API")
                .description("电影票务管理系统 - Theater Ticket Management System 后端API接口文档")
                .version("1.0.0")
                .contact(new Contact()
                    .name("TTMS Team")
                    .email("support@ttms.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
