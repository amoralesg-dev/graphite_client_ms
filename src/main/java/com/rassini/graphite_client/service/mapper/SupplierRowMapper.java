package com.rassini.graphite_client.service.mapper;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.address.ResolvedAddress;
import com.rassini.graphite_client.service.address.SupplierAddressResolver;
import com.rassini.graphite_client.service.xml.CatalogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupplierRowMapper {

    private SupplierRowMapper() {
        // utility class
    }

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
        CatalogService catalogService) 
    {

        // -------------------------------
        // Validaciones mínimas
        // -------------------------------
        if (row == null || dto == null || erp == null || catalogService == null) {
            log.warn("[MAPPER] fill cancelado por argumentos nulos. row={} dto={} erp={} catalogService={}",
                    row, dto, erp, catalogService);
            return;
        }

        // -------------------------------
        // Identidad
        // -------------------------------
        row.setSupplierCode(dto.getEntityPublicId());
        row.setSupplierCodeDisIntegrity(dto.getErpIdQad());
        row.setBusinessUnitCode(erp.getRassiniErpEntityId());
        row.setErpIdQad(dto.getErpIdQad());

        row.setSupplierName(left(dto.getEntityName(), 36));
        row.setSupplierSearchName(left(dto.getEntityName(), 20));
        row.setRfc(dto.getIntegrationTaxId());

        // -------------------------------
        // Contact Name + Contact Email
        // Primero desde headquarters, luego fallback a root
        // -------------------------------
        String contactName = null;
        String contactEmail = null;

        if (headquarters != null
                && headquarters.getLocSalesContactAlternateContactCalc() != null
                && !headquarters.getLocSalesContactAlternateContactCalc().isEmpty()
                && headquarters.getLocSalesContactAlternateContactCalc().get(0) != null) {

            contactName = headquarters.getLocSalesContactAlternateContactCalc()
                    .get(0)
                    .getName();

            contactEmail = headquarters.getLocSalesContactAlternateContactCalc()
                    .get(0)
                    .getEmail();
        }

        if ((contactName == null || contactName.isBlank())
                && headquarters != null
                && headquarters.getLocSalesContactAlternateNameCalc() != null
                && !headquarters.getLocSalesContactAlternateNameCalc().isBlank()) {

            contactName = headquarters.getLocSalesContactAlternateNameCalc();
        }

        if ((contactEmail == null || contactEmail.isBlank())
                && headquarters != null
                && headquarters.getLocSalesContactAlternateEmailCalc() != null
                && !headquarters.getLocSalesContactAlternateEmailCalc().isBlank()) {

            contactEmail = headquarters.getLocSalesContactAlternateEmailCalc();
        }

        if (contactName == null || contactName.isBlank()) {
            contactName = dto.getPaymentContactName();
        }

        if ((contactEmail == null || contactEmail.isBlank())
                && dto.getSupplierContactEmail() != null
                && !dto.getSupplierContactEmail().isBlank()) {

            contactEmail = dto.getSupplierContactEmail();
        }

        if ((contactName == null || contactName.isBlank())
                && contactEmail != null
                && !contactEmail.isBlank()) {

            contactName = contactEmail
                    .split("@")[0]
                    .toUpperCase();
        }

        row.setContactName(contactName);
        row.setContactEmail(contactEmail);


        // -------------------------------
        // Dirección (resolver por estructura real)
        // -------------------------------
        ResolvedAddress resolvedAddress = SupplierAddressResolver.resolve(dto, headquarters, erp);

        if (resolvedAddress != null) {
            row.setStreetName(resolvedAddress.getStreetName());
            row.setStreetName2(resolvedAddress.getStreetName2());
            row.setStreetName3(resolvedAddress.getStreetName3());
            row.setStreetNumber(resolvedAddress.getStreetNumber());

            row.setZipCode(resolvedAddress.getPostalCode());
            row.setCityCode(resolvedAddress.getCity());

            row.setStateCode(catalogService.getEquivalenciaState(
                    resolvedAddress.getRegion(),
                    erp.getRassiniErpEntityId()
            ));

            row.setCountryCode(
                    catalogService.mapCountry(
                            resolvedAddress.getCountry(),
                            erp.getRassiniErpEntityId()
                    )
            );

            // dejamos state_description con la ciudad/localidad resuelta
            row.setStateDescription(resolvedAddress.getCity());

            log.info(
                    "[ADDR-MAPPER] provider={} street1={} street2={} street3={} streetNumber={} city={} region={} zip={} country={}",
                    dto.getEntityPublicId(),
                    resolvedAddress.getStreetName(),
                    resolvedAddress.getStreetName2(),
                    resolvedAddress.getStreetName3(),
                    resolvedAddress.getStreetNumber(),
                    resolvedAddress.getCity(),
                    resolvedAddress.getRegion(),
                    resolvedAddress.getPostalCode(),
                    resolvedAddress.getCountry()
            );
        } else {
            row.setStreetName(null);
            row.setStreetName2(null);
            row.setStreetName3(null);
            row.setStreetNumber(null);
            row.setZipCode(null);
            row.setCityCode(null);
            row.setStateCode(null);
            row.setCountryCode(null);
            row.setStateDescription(null);

            log.warn("[ADDR-MAPPER] provider={} no resolved address found", dto.getEntityPublicId());
        }

        // -------------------------------
        // ERP-level fields
        // -------------------------------
        row.setPurchaseTypeCode(
                erp.getRassiniErpPaymentType()
        );

        row.setSupplierTypeCode(
                erp.getRassiniErpSupplierType()
        );

        // -------------------------------
        // Banco
        // -------------------------------
        if (erp.getErpBankList() != null && !erp.getErpBankList().isEmpty()) {

            GraphiteSupplierDto.Bank bank = erp.getErpBankList().get(0);

            String currency =
                    (bank.getBankCurrencyList() != null && !bank.getBankCurrencyList().isEmpty())
                            ? bank.getBankCurrencyList().get(0)
                            : null;

            row.setSupplierCurrency(
                    catalogService.mapCurrency(
                            currency,
                            erp.getRassiniErpEntityId()
                    )
            );

            row.setBeneficiaryAccountName(bank.getBankAccountHolderName());
            row.setAccountNumber(bank.getBankAccountNumber());
            row.setBankCountry(bank.getBankCountry());

            row.setBankCurrency(
                    bank.getBankAccountCurrency()
            );

            // --------------------------------------------------
            // BeneficiaryBankName
            // --------------------------------------------------
            String beneficiaryBankName = null;

            if (bank.getBankNumber() != null
                    && bank.getBankNumber().getBankName() != null
                    && !bank.getBankNumber().getBankName().isBlank()) {

                beneficiaryBankName = bank.getBankNumber().getBankName();
            }

            if ((beneficiaryBankName == null || beneficiaryBankName.isBlank())
                    && erp.getBankWireAbaRouting() != null) {

                beneficiaryBankName = erp.getBankWireAbaRouting().getBankName();
            }

            row.setBeneficiaryBankName(beneficiaryBankName);

            // RoutingCodeBIC
            if (bank.getBankSwift() != null) {
                row.setRoutingCodeSwift(bank.getBankSwift().getRouting());
            } else {
                row.setRoutingCodeSwift(null);
            }

            // RoutingCodeABA
            if (erp.getBankWireAbaRouting() != null) {
                row.setRoutingCodeAba(
                        erp.getBankWireAbaRouting().getRouting()
                );
            } else {
                row.setRoutingCodeAba(null);
            }

            // --------------------------------------------------
            // Intermediary
            // --------------------------------------------------
            row.setIntermediaryAccount(null);
            row.setIntermediaryRoutingCodeAba(null);

            if (bank.getBankAccountCurrencyCorrespondentBank() != null) {
                row.setIntermediaryRoutingCodeSwift(
                        bank.getBankAccountCurrencyCorrespondentBank().getSwift()
                );

                row.setIntermediaryBankName(
                        bank.getBankAccountCurrencyCorrespondentBank().getBankName()
                );
            } else {
                row.setIntermediaryRoutingCodeSwift(null);
                row.setIntermediaryBankName(null);
            }

            row.setIntermediaryAccountCountry(
                    bank.getBankAccountCurrencyCorrespondentBankCountry()
            );
        } else {
            row.setSupplierCurrency(null);
            row.setBeneficiaryAccountName(null);
            row.setAccountNumber(null);
            row.setBankCountry(null);
            row.setBankCurrency(null);
            row.setBeneficiaryBankName(null);
            row.setRoutingCodeSwift(null);
            row.setRoutingCodeAba(null);
            row.setIntermediaryAccount(null);
            row.setIntermediaryRoutingCodeAba(null);
            row.setIntermediaryRoutingCodeSwift(null);
            row.setIntermediaryBankName(null);
            row.setIntermediaryAccountCountry(null);
        }
    }
    // ======================================================
    // HELPERS
    // ======================================================

    public static GraphiteSupplierDto.Location findHeadquarters(GraphiteSupplierDto dto) {
        if (dto == null || dto.getLocations() == null || dto.getLocations().isEmpty()) {
            return null;
        }

        log.debug("[DEBUG] provider={} locations={}", dto.getEntityPublicId(), dto.getLocations());

        GraphiteSupplierDto.Location headquarters = dto.getLocations().stream()
                .filter(l -> l != null && "Headquarters".equalsIgnoreCase(l.getLocationName()))
                .peek(l -> log.debug("[DEBUG] provider={} found Headquarters={}", dto.getEntityPublicId(), l.getLocationName()))
                .findFirst()
                .orElse(null);

        if (headquarters != null) {
            return headquarters;
        }

        GraphiteSupplierDto.Location office = dto.getLocations().stream()
                .filter(l -> l != null && "Office".equalsIgnoreCase(l.getLocationName()))
                .peek(l -> log.info("[DEBUG] provider={} found Office={}", dto.getEntityPublicId(), l.getLocationName()))
                .findFirst()
                .orElse(null);

        if (office != null) {
            return office;
        }

        GraphiteSupplierDto.Location fallback = dto.getLocations().stream()
                .filter(l -> l != null)
                .findFirst()
                .orElse(null);

        if (fallback != null) {
            log.debug("[DEBUG] provider={} fallback location={}", dto.getEntityPublicId(), fallback.getLocationName());
        }

        return fallback;
    }

    private static String left(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }
}