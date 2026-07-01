package com.rassini.graphite_client.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphiteSupplierDto {

    // =========================
    // Datos generales
    // =========================
    @JsonProperty("RASSINI_ERP_ID")
    @JsonAlias("ERP_ID")
    private String erpIdQad;

    @JsonProperty("Entity_Public_Id")
    private String entityPublicId;

    @JsonProperty("Entity_Name")
    private String entityName;

    
    @JsonProperty("Entity_Name_Translations")
    private Map<String, String> entityNameTranslations;


    @JsonProperty("Integration_Tax_ID")
    private String integrationTaxId;

    /**
     * En algunos JSON llega como Supplier_Contact_Email.
     * En otros puede venir como Payment_Contact_Email.
     */
    @JsonAlias({ "Supplier_Contact_Email", "Payment_Contact_Email" })
    private String supplierContactEmail;

    /**
     * Mantengo el nombre del campo para no romper tu mapper actual
     * que usa dto.getPaymentContactName().
     * Pero lo mapeo al nodo correcto: Supplier_Contact_Name
     * y dejo alias a Payment_Contact_Name por compatibilidad.
     */
    @JsonAlias({ "Supplier_Contact_Name", "Payment_Contact_Name" })
    private String paymentContactName;

    /**
     * Lo dejo porque ya existe en tu DTO actual.
     * Si en algunos suppliers viene en raíz, seguirá funcionando.
     */
    @JsonProperty("Loc_Sales_Contact_Alternate_Contact_Calc")
    private List<SalesContactCalc> locSalesContactAlternateContactCalc;

    @JsonProperty("ESTATUSNOHAYMAPEO_AUN")
    private String statusERPGraphite;

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

        /**
         * En CN730447 este nodo viene aquí, dentro de la Location.
         */
        @JsonProperty("Loc_Sales_Contact_Alternate_Contact_Calc")
        private List<SalesContactCalc> locSalesContactAlternateContactCalc;

        @JsonProperty("Loc_Sales_Contact_Alternate_Name_Calc")
        private String locSalesContactAlternateNameCalc;

        @JsonProperty("Loc_Sales_Contact_Alternate_Email_Calc")
        private String locSalesContactAlternateEmailCalc;
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

        @JsonProperty("premise")
        private String premise;

        @JsonProperty("postalCode")
        private String postalCode;

        @JsonProperty("countryIso3")
        private String countryIso3;

        @JsonProperty("locality")
        private String locality;

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
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
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

        
        @JsonProperty("Bank_Account_Number_IBAN")
        private String bankAccountNumberIban;


        @JsonProperty("Bank_Country")
        private String bankCountry;

        @JsonProperty("Bank_Name")
        private String bankName;

        @JsonProperty("Bank_Account_Currency")
        private String bankAccountCurrency;

        @JsonProperty("Bank_Number")
        private BankNumber bankNumber;

        @JsonProperty("Bank_SWIFT")
        private BankSwift bankSwift;

        @JsonProperty("Bank_Account_Currency_Correspondent")
        private String bankAccountCurrencyCorrespondent;

        @JsonProperty("Bank_Account_Currency_Correspondent_Bank_Country")
        private String bankAccountCurrencyCorrespondentBankCountry;

        @JsonProperty("Bank_Account_Currency_Correspondent_Bank")
        private CorrespondentBank bankAccountCurrencyCorrespondentBank;
        
        @JsonProperty("Bank_Name_Translations")
        private Map<String, String> bankNameTranslations;

        public Map<String, String> getBankNameTranslations() {
            return bankNameTranslations;
        }

        @JsonProperty("Bank_Account_Holder_Name_Translations")
        private Map<String, String> bankAccountHolderNameTranslations;

        public Map<String, String> getBankAccountHolderNameTranslations() {
            return bankAccountHolderNameTranslations;
        }
        
        public String getBankAccountNumber() {
            if (bankAccountNumber != null && !bankAccountNumber.trim().isEmpty()) {
                return bankAccountNumber;
            }
            return bankAccountNumberIban;
        }
        

    
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