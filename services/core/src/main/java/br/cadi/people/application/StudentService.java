package br.cadi.people.application;

import br.cadi.exception.UserCreationConflict;
import br.cadi.iam.domain.Role;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.iam.infra.UserRepository;
import br.cadi.academic.infra.repositories.AttendanceRepository;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.people.api.dto.request.GuardianRequest;
import br.cadi.people.api.dto.request.StudentRequest;
import br.cadi.people.api.dto.request.StudentUpdateRequest;
import br.cadi.people.api.dto.response.addresses.AddressResponseDTO;
import br.cadi.people.api.dto.response.GuardianResponse;
import br.cadi.people.api.dto.response.StudentResponse;
import br.cadi.people.domain.entities.*;
import br.cadi.people.domain.enums.FamilyTie;
import br.cadi.people.infra.repositories.AddressRepository;
import br.cadi.people.infra.repositories.GuardianRepository;
import br.cadi.people.infra.repositories.StudentGuardianRepository;
import br.cadi.people.api.dto.StudentEnrollmentFilter;
import br.cadi.people.infra.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public StudentResponse create(StudentRequest dto) {
        validateEmailsAvailable(dto);

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new RuntimeException("Role ROLE_STUDENT not found."));
        Role guardianRole = roleRepository.findByName("ROLE_PARENT")
                .orElseThrow(() -> new RuntimeException("Role ROLE_PARENT not found."));

        // 1. Cria Endereço do Aluno
        Address studentAddress = addressRepository.save(Address.builder()
                .zipCode(dto.address().zipCode()).street(dto.address().street())
                .number(dto.address().number()).complement(dto.address().complement())
                .neighborhood(dto.address().neighborhood()).city(dto.address().city())
                .state(dto.address().state()).build());

        // 2. Cria Usuário do Aluno
        User studentUser = userRepository.save(User.builder()
                .username(dto.email()).email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(studentRole).build());

        // 3. Cria Aluno
        Student student = studentRepository.save(Student.builder()
                .name(dto.name()).email(dto.email()).cpf(dto.cpf()).phone(dto.phone())
                .birthDate(dto.birthDate()).address(studentAddress).user(studentUser)
                .grade(dto.grade()).currentSchool(dto.currentSchool()).periodStudies(dto.periodStudies())
                .chronicIllness(blankToNull(dto.chronicIllness()))
                .allergy(blankToNull(dto.allergy()))
                .medicine(blankToNull(dto.medicine()))
                .deficiency(blankToNull(dto.deficiency()))
                .followUp(blankToNull(dto.followUp()))
                .surgery(blankToNull(dto.surgery()))
                .build());

        // 4. Cria Responsáveis (com endereços próprios)
        for (GuardianRequest gDto : dto.guardians()) {
            Address gAddress = addressRepository.save(Address.builder()
                    .zipCode(gDto.address().zipCode()).street(gDto.address().street())
                    .number(gDto.address().number()).neighborhood(gDto.address().neighborhood())
                    .city(gDto.address().city()).state(gDto.address().state()).build());

            User gUser = userRepository.save(User.builder()
                    .username(gDto.email()).email(gDto.email())
                    .password(passwordEncoder.encode(gDto.password()))
                    .role(guardianRole).build());

            Guardian guardian = guardianRepository.save(Guardian.builder()
                    .name(gDto.name()).email(gDto.email()).cpf(gDto.cpf()).phone(gDto.phone())
                    .birthDate(gDto.birthDate()).address(gAddress).user(gUser)
                    .isPrimary(gDto.isPrimary()).build());

            FamilyTie tie = FamilyTie.valueOf(gDto.familyTie().toUpperCase());
            studentGuardianRepository.save(StudentGuardian.builder()
                    .student(student).guardian(guardian).familyTie(tie).build());
        }

        return mapToResponse(student);
    }


    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + id));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> pageForManagement(
            String search,
            String periodStudies,
            StudentEnrollmentFilter enrollmentFilter,
            Pageable pageable
    ) {
        String s = blankToNull(search);
        String p = blankToNull(periodStudies);
        String ef = (enrollmentFilter == null || enrollmentFilter == StudentEnrollmentFilter.ANY)
                ? "ANY"
                : enrollmentFilter.name();
        Pageable normalized = PeoplePageables.normalizeStudentPageable(pageable);
        Page<Long> idPage = studentRepository.pageIdsForEnrollmentList(s, p, ef, normalized);
        if (idPage.isEmpty()) {
            return Page.empty(normalized);
        }
        List<Long> ids = idPage.getContent();
        Map<Long, Student> byId = new LinkedHashMap<>();
        for (Student st : studentRepository.findAllWithDetailsByIds(ids)) {
            byId.put(st.getId(), st);
        }
        List<StudentResponse> content = ids.stream()
                .map(byId::get)
                .map(this::mapToResponse)
                .toList();
        return new PageImpl<>(content, normalized, idPage.getTotalElements());
    }

    /**
     * Garante que nenhum e-mail (do aluno ou dos responsáveis) já exista em {@code users}
     * e que não haja e-mails repetidos dentro da própria requisição. Sem isso, o conflito
     * só estouraria no INSERT como violação de {@code uq_user_email} (HTTP 500). Aqui ele
     * vira um {@link UserCreationConflict} (HTTP 409), conforme o contrato do endpoint.
     */
    private void validateEmailsAvailable(StudentRequest dto) {
        Set<String> emailsInRequest = new HashSet<>();
        ensureEmailAvailable(dto.email(), emailsInRequest);
        for (GuardianRequest guardian : dto.guardians()) {
            ensureEmailAvailable(guardian.email(), emailsInRequest);
        }
    }

    private void ensureEmailAvailable(String email, Set<String> emailsInRequest) {
        if (!emailsInRequest.add(email.trim().toLowerCase(Locale.ROOT))) {
            throw new UserCreationConflict("E-mail duplicado na requisição: " + email);
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserCreationConflict("E-mail já cadastrado: " + email);
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found."));

        // 1. Remove dependências acadêmicas
        attendanceRepository.findByStudentId(id).forEach(attendanceRepository::delete);
        enrollmentRepository.findByStudentId(id).forEach(enrollmentRepository::delete);

        // 2. Remove os vínculos (StudentGuardian)
        // Isso é importante para não dar erro de chave estrangeira
        studentGuardianRepository.deleteAll(student.getGuardians());

        // 3. Deleta o Aluno
        studentRepository.delete(student);

        // 4. Deleta o Usuário de login do Aluno (Identity)
        if (student.getUser() != null) {
            userRepository.deleteById(student.getUser().getId());
        }

    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found."));

        // Atualiza dados do aluno
        student.setName(dto.name());
        student.setEmail(dto.email());
        student.setCpf(dto.cpf());
        student.setPhone(dto.phone());
        student.setBirthDate(dto.birthDate());
        student.setGrade(dto.grade());
        student.setCurrentSchool(dto.currentSchool());
        student.setPeriodStudies(dto.periodStudies());
        student.setChronicIllness(blankToNull(dto.chronicIllness()));
        student.setAllergy(blankToNull(dto.allergy()));
        student.setMedicine(blankToNull(dto.medicine()));
        student.setDeficiency(blankToNull(dto.deficiency()));
        student.setFollowUp(blankToNull(dto.followUp()));
        student.setSurgery(blankToNull(dto.surgery()));

        // Atualiza endereço
        Address address = student.getAddress();
        if (address != null) {
            address.setZipCode(dto.address().zipCode());
            address.setStreet(dto.address().street());
            address.setNumber(dto.address().number());
            address.setNeighborhood(dto.address().neighborhood());
            address.setCity(dto.address().city());
            address.setState(dto.address().state());
            address.setComplement(dto.address().complement());
            addressRepository.save(address);
        }

        return mapToResponse(studentRepository.save(student));
    }

    private StudentResponse mapToResponse(Student s) {
        List<GuardianResponse> guardianDTOs = s.getGuardians().stream()
                .map(sg -> new GuardianResponse(
                        sg.getGuardian().getId(),
                        sg.getGuardian().getName(),
                        sg.getGuardian().getEmail(),
                        sg.getGuardian().getPhone(),
                        sg.getFamilyTie().name(),
                        sg.getGuardian().getIsPrimary()
                )).toList();

        AddressResponseDTO addressDTO = new AddressResponseDTO(
                s.getAddress().getZipCode(),
                s.getAddress().getStreet(),
                s.getAddress().getNumber(),
                s.getAddress().getNeighborhood(),
                s.getAddress().getCity(),
                s.getAddress().getState(),
                s.getAddress().getComplement()
        );

        return new StudentResponse(
                s.getId(), s.getName(), s.getEmail(), s.getCpf(), s.getPhone(),
                s.getGrade(), s.getCurrentSchool(), s.getPeriodStudies(),
                s.getBirthDate(), addressDTO, guardianDTOs,
                s.getChronicIllness(), s.getAllergy(), s.getMedicine(),
                s.getDeficiency(), s.getFollowUp(), s.getSurgery()
        );
    }
}