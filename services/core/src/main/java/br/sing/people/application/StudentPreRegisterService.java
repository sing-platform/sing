package br.sing.people.application;

import br.sing.academic.infra.repositories.WorkshopRepository;
import br.sing.communication.application.NotificationService;
import br.sing.communication.application.ResilientMessagingService;
import br.sing.communication.domain.EmailMessageEvent;
import br.sing.config.RabbitMQConfig;
import br.sing.exception.BusinessException;
import br.sing.exception.EntityNotFoundException;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.academic.domain.entities.Workshop;
import br.sing.people.api.dto.request.StudentPreRegisterCreateRequest;
import br.sing.people.api.dto.request.StudentPreRegisterUpdateRequest;
import br.sing.people.api.dto.response.StudentPreRegisterResponse;
import br.sing.people.api.dto.response.WaitingListVacancyAlertResponse;
import br.sing.people.domain.entities.StudentPreRegister;
import br.sing.people.domain.enums.Shift;
import br.sing.people.infra.repositories.StudentPreRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPreRegisterService {

    private final StudentPreRegisterRepository studentPreRegisterRepository;
    private final WorkshopRepository workshopRepository;
    private final WaitingListVacancyQueryService vacancyQueryService;
    private final ResilientMessagingService messagingService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public StudentPreRegisterResponse create(StudentPreRegisterCreateRequest request) {
        Workshop workshop = getWorkshopOrThrow(request.workshopId());

        StudentPreRegister entity = StudentPreRegister.builder()
                .name(request.name().trim())
                .email(request.email().trim())
                .phone(request.phone().trim())
                .workshop(workshop)
                .shift(request.shift())
                .build();

        StudentPreRegister saved = studentPreRegisterRepository.save(entity);
        notifyIfVacancyAvailable(workshop, request.shift());
        return toResponse(saved);
    }

    private void notifyIfVacancyAvailable(Workshop workshop, Shift shift) {
        long available = vacancyQueryService.countAvailableVacanciesForWorkshopShift(workshop.getId(), shift);
        if (available <= 0) {
            return;
        }
        long waiting = studentPreRegisterRepository.countByWorkshop_IdAndShift(workshop.getId(), shift);
        notificationService.createWaitingListVacancyIfAbsent(
                workshop.getId(),
                workshop.getName(),
                shift,
                available,
                waiting
        );
    }

    @Transactional(readOnly = true)
    public List<StudentPreRegisterResponse> findAll() {
        return studentPreRegisterRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentPreRegisterResponse findById(Long id) {
        return toResponse(getStudentPreRegisterOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<WaitingListVacancyAlertResponse> listVacancyAlerts() {
        return vacancyQueryService.listActiveAlerts();
    }

    @Transactional
    public StudentPreRegisterResponse update(Long id, StudentPreRegisterUpdateRequest request) {
        StudentPreRegister entity = getStudentPreRegisterOrThrow(id);
        Workshop workshop = getWorkshopOrThrow(request.workshopId());

        entity.setName(request.name().trim());
        entity.setEmail(request.email().trim());
        entity.setPhone(request.phone().trim());
        entity.setWorkshop(workshop);
        entity.setShift(request.shift());

        return toResponse(studentPreRegisterRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        StudentPreRegister entity = getStudentPreRegisterOrThrow(id);
        studentPreRegisterRepository.delete(entity);
    }

    @Transactional
    public void sendSummons(Long id, String userEmail) {
        StudentPreRegister entity = getStudentPreRegisterOrThrow(id);
        Long workshopId = entity.getWorkshop().getId();
        Shift shift = entity.getShift();

        if (!vacancyQueryService.hasVacancyForWorkshopShift(workshopId, shift)) {
            throw new BusinessException(
                    "Não há vagas disponíveis para a oficina e turno selecionados.",
                    HttpStatus.BAD_REQUEST
            );
        }

        User sender = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String workshopName = entity.getWorkshop().getName();
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to(entity.getEmail())
                .subject("Convocação – vaga disponível na escola")
                .body(buildSummonsEmailBody(entity.getName(), workshopName, shiftLabelPt(shift)))
                .html(true)
                .build();
        messagingService.send(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_RK, event);

        entity.setSummonedAt(LocalDateTime.now());
        entity.setSummonedBy(sender);
        studentPreRegisterRepository.save(entity);
    }

    private StudentPreRegister getStudentPreRegisterOrThrow(Long id) {
        return studentPreRegisterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student pre-register not found"));
    }

    private Workshop getWorkshopOrThrow(Long workshopId) {
        return workshopRepository.findById(workshopId)
                .orElseThrow(() -> new EntityNotFoundException("Workshop not found"));
    }

    private StudentPreRegisterResponse toResponse(StudentPreRegister entity) {
        boolean hasVacancy = vacancyQueryService.hasVacancyForWorkshopShift(
                entity.getWorkshop().getId(),
                entity.getShift()
        );
        return new StudentPreRegisterResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getWorkshop().getId(),
                entity.getWorkshop().getName(),
                entity.getShift(),
                entity.getCreatedAt(),
                entity.getSummonedAt(),
                hasVacancy
        );
    }

    private static String buildSummonsEmailBody(String candidateName, String workshopName, String shiftLabel) {
        return """
                <p>Prezado(a) responsável,</p>
                <p>Informamos que surgiu uma vaga disponível na oficina <strong>%s</strong> (turno da %s) \
                para o candidato <strong>%s</strong>.</p>
                <p>Para efetivar a matrícula, compareça à escola no horário de atendimento da secretaria \
                com a documentação necessária.</p>
                <p>Esta convocação não garante a vaga após o preenchimento por outro candidato. \
                Recomendamos comparecer o quanto antes.</p>
                <p>Atenciosamente,<br/>Secretaria Acadêmica – Escola Modelo</p>
                """.formatted(workshopName, shiftLabel, candidateName);
    }

    private static String shiftLabelPt(Shift shift) {
        return switch (shift) {
            case MORNING -> "manhã";
            case AFTERNOON -> "tarde";
            case EVENING -> "noite";
        };
    }
}
