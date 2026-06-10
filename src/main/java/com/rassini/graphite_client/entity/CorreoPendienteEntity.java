package com.rassini.graphite_client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "correo_pendiente")
@Getter
@Setter
@ToString
public class CorreoPendienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario", nullable = false, length = 400)
    private String to;

    @Column(name = "asunto", nullable = false, length = 300)
    private String subject;

    @Column(name = "cuerpo", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "enviado", nullable = false)
    private Boolean enviado = false;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
