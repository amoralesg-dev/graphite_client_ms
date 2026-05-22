package com.rassini.graphite_client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "catalog_manager")
@Getter
@Setter
@ToString
public class CatalogManagerEntity {

    @EmbeddedId
    private CatalogManagerId id;

    @Column(name = "equivalencia", length = 100)
    private String equivalencia;

    @Column(name = "description", length = 255)
    private String description;

    public String getIdCatalogo() {
        return id != null ? id.getIdCatalogo() : null;
    }

    public String getCode() {
        return id != null ? id.getCode() : null;
    }

    public String getBusinessUnit() {
        return id != null ? id.getBusinessUnit() : null;
    }
}
