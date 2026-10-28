package com.rassini.graphite_client.service.mapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupplierRowMapperAbaSwiftFallbackTest {

    @Mock
    private CatalogService catalogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger mapperLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        mapperLogger = (Logger) LoggerFactory.getLogger(SupplierRowMapper.class);
        mapperLogger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        mapperLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (mapperLogger != null && listAppender != null) {
            mapperLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("1. SWIFT original informado (ej. US192521 CHASUS33): se conserva sin cambios (swiftSource=Actual)")
    void testScenario1_OriginalSwiftInformed_ConservedWithoutChanges() throws Exception {
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
                      "Bank_Name": "JPMorgan Chase Bank, NA",
                      "Bank_SWIFT": {
                        "routing": "CHASUS33",
                        "valid": true
                      },
                      "Bank_ACH_ABA_Routing": {
                        "routing": "071000013",
                        "valid": true,
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "021000021",
                        "valid": true,
                        "bank_name": "JPMorgan Chase Bank, NA",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("071000013", row.getRoutingCodeAba(), "El ABA debe resolverse desde Bank_ACH_ABA_Routing.routing");
        assertEquals("CHASUS33", row.getRoutingCodeSwift(), "El SWIFT original (CHASUS33) debe conservarse sin cambios");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("supplier=US192521"));
        assertTrue(routingLog.contains("bu=1000"));
        assertTrue(routingLog.contains("account=****9292"));
        assertTrue(routingLog.contains("swiftFallbackEnabled=false"));
        assertTrue(routingLog.contains("abaFinal='071000013'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='CHASUS33'"));
        assertTrue(routingLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("2. SWIFT original vacío y Wire.routing informado (Caso US197697): SWIFT permanece null (swiftSource=None)")
    void testScenario2_OriginalSwiftEmpty_WireRoutingInformed_SwiftRemainsNull() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "RASSINI_Legacy_QAD_ID": "00000642",
              "Supplier_Is_Legacy": "y",
              "RASSINI_ERP_ID": "60003131",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "Delta Bank",
                      "Bank_Number": "",
                      "Bank_SWIFT": "",
                      "Bank_ACH_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank",
                        "swift": "",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank",
                        "swift": "",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("111101144", row.getRoutingCodeAba(), "ABA debe tomar Bank_ACH_ABA_Routing.routing");
        assertNull(row.getRoutingCodeSwift(), "SWIFT debe permanecer null sin fallback a Bank_Wire_ABA_Routing.routing");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("supplier=US197697"));
        assertTrue(routingLog.contains("bu=09"));
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("swiftFallbackEnabled=false"));
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("3. SWIFT original vacío y Wire.swift informado: SWIFT permanece null (Wire.swift nunca fue parte de la lógica original en Git)")
    void testScenario3_OriginalSwiftEmpty_WireSwiftInformed_SwiftRemainsNull() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "Delta Bank",
                      "Bank_SWIFT": "",
                      "Bank_Wire_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank",
                        "swift": "DLTBUS33XXX",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertNull(row.getRoutingCodeSwift(), "SWIFT debe permanecer null aunque Bank_Wire_ABA_Routing.swift venga informado");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("4. ABA original vacío y ACH.routing informado: ABA toma ACH.routing")
    void testScenario4_OriginalAbaEmpty_AchRoutingInformed_AbaTakesAchRouting() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "Delta Bank",
                      "Bank_SWIFT": "",
                      "Bank_ACH_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank",
                        "swift": "",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("111101144", row.getRoutingCodeAba(), "El ABA debe resolverse desde Bank_ACH_ABA_Routing.routing");
        assertNull(row.getRoutingCodeSwift());

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("5. ABA original vacío, ACH vacío y Wire.routing informado en ERP_Bank_List: ABA permanece null")
    void testScenario5_OriginalAbaEmpty_AchEmpty_BankWireRoutingInformed_AbaRemainsNull() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "Delta Bank",
                      "Bank_SWIFT": "",
                      "Bank_Wire_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank",
                        "swift": "",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertNull(row.getRoutingCodeAba(), "ABA permanece null cuando el original (ERP_Record.Bank_Wire_ABA_Routing) y ACH están vacíos");
        assertNull(row.getRoutingCodeSwift(), "SWIFT permanece null sin fallback");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='null'"));
        assertTrue(routingLog.contains("abaSource=None"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("6. ACH.routing y Wire.routing con valores distintos: Solo ACH.routing completa ABA y Wire.routing NO completa SWIFT")
    void testScenario6_DistinctAchAndWireRouting_OnlyAchCompletesAbaAndWireDoesNotCompleteSwift() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "Delta Bank",
                      "Bank_SWIFT": "",
                      "Bank_ACH_ABA_Routing": {
                        "routing": "111000111",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank ACH",
                        "swift": "DLTBACH1",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "222000222",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank Wire",
                        "swift": "DLTBWIR2",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("111000111", row.getRoutingCodeAba(), "Solo ACH.routing puede completar el ABA");
        assertNull(row.getRoutingCodeSwift(), "Wire.routing no puede completar el SWIFT");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='111000111'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("7. ABA original informado (ERP_Record.Bank_Wire_ABA_Routing.routing): se conserva y no es reemplazado por ACH.routing")
    void testScenario7_OriginalAbaInformed_ConservedNotReplacedByAchRouting() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US100001",
              "RASSINI_ERP_ID": "00000100",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "Bank_Wire_ABA_Routing": {
                    "routing": "021000021",
                    "bank_name": "JPMORGAN CHASE"
                  },
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "99990001",
                      "Bank_Name": "JPMORGAN CHASE",
                      "Bank_SWIFT": {
                        "routing": "CHASUS33",
                        "valid": true
                      },
                      "Bank_ACH_ABA_Routing": {
                        "routing": "999999991",
                        "valid": true,
                        "bank_name": "FALLBACK ACH BANK",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("021000021", row.getRoutingCodeAba());
        assertEquals("CHASUS33", row.getRoutingCodeSwift());

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='021000021'"));
        assertTrue(routingLog.contains("abaSource=Actual"));
        assertTrue(routingLog.contains("swiftFinal='CHASUS33'"));
        assertTrue(routingLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("8. Validación con payload real capturado de US197697 (SAP 00000642, BU 09): ABA=111101144 y SWIFT=null")
    void testLiveCapturedPayloadUS197697() throws Exception {
        Path liveFile = Path.of(System.getProperty("java.io.tmpdir"), "US197697_profile.json");
        if (!Files.exists(liveFile)) {
            return;
        }
        String rawJson = Files.readString(liveFile).replace("\uFEFF", "");
        SuppliersRowEntity row = mapFirstBank(rawJson);

        assertEquals("US197697", row.getSupplierCode());
        assertEquals("09", row.getBusinessUnitCode());
        assertEquals("6066401", row.getAccountNumber());
        assertEquals("111101144", row.getRoutingCodeAba());
        assertNull(row.getRoutingCodeSwift());

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("supplier=US197697"));
        assertTrue(routingLog.contains("bu=09"));
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    private SuppliersRowEntity mapFirstBank(String json) throws Exception {
        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);

        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);
        return row;
    }

    private String findRoutingLog() {
        List<ILoggingEvent> logs = listAppender.list;
        return logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("[ROUTING-RESOLUTION]"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No se encontró log [ROUTING-RESOLUTION]"));
    }
}
