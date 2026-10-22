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
     * y aplicando como FALLBACK los segmentos {@code Bank_Wire_ABA_Routing} y {@code Bank_ACH_ABA_Routing}
     * únicamente cuando los valores de la lógica actual sean nulos o vacíos.
     * <p>
     * Prioridad de fallback documentada cuando existen ambos segmentos (Wire y ACH):
     * <ol>
     *   <li>{@code Wire} ({@code Bank_Wire_ABA_Routing}): tiene prioridad sobre ACH porque en pagos
     *       bancarios e integración Integrity/QAD la transferencia cablegráfica (Wire) es el canal
     *       principal de liquidación y es consistente con la fuente primaria histórica de ABA.</li>
     *   <li>{@code ACH} ({@code Bank_ACH_ABA_Routing}): se utiliza como segunda opción de fallback
     *       si Wire no existe, no es válido o tiene el campo requerido vacío.</li>
     * </ol>
     * Para cada segmento se evalúa primero el nodo específico de la cuenta bancaria
     * ({@code ERP_Bank_List[]}) y, si no está presente, a nivel {@code ERP_Record[]}.
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

        // 2. Inspección de segmentos de fallback Bank_ACH_ABA_Routing y Bank_Wire_ABA_Routing
        GraphiteSupplierDto.BankWireAbaRouting achSegment = bank.getBankAchAbaRouting() != null
                ? bank.getBankAchAbaRouting()
                : erp.getBankAchAbaRouting();

        GraphiteSupplierDto.BankWireAbaRouting wireSegment = bank.getBankWireAbaRouting() != null
                ? bank.getBankWireAbaRouting()
                : erp.getBankWireAbaRouting();

        boolean existsAch = achSegment != null;
        boolean existsWire = wireSegment != null;

        // 3. Resolución de ABA con fallback (Prioridad: Actual -> Wire -> ACH)
        String finalAba = currentAba;
        String abaSource = !isBlank(currentAba) ? "Actual" : "None";

        if (isBlank(finalAba)) {
            if (isValidSegmentValue(wireSegment, wireSegment != null ? wireSegment.getRouting() : null)) {
                finalAba = wireSegment.getRouting().trim();
                abaSource = "Wire";
            } else if (isValidSegmentValue(achSegment, achSegment != null ? achSegment.getRouting() : null)) {
                finalAba = achSegment.getRouting().trim();
                abaSource = "ACH";
            }
        }

        // 4. Resolución de SWIFT con fallback (Prioridad: Actual -> Wire -> ACH)
        String finalSwift = currentSwift;
        String swiftSource = !isBlank(currentSwift) ? "Actual" : "None";

        if (isBlank(finalSwift)) {
            if (isValidSegmentValue(wireSegment, wireSegment != null ? wireSegment.getSwift() : null)) {
                finalSwift = wireSegment.getSwift().trim();
                swiftSource = "Wire";
            } else if (isValidSegmentValue(achSegment, achSegment != null ? achSegment.getSwift() : null)) {
                finalSwift = achSegment.getSwift().trim();
                swiftSource = "ACH";
            }
        }

        row.setRoutingCodeAba(finalAba);
        row.setRoutingCodeSwift(finalSwift);

        log.debug(
                "[ROUTING-RESOLUTION] supplier={} bu={} abaActual='{}' swiftActual='{}' " +
                "existsBankAchAbaRouting={} existsBankWireAbaRouting={} " +
                "abaFinal='{}' abaSource={} swiftFinal='{}' swiftSource={}",
                dto.getEntityPublicId(),
                erp.getRassiniErpEntityId(),
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