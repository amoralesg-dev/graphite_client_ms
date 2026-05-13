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
            CatalogService catalogService
    ) {

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
        row.setCreditorCode(dto.getEntityPublicId());
        row.setCptyAccountCode(dto.getEntityPublicId());
        row.setBusinessUnitCode(erp.getRassiniErpEntityId());
        row.setErpIDQAD(dto.getErpIDQAD());

        row.setBusinessRelationName1(dto.getEntityName());
        row.setBusinessRelationSearchName(left(dto.getEntityName(), 20));
        row.setCreditorTaxIDFederal(dto.getIntegrationTaxId());

        // -------------------------------
        // Contact Email (siempre del DTO)
        // -------------------------------
        row.setContactEmail(dto.getSupplierContactEmail());

        // -------------------------------
        // Contact Name
        // -------------------------------
        String contactName = null;

        if (dto.getLocSalesContactAlternateContactCalc() != null
                && !dto.getLocSalesContactAlternateContactCalc().isEmpty()
                && dto.getLocSalesContactAlternateContactCalc().get(0) != null) {

            contactName = dto.getLocSalesContactAlternateContactCalc()
                    .get(0)
                    .getName();
        }

        if (contactName == null || contactName.isBlank()) {
            contactName = dto.getPaymentContactName();
        }

        if ((contactName == null || contactName.isBlank())
                && dto.getSupplierContactEmail() != null
                && !dto.getSupplierContactEmail().isBlank()) {

            contactName = dto.getSupplierContactEmail()
                    .split("@")[0]
                    .toUpperCase();
        }

        row.setContactName(contactName);

        // -------------------------------
        // Dirección (resolver por estructura real)
        // -------------------------------
        ResolvedAddress resolvedAddress = SupplierAddressResolver.resolve(dto, headquarters, erp);

        if (resolvedAddress != null) {
            row.setAddressStreet1(resolvedAddress.getStreetName());
            row.setAddressStreet2(resolvedAddress.getStreetName2());
            row.setAddressStreet3(resolvedAddress.getStreetName3());
            row.setStreetNumber(resolvedAddress.getStreetNumber());

            row.setAddressZip(resolvedAddress.getPostalCode());
            row.setCityCode(resolvedAddress.getCity());

            row.setStateCode(
                    catalogService.mapState(
                            resolvedAddress.getRegion(),
                            erp.getRassiniErpEntityId()
                    )
            );

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
            row.setAddressStreet1(null);
            row.setAddressStreet2(null);
            row.setAddressStreet3(null);
            row.setStreetNumber(null);
            row.setAddressZip(null);
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

        row.setSupplierType(
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

            row.setCurrency(
                    catalogService.mapCurrency(
                            currency,
                            erp.getRassiniErpEntityId()
                    )
            );

            row.setBeneficiaryName(bank.getBankAccountHolderName());
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
                row.setRoutingCodeBIC(bank.getBankSwift().getRouting());
            } else {
                row.setRoutingCodeBIC(null);
            }

            // RoutingCodeABA
            if (erp.getBankWireAbaRouting() != null) {
                row.setRoutingCodeABA(
                        erp.getBankWireAbaRouting().getRouting()
                );
            } else {
                row.setRoutingCodeABA(null);
            }

            // --------------------------------------------------
            // Intermediary
            // --------------------------------------------------
            row.setIntermediaryAccount(null);
            row.setIntermediaryRoutingCodeABA(null);

            if (bank.getBankAccountCurrencyCorrespondentBank() != null) {
                row.setIntermediaryRoutingCodeBIC(
                        bank.getBankAccountCurrencyCorrespondentBank().getSwift()
                );

                row.setIntermediaryBankName(
                        bank.getBankAccountCurrencyCorrespondentBank().getBankName()
                );
            } else {
                row.setIntermediaryRoutingCodeBIC(null);
                row.setIntermediaryBankName(null);
            }

            row.setIntermediaryAccountCountry(
                    bank.getBankAccountCurrencyCorrespondentBankCountry()
            );
        } else {
            row.setCurrency(null);
            row.setBeneficiaryName(null);
            row.setAccountNumber(null);
            row.setBankCountry(null);
            row.setBankCurrency(null);
            row.setBeneficiaryBankName(null);
            row.setRoutingCodeBIC(null);
            row.setRoutingCodeABA(null);
            row.setIntermediaryAccount(null);
            row.setIntermediaryRoutingCodeABA(null);
            row.setIntermediaryRoutingCodeBIC(null);
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

        log.info("[DEBUG] provider={} locations={}", dto.getEntityPublicId(), dto.getLocations());

        GraphiteSupplierDto.Location headquarters = dto.getLocations().stream()
                .filter(l -> l != null && "Headquarters".equalsIgnoreCase(l.getLocationName()))
                .peek(l -> log.info("[DEBUG] provider={} found Headquarters={}", dto.getEntityPublicId(), l.getLocationName()))
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
            log.info("[DEBUG] provider={} fallback location={}", dto.getEntityPublicId(), fallback.getLocationName());
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