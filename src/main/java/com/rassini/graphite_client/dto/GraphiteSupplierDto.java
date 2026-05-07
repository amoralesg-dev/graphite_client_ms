package com.rassini.graphite_client.dto;

import java.util.List;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphiteSupplierDto {

    // =========================
    // Datos generales
    // =========================
    @JsonProperty("ERP_ID")
    private String erpIDQAD;


    @JsonProperty("Entity_Public_Id")
    private String entityPublicId;

    @JsonProperty("Entity_Name")
    private String entityName;

    @JsonProperty("Integration_Tax_ID")
    private String integrationTaxId;

    // Usado por SupplierRowMapper (NO quitar)
    @JsonProperty("Payment_Contact_Email")
    private String supplierContactEmail;

    // =========================
    // Locations
    // =========================

    @JsonProperty("Locations_List")
    private List<Location> locations;

    // =========================
    // ERP Record
    // =========================

    @JsonProperty("ERP_Record")
    private List<ErpRecord> erpRecords;

    // =========================
    // Inner classes
    // =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {

        @JsonProperty("Location_Name")
        private String locationName;

        @JsonProperty("Address")
        private Address address;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {

        @JsonProperty("Address_City")
        private String addressCity;

        @JsonProperty("Address_Region_State")
        private String addressRegionState;

        @JsonProperty("Address_Country")
        private String addressCountry;

        @JsonProperty("data")
        private AddressData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressData {

        @JsonProperty("address1")
        private String address1;

        @JsonProperty("address2")
        private String address2;

        @JsonProperty("address3")
        private String address3;

        @JsonProperty("components")
        private Components components;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Components {

        @JsonProperty("premise")
        private String premise;

        @JsonProperty("postalCode")
        private String postalCode;

        @JsonProperty("countryIso3")
        private String countryIso3;

        @JsonProperty("locality")
        private String locality;
    }

    // =========================
    // ERP RECORD
    // =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErpRecord {

        @JsonProperty("RASSINI_ERP_Entity_ID")
        private String rassiniErpEntityId;

        @JsonProperty("RASSINI_ERP_Payment_Terms")
        private String rassiniErpPaymentTerms;

        @JsonProperty("RASSINI_ERP_Tax_Class")
        private String rassiniErpTaxClass;

        @JsonProperty("RASSINI_ERP_Tax_Zone")
        private List<String> rassiniErpTaxZone;

        @JsonProperty("RASSINI_ERP_Payment_Type")
        private String rassiniErpPaymentType;

        @JsonProperty("ERP_Bank_List")
        private List<Bank> erpBankList;

        @JsonProperty("Bank_Wire_ABA_Routing")
        private BankWireAbaRouting bankWireAbaRouting;

    

    }

    // =========================
    // BANK (EXTENDIDA)
    // =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bank {

        @JsonProperty("Bank_Currency_List")
        private List<String> bankCurrencyList;

        @JsonProperty("Bank_Account_Holder_Name")
        private String bankAccountHolderName;

        @JsonProperty("Bank_Account_Number")
        private String bankAccountNumber;

        // --- nuevos campos útiles para BD ---
        @JsonProperty("Bank_Country")
        private String bankCountry;

        @JsonProperty("Bank_Name")
        private String bankName;

        // Objeto: Bank_Number { bank_name, swift, routing, ... }
        @JsonProperty("Bank_Number")
        private BankNumber bankNumber;

        // Objeto: Bank_SWIFT { routing, valid }
        @JsonProperty("Bank_SWIFT")
        private BankSwift bankSwift;

        
        @JsonProperty("Bank_Account_Currency_Correspondent")
        private String bankAccountCurrencyCorrespondent;

        @JsonProperty("Bank_Account_Currency_Correspondent_Bank_Country")
        private String bankAccountCurrencyCorrespondentBankCountry;

        @JsonProperty("Bank_Account_Currency_Correspondent_Bank")
        private CorrespondentBank bankAccountCurrencyCorrespondentBank;

    }

    // =========================
    // BANK_NUMBER (Bank_Number en JSON)
    // =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankNumber {

        @JsonProperty("bank_name")
        private String bankName;

        @JsonProperty("swift")
        private String swift;

        @JsonProperty("routing")
        private String routing;

        @JsonProperty("type")
        private String type;

        @JsonProperty("valid")
        private Boolean valid;
    }

    // =========================
    // BANK_SWIFT (Bank_SWIFT en JSON)
    // =========================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankSwift {

        @JsonProperty("routing")
        private String routing;

        @JsonProperty("valid")
        private Boolean valid;
    }

    
    @JsonProperty("Payment_Contact_Name")
    private String paymentContactName;


    
    @JsonProperty("Loc_Sales_Contact_Alternate_Contact_Calc")
    List<SalesContactCalc> locSalesContactAlternateContactCalc;

}