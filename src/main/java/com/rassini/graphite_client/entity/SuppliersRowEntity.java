package com.rassini.graphite_client.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
    name = "suppliers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_suppliers_creditor_erp",
            columnNames = {"supplier_code", "business_unit_code"}
        )
    }
)

@Data
public class SuppliersRowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private XmlStatus xmlStatus = XmlStatus.PENDING;

}
