package br.sing.communication.api.controller;

import br.sing.communication.api.dto.response.NotificationResponse;
import br.sing.communication.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notificações in-app")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Listar notificações do usuário autenticado")
    public ResponseEntity<List<NotificationResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(notificationService.listForUser(authentication.getName()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('SECRETARY', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Marcar notificação como lida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marcada como lida"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada")
    })
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('SECRETARY', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Marcar todas as notificações como lidas")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
