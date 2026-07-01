package com.rassini.graphite_client.service.mapper;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.address.ResolvedAddress;
import com.rassini.graphite_client.service.address.SupplierAddressResolver;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SupplierRowMapper {

    private static final String LOCALE = "en-US";

    private SupplierRowMapper() {}

    // ======================================================
    // PUBLIC API
    // ======================================================

    public static SuppliersRowEntity toRow(
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.Location headquarters,
            GraphiteSupplierDto.ErpRecord erp,
            CatalogService catalogService
    ) {
        SuppliersRowEntity row = new SuppliersRowEntity();
        fill(row, dto, headquarters, erp, catalogService);
        return row;
    }

    public static void fill(
            SuppliersRowEntity row,
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.Location headquarters,
            GraphiteSupplierDto.ErpRecord erp,
            CatalogService catalogService
    ) {

        if (row == null || dto == null || erp == null || catalogService == null) {
            log.warn("[MAPPER] fill cancelado por argumentos nulos");
            return;
        }

        // ===============================
        // IDENTIDAD
        // ===============================
        row.setSupplierCode(dto.getEntityPublicId());
        row.setSupplierCodeDisIntegrity(dto.getErpIdQad());
        row.setBusinessUnitCode(erp.getRassiniErpEntityId());
        row.setErpIdQad(dto.getErpIdQad());

        // ✅ SUPPLIER NAME (translations)
        String supplierName = null;

        if (dto.getEntityNameTranslations() != null) {
            supplierName = dto.getEntityNameTranslations().get(LOCALE);
        }

        if (isBlank(supplierName)) {
            supplierName = dto.getEntityName();
        }

        if (isBlank(supplierName)) {
            supplierName = dto.getIntegrationTaxId();
        }

        row.setSupplierName(left(supplierName, 36));
        row.setSupplierSearchName(left(supplierName, 20));
        row.setRfc(dto.getIntegrationTaxId());

        // ===============================
        // CONTACTO
        // ===============================
        String contactName = null;
        String contactEmail = null;

        if (headquarters != null
                && headquarters.getLocSalesContactAlternateContactCalc() != null
                && !headquarters.getLocSalesContactAlternateContactCalc().isEmpty()
                && headquarters.getLocSalesContactAlternateContactCalc().get(0) != null) {

            contactName = headquarters.getLocSalesContactAlternateContactCalc().get(0).getName();
            contactEmail = headquarters.getLocSalesContactAlternateContactCalc().get(0).getEmail();
        }

        if (isBlank(contactName) && headquarters != null) {
            contactName = headquarters.getLocSalesContactAlternateNameCalc();
        }

        if (isBlank(contactEmail) && headquarters != null) {
            contactEmail = headquarters.getLocSalesContactAlternateEmailCalc();
        }

        if (isBlank(contactName)) {
            contactName = dto.getPaymentContactName();
        }

        if (isBlank(contactEmail)) {
            contactEmail = dto.getSupplierContactEmail();
        }

        if (isBlank(contactName) && !isBlank(contactEmail)) {
            contactName = contactEmail.split("@")[0].toUpperCase();
        }

        row.setContactName(contactName);
        row.setContactEmail(contactEmail);

        // ===============================
        // DIRECCION
        // ===============================
        ResolvedAddress address = SupplierAddressResolver.resolve(dto, headquarters, erp);

        if (address != null) {

            row.setStreetName(address.getStreetName());
            row.setStreetName2(address.getStreetName2());
            row.setStreetName3(address.getStreetName3());
            row.setStreetNumber(address.getStreetNumber());

            row.setZipCode(address.getPostalCode());
            row.setCityCode(address.getCity());

            row.setStateCode(catalogService.getEquivalenciaState(
                    dto.getEntityPublicId(),
                    address.getRegion(),
                    erp.getRassiniErpEntityId()
            ));

            
            if (XMLConstants.PN.equals(erp.getRassiniErpEntityId())) {

                row.setCountryCode(
                    catalogService.mapCountry(
                        dto.getEntityPublicId(),
                        address.getCountry(),
                        erp.getRassiniErpEntityId()
                    )
                );

            } else {

                row.setCountryCode(address.getCountry());
            }

            row.setStateDescription(address.getCity());

        }

        // ===============================
        // ERP
        // ===============================
        row.setPurchaseTypeCode(erp.getRassiniErpPaymentType());
        row.setSupplierTypeCode(erp.getRassiniErpSupplierType());

        // ===============================
        // BANCO
        // ===============================
        if (erp.getErpBankList() != null && !erp.getErpBankList().isEmpty()) {

            GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);

            // currency
            String currency =
                    (bank.getBankCurrencyList() != null && !bank.getBankCurrencyList().isEmpty())
                            ? bank.getBankCurrencyList().get(0)
                            : null;

            row.setSupplierCurrency(
                    catalogService.mapCurrency(currency, erp.getRassiniErpEntityId())
            );

            // ✅ ACCOUNT NAME
            String account = null;

            if (bank.getBankAccountHolderNameTranslations() != null) {
                account = bank.getBankAccountHolderNameTranslations().get(LOCALE);
            }

            if (isBlank(account)) {
                account = bank.getBankAccountHolderName();
            }

            if (isBlank(account)) {
                account = supplierName;
            }

            row.setBeneficiaryAccountName(account);

            // datos básicos
            row.setAccountNumber(bank.getBankAccountNumber());
            row.setBankCountry(bank.getBankCountry());
            row.setBankCurrency(bank.getBankAccountCurrency());

            // ✅ BANK NAME
            String bankName = null;

            if (bank.getBankNameTranslations() != null) {
                bankName = bank.getBankNameTranslations().get(LOCALE);
            }

            if (isBlank(bankName)) {
                bankName = bank.getBankName();
            }

            if (isBlank(bankName) && erp.getBankWireAbaRouting() != null) {
                bankName = erp.getBankWireAbaRouting().getBankName();
            }

            if (isBlank(bankName) && bank.getBankNumber() != null) {
                bankName = bank.getBankNumber().getBankName();
            }

            if (isBlank(bankName) && bank.getBankAccountCurrencyCorrespondentBank() != null) {
                bankName = bank.getBankAccountCurrencyCorrespondentBank().getBankName();
            }

            if (isBlank(bankName)) {
                bankName = account;
            }

            row.setBeneficiaryBankName(bankName);

            // routing
            row.setRoutingCodeSwift(
                    bank.getBankSwift() != null ? bank.getBankSwift().getRouting() : null
            );

            row.setRoutingCodeAba(
                    erp.getBankWireAbaRouting() != null
                            ? erp.getBankWireAbaRouting().getRouting()
                            : null
            );

            // intermediario
            if (bank.getBankAccountCurrencyCorrespondentBank() != null) {
                row.setIntermediaryRoutingCodeSwift(
                        bank.getBankAccountCurrencyCorrespondentBank().getSwift());
                row.setIntermediaryBankName(
                        bank.getBankAccountCurrencyCorrespondentBank().getBankName());
            }
        }
    }

    // ======================================================
    // HELPERS
    // ======================================================

    public static GraphiteSupplierDto.Location findHeadquarters(GraphiteSupplierDto dto) {
        if (dto == null || dto.getLocations() == null) return null;

        return dto.getLocations().stream()
                .filter(l -> l != null
                        && ("Headquarters".equalsIgnoreCase(l.getLocationName())
                        || "Office".equalsIgnoreCase(l.getLocationName())))
                .findFirst()
                .orElse(dto.getLocations().stream().filter(l -> l != null).findFirst().orElse(null));
    }

    private static String left(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}