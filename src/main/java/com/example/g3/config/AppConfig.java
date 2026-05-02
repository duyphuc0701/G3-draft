package com.example.g3.config;

import com.example.g3.client.auth.OAuth2ExchangeFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient idvWebClient(WebClient.Builder builder,
                                  @Value("${idv.service.url:http://localhost:8083}") String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebClient cisWebClient(WebClient.Builder builder,
                                  OAuth2ExchangeFilter oAuth2ExchangeFilter,
                                  @Value("${cis.service.url:http://localhost:8081}") String baseUrl) {
        return authenticatedClient(builder, oAuth2ExchangeFilter, baseUrl);
    }

    @Bean
    public WebClient documentWebClient(WebClient.Builder builder,
                                       OAuth2ExchangeFilter oAuth2ExchangeFilter,
                                       @Value("${document.service.url:http://localhost:8082}") String baseUrl) {
        return authenticatedClient(builder, oAuth2ExchangeFilter, baseUrl);
    }

    @Bean
    public WebClient riskWebClient(WebClient.Builder builder,
                                   OAuth2ExchangeFilter oAuth2ExchangeFilter,
                                   @Value("${risk.service.url:http://localhost:8084}") String baseUrl) {
        return authenticatedClient(builder, oAuth2ExchangeFilter, baseUrl);
    }

    private WebClient authenticatedClient(WebClient.Builder builder,
                                          OAuth2ExchangeFilter oAuth2ExchangeFilter,
                                          String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .filter(oAuth2ExchangeFilter.asFilter())
                .build();
    }
}
