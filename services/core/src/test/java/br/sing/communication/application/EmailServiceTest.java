package br.sing.communication.application;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link EmailService}.
 *
 * <p>Substitui {@link JavaMailSender} por mock para evitar tentar conectar
 * em um servidor SMTP real.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
    }

    private MimeMessage newMimeMessage() {
        Session session = Session.getInstance(new Properties());
        return new MimeMessage(session);
    }

    @Test
    void sendHtmlEmailDeveDelegarAoJavaMailSenderQuandoHtml() throws Exception {
        MimeMessage mime = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mime);

        emailService.sendHtmlEmail("dest@sing.local", "Assunto", "<p>Corpo</p>", true);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Assunto");
        assertThat(sent.getAllRecipients()).extracting(Object::toString)
                .contains("dest@sing.local");
    }

    @Test
    void sendHtmlEmailDeveAceitarTextoPlanoQuandoHtmlFalse() throws Exception {
        MimeMessage mime = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mime);

        emailService.sendHtmlEmail("dest@sing.local", "Assunto", "Corpo simples", false);

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(mime.getSubject()).isEqualTo("Assunto");
    }

    @Test
    void sendHtmlEmailDeveLancarRuntimeExceptionQuandoMailSenderFalha() {
        MimeMessage mime = newMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        // MailSendException é RuntimeException — propagada como está pelo service
        assertThatThrownBy(() ->
                emailService.sendHtmlEmail("dest@sing.local", "Assunto", "Corpo", true))
                .isInstanceOf(RuntimeException.class);
    }
}
