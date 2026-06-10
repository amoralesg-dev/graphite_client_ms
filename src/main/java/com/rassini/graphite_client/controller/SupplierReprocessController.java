package com.rassini.graphite_client.controller;

import com.rassini.graphite_client.service.catalog.CatalogManagerCacheService;
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
    private final CatalogManagerCacheService catalogManagerCacheService;

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

     /**
     * Recarga manualmente el cache desde BD.
     */
    @PostMapping("/reload")
    public ResponseEntity<String> reload() {

        log.info("[CACHE] Iniciando recarga manual de catalog_manager");

        long start = System.currentTimeMillis();

        catalogManagerCacheService.reload();

        long elapsed = System.currentTimeMillis() - start;

        log.info(
                "[CACHE] Recarga completada correctamente en {} ms",
                elapsed
        );

        return ResponseEntity.ok(
                "Cache catalog_manager recargado correctamente en "
                        + elapsed + " ms"
        );
    }

    /**
     * Consulta cantidad de registros actualmente cargados.
     */
    @PostMapping("/reload/size")
    public ResponseEntity<Integer> size() {

        return ResponseEntity.ok(
                catalogManagerCacheService.getAll().size()
        );
    }

    @GetMapping("/reload/equivalencia")
    public ResponseEntity<String> getEquivalencia(
            @RequestParam String idCatalogo,
            @RequestParam String code,
            @RequestParam String businessUnit) {

        log.info(
                "[CACHE] Buscando equivalencia idCatalogo={}, code={}, businessUnit={}",
                idCatalogo,
                code,
                businessUnit
        );

        String equivalencia = catalogManagerCacheService.getEquivalencia(
                idCatalogo,
                code,
                businessUnit
        );

        if (equivalencia == null) {

            log.warn(
                    "[CACHE] No se encontró equivalencia para idCatalogo={}, code={}, businessUnit={}",
                    idCatalogo,
                    code,
                    businessUnit
            );

            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(equivalencia);
    }
}