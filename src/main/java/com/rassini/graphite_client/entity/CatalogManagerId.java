package com.rassini.graphite_client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CatalogManagerId implements Serializable {

    @Column(name = "id_catalogo", nullable = false, length = 100)
    private String idCatalogo;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "business_unit", nullable = false, length = 10)
    private String businessUnit;
}