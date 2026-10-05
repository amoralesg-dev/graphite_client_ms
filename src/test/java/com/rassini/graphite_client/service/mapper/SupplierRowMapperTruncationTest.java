package com.rassini.graphite_client.service.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.factory.BreakesXmlFactory;
import com.rassini.graphite_client.service.xml.factory.FrenosXmlFactory;
import com.rassini.graphite_client.service.xml.factory.OcXmlFactory;
import com.rassini.graphite_client.service.xml.factory.Pn99XmlFactory;
import com.rassini.graphite_client.service.xml.factory.PnXmlFactory;

@ExtendWith(MockitoExtension.class)
class SupplierRowMapperTruncationTest {

    @Mock
    private CatalogService catalogService;

    private GraphiteSupplierDto dto;
    private GraphiteSupplierDto.Location hq;
    private GraphiteSupplierDto.ErpRecord erp;
    private GraphiteSupplierDto.Bank bank;

    @BeforeEach
    void setUp() {
        dto = new GraphiteSupplierDto();
        dto.setEntityPublicId("SUP-TRUNC-01");
        dto.setEntityName("PROVEEDOR PRUEBA TRUNCAMIENTO S.A. DE C.V.");

        hq = new GraphiteSupplierDto.Location();
        hq.setLocationName("Headquarters");

        GraphiteSupplierDto.SalesContactCalc contact = new GraphiteSupplierDto.SalesContactCalc();
        contact.setName("LIC. ALEJANDRO GONZALEZ MORALES"); // 31 caracteres > 24
        contact.setEmail("alejandro.gonzalez@proveedor.com");
        hq.setLocSalesContactAlternateContactCalc(Collections.singletonList(contact));

        GraphiteSupplierDto.Address address = new GraphiteSupplierDto.Address();
        address.setAddress1("AVENIDA REFORMA 123");
        address.setAddress2("PISO 5");
        address.setAddress3("PARQUE INDUSTRIAL BENITO JUAREZ ETAPA TRES"); // 43 caracteres > 36
        address.setAddressCity("SAN PEDRO GARZA GARCIA"); // 22 caracteres > 20
        address.setAddressRegionState("NLE");
        address.setAddressCountry("MX");
        address.setAddressPostalCode("66220");
        hq.setAddress(address);
        dto.setLocations(Collections.singletonList(hq));

        erp = new GraphiteSupplierDto.ErpRecord();
        erp.setRassiniErpEntityId("1000");

        bank = new GraphiteSupplierDto.Bank();
        bank.setBankAccountNumber("1234567890");
        bank.setBankCurrencyList(Collections.singletonList("MXN"));
    }

    @Test
    @DisplayName("Debe truncar contactName a 24 caracteres cuando exceda el límite")
    void testContactNameTruncation() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

        assertNotNull(row.getContactName());
        assertTrue(row.getContactName().length() <= 24, "ContactName debe tener máximo 24 caracteres");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", row.getContactName());
    }

    @Test
    @DisplayName("Debe preservar contactName sin cambios si es menor o igual a 24 caracteres")
    void testContactNameUnderLimit() {
        GraphiteSupplierDto.SalesContactCalc contact = new GraphiteSupplierDto.SalesContactCalc();
        contact.setName("JUAN PEREZ"); // 10 caracteres <= 24
        hq.setLocSalesContactAlternateContactCalc(Collections.singletonList(contact));

        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

        assertEquals("JUAN PEREZ", row.getContactName());
    }

    @Test
    @DisplayName("Debe truncar streetName3 a 36 caracteres cuando exceda el límite")
    void testStreetName3Truncation() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

        assertNotNull(row.getStreetName3());
        assertTrue(row.getStreetName3().length() <= 36, "StreetName3 debe tener máximo 36 caracteres");
        assertEquals("PARQUE INDUSTRIAL BENITO JUAREZ ETAP", row.getStreetName3());
    }

    @Test
    @DisplayName("Debe truncar cityCode a 20 caracteres cuando exceda el límite")
    void testCityCodeTruncation() {
        SuppliersRowEntity row = new SuppliersRowEntity();
        SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

        assertNotNull(row.getCityCode());
        assertTrue(row.getCityCode().length() <= 20, "CityCode debe tener máximo 20 caracteres");
        assertEquals("SAN PEDRO GARZA GARC", row.getCityCode());
    }

    @Test
    @DisplayName("Debe proteger defensivamente en las 5 factorías XML si la BD tiene datos históricos largos")
    void testXmlFactoriesDefensiveTruncation() throws Exception {
        // Simular datos históricos en BD que superan los límites
        SuppliersRowEntity historicalSupplier = new SuppliersRowEntity();
        historicalSupplier.setSupplierCode("HIST001");
        historicalSupplier.setErpIdQad("60009999");
        historicalSupplier.setBusinessUnitCode("1000");
        historicalSupplier.setStreetName("CALLE PRINCIPAL NUMERO 100 INTERIOR CUATRO"); // 43 chars
        historicalSupplier.setStreetName3("PARQUE INDUSTRIAL BENITO JUAREZ ETAPA TRES"); // 43 chars
        historicalSupplier.setCityCode("SAN PEDRO GARZA GARCIA"); // 22 chars
        historicalSupplier.setContactName("LIC. ALEJANDRO GONZALEZ MORALES"); // 31 chars
        historicalSupplier.setContactEmail("test@rassini.com");
        historicalSupplier.setCountryCode("MX");

        lenient().when(catalogService.resolveTaxClass(anyString(), any())).thenReturn("DEFAULT");
        lenient().when(catalogService.resolveUpdateInfo(any())).thenReturn(new com.rassini.graphite_client.dto.UpdateInfo("false", "CREATE"));
        lenient().when(catalogService.getAction(any())).thenReturn("CREATE");
        lenient().when(catalogService.mapCountry(any(), any(), any())).thenReturn("MX");

        // 1. FrenosXmlFactory
        FrenosXmlFactory frenosFactory = new FrenosXmlFactory(catalogService);
        var frenosBusrel = frenosFactory.buildBusrelContext(historicalSupplier, "1000", "DEFAULT", null);
        assertNotNull(frenosBusrel.getAddress());
        assertTrue(frenosBusrel.getAddress().getAddressStreet3().length() <= 36, "Frenos addressStreet3 <= 36");
        assertTrue(frenosBusrel.getAddress().getAddressCity().length() <= 20, "Frenos addressCity <= 20");
        assertTrue(frenosBusrel.getAddress().getAddressCityCode().length() <= 20, "Frenos addressCityCode <= 20");
        assertEquals(24, frenosBusrel.getContact().getContactName().length(), "Frenos contactName <= 24");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", frenosBusrel.getContact().getContactName());

        // 2. BreakesXmlFactory
        BreakesXmlFactory breakesFactory = new BreakesXmlFactory(catalogService);
        var breakesBusrel = breakesFactory.buildBusrelContext(historicalSupplier, "1850", "DEFAULT", null);
        assertTrue(breakesBusrel.getAddress().getAddressStreet3().length() <= 36, "Breakes addressStreet3 <= 36");
        assertTrue(breakesBusrel.getAddress().getAddressCity().length() <= 20, "Breakes addressCity <= 20");
        assertTrue(breakesBusrel.getAddress().getAddressCityCode().length() <= 20, "Breakes addressCityCode <= 20");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", breakesBusrel.getContact().getContactName());

        // 3. OcXmlFactory
        OcXmlFactory ocFactory = new OcXmlFactory(catalogService);
        var ocBusrel = ocFactory.buildBusrelContext(historicalSupplier, "0111", "DEFAULT", null);
        assertTrue(ocBusrel.getAddress().getAddressStreet3().length() <= 36, "Oc addressStreet3 <= 36");
        assertTrue(ocBusrel.getAddress().getAddressCity().length() <= 20, "Oc addressCity <= 20");
        assertEquals("", ocBusrel.getAddress().getAddressCityCode(), "Oc addressCityCode debe ser vacío");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", ocBusrel.getContact().getContactName());

        // 4. PnXmlFactory
        PnXmlFactory pnFactory = new PnXmlFactory(catalogService);
        var pnBusrel = pnFactory.buildBusrelContext(historicalSupplier, "DEFAULT");
        assertTrue(pnBusrel.getAddress().getAddressStreet3().length() <= 36, "Pn addressStreet3 <= 36");
        assertTrue(pnBusrel.getAddress().getAddressCity().length() <= 20, "Pn addressCity <= 20");
        assertTrue(pnBusrel.getAddress().getAddressCityCode().length() <= 20, "Pn addressCityCode <= 20");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", pnBusrel.getContact().getContactName());

        // 5. Pn99XmlFactory
        Pn99XmlFactory pn99Factory = new Pn99XmlFactory(catalogService);
        var pn99Busrel = pn99Factory.buildBusrelContext(historicalSupplier, "DEFAULT", null);
        assertTrue(pn99Busrel.getAddress().getAddressStreet3().length() <= 36, "Pn99 addressStreet3 <= 36");
        assertTrue(pn99Busrel.getAddress().getAddressCity().length() <= 20, "Pn99 addressCity <= 20");
        assertTrue(pn99Busrel.getAddress().getAddressCityCode().length() <= 20, "Pn99 addressCityCode <= 20");
        assertEquals("LIC. ALEJANDRO GONZALEZ ", pn99Busrel.getContact().getContactName());
    }
}
