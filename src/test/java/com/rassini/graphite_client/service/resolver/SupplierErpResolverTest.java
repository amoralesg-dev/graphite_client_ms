package com.rassini.graphite_client.service.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SupplierErpResolverTest {

    private SupplierErpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SupplierErpResolver();
    }

    @Test
    @DisplayName("Caso 1: Proveedor con nuevo ERP en DTO (RASSINI_ERP_ID) -> Gana dtoErpIdQad sobre legacy y BD para migración")
    void testDtoErpIdQadPrioritizedForMigration() {
        String supplierCode = "MX120796";
        String legacyQadId = "COCHGMER";
        String persistedErpIdQad = "COCHGMER";
        String dtoErpIdQad = "60003031";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                legacyQadId,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("60003031", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.DTO_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 2: Proveedor existente en BD sin dtoErpIdQad -> Gana persistedErpIdQad")
    void testPersistedSupplierWithoutDtoErpPrioritizesPersistedErp() {
        String supplierCode = "NN732811";
        String legacyQadId = "COCHGMER";
        String persistedErpIdQad = "10002497";
        String dtoErpIdQad = null;

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                legacyQadId,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("10002497", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.PERSISTED_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 2b: Proveedor sin dtoErpIdQad ni persistedErpIdQad -> Fallback a legacyQadId (RASSINI_Legacy_QAD_ID)")
    void testFallbackToLegacyMappedErpWhenNoDtoNorPersisted() {
        String supplierCode = "LEGACY_SUPP";
        String legacyQadId = "COCHGMER";
        String persistedErpIdQad = null;
        String dtoErpIdQad = "";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                legacyQadId,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("COCHGMER", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.LEGACY_QAD_ID, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 3: Proveedor nuevo sin registro en BD ni legacyQadId -> Utiliza dtoErpIdQad")
    void testNewSupplierUsesDtoErpIdQad() {
        String supplierCode = "NEW_SUPP_3";
        String legacyQadId = "";
        String persistedErpIdQad = null;
        String dtoErpIdQad = "60003094";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                legacyQadId,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("60003094", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.DTO_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 4: Manejo de valores vacíos y espacios en blanco respetando jerarquía")
    void testHandlesBlanksAndWhitespacesCorrectly() {
        String supplierCode = "SUPP_WHITESPACE";
        String legacyQadId = "   ";
        String persistedErpIdQad = "   PERSISTED_999   ";
        String dtoErpIdQad = "  DTO_FALLBACK  ";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                legacyQadId,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("DTO_FALLBACK", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.DTO_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 5: Todos los valores ausentes o vacíos -> Lanza IllegalStateException controlado")
    void testAllAbsentThrowsIllegalStateException() {
        String supplierCode = "SUPP_NO_ERP";
        String legacyQadId = " ";
        String persistedErpIdQad = "";
        String dtoErpIdQad = null;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolver.resolveEffectiveErpId(
                        supplierCode,
                        legacyQadId,
                        persistedErpIdQad,
                        dtoErpIdQad,
                        "TEST_CALLER"
                )
        );

        assertNotNull(ex.getMessage());
    }
}
