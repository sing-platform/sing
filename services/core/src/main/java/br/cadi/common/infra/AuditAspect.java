package br.cadi.common.infra;

import br.cadi.common.application.AuditLogService;
import br.cadi.common.domain.Trackable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    // Intercepta saves nos repositórios do seu projeto
    @Around("execution(* br.cadi..*.repository.*Repository.save(..))")
    public Object auditSave(ProceedingJoinPoint pjp) throws Throwable {
        Object entity = pjp.getArgs()[0];

        // ignora quem não marcou com Trackable
        if (!(entity instanceof Trackable trackable)) {
            return pjp.proceed();
        }

        boolean isInsert = trackable.getId() == null;
        String oldData  = isInsert ? null : buscarEstadoAtual(trackable);
        Long   changedBy = resolverUsuarioAtual();

        Object resultado = pjp.proceed();

        String action = isInsert ? "INSERT" : "UPDATE";
        auditLogService.logAudit(
                action,
                trackable.auditTableName(),
                trackable.getId(),
                oldData != null ? Map.of("before", oldData) : null,
                entity,
                changedBy
        );

        return resultado;
    }

    // Para DELETEs (soft delete pelo seu AuditableSoftDeleteEntity
    // já cobre, mas caso use delete físico):
    @Before("execution(* br.cadi..*.repository.*Repository.delete(..))")
    public void auditDelete(JoinPoint jp) {
        Object entity = jp.getArgs()[0];
        if (!(entity instanceof Trackable trackable)) return;

        auditLogService.logAudit(
                "DELETE",
                trackable.auditTableName(),
                trackable.getId(),
                entity,
                null,
                resolverUsuarioAtual()
        );
    }

    // Busca o estado atual no banco ANTES do save processar
    private String buscarEstadoAtual(Trackable trackable) {
        try {
            Object atual = entityManager.find(trackable.getClass(), trackable.getId());
            return atual != null ? objectMapper.writeValueAsString(atual) : null;
        } catch (Exception e) {
            log.warn("Não foi possível buscar estado anterior: {}", e.getMessage());
            return null;
        }
    }

    // Pega o usuário logado via Spring Security
    private Long resolverUsuarioAtual() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
                return ((br.cadi.iam.domain.User) ud).getId();
            }
        } catch (Exception ignored) {}
        return null;
    }
}