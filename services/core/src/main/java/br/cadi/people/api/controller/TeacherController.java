package br.cadi.people.api.controller;

import br.cadi.academic.api.dto.response.EnrollmentLinkResponse;
import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.application.AcademicAuthorizationService;
import br.cadi.academic.application.EnrollmentQueryService;
import br.cadi.academic.application.SchoolRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teacher", description = "Turmas e matrículas do professor titular (contexto acadêmico).")
public class TeacherController {

    private final SchoolRoomService schoolRoomService;
    private final EnrollmentQueryService enrollmentQueryService;
    private final AcademicAuthorizationService academicAuthorizationService;

    @GetMapping("/me/schoolrooms")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Listar minhas turmas (titular), usando o colaborador do JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de turmas"),
            @ApiResponse(responseCode = "403", description = "Token sem colaborador professor")
    })
    public ResponseEntity<Page<SchoolRoomResponse>> pageMySchoolRooms(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication
    ) {
        Long myTeacherId = academicAuthorizationService.getCollaboratorOrThrow(authentication).getId();
        return ResponseEntity.ok(
                schoolRoomService.pageSchoolRoomsForTeacher(myTeacherId, activeOnly, pageable, authentication)
        );
    }

    @GetMapping("/me/enrollments")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Listar matrículas nas minhas turmas (titular), usando o colaborador do JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de matrículas"),
            @ApiResponse(responseCode = "403", description = "Token sem colaborador professor")
    })
    public ResponseEntity<Page<EnrollmentLinkResponse>> pageMyEnrollments(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "enrollmentDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long myTeacherId = academicAuthorizationService.getCollaboratorOrThrow(authentication).getId();
        return ResponseEntity.ok(
                enrollmentQueryService.pageByTeacher(myTeacherId, active, pageable, authentication)
        );
    }

    @GetMapping("/{teacherId}/schoolrooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'PROFESSOR')")
    @Operation(summary = "Listar turmas em que o professor é titular")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de turmas"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para este professor")
    })
    public ResponseEntity<Page<SchoolRoomResponse>> pageSchoolRoomsByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolRoomService.pageSchoolRoomsForTeacher(teacherId, activeOnly, pageable, authentication)
        );
    }

    @GetMapping("/{teacherId}/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'PROFESSOR')")
    @Operation(summary = "Listar matrículas nas turmas em que o professor é titular")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de matrículas"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para este professor")
    })
    public ResponseEntity<Page<EnrollmentLinkResponse>> pageEnrollmentsByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "enrollmentDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                enrollmentQueryService.pageByTeacher(teacherId, active, pageable, authentication)
        );
    }
}
