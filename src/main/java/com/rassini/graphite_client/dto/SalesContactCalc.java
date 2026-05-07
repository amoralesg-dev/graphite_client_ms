package com.rassini.graphite_client.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesContactCalc {

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;
}