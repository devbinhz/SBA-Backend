package com.bookverse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookverseOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("BookVerse API")
                        .version("v1")
                        .description("BookVerse backend API documentation"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("bookverse-public")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}

