package com.rassini.graphite_client.service.sync.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.rassini.graphite_client.client.GraphiteApiClient;
import com.rassini.graphite_client.dto.AckRequest;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;
import com.rassini.graphite_client.service.sync.GraphiteSyncService;
import com.rassini.graphite_client.service.xml.SupplierProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphiteSyncServiceImpl implements GraphiteSyncService {

    // =========================
    // DEPENDENCIAS
    // =========================
    private final GraphiteApiClient apiClient;
    private final SupplierRepository repository;
    private final SupplierProcessingService supplierProcessingService;
    private final GraphiteProfileRefreshService graphiteProfileRefreshService;

    @Value("${graphite.interface-name}")
    private String interfaceName;

    // =========================
    // API PUBLICA
    // =========================

    @Override
    public void executeFullSync() {
        log.info("[SERVICE] Iniciando Full Sync...");
        syncSpecificSuppliers(null);
    }

    @Override
    public void syncSpecificSuppliers(String publicIds) {

        log.info("[SERVICE] === INICIO DE PROCESAMIENTO DE CAMBIOS ===");
        log.info("[SERVICE] Interfaz usada: {}", interfaceName);

        try {
            JsonNode response = apiClient.getChanges(interfaceName, true, publicIds);

            if (response == null) {
                log.error("[SERVICE] Respuesta NULL desde Graphite");
                return;
            }

            JsonNode updated = response.path("updated");

            if (!updated.isArray() || updated.isEmpty()) {
                log.info("[SERVICE] No hay elementos en 'updated'");
                return;
            }

            for (int i = 0; i < updated.size(); i++) {
                String publicId = updated.get(i).asText();
                log.info("[SERVICE] [{}/{}] Procesando publicId={}",
                        i + 1, updated.size(), publicId);

                try {
                    // 1) Descarga perfil y guarda con status DESCARGA
                    boolean saved = graphiteProfileRefreshService.processAndSaveInternal(publicId);

                    if (!saved) {
                        log.warn("[SERVICE] Perfil {} no se pudo guardar", publicId);
                        continue;
                    }

                    // 2) Ejecuta pipeline interno (JPA + XML)
                    supplierProcessingService.processSupplier(publicId);

                    // 3) ACK + CONFIMADOACK (SE QUEDA AQUI)
                    sendAcknowledge(publicId);

                } catch (Exception e) {
                    log.error("[SERVICE] Error procesando {}: {}", publicId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("[SERVICE] Error general en sync", e);
        }

        log.info("[SERVICE] === FIN DE PROCESAMIENTO DE CAMBIOS ===");
    }

    // =========================
    // GUARDA JSON Y STATUS
    // =========================

    
    // =========================
    // ACK A GRAPHITE
    // =========================

    private void sendAcknowledge(String publicId) {

        try {
            AckRequest ackRequest = AckRequest.builder()
                    .javaInterface(interfaceName)
                    .connectionRole("buyer")
                    .publicId(publicId)
                    .build();

            apiClient.acknowledgeChange(ackRequest);

            SupplierEntity entity = repository.findById(publicId).orElseThrow(() -> 
                    new IllegalArgumentException("SupplierEntity not found for publicId: " + publicId));
            entity.setStatus(ProviderState.CONFIRMADOACK);
            entity.setLastSync(LocalDateTime.now());

            repository.save(entity);

            log.info("[SERVICE] ACK enviado y status CONFIRMADOACK para {}", publicId);

        } catch (Exception e) {
            log.error("[SERVICE] Error enviando ACK para {}: {}", publicId, e.getMessage(), e);
        }
    }
}