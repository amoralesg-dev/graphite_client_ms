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
    @DisplayName("Escenario 1: Lógica actual encuentra ABA y SWIFT -> Se conservan los valores actuales (sin fallback)")
    void testScenario1_CurrentLogicFindsAbaAndSwift() throws Exception {
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

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("abaActual='021000021'"));
        assertTrue(debugLog.contains("swiftActual='CHASUS33'"));
        assertTrue(debugLog.contains("abaFinal='021000021'"));
        assertTrue(debugLog.contains("abaSource=Actual"));
        assertTrue(debugLog.contains("swiftFinal='CHASUS33'"));
        assertTrue(debugLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("Escenario 2: Lógica actual no encuentra ABA; ACH/Wire sí contienen routing -> ABA se obtiene desde routing (Caso US197697 / SAP 00000642 / BU 09)")
    void testScenario2_CurrentMissesAba_FallbackGetsAbaFromRouting_CaseUS197697() throws Exception {
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

        assertEquals("111101144", row.getRoutingCodeAba(), "El ABA debe resolverse desde el fallback routing");
        assertNull(row.getRoutingCodeSwift(), "El SWIFT debe permanecer null ya que viene vacío en todas las fuentes");

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("supplier=US197697"));
        assertTrue(debugLog.contains("bu=09"));
        assertTrue(debugLog.contains("abaActual='null'"));
        assertTrue(debugLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(debugLog.contains("existsBankWireAbaRouting=true"));
        assertTrue(debugLog.contains("abaFinal='111101144'"));
        assertTrue(debugLog.contains("abaSource=Wire"));
    }

    @Test
    @DisplayName("Escenario 2B: Lógica actual no encuentra ABA y solo existe Bank_ACH_ABA_Routing -> ABA se obtiene desde ACH")
    void testScenario2B_CurrentMissesAba_OnlyAchExists() throws Exception {
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

        assertEquals("111101144", row.getRoutingCodeAba());
        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(debugLog.contains("existsBankWireAbaRouting=false"));
        assertTrue(debugLog.contains("abaFinal='111101144'"));
        assertTrue(debugLog.contains("abaSource=ACH"));
    }

    @Test
    @DisplayName("Escenario 3: Lógica actual no encuentra SWIFT; ACH/Wire contienen swift -> SWIFT se obtiene desde ACH/Wire")
    void testScenario3_CurrentMissesSwift_FallbackGetsSwiftFromAchOrWire() throws Exception {
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
                        "swift": "DLTBUS44ACH",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("111101144", row.getRoutingCodeAba());
        assertEquals("DLTBUS44ACH", row.getRoutingCodeSwift());

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("swiftActual='null'"));
        assertTrue(debugLog.contains("swiftFinal='DLTBUS44ACH'"));
        assertTrue(debugLog.contains("swiftSource=ACH"));
    }

    @Test
    @DisplayName("Escenario 4: Existen datos tanto en la lógica actual como en ACH/Wire -> Prevalece la lógica actual")
    void testScenario4_BothCurrentAndFallbackHaveData_CurrentPrevails() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "US197697",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "Bank_Wire_ABA_Routing": {
                    "routing": "021000021",
                    "bank_name": "PRIMARY WIRE BANK"
                  },
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "6066401",
                      "Bank_Name": "PRIMARY WIRE BANK",
                      "Bank_SWIFT": {
                        "routing": "CHASUS33XXX",
                        "valid": true
                      },
                      "Bank_ACH_ABA_Routing": {
                        "routing": "999999991",
                        "valid": true,
                        "bank_name": "FALLBACK ACH BANK",
                        "swift": "FALLBACKACH",
                        "type": "ABA"
                      },
                      "Bank_Wire_ABA_Routing": {
                        "routing": "888888882",
                        "valid": true,
                        "bank_name": "FALLBACK WIRE BANK",
                        "swift": "FALLBACKWIR",
                        "type": "ABA"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        SuppliersRowEntity row = mapFirstBank(json);

        assertEquals("021000021", row.getRoutingCodeAba(), "Debe prevalecer el ABA de la lógica actual");
        assertEquals("CHASUS33XXX", row.getRoutingCodeSwift(), "Debe prevalecer el SWIFT de la lógica actual");

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("abaActual='021000021'"));
        assertTrue(debugLog.contains("swiftActual='CHASUS33XXX'"));
        assertTrue(debugLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(debugLog.contains("existsBankWireAbaRouting=true"));
        assertTrue(debugLog.contains("abaFinal='021000021'"));
        assertTrue(debugLog.contains("abaSource=Actual"));
        assertTrue(debugLog.contains("swiftFinal='CHASUS33XXX'"));
        assertTrue(debugLog.contains("swiftSource=Actual"));
    }

    @Test
    @DisplayName("Escenario 5: Existen ambos segmentos (ACH y Wire) con valores distintos -> Se aplica prioridad consistente (Wire > ACH)")
    void testScenario5_BothAchAndWireExist_WirePriorityApplied() throws Exception {
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

        assertEquals("222000222", row.getRoutingCodeAba(), "Wire tiene prioridad sobre ACH para ABA");
        assertEquals("DLTBWIR2", row.getRoutingCodeSwift(), "Wire tiene prioridad sobre ACH para SWIFT");

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("existsBankAchAbaRouting=true"));
        assertTrue(debugLog.contains("existsBankWireAbaRouting=true"));
        assertTrue(debugLog.contains("abaFinal='222000222'"));
        assertTrue(debugLog.contains("abaSource=Wire"));
        assertTrue(debugLog.contains("swiftFinal='DLTBWIR2'"));
        assertTrue(debugLog.contains("swiftSource=Wire"));
    }

    @Test
    @DisplayName("Escenario 5B: Existen ambos segmentos (ACH y Wire), pero Wire tiene swift vacío -> ABA de Wire y SWIFT cae en fallback a ACH")
    void testScenario5B_WireHasEmptySwift_SwiftFallsBackToAch() throws Exception {
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

        assertEquals("222000222", row.getRoutingCodeAba());
        assertEquals("DLTBACH1", row.getRoutingCodeSwift());

        String debugLog = findRoutingLog();
        assertTrue(debugLog.contains("abaFinal='222000222'"));
        assertTrue(debugLog.contains("abaSource=Wire"));
        assertTrue(debugLog.contains("swiftFinal='DLTBACH1'"));
        assertTrue(debugLog.contains("swiftSource=ACH"));
    }

    @Test
    @DisplayName("Validación con payload real capturado desde GraphiteClient Local para US197697 (SAP 00000642, Sociedad 09)")
    void testLiveCapturedPayloadUS197697() throws Exception {
        Path liveFile = Path.of(System.getProperty("java.io.tmpdir"), "US197697_profile.json");
        if (!Files.exists(liveFile)) {
            return;
        }
        String rawJson = Files.readString(liveFile).replace("\uFEFF", "");
        SuppliersRowEntity row = mapFirstBank(rawJson);

        assertEquals("US197697", row.getSupplierCode());
        assertEquals("09", row.getBusinessUnitCode());
        assertEquals("111101144", row.getRoutingCodeAba());
        assertNull(row.getRoutingCodeSwift());

        String debugLog = findRoutingLog();
        System.out.println("LIVE PAYLOAD LOG: " + debugLog);
        assertTrue(debugLog.contains("supplier=US197697"));
        assertTrue(debugLog.contains("bu=09"));
        assertTrue(debugLog.contains("abaFinal='111101144'"));
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
                .orElseThrow(() -> new AssertionError("No se encontró log DEBUG [ROUTING-RESOLUTION]"));
    }
}
