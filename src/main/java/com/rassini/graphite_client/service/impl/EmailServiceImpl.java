package com.rassini.graphite_client.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.rassini.graphite_client.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // Tomamos el remitente del yml (spring.mail.username)
    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void enviarCorreo(@NonNull String[] destinatarios,
                             @NonNull String asunto,
                             @NonNull String contenidoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // remitente configurado en yml
            helper.setFrom(from);

            // destinatarios separados por coma
            helper.setTo(destinatarios);
            helper.setSubject(asunto);

            // cuerpo en HTML
            helper.setText(contenidoHtml, true);

            mailSender.send(message);

            log.info("Correo enviado correctamente desde {} a {}", from, (Object) destinatarios);

        } catch (MessagingException ex) {
            log.error("Error enviando correo", ex);
        }
    }
}
