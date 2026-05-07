package com.rassini.graphite_client.dto;

import lombok.Builder;
import lombok.Data;


import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
public class AckRequest {

    @JsonProperty("interface")
    private String javaInterface;   // Nombre de la interfaz
    private String connectionRole;   // Rol de conexión
    private String publicId;         // ID público para el cambio
}
