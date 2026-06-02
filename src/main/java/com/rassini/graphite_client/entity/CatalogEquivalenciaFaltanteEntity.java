package com.rassini.graphite_client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_equivalencia_faltante")
@Getter
@Setter
@ToString
public class CatalogEquivalenciaFaltanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_catalogo", nullable = false, length = 100)
    private String idCatalogo;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "business_unit", nullable = false, length = 50)
    private String businessUnit;

    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion;

    @Column(name = "total_ocurrencias", nullable = false)
    private Integer totalOcurrencias;

    @Column(name = "notificado", nullable = false)
    private Boolean notificado;

    @Column(name = "fecha_notificacion")
    private LocalDateTime fechaNotificacion;
}