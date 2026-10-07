package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.dto.UpdateInfo;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.mapper.SupplierRowMapper;
import com.rassini.graphite_client.service.resolver.SupplierErpResolver;
import com.rassini.graphite_client.service.sync.impl.IntegrityServiceImpl;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.factory.BreakesXmlFactory;
import com.rassini.graphite_client.service.xml.factory.FrenosXmlFactory;
import com.rassini.graphite_client.service.xml.factory.OcXmlFactory;
import com.rassini.graphite_client.service.xml.factory.Pn99XmlFactory;
import com.rassini.graphite_client.service.xml.factory.PnXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.XmlFrenosServiceImpl;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FrenosCurrencyMappingTest {

    @TempDir
    Path tempDir;

    @Mock
    private CatalogService catalogService;

    @Mock
    private SuppliersRowRepository suppliersRowRepository;

    @Mock
    private SupplierErpResolver supplierErpResolver;

    @Mock
    private XmlTemplateEngine xmlTemplateEngine;

    @Mock
    private XmlGenerationHelper xmlGenerationHelper;

    private IntegrityServiceImpl integrityService;
    private XmlFrenosServiceImpl xmlFrenosService;
    private FrenosXmlFactory frenosFactory;
    private OcXmlFactory ocFactory;
    private PnXmlFactory pnFactory;
    private Pn99XmlFactory pn99Factory;
    private BreakesXmlFactory breakesFactory;

    private GraphiteSupplierDto dto;
    private GraphiteSupplierDto.Location hq;
    private GraphiteSupplierDto.ErpRecord frenosErp;

    @BeforeEach
    void setUp() {
        integrityService = new IntegrityServiceImpl(
                suppliersRowRepository,
                catalogService,
                supplierErpResolver,
                tempDir.resolve("integrity").toString()
        );
        xmlFrenosService = new XmlFrenosServiceImpl(
                catalogService,
                xmlTemplateEngine,
                suppliersRowRepository,
                xmlGenerationHelper
        );
        frenosFactory = new FrenosXmlFactory(catalogService);
        ocFactory = new OcXmlFactory(catalogService);
        pnFactory = new PnXmlFactory(catalogService);
        pn99Factory = new Pn99XmlFactory(catalogService);
        breakesFactory = new BreakesXmlFactory(catalogService);

        lenient().when(catalogService.resolveTaxClass(any(), any())).thenReturn("DEFAULT");
        lenient().when(catalogService.resolveUpdateInfo(any())).thenReturn(new UpdateInfo("false", "CREATE"));
        lenient().when(catalogService.getAction(any())).thenReturn("CREATE");

        dto = new GraphiteSupplierDto();
        dto.setEntityPublicId("SUP-FRENOS-01");
        dto.setEntityName("PROVEEDOR FRENOS TEST");
        dto.setErpIdQad("60001000");

        hq = new GraphiteSupplierDto.Location();
        hq.setLocationName("Headquarters");

        frenosErp = new GraphiteSupplierDto.ErpRecord();
        frenosErp.setRassiniErpEntityId(XMLConstants.FRENOS); // "1000"
    }

    private GraphiteSupplierDto.Bank createBankWithCurrency(String currency) {
        GraphiteSupplierDto.Bank bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber("1234567890");
        bank.setBankName("BBVA BANCOMER");
        bank.setBankCountry("MX");
        if (currency != null) {
            bank.setBankCurrencyList(Collections.singletonList(currency));
        } else {
            bank.setBankCurrencyList(Collections.emptyList());
        }
        return bank;
    }

    private String invokeBuildSupplierLine(SuppliersRowEntity supplier) throws Exception {
        Method method = IntegrityServiceImpl.class.getDeclaredMethod("buildSupplierLine", SuppliersRowEntity.class);
        method.setAccessible(true);
        return (String) method.invoke(integrityService, supplier);
    }

    // =========================================================================
    // 1. SupplierRowMapper, Frenos, currency MX: Base de datos: MX, no MN
    // =========================================================================
    @Test
    @DisplayName("1. SupplierRowMapper para Frenos con currency MX -> guarda MX en BD (no MN)")
    void testSupplierRowMapper_Frenos_Currency_MX() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bank = createBankWithCurrency("MX");

        SupplierRowMapper.fill(row, dto, hq, frenosErp, bank, catalogService);

        assertEquals("MX", row.getSupplierCurrency(), "La base de datos debe conservar MX, no MN");
        assertNotEquals("MN", row.getSupplierCurrency(), "SupplierRowMapper no debe convertir a MN");
    }

    // =========================================================================
    // 2. SupplierRowMapper, Frenos, currency MEX: Base de datos: MEX, no MN
    // =========================================================================
    @Test
    @DisplayName("2. SupplierRowMapper para Frenos con currency MEX -> guarda MEX en BD (no MN)")
    void testSupplierRowMapper_Frenos_Currency_MEX() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bank = createBankWithCurrency("MEX");

        SupplierRowMapper.fill(row, dto, hq, frenosErp, bank, catalogService);

        assertEquals("MEX", row.getSupplierCurrency(), "La base de datos debe conservar MEX, no MN");
        assertNotEquals("MN", row.getSupplierCurrency(), "SupplierRowMapper no debe convertir a MN");
    }

    // =========================================================================
    // 3. SupplierRowMapper, Frenos, currency MXN: Base de datos: MXN, no MN
    // =========================================================================
    @Test
    @DisplayName("3. SupplierRowMapper para Frenos con currency MXN -> guarda MXN en BD (no MN)")
    void testSupplierRowMapper_Frenos_Currency_MXN() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bank = createBankWithCurrency("MXN");

        SupplierRowMapper.fill(row, dto, hq, frenosErp, bank, catalogService);

        assertEquals("MXN", row.getSupplierCurrency(), "La base de datos debe conservar MXN, no MN");
        assertNotEquals("MN", row.getSupplierCurrency(), "SupplierRowMapper no debe convertir a MN");
    }

    // =========================================================================
    // 4. SupplierRowMapper, Frenos, currency USD: Base de datos: USD, no US
    // =========================================================================
    @Test
    @DisplayName("4. SupplierRowMapper para Frenos con currency USD -> guarda USD en BD (no US)")
    void testSupplierRowMapper_Frenos_Currency_USD() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bank = createBankWithCurrency("USD");

        SupplierRowMapper.fill(row, dto, hq, frenosErp, bank, catalogService);

        assertEquals("USD", row.getSupplierCurrency(), "La base de datos debe conservar USD, no US");
        assertNotEquals("US", row.getSupplierCurrency(), "SupplierRowMapper no debe convertir a US");
    }

    // =========================================================================
    // 5. TXT de Integrity: Debe utilizar exactamente la moneda persistida
    // =========================================================================
    @Test
    @DisplayName("5. TXT de Integrity: utiliza la moneda persistida sin aplicar mapeo de Frenos (formato y archivo real)")
    void testIntegrityTxt_UsesPersistedCurrencyWithoutFrenosMapping() throws Exception {
        SuppliersRowEntity supplier = new SuppliersRowEntity();
        supplier.setErpIdQad("60001000");
        supplier.setBusinessUnitCode(XMLConstants.FRENOS); // "1000"
        supplier.setSupplierCode("SUP-FRENOS-01");
        supplier.setStatusIntegrity("A");
        supplier.setSupplierName("PROVEEDOR FRENOS TEST");
        supplier.setSupplierSearchName("FRENOS TEST");
        supplier.setRfc("PFT010101AAA");
        supplier.setStreetName("CALLE FRENOS");
        supplier.setStreetNumber("100");
        supplier.setZipCode("12345");
        supplier.setCityCode("CIUDAD");
        supplier.setStateCode("EDO");
        supplier.setCountryCode("MX");
        supplier.setContactEmail("contacto@frenos.com");
        supplier.setSupplierCodeDisIntegrity("60001000");

        // 5.1 Validar formato de línea real (buildSupplierLine) para MX, MEX, MXN, USD
        supplier.setSupplierCurrency("MX");
        String lineMx = invokeBuildSupplierLine(supplier);
        String[] fieldsMx = lineMx.split("\\|", -1);
        assertEquals("MX", fieldsMx[13], "Integrity TXT debe contener exactamente MX para Frenos (campo 14)");

        supplier.setSupplierCurrency("MEX");
        String lineMex = invokeBuildSupplierLine(supplier);
        String[] fieldsMex = lineMex.split("\\|", -1);
        assertEquals("MEX", fieldsMex[13], "Integrity TXT debe contener exactamente MEX para Frenos");

        supplier.setSupplierCurrency("MXN");
        String lineMnx = invokeBuildSupplierLine(supplier);
        String[] fieldsMnx = lineMnx.split("\\|", -1);
        assertEquals("MXN", fieldsMnx[13], "Integrity TXT debe contener exactamente MXN para Frenos");

        supplier.setSupplierCurrency("USD");
        String lineUsd = invokeBuildSupplierLine(supplier);
        String[] fieldsUsd = lineUsd.split("\\|", -1);
        assertEquals("USD", fieldsUsd[13], "Integrity TXT debe contener exactamente USD para Frenos (no US)");

        // 5.2 Validar generación real de archivo en disco mediante generateSupplierSyncFile en directorio temporal
        Path outDir = tempDir.resolve("integrity");
        Files.createDirectories(outDir);

        supplier.setSupplierCurrency("MX");
        integrityService.generateSupplierSyncFile(Collections.singletonList(supplier), "TEST_FRENOS_MX");

        Path latestFile;
        try (Stream<Path> files = Files.list(outDir)) {
            latestFile = files
                    .filter(p -> p.getFileName().toString().startsWith("TEST_FRENOS_MX_"))
                    .findFirst()
                    .orElse(null);
        }

        assertNotNull(latestFile, "El archivo de sincronización de Integrity debe generarse en el directorio temporal aislado");
        List<String> fileLines = Files.readAllLines(latestFile);
        assertFalse(fileLines.isEmpty(), "El archivo generado de Integrity no debe estar vacío");
        String fileLine = fileLines.get(0);
        String[] writtenFields = fileLine.split("\\|", -1);
        assertEquals("MX", writtenFields[13], "El archivo TXT en disco debe contener exactamente la moneda MX");
    }

    // =========================================================================
    // 6. XML de Frenos: MX -> MN, MEX -> MN, MXN -> MN, USD -> US, otro -> igual
    // =========================================================================
    @Test
    @DisplayName("6. XML de Frenos: MX/MEX/MXN -> MN, USD -> US, otro valor -> conserva original en tcCurrencyCode")
    void testFrenosXml_CurrencyMapping() {
        SuppliersRowEntity supplier = new SuppliersRowEntity();
        supplier.setBusinessUnitCode(XMLConstants.FRENOS);
        supplier.setErpIdQad("60001000");
        supplier.setCountryCode("MX");

        // Caso MX -> MN
        supplier.setSupplierCurrency("MX");
        CreditorXmlContext ctxMx = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("MN", ctxMx.getCreditor().getTcCurrencyCode(), "XML Frenos debe tener MN para entrada MX");

        // Caso MEX -> MN
        supplier.setSupplierCurrency("MEX");
        CreditorXmlContext ctxMex = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("MN", ctxMex.getCreditor().getTcCurrencyCode(), "XML Frenos debe tener MN para entrada MEX");

        // Caso MXN -> MN
        supplier.setSupplierCurrency("MXN");
        CreditorXmlContext ctxMnx = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("MN", ctxMnx.getCreditor().getTcCurrencyCode(), "XML Frenos debe tener MN para entrada MXN");

        // Caso USD -> US
        supplier.setSupplierCurrency("USD");
        CreditorXmlContext ctxUsd = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("US", ctxUsd.getCreditor().getTcCurrencyCode(), "XML Frenos debe tener US para entrada USD");

        // Caso EUR (otro valor) -> EUR
        supplier.setSupplierCurrency("EUR");
        CreditorXmlContext ctxEur = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("EUR", ctxEur.getCreditor().getTcCurrencyCode(), "XML Frenos debe conservar EUR sin cambios");

        // Caso CAD (otro valor) -> CAD
        supplier.setSupplierCurrency("CAD");
        CreditorXmlContext ctxCad = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("CAD", ctxCad.getCreditor().getTcCurrencyCode(), "XML Frenos debe conservar CAD sin cambios");
    }

    // =========================================================================
    // 7. XML de plantas diferentes de Frenos: Ejecutar generadores reales
    // =========================================================================
    @Test
    @DisplayName("7. XML de plantas diferentes de Frenos: generadores reales conservan moneda original (MX, MEX, MXN, USD, EUR)")
    void testOtherPlantsXml_PreservesOriginalCurrency() {
        String[] currenciesToTest = { "MX", "MEX", "MXN", "USD", "EUR" };

        // 7.1 Planta OC (0111) mediante OcXmlFactory real
        SuppliersRowEntity supplierOc = new SuppliersRowEntity();
        supplierOc.setBusinessUnitCode(XMLConstants.OC);
        supplierOc.setErpIdQad("60000111");
        supplierOc.setCountryCode("MX");

        for (String cur : currenciesToTest) {
            supplierOc.setSupplierCurrency(cur);
            CreditorXmlContext ctx = ocFactory.buildCreditorContext(supplierOc, XMLConstants.OC, "DEFAULT", List.of("MEX"), "30");
            assertEquals(cur, ctx.getCreditor().getTcCurrencyCode(), "OcXmlFactory debe conservar la moneda original " + cur);
        }

        // 7.2 Planta PN (09) mediante PnXmlFactory real
        SuppliersRowEntity supplierPn = new SuppliersRowEntity();
        supplierPn.setBusinessUnitCode(XMLConstants.PN);
        supplierPn.setErpIdQad("60000009");
        supplierPn.setCountryCode("MX");

        for (String cur : currenciesToTest) {
            supplierPn.setSupplierCurrency(cur);
            CreditorXmlContext ctx = pnFactory.buildCreditorContext(supplierPn, "DEFAULT");
            assertEquals(cur, ctx.getCreditor().getTcCurrencyCode(), "PnXmlFactory debe conservar la moneda original " + cur);
        }

        // 7.3 Planta PN99 (99) mediante Pn99XmlFactory real
        SuppliersRowEntity supplierPn99 = new SuppliersRowEntity();
        supplierPn99.setBusinessUnitCode(XMLConstants.PN99);
        supplierPn99.setErpIdQad("60000099");
        supplierPn99.setCountryCode("MX");

        for (String cur : currenciesToTest) {
            supplierPn99.setSupplierCurrency(cur);
            CreditorXmlContext ctx = pn99Factory.buildCreditorContext(supplierPn99, "DEFAULT", "MEX");
            assertEquals(cur, ctx.getCreditor().getTcCurrencyCode(), "Pn99XmlFactory debe conservar la moneda original " + cur);
        }

        // 7.4 Planta BREAKES (1850) mediante BreakesXmlFactory real
        SuppliersRowEntity supplierBreakes = new SuppliersRowEntity();
        supplierBreakes.setBusinessUnitCode(XMLConstants.BREAKES);
        supplierBreakes.setErpIdQad("60001850");
        supplierBreakes.setCountryCode("MX");

        for (String cur : currenciesToTest) {
            supplierBreakes.setSupplierCurrency(cur);
            CreditorXmlContext ctx = breakesFactory.buildCreditorContext(supplierBreakes, XMLConstants.BREAKES, "DEFAULT", List.of("MEX"), "30");
            assertEquals(cur, ctx.getCreditor().getTcCurrencyCode(), "BreakesXmlFactory debe conservar la moneda original " + cur);
        }
    }

    // =========================================================================
    // 8. Valores null o vacíos: Comportamiento seguro sin NPE
    // =========================================================================
    @Test
    @DisplayName("8. Valores null o vacíos: manejo seguro sin NPE ni valores inventados en mappers, XML y TXT")
    void testNullAndEmptyCurrencyHandling() throws Exception {
        // 8.1 SupplierRowMapper con bank sin lista de moneda o vacía
        SuppliersRowEntity row1 = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bankNoList = new GraphiteSupplierDto.Bank();
        bankNoList.setBankCurrencyList(null);
        SupplierRowMapper.fill(row1, dto, hq, frenosErp, bankNoList, catalogService);
        assertNull(row1.getSupplierCurrency(), "supplierCurrency debe ser null si no hay moneda en bank");

        SuppliersRowEntity row2 = new SuppliersRowEntity();
        GraphiteSupplierDto.Bank bankEmptyList = new GraphiteSupplierDto.Bank();
        bankEmptyList.setBankCurrencyList(Collections.emptyList());
        SupplierRowMapper.fill(row2, dto, hq, frenosErp, bankEmptyList, catalogService);
        assertNull(row2.getSupplierCurrency(), "supplierCurrency debe ser null si la lista de monedas está vacía");

        SuppliersRowEntity row3 = new SuppliersRowEntity();
        SupplierRowMapper.fill(row3, dto, hq, frenosErp, null, catalogService);
        assertNull(row3.getSupplierCurrency(), "supplierCurrency debe ser null si bank es null");

        // 8.2 FrenosXmlFactory con null, empty y espacios
        SuppliersRowEntity supplier = new SuppliersRowEntity();
        supplier.setBusinessUnitCode(XMLConstants.FRENOS);
        supplier.setErpIdQad("60001000");
        supplier.setCountryCode("MX");

        supplier.setSupplierCurrency(null);
        CreditorXmlContext ctxNull = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertNull(ctxNull.getCreditor().getTcCurrencyCode(), "tcCurrencyCode debe ser null cuando supplierCurrency es null");

        supplier.setSupplierCurrency("");
        CreditorXmlContext ctxEmpty = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("", ctxEmpty.getCreditor().getTcCurrencyCode(), "tcCurrencyCode debe ser empty cuando supplierCurrency es empty");

        supplier.setSupplierCurrency("   ");
        CreditorXmlContext ctxBlank = frenosFactory.buildCreditorContext(supplier, XMLConstants.FRENOS, "DEFAULT", List.of("MEX"), "30");
        assertEquals("   ", ctxBlank.getCreditor().getTcCurrencyCode(), "tcCurrencyCode debe conservar espacios cuando supplierCurrency tiene espacios");

        // 8.3 IntegrityServiceImpl con null currency
        supplier.setSupplierCurrency(null);
        String lineNull = invokeBuildSupplierLine(supplier);
        String[] fieldsNull = lineNull.split("\\|", -1);
        assertEquals("", fieldsNull[13], "Campo 14 de Integrity TXT debe ser cadena vacía si supplierCurrency es null");
    }

    // =========================================================================
    // 9. XmlFrenosServiceImpl: Orquestación real con FrenosXmlFactory (MX -> MN)
    // =========================================================================
    @Test
    @DisplayName("9. XmlFrenosServiceImpl invoca FrenosXmlFactory con firma real y genera Creditor XML con MN para MX")
    void testXmlFrenosServiceImpl_OrchestrationWithRealFactory_CurrencyMxToMn() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(1L);
        row.setSupplierCode("SUP-FRENOS-01");
        row.setBusinessUnitCode(XMLConstants.FRENOS);
        row.setSupplierCurrency("MX");
        row.setCountryCode("MX");
        row.setErpIdQad("60001000");
        row.setXmlStatus(XmlStatus.PENDING);

        SupplierEntity supplierEntity = new SupplierEntity();

        frenosErp.setRassiniErpTaxClass("DEFAULT");
        frenosErp.setRassiniErpTaxZone(List.of("MEX"));
        frenosErp.setRassiniErpPaymentTerms("30");
        dto.setErpRecords(List.of(frenosErp));

        when(suppliersRowRepository.findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc(
                "SUP-FRENOS-01", XMLConstants.FRENOS
        )).thenReturn(Optional.of(row));

        ArgumentCaptor<CreditorXmlContext> creditorCaptor = ArgumentCaptor.forClass(CreditorXmlContext.class);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(4);
            if (action != null) {
                action.run();
            }
            return null;
        }).when(xmlGenerationHelper).generateIfFileNotExists(
                any(), any(), any(), any(), any()
        );

        xmlFrenosService.generate(dto, supplierEntity);

        verify(xmlTemplateEngine).generateCreditorXml(
                eq(XmlConstants.TEMPLATE_FRENOS_CREDITOR),
                eq(XmlConstants.OUTPUT_FRENOS_DIR),
                creditorCaptor.capture()
        );

        CreditorXmlContext capturedCtx = creditorCaptor.getValue();
        assertNotNull(capturedCtx, "CreditorXmlContext debe ser construido por FrenosXmlFactory dentro del orquestador");
        assertEquals("MN", capturedCtx.getCreditor().getTcCurrencyCode(),
                "tcCurrencyCode debe ser transformado a MN para entrada persistida MX");
        assertNotEquals(ProviderState.ERRORMAPFRENOS, supplierEntity.getStatus(),
                "El estado del proveedor no debe marcar error en orquestación de Frenos");
    }

    // =========================================================================
    // 10. XmlFrenosServiceImpl: Orquestación real con FrenosXmlFactory (USD -> US)
    // =========================================================================
    @Test
    @DisplayName("10. XmlFrenosServiceImpl invoca FrenosXmlFactory con firma real y genera Creditor XML con US para USD")
    void testXmlFrenosServiceImpl_OrchestrationWithRealFactory_CurrencyUsdToUs() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(2L);
        row.setSupplierCode("SUP-FRENOS-01");
        row.setBusinessUnitCode(XMLConstants.FRENOS);
        row.setSupplierCurrency("USD");
        row.setCountryCode("MX");
        row.setErpIdQad("60001000");
        row.setXmlStatus(XmlStatus.PENDING);

        SupplierEntity supplierEntity = new SupplierEntity();

        frenosErp.setRassiniErpTaxClass("DEFAULT");
        frenosErp.setRassiniErpTaxZone(List.of("MEX"));
        frenosErp.setRassiniErpPaymentTerms("30");
        dto.setErpRecords(List.of(frenosErp));

        when(suppliersRowRepository.findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc(
                "SUP-FRENOS-01", XMLConstants.FRENOS
        )).thenReturn(Optional.of(row));

        ArgumentCaptor<CreditorXmlContext> creditorCaptor = ArgumentCaptor.forClass(CreditorXmlContext.class);

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(4);
            if (action != null) {
                action.run();
            }
            return null;
        }).when(xmlGenerationHelper).generateIfFileNotExists(
                any(), any(), any(), any(), any()
        );

        xmlFrenosService.generate(dto, supplierEntity);

        verify(xmlTemplateEngine).generateCreditorXml(
                eq(XmlConstants.TEMPLATE_FRENOS_CREDITOR),
                eq(XmlConstants.OUTPUT_FRENOS_DIR),
                creditorCaptor.capture()
        );

        CreditorXmlContext capturedCtx = creditorCaptor.getValue();
        assertNotNull(capturedCtx);
        assertEquals("US", capturedCtx.getCreditor().getTcCurrencyCode(),
                "tcCurrencyCode debe ser transformado a US para entrada persistida USD");
        assertNotEquals(ProviderState.ERRORMAPFRENOS, supplierEntity.getStatus(),
                "El estado del proveedor no debe marcar error en orquestación de Frenos");
    }
}
