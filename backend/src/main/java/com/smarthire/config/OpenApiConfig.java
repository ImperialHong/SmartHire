package com.smarthire.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI smartHireOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("SmartHire API")
                .description("Recruitment workflow APIs for candidates, HR and administrators.")
                .version("v1")
            )
            .servers(List.of(new Server().url("/").description("Current environment")));
    }
}
