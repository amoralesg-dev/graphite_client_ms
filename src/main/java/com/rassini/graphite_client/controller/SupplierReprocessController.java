package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.service.xml.SupplierProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers/reprocess")
@RequiredArgsConstructor
@Slf4j
public class SupplierReprocessController {

    private final SupplierProcessingService supplierProcessingService;

    /**
     * Reprocesa UN proveedor si su status es DESCARGA
     * consulta Graphite
     * NO hace ACK
     */
    @PostMapping("/{publicId}")
    public ResponseEntity<String> reprocessOne(
            @PathVariable("publicId") String publicId
    ) {

        log.info("[REPROCESS] Reprocesando proveedor {}", publicId);

        supplierProcessingService.processSupplier(publicId,"REPROCESSO MANUAL/byID");

        return ResponseEntity.ok(
                "Reproceso iniciado para proveedor " + publicId
        );
    }

    /**
     * Reprocesa VARIOS proveedores (batch)
     * Espera un JSON array con los publicId
     *
     * Example:
     * ["MX729844","US730051"]
     */
    @PostMapping
    public ResponseEntity<String> reprocessBatch(
            @RequestBody List<String> publicIds
    ) {

        log.info("[REPROCESS] Reprocesando {} proveedores", publicIds.size());

        for (String publicId : publicIds) {
            try {
                supplierProcessingService.processSupplier(publicId, "REPROCESSO MANUAL/batch");
            } catch (Exception e) {
                log.error(
                        "[REPROCESS] Error reprocesando {}: {}",
                        publicId,
                        e.getMessage(),
                        e
                );
            }
        }

        return ResponseEntity.ok(
                "Reproceso batch iniciado para " + publicIds.size() + " proveedores"
        );
    }
}