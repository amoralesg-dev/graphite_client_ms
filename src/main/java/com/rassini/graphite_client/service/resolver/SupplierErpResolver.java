package com.rassini.graphite_client.service.resolver;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SupplierErpResolver {

    /**
     * Resolves the effective ERP ID following strict business priority:
     * 1. statusERPGraphite (Legacy mapped supplier)
     * 2. persistedErpIdQad (Existing database record)
     * 3. dtoErpIdQad (New supplier payload default)
     *
     * Throws IllegalStateException if all sources are absent/empty/blank.
     */
    public ErpResolutionResult resolveEffectiveErpId(
            String supplierCode,
            String statusERPGraphite,
            String persistedErpIdQad,
            String dtoErpIdQad,
            String caller) {

        String supplier = (supplierCode != null) ? supplierCode : "";
        String statusErp = (statusERPGraphite != null) ? statusERPGraphite : "";
        String persistedErp = (persistedErpIdQad != null) ? persistedErpIdQad : "";
        String dtoErp = (dtoErpIdQad != null) ? dtoErpIdQad : "";
        String callerStr = (caller != null) ? caller : "";

        String resolvedErpId;
        ErpResolutionStrategy strategy;

        if (!statusErp.isBlank()) {
            resolvedErpId = statusErp.trim();
            strategy = ErpResolutionStrategy.STATUS_ERP_GRAPHITE;
        } else if (!persistedErp.isBlank()) {
            resolvedErpId = persistedErp.trim();
            strategy = ErpResolutionStrategy.PERSISTED_ERP_ID_QAD;
        } else if (!dtoErp.isBlank()) {
            resolvedErpId = dtoErp.trim();
            strategy = ErpResolutionStrategy.DTO_ERP_ID_QAD;
        } else {
            throw new IllegalStateException(
                    String.format("Unable to resolve effective ERP ID for supplier '%s' from caller '%s': all ERP sources are null or blank.",
                            supplier, callerStr));
        }

        log.info("[ERP-RESOLUTION] supplier={} caller={} dtoErpIdQad={} statusERPGraphite={} persistedErpIdQad={} resolvedErpId={} strategy={}",
                supplier, callerStr, dtoErp, statusErp, persistedErp, resolvedErpId, strategy);

        return new ErpResolutionResult(resolvedErpId, strategy);
    }
}
