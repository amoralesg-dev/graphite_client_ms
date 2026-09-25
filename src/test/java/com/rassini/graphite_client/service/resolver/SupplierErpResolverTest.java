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
    @DisplayName("Caso 1: Proveedor Legacy homologado -> Gana statusERPGraphite sobre BD y DTO")
    void testLegacySupplierPrioritizesStatusErpGraphite() {
        String supplierCode = "LEGACY_SUPP_1";
        String statusErpGraphite = "10002497";
        String persistedErpIdQad = "NN732811";
        String dtoErpIdQad = "60003094";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                statusErpGraphite,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("10002497", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.STATUS_ERP_GRAPHITE, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 2: Proveedor existente en BD sin statusERPGraphite -> Gana persistedErpIdQad sobre DTO")
    void testPersistedSupplierWithoutStatusErpPrioritizesPersistedErp() {
        String supplierCode = "EXISTING_SUPP_2";
        String statusErpGraphite = null;
        String persistedErpIdQad = "NN732811";
        String dtoErpIdQad = "60003094";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                statusErpGraphite,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("NN732811", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.PERSISTED_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 3: Proveedor nuevo sin registro en BD ni statusERPGraphite -> Utiliza dtoErpIdQad")
    void testNewSupplierUsesDtoErpIdQad() {
        String supplierCode = "NEW_SUPP_3";
        String statusErpGraphite = "";
        String persistedErpIdQad = null;
        String dtoErpIdQad = "60003094";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                statusErpGraphite,
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
        String statusErpGraphite = "   ";
        String persistedErpIdQad = "   PERSISTED_999   ";
        String dtoErpIdQad = "DTO_FALLBACK";

        ErpResolutionResult result = resolver.resolveEffectiveErpId(
                supplierCode,
                statusErpGraphite,
                persistedErpIdQad,
                dtoErpIdQad,
                "TEST_CALLER"
        );

        assertNotNull(result);
        assertEquals("PERSISTED_999", result.getResolvedErpId());
        assertEquals(ErpResolutionStrategy.PERSISTED_ERP_ID_QAD, result.getStrategy());
    }

    @Test
    @DisplayName("Caso 5: Todos los valores ausentes o vacíos -> Lanza IllegalStateException controlado")
    void testAllAbsentThrowsIllegalStateException() {
        String supplierCode = "SUPP_NO_ERP";
        String statusErpGraphite = " ";
        String persistedErpIdQad = "";
        String dtoErpIdQad = null;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                resolver.resolveEffectiveErpId(
                        supplierCode,
                        statusErpGraphite,
                        persistedErpIdQad,
                        dtoErpIdQad,
                        "TEST_CALLER"
                )
        );

        assertNotNull(ex.getMessage());
    }
}
