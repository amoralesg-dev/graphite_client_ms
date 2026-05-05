package com.rassini.graphite_client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "graphiteSuppliers")
@Data
public class SupplierEntity {

    @Id
    @Column(name = "public_id")
    private String publicId;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "full_json", columnDefinition = "LONGTEXT")
    private String fullJson;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    @Column(name = "status")
    @Enumerated(EnumType.STRING) // Almacena el estado como cadena
    private ProviderState status; // Nuevo campo para el estado del proveedor
}
