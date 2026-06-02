package com.rassini.graphite_client.service.catalog.impl;

import com.rassini.graphite_client.entity.CatalogEquivalenciaFaltanteEntity;
import com.rassini.graphite_client.repository.CatalogEquivalenciaFaltanteRepository;
import com.rassini.graphite_client.service.catalog.CatalogEquivalenciaFaltanteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogEquivalenciaFaltanteServiceImpl
        implements CatalogEquivalenciaFaltanteService {

    private final CatalogEquivalenciaFaltanteRepository repository;

    @Override
    public void registrar(
            String idCatalogo,
            String code,
            String businessUnit) {

        repository
                .findByIdCatalogoAndCodeAndBusinessUnitAndNotificado(
                        idCatalogo,
                        code,
                        businessUnit,
                        false)
                .ifPresentOrElse(entity -> {

                    entity.setTotalOcurrencias(
                            entity.getTotalOcurrencias() + 1);

                    repository.save(entity);

                }, () -> {

                    CatalogEquivalenciaFaltanteEntity entity =
                            new CatalogEquivalenciaFaltanteEntity();

                    entity.setIdCatalogo(idCatalogo);
                    entity.setCode(code);
                    entity.setBusinessUnit(businessUnit);
                    entity.setFechaDeteccion(LocalDateTime.now());
                    entity.setTotalOcurrencias(1);
                    entity.setNotificado(false);

                    repository.save(entity);
                });
    }
}