package br.sing.people.application;

import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.exception.EntityNotFoundException;
import br.sing.exception.UserCreationConflict;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import br.sing.people.api.dto.request.CollaboratorCreateRequest;
import br.sing.people.api.dto.request.CollaboratorUpdateRequest;
import br.sing.people.api.dto.response.CollaboratorDetailResponse;
import br.sing.people.api.dto.response.CollaboratorStatsResponse;
import br.sing.people.domain.entities.Address;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.enums.CollaboratorType;
import br.sing.people.infra.repositories.AddressRepository;
import br.sing.people.infra.repositories.CollaboratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRoomRepository schoolRoomRepository;

    @Transactional(readOnly = true)
    public List<CollaboratorDetailResponse> listAll(
            boolean activeOnly,
            CollaboratorType type,
            boolean eligibleForSchoolRoom
    ) {
        List<Collaborator> collaborators;
        if (eligibleForSchoolRoom && type == CollaboratorType.TEACHER) {
            collaborators = activeOnly
                    ? collaboratorRepository.findAllEligibleSchoolRoomTeachersActive(CollaboratorType.TEACHER)
                    : collaboratorRepository.findAllEligibleSchoolRoomTeachersIncludingInactiveUsers(
                            CollaboratorType.TEACHER);
        } else if (type != null) {
            collaborators = collaboratorRepository.findAllByTypeFiltered(type, activeOnly);
        } else if (activeOnly) {
            collaborators = collaboratorRepository.findAllActiveWithUserAndAddress();
        } else {
            collaborators = collaboratorRepository.findAllWithUserAndAddress();
        }

        return collaborators.stream()
                .map(CollaboratorDetailResponse::from)
                .sorted(Comparator.comparing(CollaboratorDetailResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public CollaboratorStatsResponse getStats() {
        Map<CollaboratorType, Long> byType = emptyTypeMap();
        for (Object[] row : collaboratorRepository.countGroupByType()) {
            byType.put((CollaboratorType) row[0], (Long) row[1]);
        }

        Map<CollaboratorType, Long> activeByType = emptyTypeMap();
        for (Object[] row : collaboratorRepository.countActiveGroupByType()) {
            activeByType.put((CollaboratorType) row[0], (Long) row[1]);
        }

        long total = byType.values().stream().mapToLong(Long::longValue).sum();
        long totalActive = activeByType.values().stream().mapToLong(Long::longValue).sum();

        return new CollaboratorStatsResponse(total, totalActive, byType, activeByType);
    }

    @Transactional(readOnly = true)
    public CollaboratorDetailResponse getById(Long id) {
        return CollaboratorDetailResponse.from(findCollaboratorOrThrow(id));
    }

    @Transactional
    public CollaboratorDetailResponse create(CollaboratorCreateRequest request) {
        if (collaboratorRepository.existsByCpf(request.cpf())) {
            throw new UserCreationConflict("CPF already registered");
        }
        if (userRepository.existsByEmail(request.email()) || collaboratorRepository.existsByEmail(request.email())) {
            throw new UserCreationConflict("E-mail already registered");
        }

        String roleName = roleNameForType(request.type());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role " + roleName + " not found"));

        String defaultPassword = "SING#" + request.cpf().replaceAll("\\D", "").substring(0, 3);

        User user = userRepository.save(User.builder()
                .username(request.email())
                .email(request.email())
                .password(passwordEncoder.encode(defaultPassword))
                .role(role)
                .isActive(true)
                .build());

        Address address = Address.builder()
                .zipCode(request.address().zipCode())
                .street(request.address().street())
                .number(request.address().number())
                .complement(request.address().complement())
                .neighborhood(request.address().neighborhood())
                .city(request.address().city())
                .state(request.address().state())
                .build();
        addressRepository.save(address);

        Collaborator collaborator = Collaborator.builder()
                .name(request.name())
                .cpf(request.cpf())
                .email(request.email())
                .phone(request.phone())
                .birthDate(request.birthDate())
                .address(address)
                .user(user)
                .educationLevel(request.educationLevel())
                .educationCertType(request.educationCertType())
                .institutionCert(request.institutionCert())
                .yearConclusion(request.yearConclusion())
                .department(request.department())
                .type(request.type())
                .build();

        return CollaboratorDetailResponse.from(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public CollaboratorDetailResponse update(Long id, CollaboratorUpdateRequest request) {
        Collaborator collaborator = findCollaboratorOrThrow(id);

        Address address = collaborator.getAddress();
        address.setZipCode(request.address().zipCode());
        address.setStreet(request.address().street());
        address.setNumber(request.address().number());
        address.setComplement(request.address().complement());
        address.setNeighborhood(request.address().neighborhood());
        address.setCity(request.address().city());
        address.setState(request.address().state());
        addressRepository.save(address);

        collaborator.setName(request.name());
        collaborator.setPhone(request.phone());
        collaborator.setBirthDate(request.birthDate());
        collaborator.setEducationLevel(request.educationLevel());
        collaborator.setEducationCertType(request.educationCertType());
        collaborator.setInstitutionCert(request.institutionCert());
        collaborator.setYearConclusion(request.yearConclusion());
        collaborator.setDepartment(request.department());

        return CollaboratorDetailResponse.from(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public void deactivate(Long id) {
        Collaborator collaborator = findCollaboratorOrThrow(id);

        if (collaborator.getType() == CollaboratorType.TEACHER) {
            List<SchoolRoom> turmas = schoolRoomRepository.findByTeacherId(collaborator.getId());
            for (SchoolRoom turma : turmas) {
                turma.setTeacher(null);
            }
            schoolRoomRepository.saveAll(turmas);
        }

        User user = collaborator.getUser();
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void reactivate(Long id) {
        Collaborator collaborator = findCollaboratorOrThrow(id);
        User user = collaborator.getUser();
        user.setActive(true);
        userRepository.save(user);
    }

    private Collaborator findCollaboratorOrThrow(Long id) {
        return collaboratorRepository.findByIdWithUserAndAddress(id)
                .orElseThrow(() -> new EntityNotFoundException("Collaborator not found"));
    }

    private static String roleNameForType(CollaboratorType type) {
        return switch (type) {
            case TEACHER -> "ROLE_PROFESSOR";
            case SECRETARY -> "ROLE_SECRETARY";
            case STAFF -> "ROLE_STAFF";
            case VOLUNTEER -> "ROLE_VOLUNTEER";
            case DIRECTOR -> "ROLE_ADMIN";
            case PSYCHOLOGIST -> "ROLE_PSYCHOLOGIST";
        };
    }

    private static Map<CollaboratorType, Long> emptyTypeMap() {
        Map<CollaboratorType, Long> map = new EnumMap<>(CollaboratorType.class);
        Arrays.stream(CollaboratorType.values()).forEach(t -> map.put(t, 0L));
        return map;
    }
}
