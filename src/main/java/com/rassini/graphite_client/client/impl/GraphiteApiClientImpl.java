package com.rassini.graphite_client.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.rassini.graphite_client.client.GraphiteApiClient;
import com.rassini.graphite_client.dto.AckRequest;
import com.rassini.graphite_client.service.auth.TokenManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphiteApiClientImpl implements GraphiteApiClient {

    private final RestTemplate restTemplate;
    private final TokenManagerService tokenManagerService;

    @Value("${graphite.api-base-url}")
    private String baseUrl;

    @Value("${graphite.interface-name}")
    private String interfaceName;

    @Value("${graphite.limit}")
    private String limit;

    @Override
    public JsonNode getChanges(String interfaceNameNo, boolean filterReady, String publicIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/changes/connections")
                .queryParam("interface", this.interfaceName)
                .queryParam("connectionPhases", "connected")
                .queryParam("connectionRole", "buyer")
                .queryParam("limit", this.limit);
                //.queryParam("filterEntitiesWithConfirmationReviews", filterReady);

        // Si se pasan IDs específicos, Graphite debería devolverlos aunque tengan ACK
        if (StringUtils.hasText(publicIds)) {
            builder.queryParam("publicIds", publicIds);
            log.debug("[DEBUG] Forzando búsqueda de IDs específicos: {}", publicIds);
        }

        String url = builder.build().toUriString();

        log.debug("[DEBUG] === INICIO SOLICITUD CHANGES ===");
        log.debug("[DEBUG] URL: {}", url);

        JsonNode response = executeRequest(url, HttpMethod.GET, null);

        log.debug("[DEBUG] RESPONSE BODY: {}", response != null ? response.toString() : "NULL");
        log.debug("[DEBUG] === FIN SOLICITUD CHANGES ===");

        return response;
    }

    @Override
    public JsonNode getProfile(String publicId, boolean applyRules) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/profile/{publicId}")
                // .queryParam("connectionRole", "buyer")
                // .queryParam("applyVisibilityRules", true)
                // .queryParam("interface", this.interfaceName)
                .buildAndExpand(publicId)
                .toUriString();

        log.debug("[DEBUG] Consultando perfil para ID: {}", publicId);
        log.debug("[DEBUG] URL Perfil: {}", url);

        return executeRequest(url, HttpMethod.GET, null);
    }

    @Override
    public void acknowledgeChange(AckRequest ackRequest) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/changes/acknowledge")
                .build()
                .toUriString();

        Map<String, Object> body = new HashMap<>();
        body.put("interface", ackRequest.getJavaInterface());
        body.put("connectionRole", ackRequest.getConnectionRole());
        body.put("publicId", ackRequest.getPublicId());

        log.debug("[DEBUG] Enviando ACK para ID: {}", ackRequest.getPublicId());

        executeRequest(url, HttpMethod.POST, body);
    }

    private JsonNode executeRequest(String url, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        String token = tokenManagerService.getValidToken();

        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");

        if (token != null && token.length() > 20) {
            log.info("[DEBUG] Token: {}...{}", token.substring(0, 10), token.substring(token.length() - 10));
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, method, entity, JsonNode.class);
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("[DEBUG] Error de Cliente ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[DEBUG] Error de Conexión: {}", e.getMessage());
            throw e;
        }
    }
}