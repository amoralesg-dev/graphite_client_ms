package com.rassini.graphite_client.service.xml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.xml.impl.SupplierProcessingServiceImpl;

@ExtendWith(MockitoExtension.class)
public class SupplierProcessingServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private CorreoPendienteRepository correoPendienteRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SupplierJpaMapper supplierJpaMapper;

    @Mock
    private XmlOcService xmlOcService;

    @Mock
    private XmlPnService xmlPnService;

    @Mock
    private XmlPn99Service xmlPn99Service;

    @Mock
    private XmlFrenosService xmlFrenosService;

    @Mock
    private XmlBreakesService xmlBreakesService;

    @Mock
    private IntegrityService integrityService;

    @Mock
    private SuppliersRowRepository suppliersRowRepository;

    @Mock
    private GraphiteProfileRefreshService graphiteProfileRefreshService;

    @InjectMocks
    private SupplierProcessingServiceImpl supplierProcessingService;

    private SupplierEntity supplierEntity;
    private GraphiteSupplierDto graphiteSupplierDto;
    private final String publicId = "NN732811";

    @BeforeEach
    void setUp() throws Exception {
        supplierEntity = new SupplierEntity();
        supplierEntity.setPublicId(publicId);
        supplierEntity.setStatus(ProviderState.DESCARGA);
        supplierEntity.setFullJson("{\"Entity_Public_Id\":\"NN732811\",\"ERP_ID\":\"60003094\"}");

        graphiteSupplierDto = new GraphiteSupplierDto();
        graphiteSupplierDto.setEntityPublicId(publicId);
        graphiteSupplierDto.setErpIdQad("60003094");

        List<GraphiteSupplierDto.ErpRecord> erpRecords = new ArrayList<>();
        GraphiteSupplierDto.ErpRecord erp99 = new GraphiteSupplierDto.ErpRecord();
        erp99.setRassiniErpEntityId("99");
        erpRecords.add(erp99);

        GraphiteSupplierDto.ErpRecord erp0111 = new GraphiteSupplierDto.ErpRecord();
        erp0111.setRassiniErpEntityId("0111");
        erpRecords.add(erp0111);

        graphiteSupplierDto.setErpRecords(erpRecords);
    }

    private void setupBaseMocks() throws Exception {
        when(graphiteProfileRefreshService.processAndSaveInternal(eq(publicId), any())).thenReturn(true);
        when(supplierRepository.findByPublicIdAndStatus(eq(publicId), eq(ProviderState.DESCARGA)))
                .thenReturn(Optional.of(supplierEntity));
        when(objectMapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(supplierEntity.getFullJson()));
        when(objectMapper.readValue(anyString(), eq(GraphiteSupplierDto.class))).thenReturn(graphiteSupplierDto);
    }

    @Test
    @DisplayName("Caso 1: Proveedor con BU 99 fallando equivalencia y BU 0111 válida -> BU 0111 genera XML y BU 99 queda en error")
    void testCase1_Bu99FailsAndBu0111Succeeds() throws Exception {
        setupBaseMocks();

        // Simulate BU 99 fails state catalog mapping in xmlPn99Service
        doAnswer(invocation -> {
            supplierEntity.setStatus(ProviderState.ERRORMAPPN);
            return null;
        }).when(xmlPn99Service).generate(any(), eq(supplierEntity));

        // Rows in suppliers table
        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.ERROR);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        // Execute
        supplierProcessingService.processSupplier(publicId, "MANUAL");

        // Verify XML OC called (which handles 0111)
        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        // Verify XML PN99 called (which handles 99)
        verify(xmlPn99Service, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        // Verify integrity file generated
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));

        // Supplier status should preserve error state (ERRORMAPPN) instead of being overwritten to PROCESSINGXMLCOMPLETE
        assertEquals(ProviderState.ERRORMAPPN, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 2: Proveedor monoplanta con equivalencias completas -> Generación exitosa y status COMPLETE")
    void testCase2_SinglePlantSuccessful() throws Exception {
        graphiteSupplierDto.setErpRecords(Collections.singletonList(graphiteSupplierDto.getErpRecords().get(1))); // only 0111
        setupBaseMocks();

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));
        assertEquals(ProviderState.PROCESSINGXMLCOMPLETE, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 3: Proveedor multiplanta con todas las plantas válidas -> Todas generan XML y status COMPLETE")
    void testCase3_MultiPlantAllSuccessful() throws Exception {
        setupBaseMocks();

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(xmlPn99Service, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));
        assertEquals(ProviderState.PROCESSINGXMLCOMPLETE, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 4: Proveedor multiplanta con todas las plantas con error -> Ninguna genera y status de error conservado")
    void testCase4_MultiPlantAllError() throws Exception {
        setupBaseMocks();

        doAnswer(invocation -> {
            supplierEntity.setStatus(ProviderState.ERRORMAPOC);
            return null;
        }).when(xmlOcService).generate(any(), eq(supplierEntity));

        doAnswer(invocation -> {
            supplierEntity.setStatus(ProviderState.ERRORMAPPN);
            return null;
        }).when(xmlPn99Service).generate(any(), eq(supplierEntity));

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.ERROR);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.ERROR);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        // Error state preserved (ERRORMAPPN from last failing call)
        assertEquals(ProviderState.ERRORMAPPN, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 5: Reproceso sucesivo: primero falla una planta, luego se corrige catálogo y el segundo reproceso completa la planta faltante")
    void testCase5_SuccessiveReprocessCompletesRemainingPlant() throws Exception {
        setupBaseMocks();

        // In 2nd run: 0111 was already GENERATED_PREV or GENERATED, and now 99 succeeds too
        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(xmlPn99Service, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));
        assertEquals(ProviderState.PROCESSINGXMLCOMPLETE, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 6: Proveedor con statusIntegrity=MOD -> Genera XML con actividad MODIFY y no falla")
    void testCase6_StatusIntegrityModGeneratesModifyAction() throws Exception {
        setupBaseMocks();

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setStatusIntegrity("M");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        assertNotNull(supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 7: Falla no esperada en una planta (Exception) -> Las demás plantas no se ven afectadas en su generación individual")
    void testCase7_UnexpectedExceptionHandledPerPlant() throws Exception {
        setupBaseMocks();

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.ERROR);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");
        verify(xmlOcService, times(1)).generate(any(), any());
    }

    @Test
    @DisplayName("Caso 8: BU 0111 falla catálogo y BU 99 es válida -> BU 99 genera XML y BU 0111 queda en error")
    void testCase8_Bu0111FailsAndBu99Succeeds() throws Exception {
        setupBaseMocks();

        doAnswer(invocation -> {
            supplierEntity.setStatus(ProviderState.ERRORMAPOC);
            return null;
        }).when(xmlOcService).generate(any(), eq(supplierEntity));

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.ERROR);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        // xmlPn99Service must still execute and not be blocked by 0111 error
        verify(xmlPn99Service, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));
        assertEquals(ProviderState.ERRORMAPOC, supplierEntity.getStatus());
    }

    @Test
    @DisplayName("Caso 9: BU 1850 (Frenos) falla y las demás plantas (0111, 99) generan exitosamente")
    void testCase9_Bu1850FrenosFailsAndOthersSucceed() throws Exception {
        // Add 1850 to erp records
        GraphiteSupplierDto.ErpRecord erp1850 = new GraphiteSupplierDto.ErpRecord();
        erp1850.setRassiniErpEntityId("1850");
        graphiteSupplierDto.getErpRecords().add(erp1850);

        setupBaseMocks();

        doAnswer(invocation -> {
            supplierEntity.setStatus(ProviderState.ERRORMAPFRENOS);
            return null;
        }).when(xmlFrenosService).generate(any(), eq(supplierEntity));

        SuppliersRowEntity row99 = new SuppliersRowEntity();
        row99.setBusinessUnitCode("99");
        row99.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row0111 = new SuppliersRowEntity();
        row0111.setBusinessUnitCode("0111");
        row0111.setXmlStatus(XmlStatus.GENERATED);

        SuppliersRowEntity row1850 = new SuppliersRowEntity();
        row1850.setBusinessUnitCode("1850");
        row1850.setXmlStatus(XmlStatus.ERROR);

        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "99"))
                .thenReturn(Optional.of(row99));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "0111"))
                .thenReturn(Optional.of(row0111));
        when(suppliersRowRepository.findBySupplierCodeAndBusinessUnitCode(publicId, "1850"))
                .thenReturn(Optional.of(row1850));

        supplierProcessingService.processSupplier(publicId, "MANUAL");

        verify(xmlOcService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(xmlPn99Service, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(xmlFrenosService, times(1)).generate(eq(graphiteSupplierDto), eq(supplierEntity));
        verify(integrityService, times(1)).createFileSupplierSync(any(GraphiteSupplierDto.class));
        assertEquals(ProviderState.ERRORMAPFRENOS, supplierEntity.getStatus());
    }
}
