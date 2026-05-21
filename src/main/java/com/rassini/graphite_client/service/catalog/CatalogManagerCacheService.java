package com.rassini.graphite_client.service.catalog;

import com.rassini.graphite_client.entity.CatalogManagerEntity;

import java.util.Map;
import java.util.Optional;

public interface CatalogManagerCacheService {

    void loadCatalogInMemory();

    void reload();

    Optional<CatalogManagerEntity> get(String idCatalogo, String code, String businessUnit);

    String getEquivalencia(String idCatalogo, String code, String businessUnit);

    String getDescription(String idCatalogo, String code, String businessUnit);

    boolean exists(String idCatalogo, String code, String businessUnit);

    Map<CatalogManagerCacheKey, CatalogManagerEntity> getAll();
}