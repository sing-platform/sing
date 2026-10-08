package br.sing.config;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.lang.NonNull;

/**
 * Permite subir a aplicação sem {@code spring.mail.host}. Envios são ignorados e logados.
 */
@Slf4j
public class NoOpJavaMailSender extends JavaMailSenderImpl {

    public NoOpJavaMailSender() {
        setHost("127.0.0.1");
        setPort(1025);
    }

    @Override
    public void send(@NonNull MimeMessage mimeMessage) {
        log.warn("SMTP não configurado — e-mail não enviado (defina spring.mail.host para produção).");
    }

    @Override
    public void send(@NonNull MimeMessage... mimeMessages) {
        for (MimeMessage m : mimeMessages) {
            send(m);
        }
    }

    @Override
    public void send(@NonNull SimpleMailMessage simpleMessage) {
        log.warn("SMTP não configurado — e-mail não enviado.");
    }

    @Override
    public void send(@NonNull SimpleMailMessage... simpleMessages) {
        for (SimpleMailMessage m : simpleMessages) {
            send(m);
        }
    }
}
