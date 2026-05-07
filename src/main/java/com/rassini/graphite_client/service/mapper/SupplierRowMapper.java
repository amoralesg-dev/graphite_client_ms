package com.rassini.graphite_client.service.mapper;


import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;

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
        // Contact Name (LÓGICA CORRECTA)
        // 1) Loc_Sales_Contact_Alternate_Contact_Calc[0].name
        // 2) Payment_Contact_Name
        // 3) prefix del Payment_Contact_Email
        // -------------------------------
        String contactName = null;

        if (dto.getLocSalesContactAlternateContactCalc() != null
                && !dto.getLocSalesContactAlternateContactCalc().isEmpty()
                && dto.getLocSalesContactAlternateContactCalc().get(0) != null) {

            contactName = dto
                    .getLocSalesContactAlternateContactCalc()
                    .get(0)
                    .getName();
        }

        if (contactName == null || contactName.isBlank()) {
            contactName = dto.getPaymentContactName();
        }

        if ((contactName == null || contactName.isBlank())
                && dto.getSupplierContactEmail() != null) {

            contactName = dto
                    .getSupplierContactEmail()
                    .split("@")[0]
                    .toUpperCase();
        }

        row.setContactName(contactName);

        // -------------------------------
        // Dirección (Headquarters)
        // -------------------------------
        if (headquarters != null
                && headquarters.getAddress() != null
                && headquarters.getAddress().getData() != null) {

            GraphiteSupplierDto.Address address = headquarters.getAddress();
            GraphiteSupplierDto.AddressData data = address.getData();
            GraphiteSupplierDto.Components comp = data.getComponents();

            row.setAddressStreet1(data.getAddress1());
            row.setAddressStreet2(data.getAddress2());
            row.setAddressStreet3(data.getAddress3());
            row.setStreetNumber(comp != null ? comp.getPremise() : null);
            row.setAddressZip(comp != null ? comp.getPostalCode() : null);
            row.setCityCode(address.getAddressCity());

            row.setStateCode(
                    catalogService.mapState(
                            address.getAddressRegionState(),
                            erp.getRassiniErpEntityId()
                    )
            );

            row.setCountryCode(
                    catalogService.mapCountry(
                            address.getAddressCountry(),
                            erp.getRassiniErpEntityId()
                    )
            );
        }

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

                // AccountNumber (directo)
                row.setAccountNumber(bank.getBankAccountNumber());

                // --------------------------------------------------
                // BeneficiaryBankName
                // 1) ERP_Bank_List[0].Bank_Number.bank_name
                // 2) Bank_Wire_ABA_Routing.bank_name
                // --------------------------------------------------
                String beneficiaryBankName = null;

                if (bank.getBankNumber() != null
                        && bank.getBankNumber().getBankName() != null
                        && !bank.getBankNumber().getBankName().isBlank()) {

                        beneficiaryBankName = bank.getBankNumber().getBankName();
                }

                if ((beneficiaryBankName == null || beneficiaryBankName.isBlank())
                        && erp.getBankWireAbaRouting() != null) {

                        beneficiaryBankName =
                                erp.getBankWireAbaRouting().getBankName();
                }

                row.setBeneficiaryBankName(beneficiaryBankName);

                row.setBankCountry(bank.getBankCountry());

                // RoutingCodeBIC
                if (bank.getBankSwift() != null) {
                        row.setRoutingCodeBIC(bank.getBankSwift().getRouting());
                } else {
                        row.setRoutingCodeBIC(null);
                }

                // RoutingCodeABA (Bank_Wire_ABA_Routing.routing)
                if (erp.getBankWireAbaRouting() != null) {
                        row.setRoutingCodeABA(
                                erp.getBankWireAbaRouting().getRouting()
                        );
                } else {
                        row.setRoutingCodeABA(null);
                }

                // --------------------------------------------------
                // Intermediary (SEGÚN REGLAS)
                // --------------------------------------------------

                // IntermediaryAccount
                row.setIntermediaryAccount(null);

                // IntermediaryRoutingCodeABA
                row.setIntermediaryRoutingCodeABA(null);

                // IntermediaryRoutingCodeBIC
                if (bank.getBankAccountCurrencyCorrespondentBank() != null) {
                        row.setIntermediaryRoutingCodeBIC(
                                bank.getBankAccountCurrencyCorrespondentBank().getRouting()
                        );
                } else {
                        row.setIntermediaryRoutingCodeBIC(null);
                }

                // IntermediaryAccountCountry
                row.setIntermediaryAccountCountry(
                        bank.getBankAccountCurrencyCorrespondentBankCountry()
                );
        }
    }

    // ======================================================
    // HELPERS
    // ======================================================

    public static GraphiteSupplierDto.Location findHeadquarters(GraphiteSupplierDto dto) {
        if (dto.getLocations() == null) return null;

        return dto.getLocations().stream()
                .filter(l -> "Headquarters".equalsIgnoreCase(l.getLocationName()))
                .findFirst()
                .orElse(null);
    }

    private static String left(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }

    
   

}
