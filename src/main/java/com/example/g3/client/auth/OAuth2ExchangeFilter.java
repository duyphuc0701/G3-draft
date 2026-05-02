package com.example.g3.client.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Component
public class OAuth2ExchangeFilter {

    private final MockOAuth2TokenService tokenService;

    public OAuth2ExchangeFilter(MockOAuth2TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public ExchangeFilterFunction asFilter() {
        return (request, next) -> next.exchange(ClientRequest.from(request)
                .headers(headers -> headers.setBearerAuth(tokenService.getToken()))
                .build());
    }
}
