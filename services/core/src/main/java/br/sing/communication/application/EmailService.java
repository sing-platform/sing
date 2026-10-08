package br.sing.communication.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pelo envio de e-mails via JavaMail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envia um e-mail com suporte a HTML.
     *
     * @param to      destinatário
     * @param subject assunto
     * @param body    corpo do e-mail
     * @param html    {@code true} para HTML, {@code false} para texto puro
     */
    public void sendHtmlEmail(String to, String subject, String body, boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);
            mailSender.send(message);
            log.info("E-mail enviado para {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage());
            throw new RuntimeException("Erro ao enviar e-mail para " + to, e);
        }
    }
}
