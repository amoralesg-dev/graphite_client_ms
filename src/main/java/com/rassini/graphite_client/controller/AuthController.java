package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.dto.AuthResponse;
import com.rassini.graphite_client.service.auth.impl.AuthTestServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthTestServiceImpl service;

    @GetMapping("/token")
    public AuthResponse token() {
        return service.getToken();
    }
}