package br.sing.audience.application;

import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.audience.api.AudienceRequest;
import br.sing.audience.api.AudienceResponse;
import br.sing.exception.BusinessException;
import br.sing.exception.EntityNotFoundException;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MuralAudienceService {

    private static final String ROLE_PROFESSOR = "ROLE_PROFESSOR";

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRoomRepository schoolRoomRepository;

    @Transactional(readOnly = true)
    public AudienceResponse resolve(String authorEmail, AudienceRequest request) {
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        String role = author.getRole().getName();
        Set<String> audienceRoles = request.audienceRoles();
        Set<Long> audienceSchoolroomIds = request.audienceSchoolroomIds();

        validateAudienceRequest(role, audienceRoles, audienceSchoolroomIds);

        List<User> recipients = resolveRecipients(role, audienceRoles, audienceSchoolroomIds, author);

        return new AudienceResponse(
                new AudienceResponse.Author(author.getId(), resolveAuthorName(author), author.getEmail(), role),
                recipients.stream()
                        .map(u -> new AudienceResponse.Recipient(u.getId(), u.getEmail()))
                        .toList());
    }

    private void validateAudienceRequest(String roleName,
                                         Set<String> audienceRoles,
                                         Set<Long> audienceSchoolroomIds) {
        boolean hasRoles = audienceRoles != null && !audienceRoles.isEmpty();
        boolean hasRooms = audienceSchoolroomIds != null && !audienceSchoolroomIds.isEmpty();

        if (ROLE_PROFESSOR.equals(roleName)) {
            if (hasRoles) {
                throw new BusinessException(
                        "Professor não pode usar público-alvo por role. Use audienceSchoolroomIds.",
                        HttpStatus.FORBIDDEN);
            }
            if (!hasRooms) {
                throw new BusinessException(
                        "Professor deve informar ao menos uma turma em audienceSchoolroomIds.",
                        HttpStatus.BAD_REQUEST);
            }
        } else if (!hasRoles && !hasRooms) {
            throw new BusinessException(
                    "Informe ao menos um campo de público-alvo: audienceRoles ou audienceSchoolroomIds.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private List<User> resolveRecipients(String authorRole,
                                         Set<String> audienceRoles,
                                         Set<Long> audienceSchoolroomIds,
                                         User author) {
        Map<Long, User> recipients = new LinkedHashMap<>();

        if (audienceRoles != null) {
            for (String role : audienceRoles) {
                userRepository.findByRole_Name(role).forEach(u -> recipients.put(u.getId(), u));
            }
        }

        if (audienceSchoolroomIds != null) {
            for (Long roomId : audienceSchoolroomIds) {
                if (ROLE_PROFESSOR.equals(authorRole)) {
                    Long teacherPersonId = personRepository.findByUserId(author.getId())
                            .map(p -> p.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Perfil do professor não encontrado"));
                    schoolRoomRepository.findById(roomId).ifPresent(room -> {
                        if (room.getTeacher() == null || !room.getTeacher().getId().equals(teacherPersonId)) {
                            throw new BusinessException(
                                    "Turma " + roomId + " não pertence ao professor autenticado.",
                                    HttpStatus.FORBIDDEN);
                        }
                    });
                }
                enrollmentRepository.findActiveBySchoolroomIdWithStudent(roomId)
                        .forEach(e -> {
                            User studentUser = e.getStudent().getUser();
                            if (studentUser != null && studentUser.isEnabled()) {
                                recipients.put(studentUser.getId(), studentUser);
                            }
                        });
            }
        }

        recipients.remove(author.getId());

        return recipients.values().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private String resolveAuthorName(User author) {
        return personRepository.findByUserId(author.getId())
                .map(p -> p.getName())
                .orElse(author.getName());
    }
}
