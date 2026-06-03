package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.service.sync.GraphiteSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final GraphiteSyncService syncService;

    @PostMapping("/graphite/full")
    public ResponseEntity<?> triggerFullSync() {
        CompletableFuture.runAsync(() -> syncService.executeFullSync("/graphite/full"));
        return ResponseEntity.accepted().body(Map.of("message", "Sincronización total iniciada"));
    }

    @Scheduled(cron = "#{@environment.getProperty('graphite.sync.cron')}")
    public void scheduledSync() {
        syncService.executeFullSync("/scheduler");
    }

    @PostMapping("/graphite/suppliers")
    public ResponseEntity<?> triggerSpecificSync(@RequestParam(value = "ids", required = false) String ids) {
        // 1. Ejecutar de forma asíncrona
        CompletableFuture.runAsync(() -> syncService.syncSpecificSuppliers(ids,"/graphite/suppliers"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sincronización bajo demanda iniciada");
        response.put("targets", ids != null ? ids : "TODOS");

        return ResponseEntity.accepted().body(response);
    }

}
