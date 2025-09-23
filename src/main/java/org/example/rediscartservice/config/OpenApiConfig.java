package org.example.rediscartservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_SCHEME = "basicAuth";

    @Bean
    public OpenAPI shopApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shop API")
                        .description("Products & Cart (Redis/Jedis) demo")
                        .version("v1"))
                // define HTTP Basic security scheme
                .components(new Components().addSecuritySchemes(
                        BASIC_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                ))
                // apply it globally (Swagger UI will show the Authorize button)
                .addSecurityItem(new SecurityRequirement().addList(BASIC_SCHEME));
    }
}