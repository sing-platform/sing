package br.sing.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/workshops").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/schoolrooms").hasAnyRole("ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.GET, "/api/v1/schoolrooms/*/students")
                        .hasAnyRole("ADMIN", "SECRETARY", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/teachers/me/schoolrooms").hasRole("PROFESSOR")
                        .requestMatchers("/api/v1/collaborators/**").hasAnyRole("ADMIN", "SECRETARY")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
