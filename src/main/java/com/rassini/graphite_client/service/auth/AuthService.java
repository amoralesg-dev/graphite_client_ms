package com.rassini.graphite_client.service.auth;

import com.rassini.graphite_client.dto.AuthResponse;

public interface AuthService {

    AuthResponse getToken();
}