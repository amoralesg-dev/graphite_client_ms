package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.service.sync.IntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/integrity")
@RequiredArgsConstructor
@Slf4j
public class IntegrityController {

    private final IntegrityService integrityService;

    @GetMapping("/suppliers/{erpIdQad}")
    public ResponseEntity<?> generateSuppliersFile( @PathVariable("erpIdQad") String erpIdQad) {
        log.info("REST request to generate supplier with ERP ID QAD {} to integrity file",erpIdQad);
        integrityService.createFileSupplierSync(erpIdQad);
        return ResponseEntity.ok(Map.of("message", "File suppliersToIntegrity generation executed for ID "+erpIdQad));
    }
}