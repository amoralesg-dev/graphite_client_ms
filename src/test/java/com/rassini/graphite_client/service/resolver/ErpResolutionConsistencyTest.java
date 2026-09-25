package com.rassini.graphite_client.service.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        existingRow.setErpIdQad("NN732811"); // persisted old erp

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
                any(),
                eq(dtoErp),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(supplierCode),
                eq(legacyStatusErp),
                any(),
                eq(dtoErp),
                eq("INTEGRITY_SERVICE")
        );
    }
}
