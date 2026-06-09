package com.rassini.graphite_client.service;

public interface EmailService {

    
    void enviarCorreo(String[] destinatarios, String asunto, String contenidoHtml);
}