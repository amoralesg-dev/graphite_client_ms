package com.rassini.graphite_client.service.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        String legacyQadId = "10002497";
        String dtoErp = "60003094";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierCode);
        dto.setLegacyMappedErpId(legacyQadId);
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

        assertEquals(dtoErp, savedRow.getErpIdQad(), "erpIdQad debe actualizarse con el nuevo ERP provisto en dtoErpIdQad");
        assertEquals(dtoErp, savedRow.getSupplierCodeDisIntegrity(), "supplierCodeDisIntegrity debe derivar del ERP resuelto por SupplierErpResolver");

        // 2. Ejecutar createFileSupplierSync (generación y consulta de integridad)
        when(suppliersRowRepository.findBySupplierCodeOrderByBusinessUnitCodeAsc(eq(supplierCode)))
                .thenReturn(List.of(savedRow));
        when(suppliersRowRepository.findDistinctAccountsBySupplierCodeExact(eq(supplierCode)))
                .thenReturn(List.of(savedRow));

        integrityService.createFileSupplierSync(dto);

        // Ambas invocaciones llamaron al resolver compartido
        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(supplierCode),
                eq(legacyQadId),
                eq("10002497"),
                eq(dtoErp),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(supplierCode),
                eq(legacyQadId),
                eq(dtoErp), // Tras guardar, el persistedErpIdQad es 60003094
                eq(dtoErp),
                eq("INTEGRITY_SERVICE")
        );
    }

    @Test
    @DisplayName("Caso Multiplanta sin dtoErpIdQad: cada planta obtiene su propio persistedErpIdQad y no toma el de otra BU")
    void testMultiPlantDoesNotTakeErpFromAnotherBusinessUnit() {
        String multiSupplierCode = "SUPP_MULTI";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(multiSupplierCode);
        dto.setLegacyMappedErpId(null);
        dto.setErpIdQad(null); // Sin dtoErpIdQad para probar que gana persistedErpIdQad de su propia BU

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
                isNull(),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        // Verificar que para BU 0111 se resolvió con persistedErpIdQad = ERP_0111 (NO ERP_99)
        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(multiSupplierCode),
                isNull(),
                eq("ERP_0111"),
                isNull(),
                eq("SUPPLIER_CODE_DIS_INTEGRITY")
        );

        assertEquals("ERP_99", row99.getErpIdQad());
        assertEquals("ERP_0111", row0111.getErpIdQad());
    }

    @Test
    @DisplayName("Incidente Producción COCHGMER: Proveedor Legacy con supplier_code persistido = COCHGMER se encuentra por fallback evitando INSERT duplicado, realizando UPDATE y migrando erp_id_qad a RASSINI_ERP_ID (60003031)")
    void testLegacySupplierCochgmerLookupAvoidsDuplicateInsert() {
        // Simulación exacta del caso de producción COCHGMER:
        // Entity_Public_Id = "MX120796"
        // RASSINI_Legacy_QAD_ID = "COCHGMER"
        // RASSINI_ERP_ID = "60003031"
        // BU = "09"
        // Account = "012180004515572542"
        String graphiteSupplierId = "MX120796";
        String legacyCode = "COCHGMER";
        String bu = "09";
        String account = "012180004515572542";
        String dtoErp = "60003031";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(graphiteSupplierId);
        dto.setLegacyMappedErpId(legacyCode);
        dto.setErpIdQad(dtoErp);

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber(account);
        banks.add(bank);
        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setSupplierIsLegacy("y");
        dto.setErpRecords(erpRecords);

        // En BD el registro histórico real tiene erp_id_qad = "COCHGMER" y supplier_code_dis_integrity = "COCHGMER1"
        SuppliersRowEntity existingLegacyRow = new SuppliersRowEntity();
        existingLegacyRow.setId(10391L);
        existingLegacyRow.setSupplierCode(legacyCode);
        existingLegacyRow.setBusinessUnitCode(bu);
        existingLegacyRow.setAccountNumber(account);
        existingLegacyRow.setErpIdQad(legacyCode);
        existingLegacyRow.setSupplierCodeDisIntegrity("COCHGMER1");

        // 1. Búsqueda por Entity_Public_Id ("MX120796") no encuentra nada
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(graphiteSupplierId), eq(bu), eq(account)))
                .thenReturn(Optional.empty());

        // 2. Búsqueda exacta de fallback por erp_id_qad ("COCHGMER"), BU y número de cuenta encuentra la fila histórica
        when(suppliersRowRepository.findByErpIdQadAndBusinessUnitCodeAndAccountNumber(eq(legacyCode), eq(bu), eq(account)))
                .thenReturn(Optional.of(existingLegacyRow));

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Ejecutar upsert
        supplierJpaMapper.upsertSuppliersRows(dto);

        // Verificar que se invocó save sobre el objeto con ID ya existente (UPDATE, no INSERT nuevo con id=null)
        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedCaptor.capture());
        SuppliersRowEntity savedRow = savedCaptor.getValue();

        assertNotNull(savedRow.getId(), "El ID no debe ser nulo; debe ser una entidad existente para ejecutar UPDATE y evitar Duplicate entry");
        assertEquals(10391L, savedRow.getId(), "Debe reusar el id 10391 existente encontrado por fallback");
        assertEquals(graphiteSupplierId, savedRow.getSupplierCode(), "El supplier_code debe quedar actualizado al ID actual MX120796");
        assertEquals(legacyCode, savedRow.getErpIdQad(), "Regla Oficial CASO 1: Para Supplier_Is_Legacy = y, erp_id_qad DEBE quedar en RASSINI_Legacy_QAD_ID (COCHGMER)");
        assertEquals("COCHGMER1", savedRow.getSupplierCodeDisIntegrity(), "Debe preservar el supplier_code_dis_integrity histórico existente para la cuenta bancaria");
        assertEquals("M", savedRow.getStatusIntegrity(), "Escenario 2: Proveedor Legacy encontrado por COCHGMER debe resolver statusIntegrity=M (Modificación)");

        // Verificar además que IntegrityService encuentra los registros persistidos gracias al fallback por legacyMappedErpId
        when(suppliersRowRepository.findBySupplierCodeOrderByBusinessUnitCodeAsc(eq(graphiteSupplierId)))
                .thenReturn(Collections.emptyList());
        when(suppliersRowRepository.findBySupplierCodeOrderByBusinessUnitCodeAsc(eq(legacyCode)))
                .thenReturn(List.of(savedRow));
        when(suppliersRowRepository.findDistinctAccountsBySupplierCodeExact(eq(graphiteSupplierId)))
                .thenReturn(Collections.emptyList());
        when(suppliersRowRepository.findDistinctAccountsBySupplierCodeExact(eq(legacyCode)))
                .thenReturn(List.of(savedRow));

        integrityService.createFileSupplierSync(dto);

        verify(supplierErpResolver).resolveEffectiveErpId(
                eq(graphiteSupplierId),
                eq(legacyCode),
                eq(legacyCode), // Tras el UPDATE con la regla oficial, erp_id_qad es COCHGMER
                eq(dtoErp),
                eq("INTEGRITY_SERVICE")
        );
    }

    @Test
    @DisplayName("Idempotencia COCHGMER: segunda ejecución localiza directamente MX120796 por supplier_code, no usa fallback legacy, reutiliza id=10391 y ejecuta UPDATE")
    void testLegacySupplierCochgmerIdempotentSecondExecution() {
        // Estado de la BD DESPUÉS de la primera ejecución:
        // supplier_code = "MX120796", erp_id_qad = "COCHGMER"
        String graphiteSupplierId = "MX120796";
        String legacyCode = "COCHGMER";
        String bu = "09";
        String account = "012180004515572542";
        String dtoErp = "60003031";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(graphiteSupplierId);
        dto.setLegacyMappedErpId(legacyCode);
        dto.setErpIdQad(dtoErp);
        dto.setSupplierIsLegacy("y");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber(account);
        banks.add(bank);
        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        // Fila en BD: supplier_code = "MX120796", erp_id_qad = "COCHGMER"
        SuppliersRowEntity alreadyMigratedRow = new SuppliersRowEntity();
        alreadyMigratedRow.setId(10391L);
        alreadyMigratedRow.setSupplierCode(graphiteSupplierId);   // MX120796
        alreadyMigratedRow.setBusinessUnitCode(bu);
        alreadyMigratedRow.setAccountNumber(account);
        alreadyMigratedRow.setErpIdQad(legacyCode);              // COCHGMER (Regla Oficial)
        alreadyMigratedRow.setSupplierCodeDisIntegrity("COCHGMER1");

        // 1. La búsqueda primaria por supplier_code="MX120796" + BU + cuenta YA encuentra la fila
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(
                eq(graphiteSupplierId), eq(bu), eq(account)))
                .thenReturn(Optional.of(alreadyMigratedRow));

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Segunda ejecución del upsert
        supplierJpaMapper.upsertSuppliersRows(dto);

        // Verificaciones:
        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedCaptor.capture());
        SuppliersRowEntity savedRow = savedCaptor.getValue();

        // id debe seguir siendo 10391 (UPDATE, no INSERT)
        assertNotNull(savedRow.getId(),
                "id no debe ser nulo en segunda ejecución: debe reutilizar la fila existente");
        assertEquals(10391L, savedRow.getId(),
                "Debe reutilizar id=10391 en segunda ejecución (idempotencia)");

        // supplier_code permanece MX120796
        assertEquals(graphiteSupplierId, savedRow.getSupplierCode(),
                "supplier_code debe permanecer MX120796 en segunda ejecución");

        // erp_id_qad permanece COCHGMER (Regla oficial para Supplier_Is_Legacy = y)
        assertEquals(legacyCode, savedRow.getErpIdQad(),
                "erp_id_qad debe permanecer COCHGMER en segunda ejecución bajo regla oficial");

        // supplier_code_dis_integrity se preserva (ya existía en la entidad recuperada)
        assertEquals("COCHGMER1", savedRow.getSupplierCodeDisIntegrity(),
                "supplier_code_dis_integrity debe preservarse en segunda ejecución (COCHGMER1)");

        // Escenario 3: Proveedor Legacy ya migrado debe resolver M (no A)
        assertEquals("M", savedRow.getStatusIntegrity(),
                "Escenario 3: Proveedor ya migrado debe resolver statusIntegrity=M (Modificación)");

        verify(suppliersRowRepository).findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(
                eq(graphiteSupplierId), eq(bu), eq(account));
    }

    @Test
    @DisplayName("Escenario 1: Proveedor totalmente nuevo (row.getId() == null) resuelve statusIntegrity=A y ejecuta INSERT")
    void testScenario1_BrandNewSupplierProducesAltaAndInsert() {
        String newSupplierId = "NEW_SUPP_888";
        String bu = "09";
        String account = "1234567890";
        String dtoErp = "60005555";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(newSupplierId);
        dto.setErpIdQad(dtoErp);

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber(account);
        banks.add(bank);
        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(newSupplierId), eq(bu), eq(account)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findFirstBySupplierCodeAndAccountNumber(eq(newSupplierId), eq(account)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.countDistinctAccountsBySupplierCode(eq(newSupplierId)))
                .thenReturn(0L);
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        supplierJpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedCaptor.capture());
        SuppliersRowEntity savedRow = savedCaptor.getValue();

        assertNull(savedRow.getId(), "Para proveedor totalmente nuevo id debe ser null (operación INSERT)");
        assertEquals("A", savedRow.getStatusIntegrity(), "Para proveedor totalmente nuevo statusIntegrity debe ser 'A' (Alta)");
        assertEquals(dtoErp, savedRow.getErpIdQad());
        assertEquals(dtoErp, savedRow.getSupplierCodeDisIntegrity());
    }

    @Test
    @DisplayName("ESCENARIO B: Supplier_Is_Legacy = y + cuenta bancaria nueva -> genera supplier_code_dis_integrity = COCHGMER_2 y statusIntegrity = A")
    void testScenarioB_LegacySupplierWithNewBankAccountProducesCochgmer2() {
        String supplierId = "MX120796";
        String legacyCode = "COCHGMER";
        String bu = "09";
        String historicalAccount = "012180004515572542";
        String newAccount = "999999999999999999";
        String dtoErp = "60003031";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierId);
        dto.setLegacyMappedErpId(legacyCode);
        dto.setErpIdQad(dtoErp);
        dto.setSupplierIsLegacy("y");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        
        // Cuenta 1: histórica
        GraphiteSupplierDto.Bank bank1 = new GraphiteSupplierDto.Bank();
        bank1.setBankAccountNumber(historicalAccount);
        banks.add(bank1);

        // Cuenta 2: nueva
        GraphiteSupplierDto.Bank bank2 = new GraphiteSupplierDto.Bank();
        bank2.setBankAccountNumber(newAccount);
        banks.add(bank2);

        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        // Cuenta 1 ya existe en BD (rowId = 10391)
        SuppliersRowEntity existingRow = new SuppliersRowEntity();
        existingRow.setId(10391L);
        existingRow.setSupplierCode(supplierId);
        existingRow.setBusinessUnitCode(bu);
        existingRow.setAccountNumber(historicalAccount);
        existingRow.setErpIdQad(legacyCode);
        existingRow.setSupplierCodeDisIntegrity("COCHGMER1");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(supplierId), eq(bu), eq(historicalAccount)))
                .thenReturn(Optional.of(existingRow));
        // Cuenta 2 no existe en BD
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(supplierId), eq(bu), eq(newAccount)))
                .thenReturn(Optional.empty());

        when(suppliersRowRepository.findFirstBySupplierCodeAndAccountNumber(eq(supplierId), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.countDistinctAccountsByErpIdQad(eq(legacyCode)))
                .thenReturn(1L);

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        supplierJpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository, org.mockito.Mockito.times(2)).save(savedCaptor.capture());
        List<SuppliersRowEntity> savedRows = savedCaptor.getAllValues();

        // Verificación Cuenta 1 (Histórica)
        SuppliersRowEntity savedHistorical = savedRows.get(0);
        assertEquals(10391L, savedHistorical.getId());
        assertEquals("M", savedHistorical.getStatusIntegrity(), "Cuenta histórica existente debe ser 'M' (Modificación)");
        assertEquals(legacyCode, savedHistorical.getErpIdQad(), "erp_id_qad debe ser COCHGMER");
        assertEquals("COCHGMER1", savedHistorical.getSupplierCodeDisIntegrity(), "supplier_code_dis_integrity histórico debe conservarse intacto");

        // Verificación Cuenta 2 (Nueva Legacy)
        SuppliersRowEntity savedNew = savedRows.get(1);
        assertNull(savedNew.getId(), "Cuenta nueva debe tener id=null (INSERT)");
        assertEquals("A", savedNew.getStatusIntegrity(), "Cuenta nueva debe ser 'A' (Alta)");
        assertEquals(legacyCode, savedNew.getErpIdQad(), "erp_id_qad debe ser COCHGMER");
        assertEquals("COCHGMER_2", savedNew.getSupplierCodeDisIntegrity(), "Cuenta nueva legacy debe usar COCHGMER como base: COCHGMER_2");
    }

    @Test
    @DisplayName("ESCENARIO C: Supplier_Is_Legacy = n + cuenta bancaria nueva -> genera supplier_code_dis_integrity = 60003031_1 y statusIntegrity = A")
    void testScenarioC_NonLegacySupplierWithNewBankAccountProducesErpSuffix() {
        String supplierId = "MX120796";
        String bu = "09";
        String newAccount = "999999999999999999";
        String dtoErp = "60003031";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierId);
        dto.setErpIdQad(dtoErp);
        dto.setSupplierIsLegacy("n");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber(newAccount);
        banks.add(bank);
        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(supplierId), eq(bu), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findFirstBySupplierCodeAndAccountNumber(eq(supplierId), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.countDistinctAccountsBySupplierCode(eq(supplierId)))
                .thenReturn(1L); // Ya existía 1 cuenta previa

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        supplierJpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedCaptor.capture());
        SuppliersRowEntity savedRow = savedCaptor.getValue();

        assertNull(savedRow.getId(), "Cuenta nueva debe tener id=null (INSERT)");
        assertEquals("A", savedRow.getStatusIntegrity(), "Cuenta nueva debe ser 'A' (Alta)");
        assertEquals(dtoErp, savedRow.getErpIdQad(), "Para Supplier_Is_Legacy = n, erp_id_qad debe ser 60003031");
        assertEquals("60003031_1", savedRow.getSupplierCodeDisIntegrity(), "Cuenta nueva no legacy debe ser 60003031_1");
    }

    @Test
    @DisplayName("Caso Proveedor Legacy 60000736 (COCASH): Cuenta nueva genera supplier_code_dis_integrity = 60000736_2 y statusIntegrity = A")
    void testLegacySupplier60000736WithNewBankAccountProduces60000736_2AndStatusA() {
        String supplierId = "MX199108";
        String legacyErpId = "60000736";
        String bu = "0111";
        String historicalAccount = "002650000136585012";
        String newAccount = "012180009999999999";
        String dtoErp = "60003144";

        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierId);
        dto.setLegacyMappedErpId(legacyErpId);
        dto.setErpIdQad(dtoErp);
        dto.setSupplierIsLegacy("y");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        List<GraphiteSupplierDto.Bank> banks = new ArrayList<>();

        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber(newAccount);
        banks.add(bank);

        erp.setErpBankList(banks);
        erpRecords.add(erp);
        dto.setErpRecords(erpRecords);

        // Cuenta nueva NO existe en BD para esa BU ni globalmente
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(eq(supplierId), eq(bu), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findByErpIdQadAndBusinessUnitCodeAndAccountNumber(eq(legacyErpId), eq(bu), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findFirstBySupplierCodeAndAccountNumber(eq(supplierId), eq(newAccount)))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findFirstByErpIdQadAndAccountNumber(eq(legacyErpId), eq(newAccount)))
                .thenReturn(Optional.empty());

        // Conteo por erpIdQad devuelve 1 (la cuenta histórica 002650000136585012)
        when(suppliersRowRepository.countDistinctAccountsByErpIdQad(eq(legacyErpId)))
                .thenReturn(1L);

        when(suppliersRowRepository.save(any(SuppliersRowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        supplierJpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(savedCaptor.capture());
        SuppliersRowEntity savedRow = savedCaptor.getValue();

        assertNull(savedRow.getId(), "Cuenta nueva debe tener id=null (INSERT)");
        assertEquals("A", savedRow.getStatusIntegrity(), "Cuenta nueva debe ser 'A' (Alta)");
        assertEquals(legacyErpId, savedRow.getErpIdQad(), "erp_id_qad debe ser 60000736");
        assertEquals("60000736_2", savedRow.getSupplierCodeDisIntegrity(), "Cuenta nueva legacy debe ser 60000736_2");
    }
}

