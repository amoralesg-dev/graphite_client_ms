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
    @DisplayName("1. ABA actual informado: Debe conservarse y no reemplazarse con ACH.routing")
    void testScenario1_CurrentAbaInformed_ConservedNotReplacedByAchRouting() throws Exception {
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

        assertEquals("021000021", row.getRoutingCodeAba(), "El ABA actual debe conservarse sin reemplazarse por ACH.routing");
        assertEquals("CHASUS33", row.getRoutingCodeSwift(), "El SWIFT actual debe conservarse");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("account=****0001"));
        assertTrue(routingLog.contains("abaActual='021000021'"));
        assertTrue(routingLog.contains("swiftActual='CHASUS33'"));
        assertTrue(routingLog.contains("abaFinal='021000021'"));
        assertTrue(routingLog.contains("abaSource=Actual"));
        assertTrue(routingLog.contains("swiftFinal='CHASUS33'"));
        assertTrue(routingLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("2. ABA actual vacío y ACH.routing informado: ABA final debe tomar ACH.routing")
    void testScenario2_CurrentAbaEmpty_AchRoutingInformed_AbaTakesAchRouting() throws Exception {
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
        assertNull(row.getRoutingCodeSwift(), "El SWIFT debe permanecer null porque Wire no está informado");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(routingLog.contains("existsBankWireAbaRouting=false"));
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("3. ABA actual vacío, ACH.routing vacío y Wire.routing informado: ABA permanece null y SWIFT toma Wire.routing (no se cruzan fuentes)")
    void testScenario3_CurrentAbaEmpty_AchRoutingEmpty_WireRoutingInformed_AbaRemainsNullAndSwiftTakesWireRouting() throws Exception {
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

        assertNull(row.getRoutingCodeAba(), "No se debe usar Wire como fallback del ABA");
        assertEquals("111101144", row.getRoutingCodeSwift(), "El SWIFT debe tomar Bank_Wire_ABA_Routing.routing");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("existsBankAchAbaRouting=false"));
        assertTrue(routingLog.contains("existsBankWireAbaRouting=true"));
        assertTrue(routingLog.contains("abaFinal='null'"));
        assertTrue(routingLog.contains("abaSource=None"));
        assertTrue(routingLog.contains("swiftFinal='111101144'"));
        assertTrue(routingLog.contains("swiftSource=Bank_Wire_ABA_Routing.routing"));
    }

    @Test
    @DisplayName("4. SWIFT actual informado: Debe conservarse y no reemplazarse con Wire.routing")
    void testScenario4_CurrentSwiftInformed_ConservedNotReplacedByWireRouting() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "PRIMARY WIRE BANK",
                      "Bank_SWIFT": {
                        "routing": "CHASUS33XXX",
                        "valid": true
                      },
                      "Bank_ACH_ABA_Routing": {
                        "routing": "111101144",
                        "valid": true,
                        "bank_name": "Delta Bank",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "888888882",
                        "valid": true,
                        "bank_name": "FALLBACK WIRE BANK",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("111101144", row.getRoutingCodeAba(), "El ABA se obtiene de Bank_ACH_ABA_Routing.routing");
        assertEquals("CHASUS33XXX", row.getRoutingCodeSwift(), "El SWIFT de la lógica actual debe prevalecer sobre Wire.routing");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='CHASUS33XXX'"));
        assertTrue(routingLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("5. SWIFT actual vacío, Wire.routing vacío y ACH.routing informado: SWIFT permanece null (no usar ACH como fallback de SWIFT)")
    void testScenario5_CurrentSwiftEmpty_WireRoutingEmpty_AchRoutingInformed_SwiftRemainsNull() throws Exception {
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
                        "routing": "",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank Wire",
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

        assertEquals("111000111", row.getRoutingCodeAba(), "El ABA toma Bank_ACH_ABA_Routing.routing");
        assertNull(row.getRoutingCodeSwift(), "No se debe usar ACH como fallback del SWIFT");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='111000111'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("6. ABA y SWIFT actuales vacíos con ACH.routing y Wire.routing informados (Caso US197697, BU 09, cuenta 6066401): ABA toma ACH.routing y SWIFT toma Wire.routing")
    void testScenario6_CaseUS197697_AbaFromAchRoutingAndSwiftFromWireRouting() throws Exception {
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

        assertEquals("6066401", row.getAccountNumber());
        assertEquals("111101144", row.getRoutingCodeAba(), "El ABA debe resolverse desde Bank_ACH_ABA_Routing.routing");
        assertEquals("111101144", row.getRoutingCodeSwift(), "El SWIFT debe resolverse desde Bank_Wire_ABA_Routing.routing");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("supplier=US197697"));
        assertTrue(routingLog.contains("bu=09"));
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("abaActual='null'"));
        assertTrue(routingLog.contains("swiftActual='null'"));
        assertTrue(routingLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(routingLog.contains("existsBankWireAbaRouting=true"));
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='111101144'"));
        assertTrue(routingLog.contains("swiftSource=Bank_Wire_ABA_Routing.routing"));
    }

    @Test
    @DisplayName("7. Ambos segmentos (ACH y Wire) con valores distintos: ACH alimenta exclusivamente ABA y Wire alimenta exclusivamente SWIFT")
    void testScenario7_DistinctAchAndWireValues_EachFeedsItsDedicatedField() throws Exception {
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
                        "swift": "",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "222000222",
                        "valid": true,
                        "fromDatabase": false,
                        "bank_name": "Delta Bank Wire",
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

        assertEquals("111000111", row.getRoutingCodeAba(), "ACH corresponde exclusivamente al ABA");
        assertEquals("222000222", row.getRoutingCodeSwift(), "Wire corresponde exclusivamente al SWIFT");

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaFinal='111000111'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='222000222'"));
        assertTrue(routingLog.contains("swiftSource=Bank_Wire_ABA_Routing.routing"));
    }

    @Test
    @DisplayName("8. Ninguna fuente aporta ABA ni SWIFT -> conservar null, abaSource=None y swiftSource=None")
    void testScenario8_NoSourceProvidesAbaOrSwift_SourceIsNone() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "MX999000",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "012180001234567890",
                      "Bank_Name": "BBVA MEXICO",
                      "Bank_SWIFT": ""
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertNull(row.getRoutingCodeAba());
        assertNull(row.getRoutingCodeSwift());

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("abaActual='null'"));
        assertTrue(routingLog.contains("swiftActual='null'"));
        assertTrue(routingLog.contains("existsBankAchAbaRouting=false"));
        assertTrue(routingLog.contains("existsBankWireAbaRouting=false"));
        assertTrue(routingLog.contains("abaFinal='null'"));
        assertTrue(routingLog.contains("abaSource=None"));
        assertTrue(routingLog.contains("swiftFinal='null'"));
        assertTrue(routingLog.contains("swiftSource=None"));
    }

    @Test
    @DisplayName("9. Validación con payload real capturado desde GraphiteClient Local para US197697 (SAP 00000642, Sociedad 09)")
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
        assertEquals("111101144", row.getRoutingCodeSwift());

        String routingLog = findRoutingLog();
        assertTrue(routingLog.contains("supplier=US197697"));
        assertTrue(routingLog.contains("bu=09"));
        assertTrue(routingLog.contains("account=****6401"));
        assertTrue(routingLog.contains("abaFinal='111101144'"));
        assertTrue(routingLog.contains("abaSource=Bank_ACH_ABA_Routing.routing"));
        assertTrue(routingLog.contains("swiftFinal='111101144'"));
        assertTrue(routingLog.contains("swiftSource=Bank_Wire_ABA_Routing.routing"));
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
