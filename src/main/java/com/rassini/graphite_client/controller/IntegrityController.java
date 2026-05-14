package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.service.sync.IntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/integrity")
@RequiredArgsConstructor
@Slf4j
public class IntegrityController {

    private final IntegrityService integrityService;

    @GetMapping("/suppliers")
    public ResponseEntity<?> generateSuppliersFile() {
        log.info("REST request to generate suppliers to integrity file");
        integrityService.createFileSupplierSync();
        return ResponseEntity.ok(Map.of("message", "File suppliersToIntegrity.txt generation executed"));
    }
}