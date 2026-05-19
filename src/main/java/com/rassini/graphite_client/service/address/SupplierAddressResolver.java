package com.rassini.graphite_client.service.address;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.dto.GraphiteSupplierDto.Location;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupplierAddressResolver {

    private SupplierAddressResolver() {
        // utility class
    }

    // =====================================================
    // API PUBLICA
    // =====================================================
    public static ResolvedAddress resolve(
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.Location headquarters,
            GraphiteSupplierDto.ErpRecord erp
    ) {

        AddressSource source = selectBestAddressSource(dto, headquarters, erp);
        if (source == null || source.address == null) {
            log.warn("[ADDR] provider={} without address source",
                    dto != null ? dto.getEntityPublicId() : null);
            return null;
        }

        AddressShape shape = detectAddressShape(source);

        log.info(
                "[ADDR] provider={} source={} usScore={} mxScore={} structureDetected={}",
                dto != null ? dto.getEntityPublicId() : null,
                source.sourceName,
                scoreUsLike(source),
                scoreMxLike(source),
                shape
        );

        switch (shape) {
            case US_LIKE:
                return resolveUnitedStates(dto, source);
            case MX_LIKE:
                return resolveMexicoLike(dto, source);
            default:
                return resolveGeneric(dto, source);
        }
    }

    // =====================================================
    // SOURCE
    // =====================================================
    private static AddressSource selectBestAddressSource(
            GraphiteSupplierDto dto,
            GraphiteSupplierDto.Location headquarters,
            GraphiteSupplierDto.ErpRecord erp
    ) {

        Object v1 = invoke(erp, "getRassiniErpAddress");
        Object v2 = invoke(erp, "getRassiniERPAddress");
        Object v3 = invoke(erp, "getRASSINIERPAddress");
        Object v4 = invoke(erp, "getRassini_ERP_Address");

        log.debug(
                "[ADDR-SOURCE] provider={} getRassiniErpAddress={} getRassiniERPAddress={} getRASSINIERPAddress={} getRassini_ERP_Address={}",
                dto != null ? dto.getEntityPublicId() : null,
                v1, v2, v3, v4
        );

        Object erpAddressWrapper = firstNonNull(v1, v2, v3, v4);
        Object erpAddress = erpAddressWrapper != null ? invoke(erpAddressWrapper, "getAddress") : null;

        log.debug(
                "[ADDR-SOURCE] provider={} erpAddressWrapper={} erpAddress={}",
                dto != null ? dto.getEntityPublicId() : null,
                erpAddressWrapper,
                erpAddress
        );

        if (erpAddress != null) {
            log.debug("[ADDR-SOURCE] provider={} using ERP source",
                    dto != null ? dto.getEntityPublicId() : null);
            return AddressSource.of("ERP", erpAddress);
        }

        Location mainLocation = headquarters != null ? headquarters : selectMainLocation(dto);
        if (mainLocation != null && mainLocation.getAddress() != null) {
            log.debug("[ADDR-SOURCE] provider={} using LOCATION source",
                    dto != null ? dto.getEntityPublicId() : null);
            return AddressSource.of("LOCATION", mainLocation.getAddress());
        }

        log.warn("[ADDR-SOURCE] provider={} no ERP source and no LOCATION source",
                dto != null ? dto.getEntityPublicId() : null);
        return null;
    }

    private static Location selectMainLocation(GraphiteSupplierDto dto) {
        if (dto == null || dto.getLocations() == null || dto.getLocations().isEmpty()) {
            return null;
        }

        return dto.getLocations().stream()
                .filter(l -> l != null)
                .findFirst()
                .orElse(null);
    }

    // =====================================================
    // DETECCION DE ESTRUCTURA (REGLAS, NO SCORE)
    // =====================================================
    private static AddressShape detectAddressShape(AddressSource source) {

        // 1) patrón US explícito
        if (hasUsPattern(source)) {
            return AddressShape.US_LIKE;
        }

        // 2) patrón MX/INTL explícito
        if (hasMxLikePattern(source)) {
            return AddressShape.MX_LIKE;
        }

        // 3) fallback por region
        String region = firstNonBlank(
                stringValue(invoke(source.address, "getAddressRegionState")),
                stringValue(invoke(source.components, "getAdministrativeAreaIso2")),
                stringValue(invoke(source.components, "getState"))
        );

        if (notBlank(region)) {
            String upper = region.trim().toUpperCase();

            if (upper.startsWith("US-")) {
                return AddressShape.US_LIKE;
            }

            if (upper.startsWith("MX-") || upper.startsWith("BR-")) {
                return AddressShape.MX_LIKE;
            }
        }

        // 4) fallback por country
        String country = firstNonBlank(
                stringValue(invoke(source.address, "getAddressCountry")),
                iso3ToIso2(stringValue(invoke(source.components, "getCountryIso3")))
        );

        if (notBlank(country)) {
            String upper = country.trim().toUpperCase();

            if ("US".equals(upper)) {
                return AddressShape.US_LIKE;
            }

            if ("MX".equals(upper) || "BR".equals(upper)) {
                return AddressShape.MX_LIKE;
            }
        }

        return AddressShape.UNKNOWN;
    }

    private static boolean hasUsPattern(AddressSource source) {
        return notBlank(stringValue(invoke(source.data, "getDeliveryLine1")))
                || notBlank(stringValue(invoke(source.data, "getLastLine")))
                || notBlank(stringValue(invoke(source.components, "getPrimaryNumber")))
                || notBlank(stringValue(invoke(source.components, "getStreetName")))
                || notBlank(stringValue(invoke(source.components, "getStreetSuffix")))
                || notBlank(stringValue(invoke(source.components, "getStreetPredirection")))
                || notBlank(stringValue(invoke(source.components, "getSecondaryDesignator")))
                || notBlank(stringValue(invoke(source.components, "getSecondaryNumber")))
                || notBlank(stringValue(invoke(source.components, "getCityName")))
                || notBlank(stringValue(invoke(source.components, "getDefaultCityName")))
                || notBlank(stringValue(invoke(source.components, "getState")))
                || notBlank(stringValue(invoke(source.components, "getZipCode")))
                || notBlank(stringValue(invoke(source.components, "getPlus4Code")));
    }

    private static boolean hasMxLikePattern(AddressSource source) {
        return notBlank(stringValue(invoke(source.data, "getAddress1")))
                || notBlank(stringValue(invoke(source.data, "getAddress2")))
                || notBlank(stringValue(invoke(source.data, "getAddress3")))
                || notBlank(stringValue(invoke(source.components, "getThoroughfare")))
                || notBlank(stringValue(invoke(source.components, "getThoroughfareName")))
                || notBlank(stringValue(invoke(source.components, "getThoroughfareType")))
                || notBlank(stringValue(invoke(source.components, "getLocality")))
                || notBlank(stringValue(invoke(source.components, "getAdministrativeArea")))
                || notBlank(stringValue(invoke(source.components, "getAdministrativeAreaIso2")))
                || notBlank(stringValue(invoke(source.components, "getPostalCode")))
                || notBlank(stringValue(invoke(source.components, "getPostalCodeShort")))
                || notBlank(stringValue(invoke(source.components, "getCountryIso3")));
    }

    private static int scoreUsLike(AddressSource source) {
        int score = 0;

        if (notBlank(stringValue(invoke(source.data, "getDeliveryLine1")))) score += 2;
        if (notBlank(stringValue(invoke(source.data, "getLastLine")))) score += 2;
        if (notBlank(stringValue(invoke(source.components, "getPrimaryNumber")))) score += 2;
        if (notBlank(stringValue(invoke(source.components, "getStreetName")))) score += 2;
        if (notBlank(stringValue(invoke(source.components, "getStreetSuffix")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getStreetPredirection")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getSecondaryDesignator")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getSecondaryNumber")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getCityName")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getDefaultCityName")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getState")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getZipCode")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getPlus4Code")))) score += 1;

        return score;
    }

    private static int scoreMxLike(AddressSource source) {
        int score = 0;

        if (notBlank(stringValue(invoke(source.data, "getAddress1")))) score += 2;
        if (notBlank(stringValue(invoke(source.data, "getAddress2")))) score += 1;
        if (notBlank(stringValue(invoke(source.data, "getAddress3")))) score += 1;
        if (notBlank(stringValue(invoke(source.address, "getAddressCity")))) score += 1;
        if (notBlank(stringValue(invoke(source.address, "getAddressRegionState")))) score += 1;
        if (notBlank(stringValue(invoke(source.address, "getAddressPostalCode")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getThoroughfare")))) score += 2;
        if (notBlank(stringValue(invoke(source.components, "getThoroughfareName")))) score += 2;
        if (notBlank(stringValue(invoke(source.components, "getThoroughfareType")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getPremise")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getPremiseNumber")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getSubBuilding")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getDependentLocality")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getBuilding")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getLocality")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getAdministrativeArea")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getAdministrativeAreaIso2")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getPostalCode")))) score += 1;
        if (notBlank(stringValue(invoke(source.components, "getPostalCodeShort")))) score += 1;

        return score;
    }

    // =====================================================
    // RESOLVERS
    // =====================================================
    private static ResolvedAddress resolveMexicoLike(GraphiteSupplierDto dto, AddressSource source) {

        log.info("[ADDR][MX_LIKE] provider={} source={}",
                dto != null ? dto.getEntityPublicId() : null,
                source.sourceName);

        String street1 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress1")),
                stringValue(invoke(source.address, "getAddress1")),
                buildMxLikeStreet1(source)
        );

        String street2 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress2")),
                stringValue(invoke(source.address, "getAddress2")),
                buildMxLikeStreet2(source)
        );

        String street3 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress3")),
                stringValue(invoke(source.address, "getAddress3")),
                buildMxLikeStreet3(source)
        );

        String city = firstNonBlank(
                stringValue(invoke(source.address, "getAddressCity")),
                stringValue(invoke(source.components, "getLocality"))
        );

        String region = firstNonBlank(
                stringValue(invoke(source.address, "getAddressRegionState")),
                stringValue(invoke(source.components, "getAdministrativeAreaIso2")),
                stringValue(invoke(source.components, "getAdministrativeArea"))
        );

        String postalCode = firstNonBlank(
                stringValue(invoke(source.address, "getAddressPostalCode")),
                stringValue(invoke(source.components, "getPostalCode")),
                stringValue(invoke(source.components, "getPostalCodeShort"))
        );

        String country = firstNonBlank(
                stringValue(invoke(source.address, "getAddressCountry")),
                iso3ToIso2(stringValue(invoke(source.components, "getCountryIso3")))
        );

        String streetNumber = firstNonBlank(
                stringValue(invoke(source.components, "getPremise")),
                stringValue(invoke(source.components, "getPremiseNumber")),
                stringValue(invoke(source.components, "getSubBuilding")),
                extractStreetNumber(street1)
        );

        return ResolvedAddress.builder()
                .streetName(street1)
                .streetName2(street2)
                .streetName3(street3)
                .streetNumber(streetNumber)
                .city(city)
                .region(region)
                .postalCode(postalCode)
                .country(country)
                .build();
    }

    private static ResolvedAddress resolveUnitedStates(GraphiteSupplierDto dto, AddressSource source) {

        log.info("[ADDR][US_LIKE] provider={} source={}",
                dto != null ? dto.getEntityPublicId() : null,
                source.sourceName);

        String street1 = firstNonBlank(
                stringValue(invoke(source.data, "getDeliveryLine1")),
                buildUsStreet1(source),
                stringValue(invoke(source.data, "getAddress1")),
                stringValue(invoke(source.address, "getAddress1"))
        );

        String street2 = firstNonBlank(
                buildUsStreet2(source),
                stringValue(invoke(source.data, "getAddress2")),
                stringValue(invoke(source.address, "getAddress2"))
        );

        String street3 = firstNonBlank(
                stringValue(invoke(source.components, "getBuilding")),
                stringValue(invoke(source.data, "getAddress3")),
                stringValue(invoke(source.address, "getAddress3"))
        );

        String city = firstNonBlank(
                stringValue(invoke(source.components, "getCityName")),
                stringValue(invoke(source.components, "getDefaultCityName")),
                stringValue(invoke(source.address, "getAddressCity"))
        );

        String region = firstNonBlank(
                stringValue(invoke(source.address, "getAddressRegionState")),
                stringValue(invoke(source.components, "getState"))
        );

        String postalCode = buildUsPostalCode(source);

        String country = firstNonBlank(
                stringValue(invoke(source.address, "getAddressCountry")),
                iso3ToIso2(stringValue(invoke(source.components, "getCountryIso3"))),
                "US"
        );

        String streetNumber = firstNonBlank(
                stringValue(invoke(source.components, "getPrimaryNumber")),
                extractStreetNumber(street1)
        );

        return ResolvedAddress.builder()
                .streetName(street1)
                .streetName2(street2)
                .streetName3(street3)
                .streetNumber(streetNumber)
                .city(city)
                .region(region)
                .postalCode(postalCode)
                .country(country)
                .build();
    }

    private static ResolvedAddress resolveGeneric(GraphiteSupplierDto dto, AddressSource source) {

        log.info("[ADDR][GENERIC] provider={} source={}",
                dto != null ? dto.getEntityPublicId() : null,
                source.sourceName);

        String street1 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress1")),
                stringValue(invoke(source.address, "getAddress1"))
        );

        String street2 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress2")),
                stringValue(invoke(source.address, "getAddress2"))
        );

        String street3 = firstNonBlank(
                stringValue(invoke(source.data, "getAddress3")),
                stringValue(invoke(source.address, "getAddress3"))
        );

        String city = firstNonBlank(
                stringValue(invoke(source.address, "getAddressCity")),
                stringValue(invoke(source.components, "getLocality")),
                stringValue(invoke(source.components, "getCityName"))
        );

        String region = firstNonBlank(
                stringValue(invoke(source.address, "getAddressRegionState")),
                stringValue(invoke(source.components, "getAdministrativeAreaIso2")),
                stringValue(invoke(source.components, "getAdministrativeArea")),
                stringValue(invoke(source.components, "getState"))
        );

        String postalCode = firstNonBlank(
                stringValue(invoke(source.address, "getAddressPostalCode")),
                stringValue(invoke(source.components, "getPostalCode")),
                stringValue(invoke(source.components, "getPostalCodeShort")),
                stringValue(invoke(source.components, "getZipCode"))
        );

        String country = firstNonBlank(
            stringValue(invoke(source.address, "getAddressCountry")),
            iso3ToIso2(stringValue(invoke(source.components, "getCountryIso3")))
        );

        return ResolvedAddress.builder()
                .streetName(street1)
                .streetName2(street2)
                .streetName3(street3)
                .streetNumber(extractStreetNumber(street1))
                .city(city)
                .region(region)
                .postalCode(postalCode)
                .country(country)
                .build();
    }

    // =====================================================
    // BUILDERS
    // =====================================================
    private static String buildUsStreet1(AddressSource source) {
        return joinNonBlank(
                stringValue(invoke(source.components, "getPrimaryNumber")),
                stringValue(invoke(source.components, "getStreetPredirection")),
                stringValue(invoke(source.components, "getStreetName")),
                stringValue(invoke(source.components, "getStreetSuffix"))
        );
    }

    private static String buildUsStreet2(AddressSource source) {
        return firstNonBlank(
                joinNonBlank(
                        stringValue(invoke(source.components, "getSecondaryDesignator")),
                        stringValue(invoke(source.components, "getSecondaryNumber"))
                ),
                stringValue(invoke(source.components, "getSubBuilding"))
        );
    }

    private static String buildUsPostalCode(AddressSource source) {
        String zip = stringValue(invoke(source.components, "getZipCode"));
        String plus4 = stringValue(invoke(source.components, "getPlus4Code"));

        if (notBlank(zip) && notBlank(plus4)) {
            return zip + "-" + plus4;
        }

        return firstNonBlank(
                zip,
                stringValue(invoke(source.address, "getAddressPostalCode")),
                stringValue(invoke(source.components, "getPostalCode"))
        );
    }

    private static String buildMxLikeStreet1(AddressSource source) {
        return joinNonBlank(
                stringValue(invoke(source.components, "getThoroughfareType")),
                firstNonBlank(
                        stringValue(invoke(source.components, "getThoroughfareName")),
                        stringValue(invoke(source.components, "getThoroughfare"))
                ),
                firstNonBlank(
                        stringValue(invoke(source.components, "getPremise")),
                        stringValue(invoke(source.components, "getPremiseNumber"))
                ),
                stringValue(invoke(source.components, "getSubBuilding"))
        );
    }

    private static String buildMxLikeStreet2(AddressSource source) {
        return firstNonBlank(
                stringValue(invoke(source.components, "getBuilding")),
                stringValue(invoke(source.components, "getDependentLocality"))
        );
    }

    private static String buildMxLikeStreet3(AddressSource source) {
        return firstNonBlank(
                joinNonBlank(
                        stringValue(invoke(source.components, "getLocality")),
                        stringValue(invoke(source.components, "getAdministrativeArea"))
                ),
                stringValue(invoke(source.address, "getAddressNeighborhood"))
        );
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String extractStreetNumber(String street1) {
        if (!notBlank(street1)) {
            return null;
        }

        String[] parts = street1.trim().split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+[A-Za-z-]*")) {
                return parts[i];
            }
        }
        return null;
    }

    private static String joinNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (notBlank(value)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }

        return sb.length() == 0 ? null : sb.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String iso3ToIso2(String iso3) {
        if (!notBlank(iso3)) {
            return null;
        }

        switch (iso3.trim().toUpperCase()) {
            case "MEX":
                return "MX";
            case "USA":
                return "US";
            case "BRA":
                return "BR";
            default:
                return iso3.trim().toUpperCase();
        }
    }

    private static final class AddressSource {
        private final String sourceName;
        private final Object address;
        private final Object data;
        private final Object components;

        private AddressSource(String sourceName, Object address, Object data, Object components) {
            this.sourceName = sourceName;
            this.address = address;
            this.data = data;
            this.components = components;
        }

        private static AddressSource of(String sourceName, Object address) {
            Object data = invoke(address, "getData");
            Object components = data != null ? invoke(data, "getComponents") : null;
            return new AddressSource(sourceName, address, data, components);
        }
    }
}
