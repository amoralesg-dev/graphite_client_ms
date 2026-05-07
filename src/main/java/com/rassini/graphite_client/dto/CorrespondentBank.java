package com.rassini.graphite_client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorrespondentBank {

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("routing")
    private String routing;

    @JsonProperty("type")
    private String type;

    @JsonProperty("valid")
    private Boolean valid;

    @JsonProperty("fromDatabase")
    private Boolean fromDatabase;
}