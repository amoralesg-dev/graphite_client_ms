package com.rassini.graphite_client.service.catalog.impl;

import com.rassini.graphite_client.entity.CatalogManagerEntity;
import com.rassini.graphite_client.repository.CatalogManagerRepository;
import com.rassini.graphite_client.service.catalog.CatalogManagerCacheKey;
import com.rassini.graphite_client.service.catalog.CatalogManagerCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogManagerCacheServiceImpl implements CatalogManagerCacheService {

    private final CatalogManagerRepository catalogManagerRepository;

    private final Map<CatalogManagerCacheKey, CatalogManagerEntity> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            loadCatalogInMemory();
        } catch (Exception e) {
            log.error("Fallo la inicializacion del cache de catalog_manager", e);
        }
    }

    @Override
    public void loadCatalogInMemory() {
        log.info("Iniciando carga de catalog_manager en memoria...");

        List<CatalogManagerEntity> records = catalogManagerRepository.findAll();
        log.info("Total registros recuperados desde repository.findAll(): {}", records.size());

        Map<String, Long> duplicates = records.stream()
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getIdCatalogo()) + "|" +
                             String.valueOf(r.getCode()) + "|" +
                             String.valueOf(r.getBusinessUnit()),
                        Collectors.counting()
                ));

        duplicates.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> log.warn("Duplicado detectado desde records: key={}, total={}", e.getKey(), e.getValue()));

        cache.clear();
        log.info("Cache limpiado correctamente.");

        for (CatalogManagerEntity record : records) {
            CatalogManagerCacheKey key = buildKey(
                    record.getIdCatalogo(),
                    record.getCode(),
                    record.getBusinessUnit()
            );

            CatalogManagerEntity existing = cache.get(key);
            if (existing != null) {
                log.warn(
                        "Registro duplicado detectado en cache para key={}. " +
                        "EXISTING -> idCatalogo=[{}], code=[{}], businessUnit=[{}], equivalencia=[{}], description=[{}]. " +
                        "INCOMING -> idCatalogo=[{}], code=[{}], businessUnit=[{}], equivalencia=[{}], description=[{}]",
                        key,
                        existing.getIdCatalogo(),
                        existing.getCode(),
                        existing.getBusinessUnit(),
                        existing.getEquivalencia(),
                        existing.getDescription(),
                        record.getIdCatalogo(),
                        record.getCode(),
                        record.getBusinessUnit(),
                        record.getEquivalencia(),
                        record.getDescription()
                );
            }

            cache.put(key, record);
        }

        log.info("Carga completada. Total registros en memoria: {}", cache.size());

        CatalogManagerCacheKey test0111 = buildKey("state", "MX-CMX", "0111");
        log.info("Validacion cache state/MX-CMX/0111 existe? {}", cache.containsKey(test0111));

        CatalogManagerCacheKey test1850 = buildKey("state", "MX-CMX", "1850");
        log.info("Validacion cache state/MX-CMX/1850 existe? {}", cache.containsKey(test1850));
    }

    @Override
    public void reload() {
        loadCatalogInMemory();
    }

    @Override
    public Optional<CatalogManagerEntity> get(String idCatalogo, String code, String businessUnit) {
        CatalogManagerCacheKey key = buildKey(idCatalogo, code, businessUnit);
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public String getEquivalencia(String idCatalogo, String code, String businessUnit) {
        CatalogManagerCacheKey key = buildKey(idCatalogo, code, businessUnit);
        CatalogManagerEntity entity = cache.get(key);

        if (entity == null) {
            log.warn("No se encontró equivalencia en cache para key={}", key);
            return null;
        }

        return entity.getEquivalencia();
    }

    @Override
    public String getDescription(String idCatalogo, String code, String businessUnit) {
        CatalogManagerCacheKey key = buildKey(idCatalogo, code, businessUnit);
        CatalogManagerEntity entity = cache.get(key);

        if (entity == null) {
            log.warn("No se encontró description en cache para key={}", key);
            return null;
        }

        return entity.getDescription();
    }

    @Override
    public boolean exists(String idCatalogo, String code, String businessUnit) {
        CatalogManagerCacheKey key = buildKey(idCatalogo, code, businessUnit);
        return cache.containsKey(key);
    }

    @Override
    public Map<CatalogManagerCacheKey, CatalogManagerEntity> getAll() {
        return Map.copyOf(cache);
    }

    private CatalogManagerCacheKey buildKey(String idCatalogo, String code, String businessUnit) {
        return new CatalogManagerCacheKey(idCatalogo, code, businessUnit);
    }
}