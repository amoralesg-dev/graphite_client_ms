package com.rassini.graphite_client.scheduler;

import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.service.impl.EmailServiceImpl;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorreoPendienteSchedulerAndEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private CorreoPendienteRepository correoPendienteRepository;

    private EmailServiceImpl emailService;
    private CorreoPendienteScheduler scheduler;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "from", "botrss@rassini.com");
        scheduler = new CorreoPendienteScheduler(correoPendienteRepository, emailService);

        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private CorreoPendienteEntity crearCorreo(Long id, String destinatario, String asunto) {
        CorreoPendienteEntity entity = new CorreoPendienteEntity();
        entity.setId(id);
        entity.setTo(destinatario);
        entity.setSubject(asunto);
        entity.setBody("<html><body>Test Body</body></html>");
        entity.setEnviado(false);
        entity.setFechaEnvio(null);
        return entity;
    }

    @Test
    @DisplayName("1. Un destinatario válido: envía por mailSender y marca enviado=true")
    void testSingleValidRecipient() throws Exception {
        CorreoPendienteEntity correo = crearCorreo(101L, "developerrss@rassini.com", "Asunto prueba 1");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));

        scheduler.enviarCorreosPendientes();

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(msgCaptor.capture());

        Address[] recipients = msgCaptor.getValue().getRecipients(Message.RecipientType.TO);
        assertNotNull(recipients);
        assertEquals(1, recipients.length);
        assertEquals("developerrss@rassini.com", recipients[0].toString());
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
        assertNotNull(correo.getFechaEnvio());
    }

    @Test
    @DisplayName("2. Destinatario con espacios alrededor: aplica trim y envía correctamente")
    void testRecipientWithSurroundingSpaces() throws Exception {
        CorreoPendienteEntity correo = crearCorreo(102L, "   developerrss@rassini.com   ", "Asunto con espacios");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));

        scheduler.enviarCorreosPendientes();

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(msgCaptor.capture());

        Address[] recipients = msgCaptor.getValue().getRecipients(Message.RecipientType.TO);
        assertEquals(1, recipients.length);
        assertEquals("developerrss@rassini.com", recipients[0].toString());
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
    }

    @Test
    @DisplayName("3. Dos destinatarios separados por coma: normaliza y envía a ambos")
    void testTwoRecipientsSeparatedByComma() throws Exception {
        CorreoPendienteEntity correo = crearCorreo(103L, "uno@rassini.com, dos@rassini.com", "Asunto coma");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));

        scheduler.enviarCorreosPendientes();

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(msgCaptor.capture());

        Address[] recipients = msgCaptor.getValue().getRecipients(Message.RecipientType.TO);
        assertEquals(2, recipients.length);
        assertEquals("uno@rassini.com", recipients[0].toString());
        assertEquals("dos@rassini.com", recipients[1].toString());
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
    }

    @Test
    @DisplayName("4. Dos destinatarios separados por punto y coma: normaliza y envía a ambos")
    void testTwoRecipientsSeparatedBySemicolon() throws Exception {
        CorreoPendienteEntity correo = crearCorreo(104L, "uno@rassini.com; dos@rassini.com", "Asunto punto y coma");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));

        scheduler.enviarCorreosPendientes();

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(msgCaptor.capture());

        Address[] recipients = msgCaptor.getValue().getRecipients(Message.RecipientType.TO);
        assertEquals(2, recipients.length);
        assertEquals("uno@rassini.com", recipients[0].toString());
        assertEquals("dos@rassini.com", recipients[1].toString());
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
    }

    @Test
    @DisplayName("5. Elementos vacíos entre separadores (ej. ',developerrss@rassini.com' de MX185296 id=195): elimina vacíos y envía")
    void testEmptyElementsBetweenSeparators_CaseMx185296Id195() throws Exception {
        CorreoPendienteEntity correo = crearCorreo(
                195L,
                ",developerrss@rassini.com",
                "[INCIDENCIAS GRAPHITE] Datos faltantes detectados | Amb: local | Prov: 1 | No Gen: 1 | Manual: 0"
        );
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));

        scheduler.enviarCorreosPendientes();

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(msgCaptor.capture());

        Address[] recipients = msgCaptor.getValue().getRecipients(Message.RecipientType.TO);
        assertEquals(1, recipients.length);
        assertEquals("developerrss@rassini.com", recipients[0].toString());
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
        assertNotNull(correo.getFechaEnvio());
    }

    @Test
    @DisplayName("6. Destinatario vacío o solo separadores: no llama mailSender.send() y mantiene enviado=false")
    void testEmptyRecipientDoesNotSendAndRemainsPending() {
        CorreoPendienteEntity correoVacio = crearCorreo(106L, "   ", "Asunto vacío");
        CorreoPendienteEntity correoComas = crearCorreo(107L, " , ; , ", "Asunto solo separadores");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correoVacio, correoComas));

        scheduler.enviarCorreosPendientes();

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(correoPendienteRepository, never()).saveAll(any());
        assertFalse(Boolean.TRUE.equals(correoVacio.getEnviado()));
        assertNull(correoVacio.getFechaEnvio());
        assertFalse(Boolean.TRUE.equals(correoComas.getEnviado()));
        assertNull(correoComas.getFechaEnvio());
    }

    @Test
    @DisplayName("7. Destinatario inválido: rechaza con InternetAddress, no llama mailSender.send() y conserva enviado=false")
    void testInvalidRecipientDoesNotSendAndRemainsPending() {
        CorreoPendienteEntity correoInvalido = crearCorreo(108L, "correo_sin_arroba_invalido", "Asunto inválido");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correoInvalido));

        scheduler.enviarCorreosPendientes();

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(correoPendienteRepository, never()).saveAll(any());
        assertFalse(Boolean.TRUE.equals(correoInvalido.getEnviado()));
        assertNull(correoInvalido.getFechaEnvio());
    }

    @Test
    @DisplayName("8. Mezcla de destinatario válido e inválido: no oculta el inválido, no envía y conserva enviado=false")
    void testMixOfValidAndInvalidRecipientRejectsSend() {
        CorreoPendienteEntity correoMix = crearCorreo(109L, "developerrss@rassini.com, direccion_invalida@", "Asunto mixto");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correoMix));

        scheduler.enviarCorreosPendientes();

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(correoPendienteRepository, never()).saveAll(any());
        assertFalse(Boolean.TRUE.equals(correoMix.getEnviado()));
        assertNull(correoMix.getFechaEnvio());
    }

    @Test
    @DisplayName("9. Falla en mailSender.send(): no marca como enviado ni guarda fechaEnvio")
    void testMailSenderFailureDoesNotMarkAsSent() {
        CorreoPendienteEntity correo = crearCorreo(110L, "developerrss@rassini.com", "Asunto falla SMTP");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(List.of(correo));
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(MimeMessage.class));

        scheduler.enviarCorreosPendientes();

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(correoPendienteRepository, never()).saveAll(any());
        assertFalse(Boolean.TRUE.equals(correo.getEnviado()));
        assertNull(correo.getFechaEnvio());
    }

    @Test
    @DisplayName("10. Contador de enviados solo cuenta correos realmente enviados y aísla errores por registro")
    void testCounterOnlyCountsSuccessfullySentEmailsInBatch() {
        CorreoPendienteEntity c1Invalido = crearCorreo(111L, "invalido@@rassini", "Correo 1 inválido");
        CorreoPendienteEntity c2Valido = crearCorreo(112L, ",developerrss@rassini.com", "Correo 2 válido normalizado");
        CorreoPendienteEntity c3Valido = crearCorreo(113L, "otro@rassini.com", "Correo 3 válido");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(Arrays.asList(c1Invalido, c2Valido, c3Valido));

        scheduler.enviarCorreosPendientes();

        verify(mailSender, times(2)).send(any(MimeMessage.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CorreoPendienteEntity>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(correoPendienteRepository, times(1)).saveAll(savedCaptor.capture());

        List<CorreoPendienteEntity> guardados = savedCaptor.getValue();
        assertEquals(2, guardados.size(), "Solo deben guardarse como enviados los 2 correos que salieron exitosamente");
        assertFalse(Boolean.TRUE.equals(c1Invalido.getEnviado()));
        assertTrue(Boolean.TRUE.equals(c2Valido.getEnviado()));
        assertTrue(Boolean.TRUE.equals(c3Valido.getEnviado()));
    }

    @Test
    @DisplayName("11. Estado del registro actualizado solo cuando el envío fue exitoso y evita reintentos infinitos si sigue inválido")
    void testRecordStateUpdatedOnlyOnSuccessAndSuppressesInfiniteRetryUntilFixed() {
        CorreoPendienteEntity correo = crearCorreo(114L, "invalido_sin_dominio@", "Asunto reintento controlado");
        when(correoPendienteRepository.findByEnviadoFalse()).thenReturn(Collections.singletonList(correo));

        // Primer ciclo: detecta dirección inválida, no envía y registra en memoria
        scheduler.enviarCorreosPendientes();
        assertFalse(Boolean.TRUE.equals(correo.getEnviado()));
        assertNull(correo.getFechaEnvio());
        verify(mailSender, never()).send(any(MimeMessage.class));

        // Segundo ciclo con el mismo valor inválido: omite reintento de validación/envío sin bloquear el lote
        scheduler.enviarCorreosPendientes();
        assertFalse(Boolean.TRUE.equals(correo.getEnviado()));
        verify(mailSender, never()).send(any(MimeMessage.class));

        // Tercer ciclo tras corregirse el destinatario en BD: detecta el cambio, envía y actualiza enviado=true
        correo.setTo("developerrss@rassini.com");
        scheduler.enviarCorreosPendientes();
        assertTrue(Boolean.TRUE.equals(correo.getEnviado()));
        assertNotNull(correo.getFechaEnvio());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
