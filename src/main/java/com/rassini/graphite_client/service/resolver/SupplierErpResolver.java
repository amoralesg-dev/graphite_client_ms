package com.rassini.graphite_client.service.resolver;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SupplierErpResolver {

    /**
     * Resolves the effective ERP ID following strict business priority:
     * 1. dtoErpIdQad (Target ERP ID e.g. RASSINI_ERP_ID when payload provides it)
     * 2. persistedErpIdQad (Existing database record)
     * 3. legacyMappedErpId (Fallback: RASSINI_Legacy_QAD_ID, historical identity when neither dto nor persisted ERP is present)
     *
     * Throws IllegalStateException if all sources are absent/empty/blank.
     */
    public ErpResolutionResult resolveEffectiveErpId(
            String supplierCode,
            String legacyMappedErpId,
            String persistedErpIdQad,
            String dtoErpIdQad,
            String caller) {

        String supplier = (supplierCode != null) ? supplierCode : "";
        String legacyQadId = (legacyMappedErpId != null) ? legacyMappedErpId : "";
        String persistedErp = (persistedErpIdQad != null) ? persistedErpIdQad : "";
        String dtoErp = (dtoErpIdQad != null) ? dtoErpIdQad : "";
        String callerStr = (caller != null) ? caller : "";

        String resolvedErpId;
        ErpResolutionStrategy strategy;

        if (!dtoErp.isBlank()) {
            resolvedErpId = dtoErp.trim();
            strategy = ErpResolutionStrategy.DTO_ERP_ID_QAD;
        } else if (!persistedErp.isBlank()) {
            resolvedErpId = persistedErp.trim();
            strategy = ErpResolutionStrategy.PERSISTED_ERP_ID_QAD;
        } else if (!legacyQadId.isBlank()) {
            resolvedErpId = legacyQadId.trim();
            strategy = ErpResolutionStrategy.LEGACY_QAD_ID;
        } else {
            throw new IllegalStateException(
                    String.format("Unable to resolve effective ERP ID for supplier '%s' from caller '%s': all ERP sources are null or blank.",
                            supplier, callerStr));
        }

        log.info("[ERP-RESOLUTION] supplier={} caller={} dtoErpIdQad={} legacyMappedErpId={} persistedErpIdQad={} resolvedErpId={} strategy={}",
                supplier, callerStr, dtoErp, legacyQadId, persistedErp, resolvedErpId, strategy);

        return new ErpResolutionResult(resolvedErpId, strategy);
    }
}
