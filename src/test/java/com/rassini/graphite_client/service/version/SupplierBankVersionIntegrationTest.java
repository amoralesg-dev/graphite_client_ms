package com.rassini.graphite_client.service.version;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.repository.SuppliersVerRepository;
import com.rassini.graphite_client.service.version.impl.SupplierBankVersionServiceImpl;

/**
 * Integración JPA (H2 en memoria, esquema generado desde las entidades) del versionado bancario:
 * transacción, rollback, historial incremental, restricción única y concurrencia.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.jakarta.persistence.lock.timeout=10000"
})
@Import(SupplierBankVersionServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankver;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "GRAPHITE_CLIENT_ID=test", "GRAPHITE_CLIENT_SECRET=test"
})
class SupplierBankVersionIntegrationTest {

    @Autowired SuppliersRowRepository rowRepo;
    @Autowired SuppliersVerRepository verRepo;
    @Autowired SupplierBankVersionService service;
    @Autowired PlatformTransactionManager txManager;

    TransactionTemplate tx;
    Long supplierId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        verRepo.deleteAll();
        rowRepo.deleteAll();
        SuppliersRowEntity r = new SuppliersRowEntity();
        r.setBusinessUnitCode("1000");
        r.setSupplierCode("SUP1");
        r.setErpIdQad("ERP1");
        r.setSupplierName("PROVEEDOR UNO");
        r.setAccountNumber("012345678901234567");
        r.setRoutingCodeSwift("BCMRMXMM");
        r.setBankCurrency("MXN");
        r.setXmlStatus(XmlStatus.GENERATED);
        supplierId = rowRepo.save(r).getId();
    }

    /** Simula el flujo del mapper: lock+snapshot, modificación, versionado y update en UNA transacción. */
    private void upsert(java.util.function.Consumer<SuppliersRowEntity> change) {
        tx.executeWithoutResult(s -> {
            SuppliersRowEntity row = rowRepo.findById(supplierId).orElseThrow();
            SuppliersVerEntity snap = service.lockAndSnapshot(row).orElseThrow();
            change.accept(row);
            boolean versioned = service.versionIfBankDataChanged(snap, row).isPresent();
            if (versioned) rowRepo.saveAndFlush(row); else rowRepo.save(row);
        });
    }

    @Test
    void noChanges_and_nonBankChanges_doNotCreateVersions() {
        upsert(r -> {});
        upsert(r -> r.setSupplierName("NOMBRE NUEVO"));
        assertEquals(0, verRepo.count());
        assertEquals("NOMBRE NUEVO", rowRepo.findById(supplierId).orElseThrow().getSupplierName());
    }

    @Test
    void firstAndSecondBankChange_keepHistory() {
        upsert(r -> r.setRoutingCodeSwift("BNMXMXMM"));
        upsert(r -> r.setBankCurrency("USD"));

        List<SuppliersVerEntity> versions = verRepo.findBySupplierIdOrderByVersionNumberAsc(supplierId);
        assertEquals(2, versions.size());
        assertEquals(1, versions.get(0).getVersionNumber());
        assertEquals("BCMRMXMM", versions.get(0).getRoutingCodeSwift());
        assertEquals("routing_code_swift", versions.get(0).getChangedFields());
        assertEquals(2, versions.get(1).getVersionNumber());
        assertEquals("BNMXMXMM", versions.get(1).getRoutingCodeSwift());
        assertEquals("MXN", versions.get(1).getBankCurrency());
        SuppliersRowEntity current = rowRepo.findById(supplierId).orElseThrow();
        assertEquals("USD", current.getBankCurrency());
        assertEquals("BNMXMXMM", current.getRoutingCodeSwift());
    }

    @Test
    void endToEndScenario_citibankToJPMorgan_createsVersionOne() {
        // 1. Estado inicial simulado: Citibank / 9988776655 / 021000089 sin historial previo
        SuppliersRowEntity row = rowRepo.findById(supplierId).orElseThrow();
        row.setBeneficiaryBankName("Citibank, N.A.");
        row.setAccountNumber("9988776655");
        row.setRoutingCodeAba("021000089");
        rowRepo.saveAndFlush(row);
        assertEquals(0, verRepo.count());

        // 2. Llegada de payload Graphite: JPMorgan / 10382910 / 021000021
        upsert(r -> {
            r.setBeneficiaryBankName("JPMorgan Chase Bank, NA");
            r.setAccountNumber("10382910");
            r.setRoutingCodeAba("021000021");
        });

        // 3. Resultado esperado:
        // - suppliers_ver contiene VERSION 1 con Citibank / 9988776655 / 021000089
        // - suppliers contiene JPMorgan / 10382910 / 021000021
        // - changed_fields contiene los tres campos modificados
        List<SuppliersVerEntity> versions = verRepo.findBySupplierIdOrderByVersionNumberAsc(supplierId);
        assertEquals(1, versions.size());
        SuppliersVerEntity v1 = versions.get(0);
        assertEquals(1, v1.getVersionNumber());
        assertEquals("Citibank, N.A.", v1.getBeneficiaryBankName());
        assertEquals("9988776655", v1.getAccountNumber());
        assertEquals("021000089", v1.getRoutingCodeAba());
        assertEquals("GRAPHITE", v1.getVersionSource());
        assertTrue(v1.getChangedFields().contains("beneficiary_bank_name"));
        assertTrue(v1.getChangedFields().contains("account_number"));
        assertTrue(v1.getChangedFields().contains("routing_code_aba"));

        SuppliersRowEntity updatedRow = rowRepo.findById(supplierId).orElseThrow();
        assertEquals("JPMorgan Chase Bank, NA", updatedRow.getBeneficiaryBankName());
        assertEquals("10382910", updatedRow.getAccountNumber());
        assertEquals("021000021", updatedRow.getRoutingCodeAba());
    }

    @Test
    void supplierUpdateFailure_rollsBack_noOrphanVersion() {
        assertThrows(Exception.class, () -> upsert(r -> {
            r.setRoutingCodeSwift("BNMXMXMM");
            r.setBusinessUnitCode(null); // NOT NULL -> falla el UPDATE de suppliers
        }));
        assertEquals(0, verRepo.count());
        SuppliersRowEntity current = rowRepo.findById(supplierId).orElseThrow();
        assertEquals("BCMRMXMM", current.getRoutingCodeSwift());
        assertEquals("1000", current.getBusinessUnitCode());
    }

    @Test
    void versionInsertFailure_rollsBack_supplierNotUpdated() {
        // Versión 1 ya ocupada por otra vía -> el INSERT viola uk_suppliers_ver_supplier_version
        // si el MAX no se recalcula; forzamos la falla con un snapshot sin business_unit_code (NOT NULL).
        assertThrows(Exception.class, () -> tx.executeWithoutResult(s -> {
            SuppliersRowEntity row = rowRepo.findById(supplierId).orElseThrow();
            SuppliersVerEntity snap = service.lockAndSnapshot(row).orElseThrow();
            snap.setBusinessUnitCode(null);
            row.setRoutingCodeSwift("BNMXMXMM");
            service.versionIfBankDataChanged(snap, row);
            rowRepo.saveAndFlush(row);
        }));
        assertEquals(0, verRepo.count());
        assertEquals("BCMRMXMM", rowRepo.findById(supplierId).orElseThrow().getRoutingCodeSwift());
    }

    @Test
    void uniqueConstraint_supplierIdVersionNumber() {
        upsert(r -> r.setRoutingCodeSwift("BNMXMXMM"));
        SuppliersVerEntity dup = SuppliersVerEntity.snapshotOf(rowRepo.findById(supplierId).orElseThrow());
        dup.setVersionNumber(1);
        dup.setCreatedAt(java.time.LocalDateTime.now());
        assertThrows(Exception.class, () -> verRepo.saveAndFlush(dup));
    }

    @Test
    void concurrentBankChanges_getDistinctVersionNumbers() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> a = () -> { start.await(); upsert(r -> r.setRoutingCodeSwift("AAAAMXMM")); return null; };
        Callable<Void> b = () -> { start.await(); upsert(r -> r.setBankCurrency("EUR")); return null; };
        Future<Void> fa = pool.submit(a);
        Future<Void> fb = pool.submit(b);
        start.countDown();
        fa.get();
        fb.get();
        pool.shutdown();

        List<Integer> numbers = verRepo.findBySupplierIdOrderByVersionNumberAsc(supplierId)
                .stream().map(SuppliersVerEntity::getVersionNumber).toList();
        assertEquals(List.of(1, 2), numbers);
    }
}
