package br.sing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Coloca o nome da instância na resposta. No cluster cada réplica do core
 * tem um nome diferente, então dá para ver o balanceamento no header X-Pod.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PodHeaderFilter extends OncePerRequestFilter {

    /** POD_NAME vem do Downward API. Fora do cluster vale o hostname do container. */
    public static String nomeInstancia() {
        String pod = System.getenv("POD_NAME");
        if (pod == null || pod.isBlank()) {
            pod = System.getenv("HOSTNAME");
        }
        if (pod == null || pod.isBlank()) {
            return "local";
        }
        return pod;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Pod", nomeInstancia());
        filterChain.doFilter(request, response);
    }
}
