package com.rassini.graphite_client.service.sync.impl;

import org.springframework.stereotype.Service;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrityServiceImpl implements IntegrityService {

    private final SuppliersRowRepository suppliersRowRepository;

    @Override
    public void createFileSupplierSync() {
        log.info("Executing createFileSupplierSync method - Fetching records and generating file");

        List<SuppliersRowEntity> suppliers = suppliersRowRepository.findAll();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("suppliersToIntegrity.txt"))) {
            // Write column names (optional, not specified if needed, but the spec says "El formato... delimitado por pipe". Let's assume no header, or just data. I will just write data)

            for (SuppliersRowEntity supplier : suppliers) {
                StringBuilder line = new StringBuilder();

                // AddModDelete (Fixed 'A') - Max: 1
                line.append("A").append("|");
                // Counterparty - Max: 10
                line.append(cleanAndTruncate(supplier.getSupplierCode(), 10)).append("|");
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
                line.append(cleanAndTruncate(supplier.getCountryCode(), 2)).append("|");
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

                writer.write(line.toString());
                writer.newLine();
            }
            log.info("File suppliersToIntegrity.txt generated successfully with {} records", suppliers.size());
        } catch (IOException e) {
            log.error("Error generating suppliersToIntegrity.txt", e);
        }
    }

    private String cleanAndTruncate(String input, int maxLength) {
        if (input == null) return "";

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