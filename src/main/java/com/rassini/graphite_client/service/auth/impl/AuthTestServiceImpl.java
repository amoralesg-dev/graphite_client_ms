package com.rassini.graphite_client.service.auth.impl;

import com.rassini.graphite_client.dto.AuthResponse;
import com.rassini.graphite_client.service.auth.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTestServiceImpl implements AuthService{

    private final RestTemplate restTemplate;

    @Value("${graphite.auth-url}")
    private String authUrl;

    @Value("${graphite.client-id}")
    private String clientId;

    @Value("${graphite.client-secret}")
    private String clientSecret;

    public AuthResponse getToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "grant_type", "client_credentials"
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                authUrl,
                HttpMethod.POST,
                request,
                AuthResponse.class
        );

        return response.getBody();
    }
}