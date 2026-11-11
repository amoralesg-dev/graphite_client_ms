package com.rassini.graphite_client.service.version;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;
import com.rassini.graphite_client.repository.SuppliersVerRepository;
import com.rassini.graphite_client.service.version.impl.SupplierBankVersionServiceImpl;

@ExtendWith(MockitoExtension.class)
class SupplierBankVersionServiceImplTest {

    @Mock jakarta.persistence.EntityManager entityManager;
    @Mock SuppliersVerRepository suppliersVerRepository;
    @InjectMocks SupplierBankVersionServiceImpl service;

    @Test
    void newSupplier_noSnapshotNoVersion() {
        SuppliersRowEntity nuevo = new SuppliersRowEntity();
        assertTrue(service.lockAndSnapshot(nuevo).isEmpty());
        verifyNoInteractions(entityManager, suppliersVerRepository);
    }

    @Test
    void existingSupplier_isLockedBeforeSnapshot() {
        SuppliersRowEntity r = SupplierBankDataComparatorTest.row();
        SuppliersVerEntity snap = service.lockAndSnapshot(r).orElseThrow();
        assertEquals(10L, snap.getSupplierId());
        verify(entityManager).refresh(r, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void noBankChange_noInsert() {
        SuppliersRowEntity r = SupplierBankDataComparatorTest.row();
        assertTrue(service.versionIfBankDataChanged(SuppliersVerEntity.snapshotOf(r), r).isEmpty());
        verifyNoInteractions(suppliersVerRepository);
    }

    @Test
    void firstBankChange_version1_withPreviousValues() {
        SuppliersRowEntity r = SupplierBankDataComparatorTest.row();
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(r);
        r.setRoutingCodeSwift("BNMXMXMM");
        when(suppliersVerRepository.findMaxVersionNumber(10L)).thenReturn(0);
        when(suppliersVerRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SuppliersVerEntity v = service.versionIfBankDataChanged(snap, r).orElseThrow();

        assertEquals(1, v.getVersionNumber());
        assertEquals("BCMRMXMM", v.getRoutingCodeSwift()); // valor ANTERIOR
        assertEquals("routing_code_swift", v.getChangedFields());
        assertEquals("GRAPHITE", v.getVersionSource());
        assertNotNull(v.getCreatedAt());
    }

    @Test
    void secondBankChange_version2() {
        SuppliersRowEntity r = SupplierBankDataComparatorTest.row();
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(r);
        r.setBankCurrency("USD");
        when(suppliersVerRepository.findMaxVersionNumber(10L)).thenReturn(1);
        when(suppliersVerRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(2, service.versionIfBankDataChanged(snap, r).orElseThrow().getVersionNumber());
    }

    @Test
    void saveFailure_propagates() {
        SuppliersRowEntity r = SupplierBankDataComparatorTest.row();
        SuppliersVerEntity snap = SuppliersVerEntity.snapshotOf(r);
        r.setBankCountry("US");
        when(suppliersVerRepository.findMaxVersionNumber(10L)).thenReturn(0);
        when(suppliersVerRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("db down"));
        assertThrows(IllegalStateException.class, () -> service.versionIfBankDataChanged(snap, r));
    }
}
