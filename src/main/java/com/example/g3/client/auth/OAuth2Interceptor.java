package com.example.g3.client.auth;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2Interceptor implements ClientHttpRequestInterceptor {

    private final MockOAuth2TokenService tokenService;

    public OAuth2Interceptor(MockOAuth2TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(tokenService.getToken());
        return execution.execute(request, body);
    }
}
