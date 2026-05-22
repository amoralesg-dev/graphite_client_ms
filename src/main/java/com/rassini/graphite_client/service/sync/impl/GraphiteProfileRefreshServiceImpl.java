package com.rassini.graphite_client.service.sync.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.rassini.graphite_client.client.GraphiteApiClient;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphiteProfileRefreshServiceImpl implements GraphiteProfileRefreshService {

    private final GraphiteApiClient apiClient;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processAndSaveInternal(String publicId) {

        log.info("[SERVICE] Solicitando perfil a Graphite para {}", publicId);

        if (publicId == null) {
            log.error("[SERVICE] publicId is null");
            return false;
        }

        // IMPORTANTE: false para no mandar reglas que te cambien el payload
        JsonNode profile = apiClient.getProfile(publicId, false);

        if (profile == null || profile.isNull() || profile.isMissingNode()) {
            log.error("[SERVICE] Perfil vacío para {}", publicId);
            return false;
        }

        SupplierEntity entity = supplierRepository.findById(publicId)
                .orElseGet(() -> {
                    SupplierEntity e = new SupplierEntity();
                    e.setPublicId(publicId);
                    return e;
                });

        entity.setStatus(ProviderState.DESCARGA);
        entity.setFullJson(profile.toString());
        entity.setLastSync(LocalDateTime.now());

        supplierRepository.save(entity);

        log.info("[SERVICE] Proveedor {} guardado con status DESCARGA", publicId);
        return true;
    }
}