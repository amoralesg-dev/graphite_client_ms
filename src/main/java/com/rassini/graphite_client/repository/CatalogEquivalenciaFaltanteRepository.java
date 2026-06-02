package com.rassini.graphite_client.repository;

import com.rassini.graphite_client.entity.CatalogEquivalenciaFaltanteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogEquivalenciaFaltanteRepository
        extends JpaRepository<CatalogEquivalenciaFaltanteEntity, Long> {

    Optional<CatalogEquivalenciaFaltanteEntity>
    findByIdCatalogoAndCodeAndBusinessUnitAndNotificado(
            String idCatalogo,
            String code,
            String businessUnit,
            Boolean notificado
    );

    List<CatalogEquivalenciaFaltanteEntity>
    findByNotificadoFalse();
}