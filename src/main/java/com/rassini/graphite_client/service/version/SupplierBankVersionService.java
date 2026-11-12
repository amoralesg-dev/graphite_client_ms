package com.rassini.graphite_client.service.version;

import java.util.Optional;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;

/**
 * Versionado de la información bancaria de suppliers.
 * Debe invocarse dentro de la transacción que actualiza suppliers.
 */
public interface SupplierBankVersionService {

    /**
     * Bloquea el registro existente y toma la fotografía completa ANTES de modificarlo.
     * Devuelve empty para proveedores nuevos (sin id).
     */
    Optional<SuppliersVerEntity> lockAndSnapshot(SuppliersRowEntity current);

    /**
     * Si cambió algún campo bancario entre la fotografía y el registro actualizado,
     * persiste la fotografía en suppliers_ver con el siguiente número de versión.
     */
    Optional<SuppliersVerEntity> versionIfBankDataChanged(SuppliersVerEntity previous, SuppliersRowEntity updated);
}
