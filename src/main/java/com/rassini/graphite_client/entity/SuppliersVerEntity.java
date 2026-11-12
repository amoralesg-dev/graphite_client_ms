package com.rassini.graphite_client.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Fotografía histórica de un registro de {@code suppliers} tomada inmediatamente
 * antes de que Graphite modificara su información bancaria.
 * <p>
 * La versión vigente siempre es el propio registro de {@code suppliers};
 * esta tabla solo conserva versiones anteriores (nunca se actualiza ni se borra).
 */
@Entity
@Table(
    name = "suppliers_ver",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_suppliers_ver_supplier_version",
            columnNames = {"supplier_id", "version_number"}
        )
    },
    indexes = {
        @Index(name = "idx_suppliers_ver_supplier_code", columnList = "supplier_code")
    }
)
@Data
public class SuppliersVerEntity {

    public static final String SOURCE_GRAPHITE = "GRAPHITE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia a suppliers.id del registro versionado. */
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "changed_fields", length = 1000)
    private String changedFields;

    @Column(name = "version_source", nullable = false, length = 30)
    private String versionSource = SOURCE_GRAPHITE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ===== Copia completa de suppliers =====

    @Column(name = "erp_id_qad")
    private String erpIdQad;

    @Column(name = "business_unit_code", nullable = false)
    private String businessUnitCode;

    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_search_name")
    private String supplierSearchName;

    @Column(name = "rfc")
    private String rfc;

    @Column(name = "street_name")
    private String streetName;

    @Column(name = "street_name2")
    private String streetName2;

    @Column(name = "street_name3")
    private String streetName3;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "state_description")
    private String stateDescription;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "supplier_code_dis_integrity")
    private String supplierCodeDisIntegrity;

    @Column(name = "supplier_currency")
    private String supplierCurrency;

    @Column(name = "purchase_type_code")
    private String purchaseTypeCode;

    @Column(name = "supplier_type_code")
    private String supplierTypeCode;

    @Column(name = "beneficiary_bank_name")
    private String beneficiaryBankName;

    @Column(name = "beneficiary_account_name")
    private String beneficiaryAccountName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "bank_currency")
    private String bankCurrency;

    @Column(name = "bank_country")
    private String bankCountry;

    @Column(name = "routing_code_aba")
    private String routingCodeAba;

    @Column(name = "routing_code_swift")
    private String routingCodeSwift;

    @Column(name = "intermediary_bank_name")
    private String intermediaryBankName;

    @Column(name = "intermediary_account")
    private String intermediaryAccount;

    @Column(name = "intermediary_account_country")
    private String intermediaryAccountCountry;

    @Column(name = "intermediary_routing_code_aba")
    private String intermediaryRoutingCodeAba;

    @Column(name = "intermediary_routing_code_swift")
    private String intermediaryRoutingCodeSwift;

    @Column(name = "status_integrity")
    private String statusIntegrity;

    @Enumerated(EnumType.STRING)
    @Column(name = "xml_status", nullable = false)
    private XmlStatus xmlStatus;

    /** Copia todos los campos de suppliers (excepto la PK) al snapshot. */
    public static SuppliersVerEntity snapshotOf(SuppliersRowEntity s) {
        SuppliersVerEntity v = new SuppliersVerEntity();
        v.setSupplierId(s.getId());
        v.setErpIdQad(s.getErpIdQad());
        v.setBusinessUnitCode(s.getBusinessUnitCode());
        v.setSupplierCode(s.getSupplierCode());
        v.setSupplierName(s.getSupplierName());
        v.setSupplierSearchName(s.getSupplierSearchName());
        v.setRfc(s.getRfc());
        v.setStreetName(s.getStreetName());
        v.setStreetName2(s.getStreetName2());
        v.setStreetName3(s.getStreetName3());
        v.setStreetNumber(s.getStreetNumber());
        v.setZipCode(s.getZipCode());
        v.setCityCode(s.getCityCode());
        v.setStateCode(s.getStateCode());
        v.setStateDescription(s.getStateDescription());
        v.setCountryCode(s.getCountryCode());
        v.setContactName(s.getContactName());
        v.setContactEmail(s.getContactEmail());
        v.setSupplierCodeDisIntegrity(s.getSupplierCodeDisIntegrity());
        v.setSupplierCurrency(s.getSupplierCurrency());
        v.setPurchaseTypeCode(s.getPurchaseTypeCode());
        v.setSupplierTypeCode(s.getSupplierTypeCode());
        v.setBeneficiaryBankName(s.getBeneficiaryBankName());
        v.setBeneficiaryAccountName(s.getBeneficiaryAccountName());
        v.setAccountNumber(s.getAccountNumber());
        v.setBankCurrency(s.getBankCurrency());
        v.setBankCountry(s.getBankCountry());
        v.setRoutingCodeAba(s.getRoutingCodeAba());
        v.setRoutingCodeSwift(s.getRoutingCodeSwift());
        v.setIntermediaryBankName(s.getIntermediaryBankName());
        v.setIntermediaryAccount(s.getIntermediaryAccount());
        v.setIntermediaryAccountCountry(s.getIntermediaryAccountCountry());
        v.setIntermediaryRoutingCodeAba(s.getIntermediaryRoutingCodeAba());
        v.setIntermediaryRoutingCodeSwift(s.getIntermediaryRoutingCodeSwift());
        v.setStatusIntegrity(s.getStatusIntegrity());
        v.setXmlStatus(s.getXmlStatus());
        return v;
    }
}
