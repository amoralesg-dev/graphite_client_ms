package com.rassini.graphite_client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type") 
    private String token_type;

    @JsonProperty("expires_at")
    private String expires_at; // Formato ISO: 2026-04-24T23:08:11.262Z
}
