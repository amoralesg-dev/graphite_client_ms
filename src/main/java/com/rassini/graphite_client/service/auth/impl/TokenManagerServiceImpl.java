package com.rassini.graphite_client.service.auth.impl;

import com.rassini.graphite_client.dto.AuthResponse;
import com.rassini.graphite_client.service.auth.AuthService;
import com.rassini.graphite_client.service.auth.TokenManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagerServiceImpl implements TokenManagerService {

    private final AuthService authService; 
    private String cachedToken;
    private Instant expiryInstant;

    @Override
    public synchronized String getValidToken() {
        if (cachedToken == null || Instant.now().isAfter(expiryInstant.minusSeconds(120))) {
            renewToken();
        }
        return cachedToken;
    }

    private void renewToken() {
        log.info("Token de Graphite caducado o nulo. Solicitando renovación...");
        AuthResponse response = authService.getToken();
        this.cachedToken = response.getAccessToken();
        this.expiryInstant = Instant.parse(response.getExpires_at());
        log.info("Nuevo token obtenido. Válido hasta: {}", response.getExpires_at());
    }
}