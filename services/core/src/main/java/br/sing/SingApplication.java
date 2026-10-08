package br.sing;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class SingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SingApplication.class, args);
    }
    @Autowired
    private Environment environment;


    //mostra o perfil ativo no console
    @PostConstruct
    public void logProfile() {
        System.out.println("Perfil ativo: " +
                Arrays.toString(environment.getActiveProfiles()));
    }
}
