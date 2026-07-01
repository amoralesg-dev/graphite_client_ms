package com.rassini.graphite_client.service.sync.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrityServiceImpl implements IntegrityService {

    private final SuppliersRowRepository suppliersRowRepository;
    private final CatalogService catalogService;

    
    @Override
    public void createFileSupplierSync(String erpIdQad) {
        log.info("Executing createFileSupplierSync method - Fetching records and generating file");

        List<SuppliersRowEntity> suppliersRows = suppliersRowRepository.findDistinctAccountsByErpIdQad(erpIdQad);
        generateSupplierSyncFile(suppliersRows,erpIdQad);
    }

    public void generateSupplierSyncFile(List<SuppliersRowEntity> suppliers, String supplierID) {

       String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        Path outDir = Paths.get(XmlConstants.OUTPUT_BASE_INTEGRITY);
        String fileName = supplierID + "_"+currentDateTime+".txt";
        Path filePath = outDir.resolve(fileName);


        try {
            Files.createDirectories(outDir);

            log.info("Generating supplier sync integrity <{supplierID}> file at: {}",supplierID, filePath.toAbsolutePath());

            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                for (SuppliersRowEntity supplier : suppliers) {
                    writer.write(buildSupplierLine(supplier));
                    writer.newLine();
                }
            }

            log.info("File {} generated successfully with {} records", filePath.toAbsolutePath(), suppliers.size());
        } catch (IOException e) {
            log.error("Error generating file {}", filePath.toAbsolutePath(), e);
        }
    }

    private String buildSupplierLine(SuppliersRowEntity supplier) {
        StringBuilder line = new StringBuilder();

        // AddModDelete - Max: 1
        line.append(cleanAndTruncate(supplier.getStatusIntegrity(), 1)).append("|");
        // Counterparty - Max: 10
        line.append(cleanAndTruncate(supplier.getErpIdQad(), 10)).append("|");
        // Full Name - Max: 140
        line.append(cleanAndTruncate(supplier.getSupplierName(), 140)).append("|");
        // Short Name - Max: 20
        line.append(cleanAndTruncate(supplier.getSupplierSearchName(), 20)).append("|");
        // Legal Entity Identifier - Max: 50
        line.append(cleanAndTruncate(supplier.getRfc(), 50)).append("|");
        // Street Name - Max: 70
        line.append(cleanAndTruncate(supplier.getStreetName(), 70)).append("|");
        // Building number - Max: 16
        line.append(cleanAndTruncate(supplier.getStreetNumber(), 16)).append("|");
        // Post Code - Max: 16
        line.append(cleanAndTruncate(supplier.getZipCode(), 16)).append("|");
        // City - Max: 35
        line.append(cleanAndTruncate(supplier.getCityCode(), 35)).append("|");
        // Province / Estate - Max: 35
        line.append(cleanAndTruncate(supplier.getStateCode(), 35)).append("|");
        // Country - Max: 2
        line.append(cleanAndTruncate(getCountryCode09(supplier), 2)).append("|");
        // Email Address - Max: 50
        line.append(cleanAndTruncate(supplier.getContactEmail(), 50)).append("|");
        // Cpty Account Code - Max: 10
        line.append(cleanAndTruncate(supplier.getSupplierCodeDisIntegrity(), 10)).append("|");
        // Currency - Max: 3
        line.append(cleanAndTruncate(supplier.getSupplierCurrency(), 3)).append("|");
        // Account Name - Max: 100
        line.append(cleanAndTruncate(supplier.getBeneficiaryAccountName(), 100)).append("|");
        // Account Number - Max: 35
        line.append(cleanAndTruncate(supplier.getAccountNumber(), 35)).append("|");
        // Beneficiary Bank - Max: 140
        line.append(cleanAndTruncate(supplier.getBeneficiaryBankName(), 140)).append("|");
        // Country (Bank) - Max: 2
        line.append(cleanAndTruncate(supplier.getBankCountry(), 2)).append("|");
        // SWIFT - Max: 11
        line.append(cleanAndTruncate(supplier.getRoutingCodeSwift(), 11)).append("|");
        // ABA - Max: 9
        line.append(cleanAndTruncate(supplier.getRoutingCodeAba(), 9)).append("|");
        // CorrespondentBank - Max: 140
        line.append(cleanAndTruncate(supplier.getIntermediaryBankName(), 140)).append("|");
        // CorrespondentSwiftCode - Max: 11
        line.append(cleanAndTruncate(supplier.getIntermediaryRoutingCodeSwift(), 11)).append("|");
        // CorrespondentABACode - Max: 9
        line.append(cleanAndTruncate(supplier.getIntermediaryRoutingCodeAba(), 9)).append("|");
        // CorrespondentAccount - No max length specified, using 100
        line.append(cleanAndTruncate(supplier.getIntermediaryAccount(), 100)).append("|");
        // CorrespondentBankCountry - Max: 2
        line.append(cleanAndTruncate(supplier.getIntermediaryAccountCountry(), 2));

        return line.toString();
    }

    private String getCountryCode09(SuppliersRowEntity supplier) {
        if(XMLConstants.PN.equals(supplier.getBusinessUnitCode())){
            return this.catalogService.mapCountry09(supplier.getSupplierCode(), supplier.getCountryCode(), supplier.getBusinessUnitCode());
        }else{
            return supplier.getCountryCode();
        }
    }

    private String cleanAndTruncate(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        String cleaned = input.trim().toUpperCase();

        // Remove accents
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD);
        cleaned = cleaned.replaceAll("[\\p{M}]", "");

        // Remove specific special chars including pipe
        cleaned = cleaned.replaceAll("[&%<>?Ñ|\\r\\n]+", "");

        if (maxLength > 0 && cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }

        return cleaned;
    }
}