package com.rassini.graphite_client.service.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedAddress {
    private String streetName;
    private String streetName2;
    private String streetName3;
    private String streetNumber;
    private String city;
    private String region;
    private String postalCode;
    private String country;
}