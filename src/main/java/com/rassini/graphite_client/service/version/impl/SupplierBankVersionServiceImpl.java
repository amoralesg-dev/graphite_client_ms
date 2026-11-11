package com.rassini.graphite_client.service.version.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;
import com.rassini.graphite_client.repository.SuppliersVerRepository;
import com.rassini.graphite_client.service.version.SupplierBankDataComparator;
import com.rassini.graphite_client.service.version.SupplierBankVersionService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierBankVersionServiceImpl implements SupplierBankVersionService {

    private final EntityManager entityManager;
    private final SuppliersVerRepository suppliersVerRepository;

    @Override
    public Optional<SuppliersVerEntity> lockAndSnapshot(SuppliersRowEntity current) {
        if (current == null || current.getId() == null) {
            return Optional.empty();
        }
        // SELECT ... FOR UPDATE: serializa el procesamiento concurrente del mismo registro
        // (y por lo tanto la asignación del número de versión) hasta el commit/rollback.
        // refresh con lock: además relee el estado confirmado más reciente, evitando un snapshot obsoleto
        // si otro proceso actualizó el registro después de que esta transacción lo leyó.
        entityManager.refresh(current, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(SuppliersVerEntity.snapshotOf(current));
    }

    @Override
    public Optional<SuppliersVerEntity> versionIfBankDataChanged(SuppliersVerEntity previous, SuppliersRowEntity updated) {
        if (previous == null || previous.getSupplierId() == null || updated == null) {
            return Optional.empty();
        }

        List<String> changed = SupplierBankDataComparator.changedFields(previous, updated);
        if (changed.isEmpty()) {
            log.info("[BANK-VERSION] supplierId={} supplier={} businessUnit={} sin cambios bancarios; no se genera version",
                    previous.getSupplierId(), previous.getSupplierCode(), previous.getBusinessUnitCode());
            return Optional.empty();
        }

        Map<String, String> before = SupplierBankDataComparator.bankData(previous);
        Map<String, String> after = SupplierBankDataComparator.bankData(updated);
        for (String field : changed) {
            log.info("[BANK-VERSION][CHANGE] supplierId={} supplier={} field={} before={} after={}",
                    previous.getSupplierId(), previous.getSupplierCode(), field,
                    SupplierBankDataComparator.safeForLog(field, before.get(field)),
                    SupplierBankDataComparator.safeForLog(field, after.get(field)));
        }

        int nextVersion = suppliersVerRepository.findMaxVersionNumber(previous.getSupplierId()) + 1;
        previous.setVersionNumber(nextVersion);
        previous.setChangedFields(String.join(",", changed));
        previous.setVersionSource(SuppliersVerEntity.SOURCE_GRAPHITE);
        previous.setCreatedAt(LocalDateTime.now());

        try {
            SuppliersVerEntity saved = suppliersVerRepository.saveAndFlush(previous);
            log.info("[BANK-VERSION][SAVED] supplierId={} supplier={} businessUnit={} version={} changedFields={}",
                    previous.getSupplierId(), previous.getSupplierCode(), previous.getBusinessUnitCode(),
                    nextVersion, previous.getChangedFields());
            return Optional.ofNullable(saved);
        } catch (RuntimeException e) {
            log.error("[BANK-VERSION][ERROR] supplierId={} supplier={} version={} no se pudo guardar la versión histórica: {}",
                    previous.getSupplierId(), previous.getSupplierCode(), nextVersion, e.getClass().getSimpleName());
            throw e;
        }
    }
}
