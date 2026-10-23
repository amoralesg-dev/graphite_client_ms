package com.rassini.graphite_client.service;

import java.util.List;

public interface EmailService {

    void enviarCorreo(String[] destinatarios, String asunto, String contenidoHtml);

    default void enviarCorreo(Long correoPendienteId, String destinatarioRaw, String asunto, String contenidoHtml) {
        List<String> validRecipients = normalizarYValidarDestinatarios(correoPendienteId, destinatarioRaw, asunto);
        enviarCorreo(validRecipients.toArray(new String[0]), asunto, contenidoHtml);
    }

    List<String> normalizarYValidarDestinatarios(Long correoPendienteId, String destinatarioRaw, String asunto);
}