package com.example.order.config.openapi;

import com.example.order.config.correlation.CorrelationIdFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("Microserviço order: ingestão idempotente de pedidos e consulta paginada"));
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
