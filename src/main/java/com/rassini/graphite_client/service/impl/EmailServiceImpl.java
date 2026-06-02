package com.rassini.graphite_client.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.rassini.graphite_client.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.notification.to}")
    private String destinatario;

    @Override
    public void enviarCorreo(
            String asunto,
            String contenido) {

        try {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(destinatario);
            message.setSubject(asunto);
            message.setText(contenido);

            mailSender.send(message);

            log.info("Correo enviado correctamente");

        } catch (Exception ex) {

            log.error(
                    "Error enviando correo",
                    ex);
        }
    }
}