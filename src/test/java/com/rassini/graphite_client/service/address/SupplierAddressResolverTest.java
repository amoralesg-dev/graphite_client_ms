package com.rassini.graphite_client.service.address;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.mapper.SupplierRowMapper;
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
class SupplierAddressResolverTest {

    @Mock
    private CatalogService catalogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger resolverLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        resolverLogger = (Logger) LoggerFactory.getLogger(SupplierAddressResolver.class);
        resolverLogger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        resolverLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (resolverLogger != null && listAppender != null) {
            resolverLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("Escenario 1: Address_1 presente -> Toma Address_1 con prioridad sobre data.address1")
    void testAddress1PresentPrevailsOverDataAddress1() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "MX100001",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "RASSINI_ERP_Address": {
                    "Address": {
                      "Address_Country": "MX",
                      "Address_1": "AV. INDUSTRIA METALURGICA 450",
                      "Address_City": "MONTERREY",
                      "Address_Region_State": "MX-NLE",
                      "Address_Postal_Code": "64000",
                      "data": {
                        "address1": "Avenida Industria 450",
                        "components": {
                          "countryIso3": "MEX",
                          "locality": "Monterrey",
                          "administrativeAreaIso2": "MX-NLE",
                          "postalCode": "64000",
                          "premise": "450"
                        }
                      }
                    }
                  },
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "012180001111111111"
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);

        ResolvedAddress resolved = SupplierAddressResolver.resolve(dto, hq, erp);
        assertNotNull(resolved);
        assertEquals("AV. INDUSTRIA METALURGICA 450", resolved.getStreetName());
        assertEquals("450", resolved.getStreetNumber());

        String debugLog = findAddressLog();
        assertTrue(debugLog.contains("Address_1='AV. INDUSTRIA METALURGICA 450'"));
        assertTrue(debugLog.contains("data.address1='Avenida Industria 450'"));
        assertTrue(debugLog.contains("streetNameFinal='AV. INDUSTRIA METALURGICA 450'"));
        assertTrue(debugLog.contains("streetNumberFinal='450'"));
        assertTrue(debugLog.contains("selectedSource=Address.Address_1"));
    }

    @Test
    @DisplayName("Escenario 2: Address_1 vacío o nulo -> Fallback a data.address1")
    void testFallbackToDataAddress1WhenAddress1IsBlank() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "MX100002",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "RASSINI_ERP_Address": {
                    "Address": {
                      "Address_Country": "MX",
                      "Address_1": "   ",
                      "Address_City": "SALTILLO",
                      "Address_Region_State": "MX-COA",
                      "Address_Postal_Code": "25000",
                      "data": {
                        "address1": "Blvd. Venustiano Carranza 1200",
                        "components": {
                          "countryIso3": "MEX",
                          "locality": "Saltillo",
                          "administrativeAreaIso2": "MX-COA",
                          "postalCode": "25000"
                        }
                      }
                    }
                  }
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);

        ResolvedAddress resolved = SupplierAddressResolver.resolve(dto, hq, erp);
        assertNotNull(resolved);
        assertEquals("Blvd. Venustiano Carranza 1200", resolved.getStreetName());
        assertEquals("1200", resolved.getStreetNumber());

        String debugLog = findAddressLog();
        assertTrue(debugLog.contains("data.address1='Blvd. Venustiano Carranza 1200'"));
        assertTrue(debugLog.contains("streetNameFinal='Blvd. Venustiano Carranza 1200'"));
        assertTrue(debugLog.contains("streetNumberFinal='1200'"));
        assertTrue(debugLog.contains("selectedSource=Address.data.address1"));
    }

    @Test
    @DisplayName("Escenario 3: Ambos Address_1 y data.address1 vacíos -> Fallback o null controlado con selectedSource")
    void testBothAddress1AndDataAddress1Empty() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "MX100003",
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "RASSINI_ERP_Address": {
                    "Address": {
                      "Address_Country": "MX",
                      "Address_1": "",
                      "Address_City": "CHICOLOAPAN",
                      "Address_Region_State": "MX-MEX",
                      "Address_Postal_Code": "56375",
                      "data": {
                        "address1": ""
                      }
                    }
                  }
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);

        ResolvedAddress resolved = SupplierAddressResolver.resolve(dto, hq, erp);
        assertNotNull(resolved);
        assertNull(resolved.getStreetName());
        assertNull(resolved.getStreetNumber());

        String debugLog = findAddressLog();
        assertTrue(debugLog.contains("Address_1=''"));
        assertTrue(debugLog.contains("data.address1=''"));
        assertTrue(debugLog.contains("streetNameFinal='null'"));
        assertTrue(debugLog.contains("streetNumberFinal='null'"));
        assertTrue(debugLog.contains("selectedSource=None"));
    }

    @Test
    @DisplayName("Escenario 4: Caso Real MX185296 -> Resuelve Address_1 ('CALLE CENTRAL DE ABASTOS...'), trunca a 36 chars en entidad y extrae street_number = '8'")
    void testRealCaseMX185296() throws Exception {
        String json = """
            {
              "Entity_Public_Id": "MX185296",
              "Entity_Name": "GONCAR LOGISTICS SA DE CV",
              "RASSINI_ERP_ID": "60003000",
              "Locations_List": [
                {
                  "Location_Name": "GONCAR LOGISTICS",
                  "Address": {
                    "Address_Country": "MX",
                    "Address_1": "CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE",
                    "Address_2": "",
                    "Address_Neighborhood": "",
                    "Address_City": "CHICOLOAPAN DE JUAREZ",
                    "Address_Region_State": "MX-MEX",
                    "Address_Postal_Code": "56375",
                    "data": {
                      "address1": "Calle Coral De Abastos Nave F Local",
                      "address2": "Uhab Hacienda Piedras Negras",
                      "address3": "56375 San Vicente Chicoloapan De Juárez, MEX",
                      "components": {
                        "countryIso3": "MEX",
                        "administrativeArea": "MEX",
                        "administrativeAreaIso2": "MX-MEX",
                        "dependentLocality": "Uhab Hacienda Piedras Negras",
                        "locality": "San Vicente Chicoloapan De Juárez",
                        "postalCode": "56375",
                        "postalCodeShort": "56375",
                        "thoroughfare": "Calle Coral",
                        "thoroughfareName": "Central",
                        "thoroughfareType": "Calle",
                        "building": "De Abastos Nave F Local"
                      }
                    },
                    "valid": false
                  }
                }
              ],
              "ERP_Record": [
                {
                  "RASSINI_ERP_Entity_ID": "09",
                  "RASSINI_ERP_Address": {
                    "Address": {
                      "Address_Country": "MX",
                      "Address_1": "CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE",
                      "Address_2": "",
                      "Address_Neighborhood": "",
                      "Address_City": "CHICOLOAPAN DE JUAREZ",
                      "Address_Region_State": "MX-MEX",
                      "Address_Postal_Code": "56375",
                      "data": {
                        "address1": "Calle Coral De Abastos Nave F Local",
                        "address2": "Uhab Hacienda Piedras Negras",
                        "address3": "56375 San Vicente Chicoloapan De Juárez, MEX",
                        "components": {
                          "countryIso3": "MEX",
                          "administrativeArea": "MEX",
                          "administrativeAreaIso2": "MX-MEX",
                          "dependentLocality": "Uhab Hacienda Piedras Negras",
                          "locality": "San Vicente Chicoloapan De Juárez",
                          "postalCode": "56375",
                          "postalCodeShort": "56375",
                          "thoroughfare": "Calle Coral",
                          "thoroughfareName": "Central",
                          "thoroughfareType": "Calle",
                          "building": "De Abastos Nave F Local"
                        }
                      },
                      "valid": false
                    }
                  },
                  "ERP_Bank_List": [
                    {
                      "Bank_Account_Number": "002180700298540312",
                      "Bank_Name": "Banco Nacional de Mexico SA",
                      "Bank_Currency_List": ["MXN"]
                    }
                  ]
                }
              ]
            }
            """;

        GraphiteSupplierDto dto = objectMapper.readValue(json, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);

        // 1. Verificar ResolvedAddress
        ResolvedAddress resolved = SupplierAddressResolver.resolve(dto, hq, erp);
        assertNotNull(resolved);
        assertEquals("CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE", resolved.getStreetName());
        assertEquals("8", resolved.getStreetNumber(), "Debe extraer el número '8' desde Address_1");
        assertEquals("Uhab Hacienda Piedras Negras", resolved.getStreetName2());
        assertEquals("56375 San Vicente Chicoloapan De Juárez, MEX", resolved.getStreetName3());

        // 2. Verificar log DEBUG
        String debugLog = findAddressLog();
        System.out.println("MX185296 DEBUG LOG: " + debugLog);
        assertTrue(debugLog.contains("provider=MX185296"));
        assertTrue(debugLog.contains("Address_1='CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE'"));
        assertTrue(debugLog.contains("data.address1='Calle Coral De Abastos Nave F Local'"));
        assertTrue(debugLog.contains("streetNameFinal='CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE'"));
        assertTrue(debugLog.contains("streetNumberFinal='8'"));
        assertTrue(debugLog.contains("selectedSource=Address.Address_1"));

        // 3. Verificar entidad SuppliersRowEntity tras SupplierRowMapper.fill (left 36 chars en streetName)
        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);
        assertEquals("CALLE CENTRAL DE ABASTOS NAVE F LOCA", row.getStreetName());
        assertEquals(36, row.getStreetName().length());
        assertEquals("8", row.getStreetNumber());
    }

    @Test
    @DisplayName("Escenario 5: Validación con payload vivo capturado desde GraphiteClient Local para MX185296")
    void testLiveCapturedPayloadMX185296() throws Exception {
        Path liveFile = Path.of(System.getProperty("java.io.tmpdir"), "MX185296_profile.json");
        if (!Files.exists(liveFile)) {
            return;
        }
        String rawJson = Files.readString(liveFile).replace("\uFEFF", "");
        GraphiteSupplierDto dto = objectMapper.readValue(rawJson, GraphiteSupplierDto.class);
        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);
        GraphiteSupplierDto.ErpRecord erp = dto.getErpRecords().get(0);
        GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);

        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

        assertEquals("CALLE CENTRAL DE ABASTOS NAVE F LOCA", row.getStreetName());
        assertEquals("8", row.getStreetNumber());

        String debugLog = findAddressLog();
        assertTrue(debugLog.contains("Address_1='CALLE CENTRAL DE ABASTOS NAVE F LOCAL 8 SAN VICENTE'"));
        assertTrue(debugLog.contains("streetNumberFinal='8'"));
        assertTrue(debugLog.contains("selectedSource=Address.Address_1"));
    }

    private String findAddressLog() {
        List<ILoggingEvent> logs = listAppender.list;
        return logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("[ADDR-RESOLUTION]"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No se encontró log DEBUG [ADDR-RESOLUTION]"));
    }
}
