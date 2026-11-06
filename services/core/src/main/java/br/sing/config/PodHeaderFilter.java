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
 * Coloca o hostname na resposta. No cluster cada réplica do core tem um
 * hostname diferente, então dá para ver o balanceamento no header X-Pod.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PodHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String pod = System.getenv("HOSTNAME");
        if (pod == null || pod.isBlank()) {
            pod = "local";
        }
        response.setHeader("X-Pod", pod);
        filterChain.doFilter(request, response);
    }
}
