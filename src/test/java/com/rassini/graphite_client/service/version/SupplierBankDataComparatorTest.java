package com.rassini.graphite_client.service.version;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;

class SupplierBankDataComparatorTest {

    static SuppliersRowEntity row() {
        SuppliersRowEntity r = new SuppliersRowEntity();
        r.setId(10L);
        r.setBusinessUnitCode("1000");
        r.setSupplierCode("SUP1");
        r.setSupplierName("PROVEEDOR UNO");
        r.setAccountNumber("012345678901234567");
        r.setBeneficiaryBankName("BBVA");
        r.setBankCurrency("MXN");
        r.setBankCountry("MX");
        r.setRoutingCodeSwift("BCMRMXMM");
        r.setRoutingCodeAba(null);
        r.setIntermediaryBankName("");
        return r;
    }

    @Test
    @DisplayName("Sin cambios: snapshot idéntico no produce diferencias")
    void identical() {
        SuppliersRowEntity r = row();
        assertTrue(SupplierBankDataComparator.changedFields(SuppliersVerEntity.snapshotOf(r), r).isEmpty());
    }

    @Test
    @DisplayName("null, cadena vacía, blancos y espacios extremos no generan versiones falsas")
    void nullEmptyAndTrim() {
        SuppliersRowEntity before = row();
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(before);
        SuppliersRowEntity after = row();
        after.setRoutingCodeAba("");            // null -> ""
        after.setIntermediaryBankName(null);    // "" -> null
        after.setIntermediaryAccount("   ");    // null -> blancos
        after.setBeneficiaryBankName("  BBVA ");// espacios extremos
        assertTrue(SupplierBankDataComparator.changedFields(snap, after).isEmpty());
    }

    @Test
    @DisplayName("Solo cambios no bancarios: no hay diferencias bancarias")
    void nonBankChanges() {
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(row());
        SuppliersRowEntity after = row();
        after.setSupplierName("OTRO NOMBRE");
        after.setStreetName("NUEVA CALLE");
        after.setContactEmail("x@y.com");
        after.setStatusIntegrity("M");
        assertTrue(SupplierBankDataComparator.changedFields(snap, after).isEmpty());
    }

    @Test
    @DisplayName("Detecta exactamente qué campos bancarios cambiaron")
    void detectsChangedFields() {
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(row());
        SuppliersRowEntity after = row();
        after.setRoutingCodeSwift("BNMXMXMM");
        after.setBankCurrency("USD");
        after.setIntermediaryRoutingCodeAba("021000021");
        assertEquals(List.of("bank_currency", "routing_code_swift", "intermediary_routing_code_aba"),
                SupplierBankDataComparator.changedFields(snap, after));
    }

    @Test
    @DisplayName("Diferencias de mayúsculas sí se consideran cambio (no se inventa normalización)")
    void caseIsSignificant() {
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(row());
        SuppliersRowEntity after = row();
        after.setBeneficiaryBankName("bbva");
        assertEquals(List.of("beneficiary_bank_name"), SupplierBankDataComparator.changedFields(snap, after));
    }

    @Test
    @DisplayName("Enmascaramiento conserva solo los últimos 4 caracteres")
    void masking() {
        assertEquals("**************4567", SupplierBankDataComparator.mask("012345678901234567"));
        assertEquals("****", SupplierBankDataComparator.mask("1234"));
        assertEquals("N/A", SupplierBankDataComparator.mask(null));
        assertEquals("*****0021", SupplierBankDataComparator.safeForLog("routing_code_aba", "021000021"));
        assertEquals("BCMRMXMM", SupplierBankDataComparator.safeForLog("routing_code_swift", "BCMRMXMM"));
    }
}
