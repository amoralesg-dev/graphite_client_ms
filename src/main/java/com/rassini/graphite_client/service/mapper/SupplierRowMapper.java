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
        fill(row, dto, headquarters, erp,null, catalogService);
        return row;
    }

    public static void fill(
            SuppliersRowEntity row,
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.Location headquarters,
            GraphiteSupplierDto.ErpRecord erp,
            GraphiteSupplierDto.Bank bank,
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

        row.setContactName(truncateWithLog(contactName, 24, "contactName", dto.getEntityPublicId(), erp.getRassiniErpEntityId()));
        row.setContactEmail(contactEmail);

        // ===============================
        // DIRECCION
        // ===============================
        ResolvedAddress address = SupplierAddressResolver.resolve(dto, headquarters, erp);

        if (address != null) {

            row.setStreetName(left(address.getStreetName(),36));
            row.setStreetName2(left(address.getStreetName2(),36));
            row.setStreetName3(truncateWithLog(address.getStreetName3(), 36, "streetName3", dto.getEntityPublicId(), erp.getRassiniErpEntityId()));
            row.setStreetNumber(address.getStreetNumber());

            row.setZipCode(address.getPostalCode());
            row.setCityCode(truncateWithLog(address.getCity(), 20, "cityCode", dto.getEntityPublicId(), erp.getRassiniErpEntityId()));

            row.setStateCode(catalogService.getEquivalenciaState(
                    dto.getEntityPublicId(),
                    address.getRegion(),
                    erp.getRassiniErpEntityId()
            ));

            //ya no se hace la traducción de estado, se deja tal cual viene del address resolver incluso para PN
            row.setCountryCode(address.getCountry());

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
        if (bank != null) {

            // currency
            String currency =
                    (bank.getBankCurrencyList() != null && !bank.getBankCurrencyList().isEmpty())
                            ? bank.getBankCurrencyList().get(0)
                            : null;

            row.setSupplierCurrency(currency);

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
            resolveRoutingCodes(row, dto, erp, bank);

            // intermediario
            if (bank.getBankAccountCurrencyCorrespondentBank() != null) {
                row.setIntermediaryRoutingCodeSwift(
                        bank.getBankAccountCurrencyCorrespondentBank().getSwift());
                row.setIntermediaryBankName(
                        bank.getBankAccountCurrencyCorrespondentBank().getBankName());
            }
        }
    }

    /**
     * Resuelve los códigos ABA y SWIFT manteniendo intacta la lógica actual como primera fuente
     * y aplicando como FALLBACK exclusivo de cada canal:
     * <ul>
     *   <li><b>ABA:</b>
     *     <ol>
     *       <li>Lógica actual ({@code erp.getBankWireAbaRouting().getRouting()}).</li>
     *       <li>{@code Bank_ACH_ABA_Routing.routing} (correlacionado con la cuenta procesada).</li>
     *       <li>Si ambas fuentes están vacías, conserva {@code null} ({@code abaSource=None}).</li>
     *     </ol>
     *   </li>
     *   <li><b>SWIFT:</b>
     *     <ol>
     *       <li>Lógica actual ({@code bank.getBankSwift().getRouting()}).</li>
     *       <li>{@code Bank_Wire_ABA_Routing.routing} (correlacionado con la cuenta procesada).</li>
     *       <li>Si ambas fuentes están vacías, conserva {@code null} ({@code swiftSource=None}).</li>
     *     </ol>
     *   </li>
     * </ul>
     * No se cruzan las fuentes (Wire no alimenta ABA en fallback; ACH no alimenta SWIFT).
     */
    private static void resolveRoutingCodes(
            SuppliersRowEntity row,
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.ErpRecord erp,
            GraphiteSupplierDto.Bank bank
    ) {
        // 1. Lógica actual (primera fuente intacta)
        String currentSwift = bank.getBankSwift() != null ? bank.getBankSwift().getRouting() : null;
        String currentAba = erp.getBankWireAbaRouting() != null ? erp.getBankWireAbaRouting().getRouting() : null;

        // 2. Inspección de segmentos de fallback correlacionados con la misma cuenta bancaria procesada
        boolean isSingleErpAccount = erp.getErpBankList() == null || erp.getErpBankList().size() <= 1;

        GraphiteSupplierDto.BankWireAbaRouting achSegment = bank.getBankAchAbaRouting() != null
                ? bank.getBankAchAbaRouting()
                : (isSingleErpAccount ? erp.getBankAchAbaRouting() : null);

        GraphiteSupplierDto.BankWireAbaRouting wireSegment = bank.getBankWireAbaRouting() != null
                ? bank.getBankWireAbaRouting()
                : (isSingleErpAccount ? erp.getBankWireAbaRouting() : null);

        boolean existsAch = achSegment != null;
        boolean existsWire = wireSegment != null;

        // 3. Resolución de ABA (Prioridad definitiva: 1. Lógica actual -> 2. Bank_ACH_ABA_Routing.routing -> 3. null)
        String finalAba = !isBlank(currentAba) ? currentAba.trim() : null;
        String abaSource = !isBlank(finalAba) ? "Actual" : "None";

        if (isBlank(finalAba)) {
            if (isValidSegmentValue(achSegment, achSegment != null ? achSegment.getRouting() : null)) {
                finalAba = achSegment.getRouting().trim();
                abaSource = "Bank_ACH_ABA_Routing.routing";
            }
        }

        // 4. Resolución de SWIFT (Prioridad definitiva: 1. Lógica actual -> 2. Bank_Wire_ABA_Routing.routing -> 3. null)
        String finalSwift = !isBlank(currentSwift) ? currentSwift.trim() : null;
        String swiftSource = !isBlank(finalSwift) ? "Actual" : "None";

        if (isBlank(finalSwift)) {
            if (isValidSegmentValue(wireSegment, wireSegment != null ? wireSegment.getRouting() : null)) {
                finalSwift = wireSegment.getRouting().trim();
                swiftSource = "Bank_Wire_ABA_Routing.routing";
            }
        }

        row.setRoutingCodeAba(finalAba);
        row.setRoutingCodeSwift(finalSwift);

        String maskedAccount = maskAccount(bank.getBankAccountNumber());

        log.info(
                "[ROUTING-RESOLUTION] supplier={} bu={} account={} abaActual='{}' swiftActual='{}' " +
                "existsBankAchAbaRouting={} existsBankWireAbaRouting={} " +
                "abaFinal='{}' abaSource={} swiftFinal='{}' swiftSource={}",
                dto.getEntityPublicId(),
                erp.getRassiniErpEntityId(),
                maskedAccount,
                currentAba,
                currentSwift,
                existsAch,
                existsWire,
                finalAba,
                abaSource,
                finalSwift,
                swiftSource
        );
    }

    private static String maskAccount(String account) {
        if (isBlank(account)) {
            return "N/A";
        }
        String trimmed = account.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private static boolean isValidSegmentValue(GraphiteSupplierDto.BankWireAbaRouting segment, String value) {
        if (segment == null || isBlank(value)) {
            return false;
        }
        return !Boolean.FALSE.equals(segment.getValid());
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

    private static String truncateWithLog(String value, int length, String fieldName, String supplierCode, String bu) {
        if (value == null) return null;
        if (value.length() > length) {
            String truncated = value.substring(0, length);
            log.warn("[DATA-TRUNCATION] supplierCode='{}' bu='{}' field='{}' origLength={} maxLength={} origValue='{}' truncatedValue='{}'",
                    supplierCode, bu, fieldName, value.length(), length, value, truncated);
            return truncated;
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}