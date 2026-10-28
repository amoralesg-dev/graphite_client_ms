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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierAccountIbanResolutionTest {

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
    private SupplierJpaMapperImpl jpaMapper;

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
        jpaMapper = new SupplierJpaMapperImpl(suppliersRowRepository, catalogService, supplierErpResolver);
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

    @Test
    @DisplayName("1. Bank_Account_Number informado: conservarlo y no reemplazarlo por IBAN")
    void test1_BankAccountNumberInformed_ConservedNotReplacedByIban() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "DE185951",
              "RASSINI_ERP_ID": "60003025",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "0099887766",
                      "Bank_Account_Number_IBAN": "0108365131",
                      "Bank_Account_IBAN_No_Formatting": "DE94660501010108365131",
                      "Bank_Account_IBAN": {
                        "type": "IBAN",
                        "valid": true,
                        "routing": "DE94 6605 0101 0108 3651 31"
                      },
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "EUR"
                    }
                  ]
                }
              ]
            }
            """;

        stubDefaultErp("60003025");
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "0099887766"))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> {
            SuppliersRowEntity r = inv.getArgument(0);
            if (r.getId() == null) r.setId(101L);
            return r;
        });

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> captor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(captor.capture());
        assertEquals("0099887766", captor.getValue().getAccountNumber());

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("accountActualPresent=true"));
        assertTrue(logMsg.contains("ibanPresent=true"));
        assertTrue(logMsg.contains("accountFinal=****7766"));
        assertTrue(logMsg.contains("source=Bank_Account_Number"));
        assertTrue(logMsg.contains("matchStrategy=NEW_ROW"));
        assertFalse(logMsg.contains("0099887766"));
        assertFalse(logMsg.contains("DE94660501010108365131"));
    }

    @Test
    @DisplayName("2. Bank_Account_Number vacío e IBAN_No_Formatting válido: usar IBAN completo")
    void test2_BankAccountNumberEmptyAndIbanNoFormattingValid_UsesFullIban() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "DE185951",
              "RASSINI_ERP_ID": "60003025",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "0108365131",
                      "Bank_Account_IBAN_No_Formatting": "DE94660501010108365131",
                      "Bank_Account_IBAN": {
                        "bank_name": "Sparkasse Karlsruhe",
                        "swift": "KARSDE66",
                        "type": "IBAN",
                        "routing": "DE94 6605 0101 0108 3651 31",
                        "valid": true,
                        "fromDatabase": false
                      },
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "EUR"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);

        assertEquals("Bank_Account_IBAN_No_Formatting", bank.resolveBankAccountSource());
        assertEquals("DE94660501010108365131", bank.getBankAccountNumber());
    }

    @Test
    @DisplayName("3. IBAN_No_Formatting vacío y Bank_Account_IBAN.routing válido: usar routing normalizado")
    void test3_IbanNoFormattingEmptyAndRoutingValid_UsesRouting() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "ES186161",
              "RASSINI_ERP_ID": "60003027",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "400200054858",
                      "Bank_Account_IBAN_No_Formatting": "",
                      "Bank_Account_IBAN": {
                        "bank_name": "Banco Bilbao Vizcaya Argentaria SA",
                        "swift": "BBVAESMM",
                        "type": "IBAN",
                        "routing": "ES35 0182 5333 4002 0005 4858",
                        "valid": true,
                        "fromDatabase": false
                      },
                      "Bank_Country": "ES",
                      "Bank_Account_Currency": "EUR"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);

        assertEquals("Bank_Account_IBAN", bank.resolveBankAccountSource());
        assertEquals("ES3501825333400200054858", bank.getBankAccountNumber());
    }

    @Test
    @DisplayName("4. IBAN con espacios internos y laterales: normalizar eliminando todos los espacios")
    void test4_IbanWithSpaces_RemovesSpaces() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "BR188875",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "   ",
                      "Bank_Account_IBAN_No_Formatting": "  BR26 0036 0305 0747 9579 3154 791C 1  ",
                      "Bank_Account_IBAN": {
                        "type": "IBAN",
                        "valid": true,
                        "routing": "BR26 0036 0305 0747 9579 3154 791C 1"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        assertEquals("BR2600360305074795793154791C1",
                dto.getErpRecords().get(0).getErpBankList().get(0).getBankAccountNumber());
    }

    @Test
    @DisplayName("5. IBAN con letras minúsculas: convertir a mayúsculas con Locale.ROOT")
    void test5_IbanWithLowercase_ConvertsToUppercase() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "BR188875",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_IBAN_No_Formatting": "br26 0036 0305 0747 9579 3154 791c 1",
                      "Bank_Account_IBAN": {
                        "type": "iban",
                        "valid": true,
                        "routing": "br26 0036 0305 0747 9579 3154 791c 1"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        assertEquals("BR2600360305074795793154791C1",
                dto.getErpRecords().get(0).getErpBankList().get(0).getBankAccountNumber());
    }

    @Test
    @DisplayName("6. Ceros a la izquierda y caracteres alfanuméricos: conservarlos sin conversión numérica")
    void test6_LeadingZerosAndLetters_Conserved() throws Exception {
        String jsonDomesticZeros = """
            {
              "Entity_Public_Id": "MX100001",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "00001234567890"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto1 = objectMapper.readValue(jsonDomesticZeros, GraphiteSupplierDto.class);
        assertEquals("00001234567890", dto1.getErpRecords().get(0).getErpBankList().get(0).getBankAccountNumber());

        String jsonFallbackDomesticZeros = """
            {
              "Entity_Public_Id": "DE185951",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "0108365131"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto2 = objectMapper.readValue(jsonFallbackDomesticZeros, GraphiteSupplierDto.class);
        assertEquals("0108365131", dto2.getErpRecords().get(0).getErpBankList().get(0).getBankAccountNumber());
    }

    @Test
    @DisplayName("7. Fuentes vacías: retorna null (source=None) y mantiene incidencia BANK_ACCOUNT_MISSING")
    void test7_AllSourcesEmpty_MaintainsBankAccountMissing() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "DE999999",
              "RASSINI_ERP_ID": "60009999",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "",
                      "Bank_Account_IBAN_No_Formatting": "",
                      "Bank_Account_IBAN": "",
                      "Bank_Account_IBAN_Override": "y",
                      "Bank_IBAN_Override_BR": "y",
                      "Bank_Name": "Sparkasse",
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "EUR",
                      "Bank_Currency_List": ["EUR"]
                    }
                  ]
                }
              ]
            }
            """;

        stubDefaultErp("60009999");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        ArgumentCaptor<SuppliersRowEntity> captor = ArgumentCaptor.forClass(SuppliersRowEntity.class);
        verify(suppliersRowRepository).save(captor.capture());
        SuppliersRowEntity savedRow = captor.getValue();
        assertNull(savedRow.getAccountNumber(), "Override 'y' no debe usarse como cuenta bancaria");

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("source=None"));
        assertTrue(logMsg.contains("matchStrategy=NONE"));

        MissingDataCollector collector = new MissingDataCollector();
        OutputValidationService validationService = new OutputValidationService(collector);
        IssueSeverity severity = validationService.evaluateIntegrityRow(savedRow, "1000");
        assertEquals(IssueSeverity.BLOCKING, severity);
        List<MissingDataIssue> issues = collector.getIssues();
        assertEquals(1, issues.size());
        assertEquals(IssueType.BANK_ACCOUNT_MISSING, issues.get(0).getIssueType());
    }

    @Test
    @DisplayName("8. Dos cuentas con distintos IBAN: crear o actualizar cada fila de forma independiente")
    void test8_TwoAccountsWithDifferentIbans_CorrelatedIndependently() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "DE185951",
              "RASSINI_ERP_ID": "60003025",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "0108365131",
                      "Bank_Account_IBAN_No_Formatting": "DE94660501010108365131",
                      "Bank_Account_IBAN": { "type": "IBAN", "valid": true, "routing": "DE94 6605 0101 0108 3651 31" },
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "EUR"
                    },
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "0209998877",
                      "Bank_Account_IBAN_No_Formatting": "DE12500105170209998877",
                      "Bank_Account_IBAN": { "type": "IBAN", "valid": true, "routing": "DE12 5001 0517 0209 9988 77" },
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "USD"
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row1 = new SuppliersRowEntity();
        row1.setId(10L);
        row1.setSupplierCode("DE185951");
        row1.setBusinessUnitCode("1000");
        row1.setAccountNumber("0108365131");
        row1.setSupplierCodeDisIntegrity("60003025");

        SuppliersRowEntity row2 = new SuppliersRowEntity();
        row2.setId(20L);
        row2.setSupplierCode("DE185951");
        row2.setBusinessUnitCode("1000");
        row2.setAccountNumber("DE12500105170209998877");
        row2.setSupplierCodeDisIntegrity("60003025_1");

        stubDefaultErp("60003025");
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "DE94660501010108365131"))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "0108365131"))
                .thenReturn(Optional.of(row1));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "DE12500105170209998877"))
                .thenReturn(Optional.of(row2));
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        assertEquals(10L, row1.getId());
        assertEquals("DE94660501010108365131", row1.getAccountNumber());
        assertEquals("60003025", row1.getSupplierCodeDisIntegrity());

        assertEquals(20L, row2.getId());
        assertEquals("DE12500105170209998877", row2.getAccountNumber());
        assertEquals("60003025_1", row2.getSupplierCodeDisIntegrity());
    }

    @Test
    @DisplayName("9. Mismo IBAN con y sin espacios en BD: encontrar la misma fila (EXACT_IBAN) y no duplicar")
    void test9_SameIbanWithAndWithoutSpaces_MatchesSameRow() throws Exception {
        SuppliersRowEntity existingWithSpaces = new SuppliersRowEntity();
        existingWithSpaces.setId(55L);
        existingWithSpaces.setSupplierCode("DE185951");
        existingWithSpaces.setBusinessUnitCode("1000");
        existingWithSpaces.setAccountNumber("DE94 6605 0101 0108 3651 31");
        existingWithSpaces.setSupplierCodeDisIntegrity("60003025");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "DE94660501010108365131"))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findAllBySupplierCodeAndBusinessUnitCode("DE185951", "1000"))
                .thenReturn(List.of(existingWithSpaces));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "0108365131"))
                .thenReturn(Optional.empty());
        stubDefaultErp("60003025");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String json = buildSingleBankIbanJson("DE185951", "60003025", "1000", "DE", "EUR",
                "", "0108365131", "DE94660501010108365131", "DE94 6605 0101 0108 3651 31", true);

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        assertEquals(55L, existingWithSpaces.getId());
        assertEquals("DE94660501010108365131", existingWithSpaces.getAccountNumber());
        assertEquals("60003025", existingWithSpaces.getSupplierCodeDisIntegrity());

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("matchStrategy=EXACT_IBAN"));
        assertTrue(logMsg.contains("selectedRowId=55"));
        assertTrue(logMsg.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("10. Proveedor no IBAN que ya funciona: conservar comportamiento actual (EXACT_ACCOUNT)")
    void test10_NonIbanSupplier_PreservesCurrentBehavior() throws Exception {
        SuppliersRowEntity existing = new SuppliersRowEntity();
        existing.setId(99L);
        existing.setSupplierCode("US192521");
        existing.setBusinessUnitCode("1000");
        existing.setAccountNumber("687769292");
        existing.setSupplierCodeDisIntegrity("60003088");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("US192521", "1000", "687769292"))
                .thenReturn(Optional.of(existing));
        stubDefaultErp("60003088");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String json = """
            {
              "Entity_Public_Id": "US192521",
              "RASSINI_ERP_ID": "60003088",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "687769292",
                      "Bank_Account_IBAN": "",
                      "Bank_Country": "US",
                      "Bank_Account_Currency": "USD"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        assertEquals(99L, existing.getId());
        assertEquals("687769292", existing.getAccountNumber());
        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("source=Bank_Account_Number"));
        assertTrue(logMsg.contains("matchStrategy=EXACT_ACCOUNT"));
        assertTrue(logMsg.contains("selectedRowId=99"));
        assertTrue(logMsg.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("11. Solo existe fila doméstica: migrarla a IBAN completo conservando id y supplier_code_dis_integrity (LEGACY_DOMESTIC_MIGRATION)")
    void test11_OnlyDomesticRowExists_MigratesConservingIdAndDisIntegrity() throws Exception {
        SuppliersRowEntity domesticRow = new SuppliersRowEntity();
        domesticRow.setId(6L);
        domesticRow.setSupplierCode("DE185951");
        domesticRow.setErpIdQad("60003025");
        domesticRow.setBusinessUnitCode("1000");
        domesticRow.setAccountNumber("0108365131");
        domesticRow.setSupplierCodeDisIntegrity("60003025");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "DE94660501010108365131"))
                .thenReturn(Optional.empty());
        when(suppliersRowRepository.findAllBySupplierCodeAndBusinessUnitCode("DE185951", "1000"))
                .thenReturn(List.of(domesticRow));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("DE185951", "1000", "0108365131"))
                .thenReturn(Optional.of(domesticRow));
        stubDefaultErp("60003025");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String json = buildSingleBankIbanJson("DE185951", "60003025", "1000", "DE", "EUR",
                "", "0108365131", "DE94660501010108365131", "DE94 6605 0101 0108 3651 31", true);

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        assertEquals(6L, domesticRow.getId());
        assertEquals("DE94660501010108365131", domesticRow.getAccountNumber());
        assertEquals("60003025", domesticRow.getSupplierCodeDisIntegrity());

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("matchStrategy=LEGACY_DOMESTIC_MIGRATION"));
        assertTrue(logMsg.contains("selectedRowId=6"));
        assertTrue(logMsg.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("12. Solo existe fila con IBAN completo: actualizarla sin insertar otra (EXACT_IBAN)")
    void test12_OnlyIbanRowExists_UpdatesWithoutInserting() throws Exception {
        SuppliersRowEntity ibanRow = new SuppliersRowEntity();
        ibanRow.setId(19L);
        ibanRow.setSupplierCode("ES186161");
        ibanRow.setErpIdQad("60003027");
        ibanRow.setBusinessUnitCode("1000");
        ibanRow.setAccountNumber("ES3501825333400200054858");
        ibanRow.setSupplierCodeDisIntegrity("600030271");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("ES186161", "1000", "ES3501825333400200054858"))
                .thenReturn(Optional.of(ibanRow));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("ES186161", "1000", "400200054858"))
                .thenReturn(Optional.empty());
        stubDefaultErp("60003027");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String json = buildSingleBankIbanJson("ES186161", "60003027", "1000", "ES", "EUR",
                "", "400200054858", "ES3501825333400200054858", "ES35 0182 5333 4002 0005 4858", true);

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        assertEquals(19L, ibanRow.getId());
        assertEquals("ES3501825333400200054858", ibanRow.getAccountNumber());
        assertEquals("600030271", ibanRow.getSupplierCodeDisIntegrity());

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("matchStrategy=EXACT_IBAN"));
        assertTrue(logMsg.contains("selectedRowId=19"));
        assertTrue(logMsg.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("13. Existen simultáneamente fila IBAN y fila doméstica: actualizar la fila IBAN, no tocar la doméstica y no insertar tercera (EXISTING_IBAN_WITH_LEGACY_DOMESTIC_ROW)")
    void test13_BothIbanAndDomesticRowsExist_UpdatesIbanRowLeavesDomesticUntouched() throws Exception {
        SuppliersRowEntity ibanRow = new SuppliersRowEntity();
        ibanRow.setId(100L);
        ibanRow.setSupplierCode("BR188875");
        ibanRow.setErpIdQad("60003043");
        ibanRow.setBusinessUnitCode("1000");
        ibanRow.setAccountNumber("BR2600360305074795793154791C1");
        ibanRow.setSupplierCodeDisIntegrity("600030431");

        SuppliersRowEntity domesticRow = new SuppliersRowEntity();
        domesticRow.setId(33744L);
        domesticRow.setSupplierCode("BR188875");
        domesticRow.setErpIdQad("60003043");
        domesticRow.setBusinessUnitCode("1000");
        domesticRow.setAccountNumber("5793154791C1");
        domesticRow.setSupplierCodeDisIntegrity("60003043_OLD");

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("BR188875", "1000", "BR2600360305074795793154791C1"))
                .thenReturn(Optional.of(ibanRow));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber("BR188875", "1000", "5793154791C1"))
                .thenReturn(Optional.of(domesticRow));
        stubDefaultErp("60003043");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String json = buildSingleBankIbanJson("BR188875", "60003043", "1000", "BR", "BRL",
                "", "5793154791C1", "BR2600360305074795793154791C1", "BR26 0036 0305 0747 9579 3154 791C 1", true);

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        jpaMapper.upsertSuppliersRows(dto);

        verify(suppliersRowRepository, times(1)).save(ibanRow);
        assertEquals(100L, ibanRow.getId());
        assertEquals("BR2600360305074795793154791C1", ibanRow.getAccountNumber());
        assertEquals("600030431", ibanRow.getSupplierCodeDisIntegrity());

        // La fila doméstica no fue modificada
        assertEquals(33744L, domesticRow.getId());
        assertEquals("5793154791C1", domesticRow.getAccountNumber());
        assertEquals("60003043_OLD", domesticRow.getSupplierCodeDisIntegrity());

        String logMsg = findAccountLog();
        assertTrue(logMsg.contains("matchStrategy=EXISTING_IBAN_WITH_LEGACY_DOMESTIC_ROW"));
        assertTrue(logMsg.contains("selectedRowId=100"));
        assertTrue(logMsg.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("14. Segundo reproceso idempotente: primer reproceso migra fila doméstica y segundo reproceso actualiza la misma fila por EXACT_IBAN con cero duplicados")
    void test14_SecondReprocessIdempotent_SameIdZeroDuplicates() throws Exception {
        List<SuppliersRowEntity> dbTable = new ArrayList<>();
        SuppliersRowEntity row = new SuppliersRowEntity();
        row.setId(6L);
        row.setSupplierCode("DE185951");
        row.setErpIdQad("60003025");
        row.setBusinessUnitCode("1000");
        row.setAccountNumber("0108365131");
        row.setSupplierCodeDisIntegrity("60003025");
        dbTable.add(row);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String sCode = inv.getArgument(0);
                    String bu = inv.getArgument(1);
                    String acc = inv.getArgument(2);
                    return dbTable.stream()
                            .filter(r -> sCode.equals(r.getSupplierCode()) && bu.equals(r.getBusinessUnitCode()) && java.util.Objects.equals(acc, r.getAccountNumber()))
                            .findFirst();
                });
        when(suppliersRowRepository.findAllBySupplierCodeAndBusinessUnitCode(anyString(), anyString()))
                .thenAnswer(inv -> dbTable);
        stubDefaultErp("60003025");
        when(suppliersRowRepository.save(any(SuppliersRowEntity.class))).thenAnswer(inv -> {
            SuppliersRowEntity saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(999L);
                dbTable.add(saved);
            }
            return saved;
        });

        String json = buildSingleBankIbanJson("DE185951", "60003025", "1000", "DE", "EUR",
                "", "0108365131", "DE94660501010108365131", "DE94 6605 0101 0108 3651 31", true);
        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);

        // 1er reproceso: migra la fila id=6
        jpaMapper.upsertSuppliersRows(dto);
        assertEquals(1, dbTable.size());
        assertEquals(6L, dbTable.get(0).getId());
        assertEquals("DE94660501010108365131", dbTable.get(0).getAccountNumber());
        assertTrue(findAccountLog().contains("matchStrategy=LEGACY_DOMESTIC_MIGRATION"));

        // 2do reproceso: selecciona exactamente la misma fila id=6 con EXACT_IBAN
        listAppender.list.clear();
        jpaMapper.upsertSuppliersRows(dto);
        assertEquals(1, dbTable.size(), "Cero duplicados en el segundo reproceso");
        assertEquals(6L, dbTable.get(0).getId());
        assertEquals("DE94660501010108365131", dbTable.get(0).getAccountNumber());
        assertEquals("60003025", dbTable.get(0).getSupplierCodeDisIntegrity());
        String secondLog = findAccountLog();
        assertTrue(secondLog.contains("matchStrategy=EXACT_IBAN"));
        assertTrue(secondLog.contains("selectedRowId=6"));
        assertTrue(secondLog.contains("insertPerformed=false"));
    }

    @Test
    @DisplayName("15. Bank_Account_IBAN.valid=false: no usar IBAN_No_Formatting ni routing; hacer fallback a Bank_Account_Number_IBAN o null")
    void test15_BankAccountIbanValidFalse_DoesNotUseIbanAndFallsBackToDomesticOrNull() throws Exception {
        String jsonWithDomesticFallback = buildSingleBankIbanJson("DE185951", "60003025", "1000", "DE", "EUR",
                "", "0108365131", "DE94660501010108365131", "DE94 6605 0101 0108 3651 31", false);

        GraphiteSupplierDto dto1 = objectMapper.readValue(jsonWithDomesticFallback, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Bank bank1 = dto1.getErpRecords().get(0).getErpBankList().get(0);
        assertEquals("Bank_Account_Number_IBAN", bank1.resolveBankAccountSource());
        assertEquals("0108365131", bank1.getBankAccountNumber());

        String jsonWithoutDomesticFallback = buildSingleBankIbanJson("DE185951", "60003025", "1000", "DE", "EUR",
                "", "", "DE94660501010108365131", "DE94 6605 0101 0108 3651 31", false);

        GraphiteSupplierDto dto2 = objectMapper.readValue(jsonWithoutDomesticFallback, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Bank bank2 = dto2.getErpRecords().get(0).getErpBankList().get(0);
        assertEquals("None", bank2.resolveBankAccountSource());
        assertNull(bank2.getBankAccountNumber());
    }

    @Test
    @DisplayName("16. Bank_Account_IBAN como string vacío: deserializar sin error")
    void test16_BankAccountIbanAsEmptyString_DeserializesWithoutError() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "NN199481",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "0111",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "646180378600200957",
                      "Bank_Account_IBAN": ""
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = assertDoesNotThrow(() -> objectMapper.readValue(json, GraphiteSupplierDto.class));
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);
        assertNull(bank.getBankAccountIban());
        assertEquals("646180378600200957", bank.getBankAccountNumber());
    }

    @Test
    @DisplayName("17. Bank_Account_IBAN como string no vacío: capturarlo como routing y normalizarlo")
    void test17_BankAccountIbanAsNonEmptyString_CapturesAsRoutingAndNormalizes() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "ES186161",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_IBAN_No_Formatting": "",
                      "Bank_Account_IBAN": "es35 0182 5333 4002 0005 4858"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Bank bank = dto.getErpRecords().get(0).getErpBankList().get(0);
        assertNotNull(bank.getBankAccountIban());
        assertEquals("Bank_Account_IBAN", bank.resolveBankAccountSource());
        assertEquals("ES3501825333400200054858", bank.getBankAccountNumber());
    }

    @Test
    @DisplayName("18. Migración de cuenta doméstica a IBAN completo con banco intermediario: mantiene correlación sin CORRELATION_ERROR")
    void test18_MigrationWithIntermediaryBank_MaintainsCorrelationWithoutError() throws Exception {
        SuppliersRowEntity domesticRow = new SuppliersRowEntity();
        domesticRow.setId(6L);
        domesticRow.setSupplierCode("DE185951");
        domesticRow.setBusinessUnitCode("1000");
        domesticRow.setAccountNumber("0108365131");
        domesticRow.setIntermediaryBankName("COMMERZBANK AG");
        domesticRow.setIntermediaryRoutingCodeSwift("COBADEFF");

        String json = """
            {
              "Entity_Public_Id": "DE185951",
              "RASSINI_ERP_ID": "60003025",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "1000",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "",
                      "Bank_Account_Number_IBAN": "0108365131",
                      "Bank_Account_IBAN_No_Formatting": "DE94660501010108365131",
                      "Bank_Account_IBAN": {
                        "type": "IBAN",
                        "valid": true,
                        "routing": "DE94 6605 0101 0108 3651 31"
                      },
                      "Bank_Country": "DE",
                      "Bank_Account_Currency": "EUR",
                      "Bank_Account_Currency_Correspondent": "y",
                      "Bank_Account_Currency_Correspondent_Bank_Country": "DE",
                      "Bank_Account_Currency_Correspondent_Account": "DE-INT-001",
                      "Bank_Account_Currency_Correspondent_Bank": {
                        "bank_name": "DEUTSCHE BANK AG",
                        "type": "SWIFT",
                        "routing": "DEUTDEFF",
                        "valid": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);
        SupplierRowMapper.fill(domesticRow, dto, SupplierRowMapper.findHeadquarters(dto), erp, bank, catalogService);

        assertEquals("DE94660501010108365131", domesticRow.getAccountNumber());
        assertEquals("DEUTSCHE BANK AG", domesticRow.getIntermediaryBankName());
        assertEquals("DEUTDEFF", domesticRow.getIntermediaryRoutingCodeSwift());
        assertEquals("DE-INT-001", domesticRow.getIntermediaryAccount());
        assertEquals("DE", domesticRow.getIntermediaryAccountCountry());

        String intermLog = findIntermediaryLog();
        assertFalse(intermLog.contains("CORRELATION_ERROR"), "No debe producirse CORRELATION_ERROR durante la migración de cuenta doméstica a IBAN");
        assertTrue(intermLog.contains("action=UPDATED"));
    }

    private void stubDefaultErp(String erpId) {
        lenient().when(supplierErpResolver.resolveEffectiveErpId(any(), any(), any(), any(), any()))
                .thenReturn(new ErpResolutionResult(erpId, ErpResolutionStrategy.DTO_ERP_ID_QAD));
    }

    private String buildSingleBankIbanJson(
            String publicId,
            String erpId,
            String bu,
            String country,
            String currency,
            String bankAccountNumber,
            String bankAccountNumberIban,
            String ibanNoFormatting,
            String ibanRouting,
            boolean ibanValid
    ) {
        return """
            {
              "Entity_Public_Id": "%s",
              "RASSINI_ERP_ID": "%s",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "%s",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "%s",
                      "Bank_Account_Number_IBAN": "%s",
                      "Bank_Account_IBAN_No_Formatting": "%s",
                      "Bank_Account_IBAN": {
                        "type": "IBAN",
                        "valid": %s,
                        "routing": "%s"
                      },
                      "Bank_Country": "%s",
                      "Bank_Account_Currency": "%s",
                      "Bank_Currency_List": ["%s"]
                    }
                  ]
                }
              ]
            }
            """.formatted(publicId, erpId, bu, bankAccountNumber, bankAccountNumberIban, ibanNoFormatting, ibanValid, ibanRouting, country, currency, currency);
    }

    private String findAccountLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("[ACCOUNT-NUMBER-RESOLUTION]"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No se encontró log [ACCOUNT-NUMBER-RESOLUTION]"));
    }

    private String findIntermediaryLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("[INTERMEDIARY-BANK-RESOLUTION]"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No se encontró log [INTERMEDIARY-BANK-RESOLUTION]"));
    }
}
