package com.rassini.graphite_client;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.ErpIdMigrationRequest;
import com.rassini.graphite_client.dto.SupplierMigrationResponse;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.xml.XmlConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest(properties = {
    "XML_OUTPUT_PATH=target/test-output",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class IntegrityMigrationByErpIdTest {

    static {
        System.setProperty("XML_OUTPUT_PATH", "target/test-output");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuppliersRowRepository suppliersRowRepository;

    @Autowired
    private IntegrityService integrityService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        suppliersRowRepository.deleteAll();
        
        // Clean test output directory
        try {
            Path testOutputDir = Paths.get(XmlConstants.OUTPUT_BASE_INTEGRITY);
            if (Files.exists(testOutputDir)) {
                // Delete files starting with EM to clean test output
                Files.walk(testOutputDir)
                     .filter(p -> p.getFileName().toString().startsWith("EM"))
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    public void testMigrationByErpIdsService() throws Exception {
        // Arrange
        SuppliersRowEntity s1 = new SuppliersRowEntity();
        s1.setErpIdQad("EM20651");
        s1.setBusinessUnitCode("PN");
        s1.setSupplierCode("SUP001");
        s1.setSupplierCodeDisIntegrity("SUP001");
        s1.setStatusIntegrity("A");
        s1.setAccountNumber("ACCT1");
        s1.setSupplierName("SUPPLIER ONE");

        // Second record for the same ERP ID (multiple accounts per ERP ID scenario)
        SuppliersRowEntity s1_2 = new SuppliersRowEntity();
        s1_2.setErpIdQad("EM20651");
        s1_2.setBusinessUnitCode("PN");
        s1_2.setSupplierCode("SUP001_2");
        s1_2.setSupplierCodeDisIntegrity("SUP001_2");
        s1_2.setStatusIntegrity("A");
        s1_2.setAccountNumber("ACCT1_2");
        s1_2.setSupplierName("SUPPLIER ONE ACCOUNT TWO");

        SuppliersRowEntity s2 = new SuppliersRowEntity();
        s2.setErpIdQad("EM2069");
        s2.setBusinessUnitCode("PN");
        s2.setSupplierCode("SUP002");
        s2.setSupplierCodeDisIntegrity("SUP002");
        s2.setStatusIntegrity("B");
        s2.setAccountNumber("ACCT2");
        s2.setSupplierName("SUPPLIER TWO");

        // This one has empty supplierCodeDisIntegrity so it must be EXCLUDED from export and status_integrity must NOT change
        SuppliersRowEntity s3 = new SuppliersRowEntity();
        s3.setErpIdQad("EM2083");
        s3.setBusinessUnitCode("PN");
        s3.setSupplierCode("SUP003");
        s3.setSupplierCodeDisIntegrity("");
        s3.setStatusIntegrity("C");
        s3.setAccountNumber("ACCT3");
        s3.setSupplierName("SUPPLIER THREE");

        suppliersRowRepository.saveAll(Arrays.asList(s1, s1_2, s2, s3));

        // Act
        // Pass duplicate entries ("EM20651", " EM20651 ") and spaced IDs to test normalization and deduplication
        SupplierMigrationResponse response = integrityService.createFileSupplierMigrationByErpIds(
                Arrays.asList("EM20651", " EM2069 ", "EM2083", "EM99999", "EM20651"));

        // Assert response object
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(5, response.getReceivedErpIds());
        assertEquals(4, response.getUniqueErpIds());
        assertEquals(3, response.getFoundErpIds());
        assertEquals(1, response.getNotFoundErpIds());
        assertEquals(4, response.getFoundRecords());
        assertEquals(4, response.getExportedRecords());
        assertEquals(4, response.getUpdatedRecords());
        assertEquals(1, response.getFallbackRecords()); // EM2083
        assertEquals(3, response.getGeneratedFiles());
        assertEquals(1, response.getNotFoundList().size());
        assertEquals("EM99999", response.getNotFoundList().get(0));
        assertEquals(1, response.getFallbackErpIds().getTotalItems());
        assertFalse(response.getFallbackErpIds().isTruncated());
        assertEquals(1, response.getFallbackErpIds().getItems().size());
        assertEquals("EM2083", response.getFallbackErpIds().getItems().get(0));
        assertEquals(1, response.getMultiRecordErpIds().size());
        assertEquals("EM20651", response.getMultiRecordErpIds().get(0).getErpId());
        assertEquals(2, response.getMultiRecordErpIds().get(0).getRecords());
        assertEquals(3, response.getUpdatedErpIds().getTotalItems());
        assertFalse(response.getUpdatedErpIds().isTruncated());
        assertEquals(3, response.getUpdatedErpIds().getItems().size());
        assertTrue(response.getUpdatedErpIds().getItems().contains("EM20651"));
        assertEquals(3, response.getExportedErpIds().getTotalItems());
        assertFalse(response.getExportedErpIds().isTruncated());
        assertEquals(3, response.getExportedErpIds().getItems().size());
        assertTrue(response.getExportedErpIds().getItems().contains("EM20651"));
        assertNotNull(response.getSummary());
        assertEquals("SUCCESS", response.getSummary().getStatus());
        assertEquals("3 ERP IDs procesados, 4 registros exportados y 3 archivos generados.", response.getSummary().getMessage());

        // Assert updates in database
        SuppliersRowEntity updatedS1 = suppliersRowRepository.findById(s1.getId()).orElseThrow();
        SuppliersRowEntity updatedS1_2 = suppliersRowRepository.findById(s1_2.getId()).orElseThrow();
        SuppliersRowEntity updatedS2 = suppliersRowRepository.findById(s2.getId()).orElseThrow();
        SuppliersRowEntity updatedS3 = suppliersRowRepository.findById(s3.getId()).orElseThrow();

        assertEquals("M", updatedS1.getStatusIntegrity(), "EM20651 should be updated to M");
        assertEquals("M", updatedS1_2.getStatusIntegrity(), "EM20651 account 2 should be updated to M");
        assertEquals("M", updatedS2.getStatusIntegrity(), "EM2069 should be updated to M");
        assertEquals("M", updatedS3.getStatusIntegrity(), "EM2083 should be updated to M (not excluded anymore)");

        // Assert file generation
        Path testOutputDir = Paths.get(XmlConstants.OUTPUT_BASE_INTEGRITY);
        assertTrue(Files.exists(testOutputDir), "Output directory should exist");
        
        File[] files = testOutputDir.toFile().listFiles((dir, name) -> name.startsWith("EM"));
        assertNotNull(files);
        
        // We expect exactly 3 files starting with EM (one for EM20651, EM2069, EM2083)
        assertEquals(3, files.length, "Should generate exactly 3 files starting with EM");
        
        boolean foundEM20651 = false;
        boolean foundEM2069 = false;
        boolean foundEM2083 = false;
        
        for (File file : files) {
            if (file.getName().startsWith("EM20651_")) {
                foundEM20651 = true;
                List<String> lines = Files.readAllLines(file.toPath());
                // We expect 2 lines because EM20651 has 2 accounts
                assertEquals(2, lines.size());
                assertTrue(lines.get(0).startsWith("M|EM20651|SUPPLIER ONE|") || lines.get(0).startsWith("M|EM20651|SUPPLIER ONE ACCOUNT TWO|"));
                assertTrue(lines.get(1).startsWith("M|EM20651|SUPPLIER ONE|") || lines.get(1).startsWith("M|EM20651|SUPPLIER ONE ACCOUNT TWO|"));
            } else if (file.getName().startsWith("EM2069_")) {
                foundEM2069 = true;
                List<String> lines = Files.readAllLines(file.toPath());
                assertEquals(1, lines.size());
                assertTrue(lines.get(0).startsWith("M|EM2069|SUPPLIER TWO|"), "Line content mismatch: " + lines.get(0));
            } else if (file.getName().startsWith("EM2083_")) {
                foundEM2083 = true;
                List<String> lines = Files.readAllLines(file.toPath());
                assertEquals(1, lines.size());
                // Ensure position of supplierCodeDisIntegrity (Cpty Account Code, 13th pipe field) has EM2083 instead of empty
                String line = lines.get(0);
                String[] parts = line.split("\\|", -1);
                assertEquals("EM2083", parts[12], "Should fallback to erpIdQad in place of empty supplierCodeDisIntegrity");
            }
        }
        
        assertTrue(foundEM20651, "File for EM20651 should be generated");
        assertTrue(foundEM2069, "File for EM2069 should be generated");
        assertTrue(foundEM2083, "File for EM2083 should be generated");
    }

    @Test
    public void testMigrationByErpIdsEndpoint() throws Exception {
        // Arrange
        SuppliersRowEntity s1 = new SuppliersRowEntity();
        s1.setErpIdQad("EM2123");
        s1.setBusinessUnitCode("PN");
        s1.setSupplierCode("SUP004");
        s1.setSupplierCodeDisIntegrity("SUP004");
        s1.setStatusIntegrity("A");
        s1.setAccountNumber("ACCT4");
        s1.setSupplierName("SUPPLIER FOUR");
        suppliersRowRepository.save(s1);

        ErpIdMigrationRequest request = ErpIdMigrationRequest.builder()
                .erpIds(Arrays.asList("EM2123", "EMUNKNOWN"))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/sync/graphite/suppliers/integrity/migration/by-erp-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.receivedErpIds").value(2))
                .andExpect(jsonPath("$.uniqueErpIds").value(2))
                .andExpect(jsonPath("$.foundErpIds").value(1))
                .andExpect(jsonPath("$.notFoundErpIds").value(1))
                .andExpect(jsonPath("$.notFoundList[0]").value("EMUNKNOWN"))
                .andExpect(jsonPath("$.summary.status").value("SUCCESS"))
                .andExpect(jsonPath("$.summary.message").value("1 ERP IDs procesados, 1 registros exportados y 1 archivos generados."));

        // Verify status_integrity updated in database
        SuppliersRowEntity updated = suppliersRowRepository.findAll().stream()
                .filter(s -> "EM2123".equals(s.getErpIdQad()))
                .findFirst().orElseThrow();
        assertEquals("M", updated.getStatusIntegrity());
    }

    @Test
    public void testEmptyAndNullList() throws Exception {
        // Act & Assert
        // Null list request
        ErpIdMigrationRequest requestNull = ErpIdMigrationRequest.builder().erpIds(null).build();
        mockMvc.perform(post("/api/sync/graphite/suppliers/integrity/migration/by-erp-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestNull)))
                .andExpect(status().isOk());

        // Empty list request
        ErpIdMigrationRequest requestEmpty = ErpIdMigrationRequest.builder().erpIds(Collections.emptyList()).build();
        mockMvc.perform(post("/api/sync/graphite/suppliers/integrity/migration/by-erp-ids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestEmpty)))
                .andExpect(status().isOk());
    }
}
