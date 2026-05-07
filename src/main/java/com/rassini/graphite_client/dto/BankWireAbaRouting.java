package com.rassini.graphite_client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankWireAbaRouting {

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("swift")
    private String swift;

    @JsonProperty("type")
    private String type;   // ABA

    @JsonProperty("routing")
    private String routing;

    @JsonProperty("valid")
    private Boolean valid;

    @JsonProperty("fromDatabase")
    private Boolean fromDatabase;

    
}