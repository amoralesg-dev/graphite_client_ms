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
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        String rawDestinatarios = destinatarios == null ? null : String.join(",", destinatarios);
        enviarCorreo(null, rawDestinatarios, asunto, contenidoHtml);
    }

    @Override
    public void enviarCorreo(Long correoPendienteId,
                             String destinatarioRaw,
                             @NonNull String asunto,
                             @NonNull String contenidoHtml) {
        List<String> validRecipients = normalizarYValidarDestinatarios(correoPendienteId, destinatarioRaw, asunto);
        String[] destinatariosValidos = validRecipients.toArray(new String[0]);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // remitente configurado en yml y el mismo que el usuario autenticado en el mail server
            helper.setFrom(from);

            // destinatarios validados y normalizados
            helper.setTo(destinatariosValidos);
            helper.setSubject(asunto);

            // cuerpo en HTML
            helper.setText(contenidoHtml, true);

            mailSender.send(message);

            log.info("[EMAIL-SENT] correoPendienteId={} desde={} a={}",
                    correoPendienteId, from, Arrays.toString(destinatariosValidos));

        } catch (MessagingException ex) {
            log.error("[EMAIL-SEND-ERROR] correoPendienteId={} asunto='{}' error='{}'",
                    correoPendienteId, asunto, ex.getMessage(), ex);
            throw new IllegalStateException("Error construyendo o enviando correo (id=" + correoPendienteId + "): " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> normalizarYValidarDestinatarios(Long correoPendienteId, String destinatarioRaw, String asunto) {
        int rawLength = destinatarioRaw == null ? 0 : destinatarioRaw.length();

        if (destinatarioRaw == null || destinatarioRaw.trim().isEmpty()) {
            log.info("[EMAIL-RECIPIENT-CHECK] correoPendienteId={} asunto='{}' destinatarioRecibido='{}' longitud={} cantidadDestinatariosDetectados=0",
                    correoPendienteId, asunto, destinatarioRaw, rawLength);
            log.error("[EMAIL-INVALID-RECIPIENT] correoPendienteId={} destinatarioInvalido='{}' motivo='Destinatario nulo o vacío'",
                    correoPendienteId, destinatarioRaw);
            throw new IllegalArgumentException(
                    "Destinatario vacío o nulo para correo_pendiente id=" + correoPendienteId + ": '" + destinatarioRaw + "'");
        }

        String trimmedRaw = destinatarioRaw.trim();
        String[] rawTokens = trimmedRaw.split("[,;]");

        List<String> nonEmptyCandidates = new ArrayList<>();
        for (String token : rawTokens) {
            if (token != null) {
                String cleaned = token.trim();
                if (!cleaned.isEmpty()) {
                    nonEmptyCandidates.add(cleaned);
                }
            }
        }

        log.info("[EMAIL-RECIPIENT-CHECK] correoPendienteId={} asunto='{}' destinatarioRecibido='{}' longitud={} cantidadDestinatariosDetectados={}",
                correoPendienteId, asunto, destinatarioRaw, rawLength, nonEmptyCandidates.size());

        if (nonEmptyCandidates.isEmpty()) {
            log.error("[EMAIL-INVALID-RECIPIENT] correoPendienteId={} destinatarioInvalido='{}' motivo='Sin direcciones tras eliminar elementos vacíos'",
                    correoPendienteId, destinatarioRaw);
            throw new IllegalArgumentException(
                    "Sin destinatarios válidos tras normalización para correo_pendiente id=" + correoPendienteId + ": '" + destinatarioRaw + "'");
        }

        List<String> validAddresses = new ArrayList<>(nonEmptyCandidates.size());
        for (String candidate : nonEmptyCandidates) {
            try {
                InternetAddress internetAddress = new InternetAddress(candidate, true);
                internetAddress.validate();
                validAddresses.add(internetAddress.getAddress());
            } catch (AddressException ex) {
                log.error("[EMAIL-INVALID-RECIPIENT] correoPendienteId={} destinatarioRecibido='{}' valorInvalido='{}' motivo='{}'",
                        correoPendienteId, destinatarioRaw, candidate, ex.getMessage());
                throw new IllegalArgumentException(
                        "Dirección de correo inválida '" + candidate + "' en correo_pendiente id=" + correoPendienteId, ex);
            }
        }

        return validAddresses;
    }
}
