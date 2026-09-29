package com.rassini.graphite_client;

import static org.junit.jupiter.api.Assertions.*;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.dto.UpdateInfo;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlPnService;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SpringBootTest(properties = {
    "XML_OUTPUT_PATH=target/test-output",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
public class Mx190235MultiAccountXmlTest {

    @Autowired
    private SuppliersRowRepository suppliersRowRepository;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private XmlPnService xmlPnService;

    @BeforeEach
    public void setup() {
        suppliersRowRepository.deleteAll();
    }

    @Test
    @DisplayName("MX190235: Coexistencia de cuenta histórica (status=A, xmlStatus=GENERATED) y cuenta nueva -> Sin NonUniqueResultException, selecciona fila histórica y genera Modify")
    public void testMx190235CoexistenceGeneratesModify() {
        // 1. Fila histórica existente en BD para MX190235 en BU 09
        SuppliersRowEntity historicalRow = new SuppliersRowEntity();
        historicalRow.setSupplierCode("MX190235");
        historicalRow.setBusinessUnitCode("09");
        historicalRow.setErpIdQad("60003062");
        historicalRow.setAccountNumber("012180001322930215");
        historicalRow.setSupplierName("PROVEEDOR MX190235 SA DE CV");
        historicalRow.setStateCode("COAH");
        historicalRow.setCountryCode("MEX");
        historicalRow.setRfc("PRV900101ABC");
        historicalRow.setStatusIntegrity("A"); // Histórico nacido con A
        historicalRow.setXmlStatus(XmlStatus.GENERATED); // Ya generado previamente en QAD
        historicalRow = suppliersRowRepository.save(historicalRow);

        // 2. Fila nueva insertada durante la transacción (segunda cuenta para misma BU)
        SuppliersRowEntity newAccountRow = new SuppliersRowEntity();
        newAccountRow.setSupplierCode("MX190235");
        newAccountRow.setBusinessUnitCode("09");
        newAccountRow.setErpIdQad("60003062");
        newAccountRow.setAccountNumber("012180009999992110");
        newAccountRow.setSupplierName("PROVEEDOR MX190235 SA DE CV");
        newAccountRow.setStateCode("COAH");
        newAccountRow.setCountryCode("MEX");
        newAccountRow.setRfc("PRV900101ABC");
        newAccountRow.setStatusIntegrity("A"); // Cuenta nueva con A
        newAccountRow.setXmlStatus(XmlStatus.PENDING);
        newAccountRow = suppliersRowRepository.save(newAccountRow);

        // 3. Verificar que en la base coexisten 2 filas para (MX190235, 09)
        List<SuppliersRowEntity> allRows = suppliersRowRepository.findAllBySupplierCodeAndBusinessUnitCode("MX190235", "09");
        assertEquals(2, allRows.size(), "Deben existir exactamente 2 filas coexistiendo");

        // 4. Verificar que findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc selecciona exactamente 1 fila (la histórica)
        Optional<SuppliersRowEntity> selectedOpt = suppliersRowRepository
                .findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc("MX190235", "09");
        assertTrue(selectedOpt.isPresent());
        SuppliersRowEntity selected = selectedOpt.get();
        assertEquals(historicalRow.getId(), selected.getId(), "Debe seleccionar la fila histórica");
        assertEquals("012180001322930215", selected.getAccountNumber());

        // 5. Verificar que CatalogService evalúa que existe en QAD y genera Modify
        UpdateInfo updateInfo = catalogService.resolveUpdateInfo(selected);
        assertEquals(XMLConstants.MODIFY, updateInfo.getActivityCode(), "tcActivityCode debe ser Modify");
        assertEquals(XMLConstants.TRUE, updateInfo.getPartialUpdate(), "tlPartialUpdate debe ser true");

        String activityCode = catalogService.getActivityCode(selected);
        assertEquals(XMLConstants.MODIFY, activityCode, "getActivityCode debe ser Modify");

        String action = catalogService.getAction(selected);
        assertEquals(XMLConstants.MODIFY, action, "getAction debe ser Modify");

        // 6. Ejecutar XmlPnService.generate sin lanzar NonUniqueResultException
        GraphiteSupplierDto dto = new GraphiteSupplierDto();
        dto.setEntityPublicId("MX190235");
        GraphiteSupplierDto.ErpRecord erp09 = new GraphiteSupplierDto.ErpRecord();
        erp09.setRassiniErpEntityId("09");
        dto.setErpRecords(Collections.singletonList(erp09));

        assertDoesNotThrow(() -> xmlPnService.generate(dto, null));
    }

    @Test
    @DisplayName("Proveedor 100% Nuevo: No tiene filas previas GENERATED -> genera Create / Save")
    public void testBrandNewSupplierGeneratesCreate() {
        SuppliersRowEntity newRow = new SuppliersRowEntity();
        newRow.setSupplierCode("MX999999");
        newRow.setBusinessUnitCode("09");
        newRow.setErpIdQad("60009999");
        newRow.setAccountNumber("012180005555555555");
        newRow.setSupplierName("PROVEEDOR NUEVO SA DE CV");
        newRow.setStateCode("COAH");
        newRow.setCountryCode("MEX");
        newRow.setRfc("NUE900101ABC");
        newRow.setStatusIntegrity("A");
        newRow.setXmlStatus(XmlStatus.PENDING); // Nunca ha sido GENERATED
        newRow = suppliersRowRepository.save(newRow);

        Optional<SuppliersRowEntity> selectedOpt = suppliersRowRepository
                .findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc("MX999999", "09");
        assertTrue(selectedOpt.isPresent());

        UpdateInfo updateInfo = catalogService.resolveUpdateInfo(selectedOpt.get());
        assertEquals(XMLConstants.CREATE, updateInfo.getActivityCode(), "tcActivityCode debe ser Create");
        assertEquals(XMLConstants.FALSE, updateInfo.getPartialUpdate(), "tlPartialUpdate debe ser false");

        String action = catalogService.getAction(selectedOpt.get());
        assertEquals(XMLConstants.SAVE, action, "getAction debe ser Save");
    }
}
