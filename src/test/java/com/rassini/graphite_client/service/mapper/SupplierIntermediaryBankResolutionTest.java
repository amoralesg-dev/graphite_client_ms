package com.rassini.graphite_client.service.mapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.resolver.ErpResolutionResult;
import com.rassini.graphite_client.service.resolver.ErpResolutionStrategy;
import com.rassini.graphite_client.service.resolver.SupplierErpResolver;
import com.rassini.graphite_client.service.validation.collector.MissingDataCollector;
import com.rassini.graphite_client.service.validation.model.IssueSeverity;
import com.rassini.graphite_client.service.validation.model.IssueType;
import com.rassini.graphite_client.service.validation.model.MissingDataIssue;
import com.rassini.graphite_client.service.validation.model.OutputType;
import com.rassini.graphite_client.service.validation.service.OutputValidationService;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.impl.SupplierJpaMapperImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierIntermediaryBankResolutionTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private SuppliersRowRepository suppliersRowRepository;

    @Mock
    private SupplierErpResolver supplierErpResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger mapperLogger;
    private Logger jpaMapperLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        mapperLogger = (Logger) LoggerFactory.getLogger(SupplierRowMapper.class);
        jpaMapperLogger = (Logger) LoggerFactory.getLogger(SupplierJpaMapperImpl.class);
        mapperLogger.setLevel(Level.DEBUG);
        jpaMapperLogger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        mapperLogger.addAppender(listAppender);
        jpaMapperLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (mapperLogger != null && listAppender != null) {
            mapperLogger.detachAppender(listAppender);
        }
        if (jpaMapperLogger != null && listAppender != null) {
            jpaMapperLogger.detachAppender(listAppender);
        }
    }

    private SuppliersRowEntity crearFilaConIntermediarioAntiguo(Long id, String supplierCode, String bu, String account) {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(id);
        row.setErpIdQad("60000809");
        row.setSupplierCode(supplierCode);
        row.setBusinessUnitCode(bu);
        row.setAccountNumber(account);
        row.setBeneficiaryBankName("CAPITAL CITY BANK");
        row.setBeneficiaryAccountName("ELBERTA LOGISTICS INTERNATIONAL SOLUTIONS LLC");
        row.setBankCountry("US");
        row.setBankCurrency("USD");
        row.setRoutingCodeAba("063100688");
        row.setRoutingCodeSwift("AABBCCXX");
        row.setIntermediaryBankName("FIRST NATIONAL BANK");
        row.setIntermediaryAccount("INT-ACC-999");
        row.setIntermediaryAccountCountry("US");
        row.setIntermediaryRoutingCodeAba("065402892");
        row.setIntermediaryRoutingCodeSwift("FRNAUS44");
        return row;
    }

    @Test
    @DisplayName("1. Registro existente sin datos intermediarios previos y payload sin intermediario: permanece null (action=UNCHANGED)")
    void testScenario1_NoPreviousIntermediaryAndNoPayloadIntermediary_RemainsNull() throws Exception {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(1001L);
        row.setAccountNumber("10000730522");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "RASSINI_Legacy_QAD_ID": "60000809",
              "Supplier_Is_Legacy": "y",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_ACH_ABA_Routing": {
                        "routing": "063100688",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertNull(row.getIntermediaryBankName());
        assertNull(row.getIntermediaryAccount());
        assertNull(row.getIntermediaryAccountCountry());
        assertNull(row.getIntermediaryRoutingCodeAba());
        assertNull(row.getIntermediaryRoutingCodeSwift());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("supplier=US23134"));
        assertTrue(logMsg.contains("bu=09"));
        assertTrue(logMsg.contains("account=****0522"));
        assertTrue(logMsg.contains("payloadIntermediaryPresent=false"));
        assertTrue(logMsg.contains("previousIntermediaryDataPresent=false"));
        assertTrue(logMsg.contains("action=UNCHANGED"));
        assertTrue(logMsg.contains("clearedFields=[]"));
    }

    @Test
    @DisplayName("2. Registro existente con datos intermediarios y payload actual con nuevos valores: se actualizan los valores (action=UPDATED)")
    void testScenario2_PreviousIntermediaryAndNewPayloadIntermediary_UpdatesValues() throws Exception {
        SuppliersRowEntity row = crearFilaConIntermediarioAntiguo(10903L, "US23134", "09", "10000730522");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_Account_Currency_Correspondent": "y",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "CA",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "type": "SWIFT",
                        "routing": "CHASUS33",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertEquals("JPMorgan Chase Bank, NA", row.getIntermediaryBankName());
        assertEquals("CHASUS33", row.getIntermediaryRoutingCodeSwift());
        assertEquals("CA", row.getIntermediaryAccountCountry());
        assertNull(row.getIntermediaryAccount(), "El campo no enviado en el nuevo intermediario debe limpiarse");
        assertNull(row.getIntermediaryRoutingCodeAba(), "El ABA intermediario antiguo debe limpiarse cuando el nuevo es SWIFT");

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("payloadIntermediaryPresent=true"));
        assertTrue(logMsg.contains("previousIntermediaryDataPresent=true"));
        assertTrue(logMsg.contains("action=UPDATED"));
        assertTrue(logMsg.contains("intermediary_account"));
        assertTrue(logMsg.contains("intermediary_routing_code_aba"));
    }

    @Test
    @DisplayName("3. Registro existente con datos intermediarios y payload actual sin el segmento (Caso US23134): todos los campos intermediarios se actualizan a null (action=CLEARED)")
    void testScenario3_PreviousIntermediaryAndPayloadWithoutSegment_ClearsAllIntermediaryFields() throws Exception {
        SuppliersRowEntity row = crearFilaConIntermediarioAntiguo(10903L, "", "09", "10000730522");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "RASSINI_Legacy_QAD_ID": "60000809",
              "Supplier_Is_Legacy": "y",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_ACH_ABA_Routing": {
                        "bank_name": "Capital City Bank",
                        "type": "ABA",
                        "routing": "063100688",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertNull(row.getIntermediaryBankName());
        assertNull(row.getIntermediaryAccount());
        assertNull(row.getIntermediaryAccountCountry());
        assertNull(row.getIntermediaryRoutingCodeAba());
        assertNull(row.getIntermediaryRoutingCodeSwift());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("supplier=US23134"));
        assertTrue(logMsg.contains("bu=09"));
        assertTrue(logMsg.contains("account=****0522"));
        assertTrue(logMsg.contains("payloadIntermediaryPresent=false"));
        assertTrue(logMsg.contains("previousIntermediaryDataPresent=true"));
        assertTrue(logMsg.contains("action=CLEARED"));
        assertTrue(logMsg.contains("intermediary_bank_name"));
        assertTrue(logMsg.contains("intermediary_account"));
        assertTrue(logMsg.contains("intermediary_account_country"));
        assertTrue(logMsg.contains("intermediary_routing_code_aba"));
        assertTrue(logMsg.contains("intermediary_routing_code_swift"));
    }

    @Test
    @DisplayName("4. Registro existente con datos intermediarios y segmento presente con campos vacíos: los campos intermediarios se limpian (action=CLEARED)")
    void testScenario4_PreviousIntermediaryAndSegmentWithBlankFields_ClearsIntermediaryFields() throws Exception {
        SuppliersRowEntity row = crearFilaConIntermediarioAntiguo(10903L, "US23134", "09", "10000730522");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_Account_Currency_Correspondent": "",
                      "Bank_Account_Currency_Correspondent_Account": "   ",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "   ",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "   ",
                        "account_number": " ",
                        "swift": "",
                        "routing": " ",
                        "country": ""
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertNull(row.getIntermediaryBankName());
        assertNull(row.getIntermediaryAccount());
        assertNull(row.getIntermediaryAccountCountry());
        assertNull(row.getIntermediaryRoutingCodeAba());
        assertNull(row.getIntermediaryRoutingCodeSwift());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("payloadIntermediaryPresent=false"));
        assertTrue(logMsg.contains("previousIntermediaryDataPresent=true"));
        assertTrue(logMsg.contains("action=CLEARED"));
    }

    @Test
    @DisplayName("5. Proveedor con dos cuentas: limpia únicamente la cuenta correlacionada y no modifica la información intermediaria de la otra cuenta")
    void testScenario5_SupplierWithTwoAccounts_ClearsOnlyCorrelatedAccount() throws Exception {
        SuppliersRowEntity rowAccount1 = crearFilaConIntermediarioAntiguo(2001L, "US23134", "09", "10000730522");
        SuppliersRowEntity rowAccount2 = crearFilaConIntermediarioAntiguo(2002L, "US23134", "09", "99998888777");
        rowAccount2.setIntermediaryBankName("CITIBANK NA");
        rowAccount2.setIntermediaryRoutingCodeSwift("CITIUS33");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "RASSINI_Legacy_QAD_ID": "60000809",
              "Supplier_Is_Legacy": "y",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("US23134", "09", "10000730522"))
                .thenReturn(Optional.of(rowAccount1));
        when(supplierErpResolver.resolveEffectiveErpId(any(), any(), any(), any(), any()))
                .thenReturn(new ErpResolutionResult("60000809", ErpResolutionStrategy.LEGACY_QAD_ID));
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierJpaMapperImpl jpaMapper = new SupplierJpaMapperImpl(suppliersRowRepository, catalogService, supplierErpResolver);
        jpaMapper.upsertSuppliersRows(dto);

        // Cuenta 1 correlacionada -> intermediario limpiado
        assertNull(rowAccount1.getIntermediaryBankName());
        assertNull(rowAccount1.getIntermediaryRoutingCodeSwift());
        assertNull(rowAccount1.getIntermediaryAccountCountry());
        assertNull(rowAccount1.getIntermediaryRoutingCodeAba());

        // Cuenta 2 NO correlacionada con ese elemento -> intacta
        assertEquals("CITIBANK NA", rowAccount2.getIntermediaryBankName());
        assertEquals("CITIUS33", rowAccount2.getIntermediaryRoutingCodeSwift());
        assertEquals("US", rowAccount2.getIntermediaryAccountCountry());
    }

    @Test
    @DisplayName("6. Fallo de correlación de cuenta (mismatch): no limpia datos de otra fila y registra action=CORRELATION_ERROR")
    void testScenario6_AccountCorrelationFailure_DoesNotClearOtherRowAndLogsProblem() throws Exception {
        SuppliersRowEntity otherAccountRow = crearFilaConIntermediarioAntiguo(3001L, "US23134", "09", "99990000111");

        String jsonWithMismatchedAccount = """
            {
              "Entity_Public_Id": "US23134",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank"
                    }
                  ]
                }
              ]
            }
            """;

        // Intentar mapear un banco de cuenta ****0522 sobre una fila existente cuya cuenta es ****0111
        mapOntoRow(otherAccountRow, jsonWithMismatchedAccount, 0);

        // Los datos intermediarios de la otra fila NO deben haberse limpiado
        assertEquals("FIRST NATIONAL BANK", otherAccountRow.getIntermediaryBankName());
        assertEquals("INT-ACC-999", otherAccountRow.getIntermediaryAccount());
        assertEquals("US", otherAccountRow.getIntermediaryAccountCountry());
        assertEquals("065402892", otherAccountRow.getIntermediaryRoutingCodeAba());
        assertEquals("FRNAUS44", otherAccountRow.getIntermediaryRoutingCodeSwift());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("action=CORRELATION_ERROR"));
        assertTrue(logMsg.contains("clearedFields=[]"));
    }

    @Test
    @DisplayName("7. Confirmar que banco beneficiario, cuenta principal, ABA y SWIFT principal NO se limpian al limpiar el banco intermediario")
    void testScenario7_BeneficiaryBankMainAccountAbaAndSwiftArePreservedWhenIntermediaryCleared() throws Exception {
        SuppliersRowEntity row = crearFilaConIntermediarioAntiguo(10903L, "US23134", "09", "10000730522");

        String json = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "RASSINI_Legacy_QAD_ID": "60000809",
              "Supplier_Is_Legacy": "y",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "10000730522",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Account_Holder_Name": "Elberta Logistics International Solutions, LLC",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_Currency_List": ["USD"],
                      "Bank_SWIFT": {
                        "routing": "CCBKUS3T",
                        "valid": true
                      },
                      "Bank_ACH_ABA_Routing": {
                        "bank_name": "Capital City Bank",
                        "type": "ABA",
                        "routing": "063100688",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        // Campos principales intactos / actualizados según el payload actual
        assertEquals("Capital City Bank", row.getBeneficiaryBankName());
        assertEquals("Elberta Logistics International Solutions, LLC", row.getBeneficiaryAccountName());
        assertEquals("10000730522", row.getAccountNumber());
        assertEquals("US", row.getBankCountry());
        assertEquals("USD", row.getBankCurrency());
        assertEquals("063100688", row.getRoutingCodeAba());
        assertEquals("CCBKUS3T", row.getRoutingCodeSwift());

        // Y únicamente los 5 campos del intermediario quedan null
        assertNull(row.getIntermediaryBankName());
        assertNull(row.getIntermediaryAccount());
        assertNull(row.getIntermediaryAccountCountry());
        assertNull(row.getIntermediaryRoutingCodeAba());
        assertNull(row.getIntermediaryRoutingCodeSwift());
    }

    @Test
    @DisplayName("8. PUNTO 1 (5): Payload con nombre de banco intermediario, cuenta intermediaria, país y SWIFT/ABA persiste intermediary_account")
    void testScenario8_PayloadWithIntermediaryAccountAndFullBlock_PersistsIntermediaryAccount() throws Exception {
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(4001L);
        row.setAccountNumber("4001459");

        String json = """
            {
              "Entity_Public_Id": "CA194265",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "4001459",
                      "Bank_Name": "Royal Bank of Canada",
                      "Bank_Country": "CA",
                      "Bank_Account_Currency": "USD",
                      "Bank_Account_Currency_Correspondent": "y",
                      "Bank_Account_Currency_Correspondent_Account": "CORR-ACC-778899",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "US",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "type": "SWIFT",
                        "routing": "CHASUS33",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertEquals("JPMorgan Chase Bank, NA", row.getIntermediaryBankName());
        assertEquals("CORR-ACC-778899", row.getIntermediaryAccount(), "intermediary_account debe persistir el valor recibido, no null");
        assertEquals("US", row.getIntermediaryAccountCountry());
        assertEquals("CHASUS33", row.getIntermediaryRoutingCodeSwift());
        assertNull(row.getIntermediaryRoutingCodeAba());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("payloadIntermediaryPresent=true"));
        assertTrue(logMsg.contains("action=UPDATED"));
        assertTrue(logMsg.contains("clearedFields=[]"));
    }

    @Test
    @DisplayName("9. PUNTO 1 (6): Actualización de cuenta intermediaria cuando BD contiene una cuenta anterior y el payload trae una nueva")
    void testScenario9_UpdateIntermediaryAccount_ReplacesOldAccountWithNew() throws Exception {
        SuppliersRowEntity row = crearFilaConIntermediarioAntiguo(4002L, "CA194265", "09", "4001459");
        row.setIntermediaryAccount("OLD-INTERM-0001");

        String json = """
            {
              "Entity_Public_Id": "CA194265",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "4001459",
                      "Bank_Name": "Royal Bank of Canada",
                      "Bank_Country": "CA",
                      "Bank_Account_Currency": "USD",
                      "Bank_Account_Currency_Correspondent": "y",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "US",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "account_number": "NEW-INTERM-9999",
                        "type": "ABA",
                        "routing": "021000021",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mapOntoRow(row, json, 0);

        assertEquals("JPMorgan Chase Bank, NA", row.getIntermediaryBankName());
        assertEquals("NEW-INTERM-9999", row.getIntermediaryAccount(), "La cuenta intermediaria anterior debe reemplazarse por la nueva");
        assertEquals("US", row.getIntermediaryAccountCountry());
        assertEquals("021000021", row.getIntermediaryRoutingCodeAba());
        assertNull(row.getIntermediaryRoutingCodeSwift());

        String logMsg = findIntermediaryLog();
        assertTrue(logMsg.contains("payloadIntermediaryPresent=true"));
        assertTrue(logMsg.contains("previousIntermediaryDataPresent=true"));
        assertTrue(logMsg.contains("action=UPDATED"));
    }

    @Test
    @DisplayName("10. PUNTO 2: Bank_Account_Number vacío con y sin datos intermediarios mantiene el flujo histórico e incidencia sin limpiar otra cuenta")
    void testScenario10_BlankBankAccountNumber_WithAndWithoutIntermediary_PreservesHistoricalFlowAndDoesNotClearOtherAccount() throws Exception {
        SuppliersRowEntity existingOtherAccount = crearFilaConIntermediarioAntiguo(5001L, "US23134", "09", "10000730522");

        // Caso A: Bank_Account_Number vacío CON datos intermediarios en payload
        String jsonBlankWithIntermediary = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD",
                      "Bank_Currency_List": ["USD"],
                      "Bank_Account_Currency_Correspondent": "y",
                      "Bank_Account_Currency_Correspondent_Account": "INT-777",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "US",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "type": "SWIFT",
                        "routing": "CHASUS33",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("US23134", "09", null))
                .thenReturn(Optional.empty());
        when(supplierErpResolver.resolveEffectiveErpId(any(), any(), any(), any(), any()))
                .thenReturn(new ErpResolutionResult("60003152", ErpResolutionStrategy.DTO_ERP_ID_QAD));
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierJpaMapperImpl jpaMapper = new SupplierJpaMapperImpl(suppliersRowRepository, catalogService, supplierErpResolver);
        GraphiteSupplierDto dtoWithInterm = objectMapper.readValue(jsonBlankWithIntermediary, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dtoWithInterm);

        ArgumentCaptor<SuppliersRowEntity> savedCaptor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository, times(1)).save(savedCaptor.capture());
        SuppliersRowEntity savedRowWithInterm = savedCaptor.getValue();

        // 1) Se conserva el flujo histórico de la fila bancaria (no se omite con continue)
        assertEquals("US23134", savedRowWithInterm.getSupplierCode());
        assertEquals("09", savedRowWithInterm.getBusinessUnitCode());
        assertEquals("Capital City Bank", savedRowWithInterm.getBeneficiaryBankName());
        assertNull(savedRowWithInterm.getAccountNumber());

        // 2) No se ejecutó actualización de banco intermediario por falta de correlación segura (action=CORRELATION_ERROR)
        assertNull(savedRowWithInterm.getIntermediaryBankName());
        assertNull(savedRowWithInterm.getIntermediaryAccount());
        String logWithInterm = findIntermediaryLog();
        assertTrue(logWithInterm.contains("account=N/A"));
        assertTrue(logWithInterm.contains("payloadIntermediaryPresent=true"));
        assertTrue(logWithInterm.contains("action=CORRELATION_ERROR"));

        // 3) No se limpió la otra cuenta existente del proveedor
        assertEquals("FIRST NATIONAL BANK", existingOtherAccount.getIntermediaryBankName());
        assertEquals("INT-ACC-999", existingOtherAccount.getIntermediaryAccount());
        assertEquals("FRNAUS44", existingOtherAccount.getIntermediaryRoutingCodeSwift());

        // 4) Se mantiene la validación e incidencia histórica bloqueante en OutputValidationService (BANK_ACCOUNT_MISSING)
        MissingDataCollector collector = new MissingDataCollector();
        OutputValidationService validationService = new OutputValidationService(collector);
        IssueSeverity severity = validationService.evaluateIntegrityRow(savedRowWithInterm, "09");
        assertEquals(IssueSeverity.BLOCKING, severity);
        List<MissingDataIssue> issues = collector.getIssues();
        assertEquals(1, issues.size());
        assertEquals(IssueType.BANK_ACCOUNT_MISSING, issues.get(0).getIssueType());
        assertEquals("accountNumber", issues.get(0).getFieldName());
        assertEquals(OutputType.INTEGRITY, issues.get(0).getOutputType());

        // Caso B: Bank_Account_Number vacío SIN datos intermediarios en payload
        listAppender.list.clear();
        reset(suppliersRowRepository);
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("US23134", "09", null))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String jsonBlankWithoutIntermediary = """
            {
              "Entity_Public_Id": "US23134",
              "RASSINI_ERP_ID": "60003152",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Name": "Capital City Bank",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dtoWithoutInterm = objectMapper.readValue(jsonBlankWithoutIntermediary, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dtoWithoutInterm);

        verify(suppliersRowRepository, times(1)).save(any(SuppliersRowEntity.class));
        String logWithoutInterm = findIntermediaryLog();
        assertTrue(logWithoutInterm.contains("account=N/A"));
        assertTrue(logWithoutInterm.contains("payloadIntermediaryPresent=false"));
        assertTrue(logWithoutInterm.contains("action=CORRELATION_ERROR"));
        assertEquals("FIRST NATIONAL BANK", existingOtherAccount.getIntermediaryBankName());
    }

    private void mapOntoRow(SuppliersRowEntity row, String json, int bankIndex) throws Exception {
        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(bankIndex);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);
    }

    private String findIntermediaryLog() {
        List<String> logs = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("[INTERMEDIARY-BANK-RESOLUTION]"))
                .toList();
        assertFalse(logs.isEmpty(), "Debe existir al menos un log [INTERMEDIARY-BANK-RESOLUTION]");
        return logs.get(logs.size() - 1);
    }
}
