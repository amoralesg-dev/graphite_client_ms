package com.rassini.graphite_client.service.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class CatalogManagerCacheKey {

    private final String idCatalogo;
    private final String code;
    private final String businessUnit;

    public CatalogManagerCacheKey(String idCatalogo, String code, String businessUnit) {
        this.idCatalogo = idCatalogo;
        this.code = code;
        this.businessUnit = businessUnit;
    }
}