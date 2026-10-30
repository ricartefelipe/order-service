package com.example.order.config.openapi;

import com.example.order.config.correlation.CorrelationIdFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String API_VERSION = "2.0.0";
    static final String SECURITY_SCHEME = "clientToken";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token do sistema integrador: sub = origem, scope = orders:create e/ou orders:read")))
                .info(new Info()
                        .title("Order Service API")
                        .version(API_VERSION)
                        .description("Ingestão idempotente de pedidos por origem autenticada e consulta paginada. "
                                + "Mudanças incompatíveis sobem a versão maior e são anunciadas no CHANGELOG com "
                                + "um ciclo de depreciação; campos novos são sempre aditivos."));
    }

    @Bean
    public OpenApiCustomizer correlationIdHeaderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> operation.addParametersItem(new Parameter()
                            .in("header")
                            .name(CorrelationIdFilter.HEADER_NAME)
                            .required(false)
                            .description("Correlation id. Se ausente, o serviço gera e devolve no header.")
                            .schema(new StringSchema()))));
        };
    }
}
