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

    @JsonProperty("Payment_Contact_Name")
    private String paymentContactName;

    @JsonProperty("Loc_Sales_Contact_Alternate_Contact_Calc")
    private List<SalesContactCalc> locSalesContactAlternateContactCalc;

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

        @JsonProperty("Address_1")
        private String address1;

        @JsonProperty("Address_2")
        private String address2;

        @JsonProperty("Address_3")
        private String address3;

        @JsonProperty("Address_Neighborhood")
        private String addressNeighborhood;

        @JsonProperty("Address_Postal_Code")
        private String addressPostalCode;

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

         @JsonProperty("deliveryLine1")
        private String deliveryLine1;

        @JsonProperty("lastLine")
        private String lastLine;

        @JsonProperty("components")
        private Components components;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CorrespondentBank {

        @JsonProperty("bank_name")
        private String bankName;

        @JsonProperty("swift")
        private String swift;

        @JsonProperty("country")
        private String country;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Components {

        // ===== actuales =====
        @JsonProperty("premise")
        private String premise;

        @JsonProperty("postalCode")
        private String postalCode;

        @JsonProperty("countryIso3")
        private String countryIso3;

        @JsonProperty("locality")
        private String locality;

        // ===== faltantes MX/INTL =====
        @JsonProperty("premiseNumber")
        private String premiseNumber;

        @JsonProperty("subBuilding")
        private String subBuilding;

        @JsonProperty("postalCodeShort")
        private String postalCodeShort;

        @JsonProperty("administrativeArea")
        private String administrativeArea;

        @JsonProperty("administrativeAreaIso2")
        private String administrativeAreaIso2;

        @JsonProperty("thoroughfare")
        private String thoroughfare;

        @JsonProperty("thoroughfareName")
        private String thoroughfareName;

        @JsonProperty("thoroughfareType")
        private String thoroughfareType;

        @JsonProperty("dependentLocality")
        private String dependentLocality;

        @JsonProperty("building")
        private String building;

        // ===== faltantes US =====
        @JsonProperty("primaryNumber")
        private String primaryNumber;

        @JsonProperty("streetName")
        private String streetName;

        @JsonProperty("streetPredirection")
        private String streetPredirection;

        @JsonProperty("streetSuffix")
        private String streetSuffix;

        @JsonProperty("secondaryNumber")
        private String secondaryNumber;

        @JsonProperty("secondaryDesignator")
        private String secondaryDesignator;

        @JsonProperty("cityName")
        private String cityName;

        @JsonProperty("defaultCityName")
        private String defaultCityName;

        @JsonProperty("state")
        private String state;

        @JsonProperty("zipCode")
        private String zipCode;

        @JsonProperty("plus4Code")
        private String plus4Code;
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

        @JsonProperty("RASSINI_ERP_Supplier_Type")
        private String rassiniErpSupplierType;

        @JsonProperty("RASSINI_ERP_Address")
        private ErpAddressContainer rassiniErpAddress;
    }

    // =========================
    // ERP ADDRESS CONTAINER
    // =========================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErpAddressContainer {

        @JsonProperty("Address")
        private Address address;
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

        @JsonProperty("Bank_Account_Currency")
        private String bankAccountCurrency;

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

    // =========================
    // BANK WIRE ABA ROUTING
    // =========================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankWireAbaRouting {

        @JsonProperty("bank_name")
        private String bankName;

        @JsonProperty("routing")
        private String routing;

        @JsonProperty("swift")
        private String swift;
    }

    // =========================
    // SALES CONTACT CALC
    // =========================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SalesContactCalc {

        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;
    }
}