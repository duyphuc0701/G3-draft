package com.example.g3.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplate cisRestTemplate(RestTemplateBuilder builder, com.example.g3.client.auth.OAuth2Interceptor oAuth2Interceptor) {
        return builder.additionalInterceptors(oAuth2Interceptor).build();
    }

    @Bean
    public RestTemplate documentRestTemplate(RestTemplateBuilder builder, com.example.g3.client.auth.OAuth2Interceptor oAuth2Interceptor) {
        return builder.additionalInterceptors(oAuth2Interceptor).build();
    }

    @Bean
    public RestTemplate riskRestTemplate(RestTemplateBuilder builder, com.example.g3.client.auth.OAuth2Interceptor oAuth2Interceptor) {
        return builder.additionalInterceptors(oAuth2Interceptor).build();
    }
}
