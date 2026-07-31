package com.vitorraphael.ifood.merchant.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ifoodMerchantApiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("iFood Merchant API")
                .description("API que acompanha pedidos, vendas e repasses financeiros de uma loja iFood, "
                        + "consumindo a Merchant API oficial do iFood por trás dos panos.")
                .version("0.0.1"));
    }
}
