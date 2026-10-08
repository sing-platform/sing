package br.sing.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailFallbackConfiguration {

    @Bean
    // SÓ vai criar o e-mail de mentira se a propriedade 'spring.mail.host' NÃO existir no YAML
    @ConditionalOnProperty(name = "spring.mail.host", havingValue = "false", matchIfMissing = true)
    public JavaMailSender noopJavaMailSender() {
        return new NoOpJavaMailSender();
    }
}