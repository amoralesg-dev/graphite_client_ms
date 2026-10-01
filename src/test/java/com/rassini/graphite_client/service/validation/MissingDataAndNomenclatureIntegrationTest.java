package com.rassini.graphite_client.service.validation;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.entity.CatalogManagerEntity;
import com.rassini.graphite_client.entity.CatalogManagerId;
import com.rassini.graphite_client.repository.CatalogManagerRepository;
import com.rassini.graphite_client.service.catalog.CatalogManagerCacheService;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.validation.collector.MissingDataCollector;
import com.rassini.graphite_client.service.validation.service.ManualOutputPathResolver;
import com.rassini.graphite_client.service.validation.service.MissingDataNotificationService;
import com.rassini.graphite_client.service.validation.service.OutputValidationService;
import com.rassini.graphite_client.service.xml.*;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "XML_OUTPUT_PATH=target/test-output-validation",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class MissingDataAndNomenclatureIntegrationTest {

    static {
        System.setProperty("XML_OUTPUT_PATH", "target/test-output-validation");
    }

    @Autowired
    private SuppliersRowRepository suppliersRowRepository;

    @Autowired
    private CatalogManagerRepository catalogManagerRepository;

    @Autowired
    private CatalogManagerCacheService catalogManagerCacheService;

    @Autowired
    private com.rassini.graphite_client.repository.CorreoPendienteRepository correoPendienteRepository;

    @Autowired
    private XmlPnService xmlPnService;

    @Autowired
    private XmlPn99Service xmlPn99Service;

    @Autowired
    private XmlOcService xmlOcService;

    @Autowired
    private XmlFrenosService xmlFrenosService;

    @Autowired
    private XmlBreakesService xmlBreakesService;

    @Autowired
    private IntegrityService integrityService;

    @Autowired
    private MissingDataCollector missingDataCollector;

    @Autowired
    private MissingDataNotificationService missingDataNotificationService;

    @Autowired
    private ManualOutputPathResolver manualOutputPathResolver;

    private String getOutputBase() {
        return XmlConstants.OUTPUT;
    }

    @BeforeEach
    public void setup() throws IOException {
        suppliersRowRepository.deleteAll();
        catalogManagerRepository.deleteAll();
        correoPendienteRepository.deleteAll();
        missingDataCollector.clear();
        deleteDirectoryRecursively(new File(getOutputBase()));

        // Pre-cargar catálogos necesarios para los tests de integración
        seedCatalog("country", "MEX", "09", "MEX");
        seedCatalog("country_integrity", "MEX", "09", "MEX");
        seedCatalog("state", "COAH", "09", "COAH");
        seedCatalog("country", "MEX", "99", "MEX");
        seedCatalog("country_integrity", "MEX", "99", "MEX");
        seedCatalog("state", "COAH", "99", "COAH");
        catalogManagerCacheService.loadCatalogInMemory();
    }

    private void seedCatalog(String idCatalogo, String code, String bu, String equivalencia) {
        CatalogManagerEntity entity = new CatalogManagerEntity();
        entity.setId(new CatalogManagerId(idCatalogo, code, bu));
        entity.setEquivalencia(equivalencia);
        catalogManagerRepository.save(entity);
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectoryRecursively(f);
                }
            }
            file.delete();
        }
    }

    @Test
    @DisplayName("Nomenclatura XML: Sin duplicar planta en 09, 99, 0111, 0301, 1000 y 1850")
    public void testXmlNomenclatureNoDuplicatePlant() throws IOException {
        // Proveedor completo en BU 09
        SuppliersRowEntity row09 = createValidRow("PRV001", "09", "60003062", "012180001322930215");
        suppliersRowRepository.save(row09);

        GraphiteSupplierDto dto09 = createDto("PRV001", "60003062", "09");
        xmlPnService.generate(dto09, null);

        Path normalBusrel09 = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003062_09.xml");
        Path normalCreditor09 = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_creditor_60003062_09.xml");
        Path duplicateBusrel09 = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003062_09_09.xml");

        assertTrue(Files.exists(normalBusrel09), "Debe existir RPIEDRAS_busrel_60003062_09.xml");
        assertTrue(Files.exists(normalCreditor09), "Debe existir RPIEDRAS_creditor_60003062_09.xml");
        assertFalse(Files.exists(duplicateBusrel09), "NO debe existir RPIEDRAS_busrel_60003062_09_09.xml");

        // Proveedor completo en BU 0111 (OC)
        SuppliersRowEntity row0111 = createValidRow("PRV002", "0111", "60003063", "012180001322930216");
        suppliersRowRepository.save(row0111);
        GraphiteSupplierDto dto0111 = createDto("PRV002", "60003063", "0111");
        xmlOcService.generate(dto0111, null);

        Path normalBusrel0111 = Paths.get(getOutputBase(), "xml", "OCBYP", "busrel_60003063_0111.xml");
        Path duplicateBusrel0111 = Paths.get(getOutputBase(), "xml", "OCBYP", "busrel_60003063_0111_0111.xml");
        assertTrue(Files.exists(normalBusrel0111), "Debe existir busrel_60003063_0111.xml");
        assertFalse(Files.exists(duplicateBusrel0111), "NO debe existir busrel_60003063_0111_0111.xml");

        // Proveedor completo en BU 1850 (BREAKES)
        SuppliersRowEntity row1850 = createValidRow("PRV003", "1850", "60003064", "012180001322930217");
        suppliersRowRepository.save(row1850);
        GraphiteSupplierDto dto1850 = createDto("PRV003", "60003064", "1850");
        xmlBreakesService.generate(dto1850, null);

        Path normalBusrel1850 = Paths.get(getOutputBase(), "xml", "BREAKES", "busrel_60003064_1850.xml");
        Path duplicateBusrel1850 = Paths.get(getOutputBase(), "xml", "BREAKES", "busrel_60003064_1850_1850.xml");
        assertTrue(Files.exists(normalBusrel1850), "Debe existir busrel_60003064_1850.xml");
        assertFalse(Files.exists(duplicateBusrel1850), "NO debe existir busrel_60003064_1850_1850.xml");
    }

    @Test
    @DisplayName("Idempotencia con archivo histórico duplicado: Respeta archivo existente sin generar duplicado nuevo")
    public void testIdempotenceWithLegacyDuplicateFile() throws IOException {
        Path pnDir = Paths.get(getOutputBase(), "xml", "PN");
        Files.createDirectories(pnDir);
        Path legacyDuplicate = pnDir.resolve("RPIEDRAS_busrel_60003062_09_09.xml");
        Files.writeString(legacyDuplicate, "<mock>legacy content</mock>");

        SuppliersRowEntity row09 = createValidRow("PRV001", "09", "60003062", "012180001322930215");
        suppliersRowRepository.save(row09);

        GraphiteSupplierDto dto09 = createDto("PRV001", "60003062", "09");
        xmlPnService.generate(dto09, null);

        Path newName = pnDir.resolve("RPIEDRAS_busrel_60003062_09.xml");
        assertFalse(Files.exists(newName), "No debe generar archivo nuevo si ya existe el histórico duplicado (idempotencia)");
        assertTrue(Files.exists(legacyDuplicate), "El archivo histórico debe permanecer intacto");
    }

    @Test
    @DisplayName("Faltante no bloqueante: XML se genera en ruta Manual y no en ruta normal")
    public void testNonBlockingMissingDataGeneratesInManual() throws IOException {
        SuppliersRowEntity row = createValidRow("PRV_WARN", "09", "60003080", "012180001322930218");
        row.setStreetName(null); // Warning no bloqueante (calle vacía)
        suppliersRowRepository.save(row);

        GraphiteSupplierDto dto = createDto("PRV_WARN", "60003080", "09");
        xmlPnService.generate(dto, null);

        Path manualBusrel = Paths.get(getOutputBase(), "xml", "Manual", "PN", "RPIEDRAS_busrel_60003080_09.xml");
        Path normalBusrel = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003080_09.xml");

        assertTrue(Files.exists(manualBusrel), "Debe generarse en la subcarpeta Manual/PN");
        assertFalse(Files.exists(normalBusrel), "NO debe existir en la carpeta normal PN");
    }

    @Test
    @DisplayName("Faltante bloqueante: XML NO se genera (NOT_GENERATED), sin archivo corrupto")
    public void testBlockingMissingDataDoesNotGenerateXml() {
        SuppliersRowEntity row = createValidRow("PRV_BLOCK", "09", "60003081", "012180001322930219");
        row.setStateCode(null); // Bloqueante (falta equivalencia estado)
        suppliersRowRepository.save(row);

        GraphiteSupplierDto dto = createDto("PRV_BLOCK", "60003081", "09");
        xmlPnService.generate(dto, null);

        Path manualBusrel = Paths.get(getOutputBase(), "xml", "Manual", "PN", "RPIEDRAS_busrel_60003081_09.xml");
        Path normalBusrel = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003081_09.xml");

        assertFalse(Files.exists(manualBusrel), "No debe generarse archivo en Manual");
        assertFalse(Files.exists(normalBusrel), "No debe generarse archivo en Normal");
    }

    @Test
    @DisplayName("Integrity: Separación estricta por unidad de negocio normal y Manual")
    public void testIntegritySeparatedByBusinessUnit() throws IOException {
        SuppliersRowEntity acc1 = createValidRow("PRV_MULTI_BU", "09", "60003090", "012180001111111111");
        SuppliersRowEntity acc2 = createValidRow("PRV_MULTI_BU", "0111", "60003090", "012180002222222222");
        acc2.setBeneficiaryBankName(null); // Warning -> Integrity para 0111 va a Manual
        suppliersRowRepository.save(acc1);
        suppliersRowRepository.save(acc2);

        GraphiteSupplierDto dto = createDto("PRV_MULTI_BU", "60003090", "09");
        integrityService.createFileSupplierSync(dto);

        Path normalBu09Dir = Paths.get(getOutputBase(), "integrity", "09");
        Path manualBu0111Dir = Paths.get(getOutputBase(), "integrity", "Manual", "0111");

        assertTrue(Files.exists(normalBu09Dir), "Debe existir carpeta <integrity>/09");
        assertTrue(Files.exists(manualBu0111Dir), "Debe existir carpeta <integrity>/Manual/0111");

        File[] files09 = normalBu09Dir.toFile().listFiles((d, name) -> name.startsWith("60003090"));
        assertNotNull(files09);
        assertTrue(files09.length > 0, "Debe haberse generado el TXT de BU 09 en su carpeta propia");

        File[] files0111 = manualBu0111Dir.toFile().listFiles((d, name) -> name.startsWith("60003090"));
        assertNotNull(files0111);
        assertTrue(files0111.length > 0, "Debe haberse generado el TXT de BU 0111 en <integrity>/Manual/0111");
    }

    @Test
    @DisplayName("Nomenclatura XML en 0301 y 1000: Nombres limpios sin sufijos duplicados")
    public void testXmlNomenclature0301And1000() throws IOException {
        // BU 0301 (BYPASA)
        SuppliersRowEntity row0301 = createValidRow("PRV0301", "0301", "60003065", "012180001322930220");
        suppliersRowRepository.save(row0301);
        GraphiteSupplierDto dto0301 = createDto("PRV0301", "60003065", "0301");
        xmlOcService.generate(dto0301, null);

        Path busrel0301 = Paths.get(getOutputBase(), "xml", "OCBYP", "busrel_60003065_0301.xml");
        Path dupBusrel0301 = Paths.get(getOutputBase(), "xml", "OCBYP", "busrel_60003065_0301_0301.xml");
        assertTrue(Files.exists(busrel0301), "Debe existir busrel_60003065_0301.xml");
        assertFalse(Files.exists(dupBusrel0301), "NO debe existir busrel_60003065_0301_0301.xml");

        // BU 1000 (FRENOS)
        SuppliersRowEntity row1000 = createValidRow("PRV1000", "1000", "60003066", "012180001322930221");
        suppliersRowRepository.save(row1000);
        GraphiteSupplierDto dto1000 = createDto("PRV1000", "60003066", "1000");
        xmlFrenosService.generate(dto1000, null);

        Path busrel1000 = Paths.get(getOutputBase(), "xml", "FRENOS", "busrel_60003066_1000.xml");
        Path dupBusrel1000 = Paths.get(getOutputBase(), "xml", "FRENOS", "busrel_60003066_1000_1000.xml");
        assertTrue(Files.exists(busrel1000), "Debe existir busrel_60003066_1000.xml");
        assertFalse(Files.exists(dupBusrel1000), "NO debe existir busrel_60003066_1000_1000.xml");
    }

    @Test
    @DisplayName("Recuperación de Manual a Normal: Si un XML estuvo en Manual, datos completos posteriores generan en Normal sin bloqueo")
    public void testRecoveryFromManualToNormal() throws IOException {
        // 1. Ejecución con datos incompletos -> se genera en Manual/PN
        SuppliersRowEntity rowWarn = createValidRow("PRV_RECOV", "09", "60003082", "012180001322930222");
        rowWarn.setStreetName(null);
        suppliersRowRepository.save(rowWarn);

        GraphiteSupplierDto dtoWarn = createDto("PRV_RECOV", "60003082", "09");
        xmlPnService.generate(dtoWarn, null);

        Path manualBusrel = Paths.get(getOutputBase(), "xml", "Manual", "PN", "RPIEDRAS_busrel_60003082_09.xml");
        Path normalBusrel = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003082_09.xml");
        assertTrue(Files.exists(manualBusrel), "Inicialmente debe generarse en Manual/PN");
        assertFalse(Files.exists(normalBusrel), "No debe estar en normal inicialmente");

        // 2. Simular llegada de datos completos y nuevo ciclo
        missingDataCollector.clear();
        rowWarn.setStreetName("Av. Industrial Corregida 456");
        rowWarn.setXmlStatus(XmlStatus.PENDING);
        suppliersRowRepository.save(rowWarn);

        xmlPnService.generate(dtoWarn, null);

        // Debe haberse generado exitosamente en normal sin quedar bloqueado por el archivo manual existente
        assertTrue(Files.exists(normalBusrel), "Debe generarse en la carpeta normal tras corregir los datos");
    }

    @Test
    @DisplayName("Nodos alternos de datos bancarios: Inequívoco con 1 cuenta vs Ambiguo con múltiples cuentas")
    public void testAlternateBankNodesEvaluation() {
        OutputValidationService validationService = new OutputValidationService(missingDataCollector);

        // Caso 1: Nodos alternos con 1 cuenta (asociación inequívoca)
        GraphiteSupplierDto dtoSingle = createDto("PRV_ALT_1", "60003083", "09");
        GraphiteSupplierDto.ErpRecord erpSingle = dtoSingle.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank1 = new GraphiteSupplierDto.Bank();
        bank1.setBankAccountNumber("012180001111111111");
        erpSingle.setErpBankList(Collections.singletonList(bank1));

        GraphiteSupplierDto.BankWireAbaRouting altWire1 = new GraphiteSupplierDto.BankWireAbaRouting();
        altWire1.setRouting("123456789");
        altWire1.setBankName("CITIBANK NY");
        erpSingle.setBankWireAbaRouting(altWire1);

        validationService.validateAlternateBankNodes(dtoSingle);
        assertFalse(missingDataCollector.hasWarningIssues("09", null), "Con 1 sola cuenta la asociación es inequívoca, no genera issue");

        // Caso 2: Nodos alternos con 2 cuentas (asociación ambigua -> AMBIGUOUS_NODE WARNING)
        GraphiteSupplierDto dtoMulti = createDto("PRV_ALT_2", "60003084", "09");
        GraphiteSupplierDto.ErpRecord erpMulti = dtoMulti.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank2 = new GraphiteSupplierDto.Bank();
        bank2.setBankAccountNumber("012180002222222222");
        erpMulti.setErpBankList(List.of(bank1, bank2));
        erpMulti.setBankWireAbaRouting(altWire1);

        validationService.validateAlternateBankNodes(dtoMulti);
        assertTrue(missingDataCollector.hasWarningIssues("09", null), "Con múltiples cuentas debe marcar advertencia ambigua");
        assertEquals(com.rassini.graphite_client.service.validation.model.IssueType.AMBIGUOUS_NODE,
                missingDataCollector.getIssues().get(0).getIssueType(), "Debe ser AMBIGUOUS_NODE");
    }

    @Test
    @DisplayName("Aislamiento de errores: Falla un proveedor en el lote, el siguiente continúa y genera sus archivos")
    public void testBatchIsolationBetweenSuppliers() throws IOException {
        // Proveedor 1 con dato bloqueante (falla generación)
        SuppliersRowEntity p1 = createValidRow("PRV_FAIL", "09", "60003085", "012180001322930225");
        p1.setCountryCode(null);
        suppliersRowRepository.save(p1);

        // Proveedor 2 válido y completo
        SuppliersRowEntity p2 = createValidRow("PRV_OK", "09", "60003086", "012180001322930226");
        suppliersRowRepository.save(p2);

        GraphiteSupplierDto dto1 = createDto("PRV_FAIL", "60003085", "09");
        GraphiteSupplierDto dto2 = createDto("PRV_OK", "60003086", "09");

        // Ejecutar p1
        xmlPnService.generate(dto1, null);
        // Ejecutar p2
        xmlPnService.generate(dto2, null);

        Path p1File = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003085_09.xml");
        Path p2File = Paths.get(getOutputBase(), "xml", "PN", "RPIEDRAS_busrel_60003086_09.xml");

        assertFalse(Files.exists(p1File), "Proveedor con error NO debe haber generado archivo");
        assertTrue(Files.exists(p2File), "Proveedor siguiente DEBE haber generado su archivo con éxito sin ser afectado");
    }

    @Test
    @DisplayName("Correo pendiente: Registro en correo_pendiente con cuentas enmascaradas y enviado=false")
    public void testMissingDataNotificationPersistsPendingEmail() {
        MissingDataCollector testCollector = new MissingDataCollector();
        testCollector.recordIssue(com.rassini.graphite_client.service.validation.model.MissingDataIssue.builder()
                .supplierCode("PRV_MAIL")
                .erpIdQad("60003087")
                .businessUnitCode("09")
                .maskedAccountNumber("****0227")
                .outputType(com.rassini.graphite_client.service.validation.model.OutputType.INTEGRITY)
                .subType("sync_file")
                .fieldName("beneficiaryBankName")
                .issueType(com.rassini.graphite_client.service.validation.model.IssueType.BANK_INFO_INCOMPLETE)
                .severity(com.rassini.graphite_client.service.validation.model.IssueSeverity.WARNING)
                .result(com.rassini.graphite_client.service.validation.model.OutputResult.GENERATED_MANUAL)
                .technicalMessage("Falta nombre banco")
                .build());

        missingDataNotificationService.processAndNotify(testCollector, 1);

        List<com.rassini.graphite_client.entity.CorreoPendienteEntity> correos = correoPendienteRepository.findAll();
        assertFalse(correos.isEmpty(), "Debe haberse persistido un correo en la tabla correos_pendientes");

        com.rassini.graphite_client.entity.CorreoPendienteEntity correo = correos.get(correos.size() - 1);
        assertFalse(Boolean.TRUE.equals(correo.getEnviado()), "El campo enviado debe ser false para procesamiento por scheduler");
        assertTrue(correo.getSubject().contains("[INCIDENCIAS GRAPHITE]"), "El asunto debe identificar incidencias");
        assertTrue(correo.getBody().contains("****0227"), "El cuerpo debe contener la cuenta enmascarada");
        assertFalse(correo.getBody().contains("012180001322930227"), "NO debe contener el número de cuenta completo en texto claro");
    }

    private SuppliersRowEntity createValidRow(String supplierCode, String bu, String erpId, String account) {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setSupplierCode(supplierCode);
        row.setBusinessUnitCode(bu);
        row.setErpIdQad(erpId);
        row.setAccountNumber(account);
        row.setSupplierName("PROVEEDOR TEST SA DE CV");
        row.setStateCode("COAH");
        row.setCountryCode("MEX");
        row.setRfc("TST900101ABC");
        row.setSupplierCurrency("MXN");
        row.setStreetName("Av. Industrial 123");
        row.setZipCode("26000");
        row.setCityCode("Piedras Negras");
        row.setBeneficiaryBankName("BBVA BANCOMER");
        row.setBeneficiaryAccountName("PROVEEDOR TEST SA DE CV");
        row.setSupplierTypeCode("NORMAL");
        row.setPurchaseTypeCode("TRANSFER");
        row.setStatusIntegrity("A");
        row.setXmlStatus(XmlStatus.PENDING);
        return row;
    }

    private GraphiteSupplierDto createDto(String supplierCode, String erpId, String bu) {
        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId(supplierCode);
        dto.setErpIdQad(erpId);
        GraphiteSupplierDto.ErpRecord erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId(bu);
        dto.setErpRecords(Collections.singletonList(erp));
        return dto;
    }
}
