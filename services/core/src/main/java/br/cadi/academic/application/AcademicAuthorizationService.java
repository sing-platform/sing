package br.cadi.academic.application;

import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.exception.BusinessException;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Person;
import br.cadi.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AcademicAuthorizationService {

    private static final Set<String> MANAGEMENT_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_SECRETARY",
            "ROLE_MANAGER"
    );

    private static final String ROLE_PROFESSOR = "ROLE_PROFESSOR";

    private final PersonRepository personRepository;

    public boolean isManagement(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(MANAGEMENT_ROLES::contains);
    }

    public boolean isProfessor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_PROFESSOR::equals);
    }

    public void assertAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Gestão: liberado. Professor: apenas se for o titular da turma (e a turma tiver professor designado).
     */
    public void assertCanAccessSchoolRoomAsProfessor(Authentication authentication, SchoolRoom schoolRoom) {
        assertAuthenticated(authentication);
        if (isManagement(authentication)) {
            return;
        }
        if (!isProfessor(authentication)) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        Collaborator teacher = schoolRoom.getTeacher();
        if (teacher == null) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        Collaborator collaborator = getCollaboratorOrThrow(authentication);
        if (!teacher.getId().equals(collaborator.getId())) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Gestão: qualquer teacherId. Professor: apenas o próprio colaborador.
     */
    public void assertProfessorOwnsTeacherIdOrManagement(Authentication authentication, Long teacherId) {
        assertAuthenticated(authentication);
        if (isManagement(authentication)) {
            return;
        }
        if (!isProfessor(authentication)) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        Collaborator collaborator = getCollaboratorOrThrow(authentication);
        if (!collaborator.getId().equals(teacherId)) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Resolve o colaborador logado (por e-mail do token). Usado para filtrar matrículas no escopo do professor.
     */
    public Collaborator getCollaboratorOrThrow(Authentication authentication) {
        Person person = personRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));
        if (!(person instanceof Collaborator collaborator)) {
            throw new BusinessException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        return collaborator;
    }
}
