package com.event_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger configuration for Event Management System
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        // Create schema for LocalDateTime to ensure consistent formatting
        Schema<?> localDateTimeSchema = new Schema<String>()
                .type("string")
                .format("date-time")
                .example("2025-12-20 10:00:00")
                .description("Date and time in format: yyyy-MM-dd HH:mm:ss");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Event Management System API")
                        .description("RESTful API for Event Management System with role-based access control")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login"))
                        .addSchemas("LocalDateTime", localDateTimeSchema));
    }
}