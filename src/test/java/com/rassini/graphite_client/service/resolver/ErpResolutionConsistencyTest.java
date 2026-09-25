package com.rassini.graphite_client.service.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.sync.impl.IntegrityServiceImpl;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.impl.SupplierJpaMapperImpl;

@ExtendWith(MockitoExtension.class)
public class ErpResolutionConsistencyTest {

    @Mock
    private SuppliersRowRepository suppliersRowRepository;

    @Mock
    private CatalogService catalogService;

    @Spy
    private SupplierErpResolver supplierErpResolver = new SupplierErpResolver();

    @InjectMocks
    private SupplierJpaMapperImpl supplierJpaMapper;

    @InjectMocks
    private IntegrityServiceImpl integrityService;

    private final String supplierCode = "SUPP_TEST_100";

    @Test
    @DisplayName("Garantiza consistencia absoluta: Legacy Supplier resuelve exactamente el mismo ERP en IntegrityService y en supplier_code_dis_integrity")
    void testLegacySupplierErpResolutionConsistency() {
        String legacyStatusErp = "10002497";
        String dtoErp = "60003094";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierCode);
        dto.setStatusERPGraphite(legacyStatusErp);
        dto.setErpIdQad(dtoErp);

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId("0111");
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber("12345678");
        banks.add(bank);
        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        SuppliersRowEntity existingRow = new SuppliersRowEntity();
        existingRow.setId(10L);
        existingRow.setSupplierCode(supplierCode);
        existingRow.setBusinessUnitCode("0111");
        existingRow.setAccountNumber("12345678");
        existingRow.setErpIdQad("10002497"); // persisted erp

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(supplierCode), eq("0111"), eq("12345678")))
                .thenReturn(Optional.of(existingRow));
        when(suppliersRowRepository.findFirstBySupplierCodeAndAccountNumber(eq(supplierCode), eq("12345678")))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.countDistinctAccountsBySupplierCode(eq(supplierCode)))
                .thenReturn(0L);
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 1. Ejecutar upsertSuppliersRows (cálculo de supplier_code_dis_integrity)
        supplierJpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> savedRowCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedRowCaptor.capture());
        SuppliersRowEntity savedRow = savedRowCaptor.getValue();

        assertEquals(legacyStatusErp, savedRow.getErpIdQad(), "erpIdQad debe coincidir con statusERPGraphite");
        assertEquals(legacyStatusErp, savedRow.getSupplierCodeDisIntegrity(), "supplierCodeDisIntegrity debe derivar del ERP resuelto por SupplierErpResolver");

        // 2. Ejecutar createFileSupplierSync (generación y consulta de integridad)
        when(suppliersRowRepository.findBySupplierCodeOrderByBusinessUnitCodeAsc(eq(supplierCode)))
                .thenReturn(List.of(savedRow));
        when(suppliersRowRepository.findDistinctAccountsBySupplierCodeExact(eq(supplierCode)))
                .thenReturn(List.of(savedRow));

        integrityService.createFileSupplierSync(dto);

        // Ambas invocaciones llamaron al resolver compartido
        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(supplierCode),
                eq(legacyStatusErp),
                eq("10002497"),
                eq(dtoErp),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(supplierCode),
                eq(legacyStatusErp),
                eq("10002497"),
                eq(dtoErp),
                eq("INTEGRITY_SERVICE")
        );
    }

    @Test
    @DisplayName("Caso Multiplanta: cada planta obtiene su propio persistedErpIdQad y no toma el de otra BU")
    void testMultiPlantDoesNotTakeErpFromAnotherBusinessUnit() {
        String multiSupplierCode = "SUPP_MULTI";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(multiSupplierCode);
        dto.setStatusERPGraphite(null); // Sin statusERPGraphite para probar que gana persistedErpIdQad de su propia BU
        dto.setErpIdQad("60003094");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();

        // BU 99
        GraphiteSupplierDto.ErpRecord erp99 = new GraphiteSupplierDto.ErpRecord();
        erp99.setRassiniErpEntityId("99");
        List<GraphiteSupplierDto.Bank> banks99 = new ArrayList<>();
        GraphiteSupplierDto.Bank bank99 = new GraphiteSupplierDto.Bank();
        bank99.setBankAccountNumber("11119999");
        banks99.add(bank99);
        erp99.setErpBankList(banks99);
        erpRecords.add(erp99);

        // BU 0111
        GraphiteSupplierDto.ErpRecord erp0111 = new GraphiteSupplierDto.ErpRecord();
        erp0111.setRassiniErpEntityId("0111");
        List<GraphiteSupplierDto.Bank> banks0111 = new ArrayList<>();
        GraphiteSupplierDto.Bank bank0111 = new GraphiteSupplierDto.Bank();
        bank0111.setBankAccountNumber("22220111");
        banks0111.add(bank0111);
        erp0111.setErpBankList(banks0111);
        erpRecords.add(erp0111);

        dto.setErpRecords(erpRecords);

        // Fila persistida para BU 99
        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setId(101L);
        row99.setSupplierCode(multiSupplierCode);
        row99.setBusinessUnitCode("99");
        row99.setAccountNumber("11119999");
        row99.setErpIdQad("ERP_99");

        // Fila persistida para BU 0111
        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setId(102L);
        row0111.setSupplierCode(multiSupplierCode);
        row0111.setBusinessUnitCode("0111");
        row0111.setAccountNumber("22220111");
        row0111.setErpIdQad("ERP_0111");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(multiSupplierCode), eq("99"), eq("11119999")))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(multiSupplierCode), eq("0111"), eq("22220111")))
                .thenReturn(Optional.of(row0111));

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        supplierJpaMapper.upsertSuppliersRows(dto);

        // Verificar que para BU 99 se resolvió con persistedErpIdQad = ERP_99
        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(multiSupplierCode),
                isNull(),
                eq("ERP_99"),
                eq("60003094"),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        // Verificar que para BU 0111 se resolvió con persistedErpIdQad = ERP_0111 (NO ERP_99)
        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(multiSupplierCode),
                isNull(),
                eq("ERP_0111"),
                eq("60003094"),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        assertEquals("ERP_99", row99.getErpIdQad());
        assertEquals("ERP_0111", row0111.getErpIdQad());
    }
}
