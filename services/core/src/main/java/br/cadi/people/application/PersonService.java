package br.cadi.people.application;

import br.cadi.academic.api.dto.response.LectureStudentResponse;
import br.cadi.academic.domain.entities.*;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.iam.application.AuthService;
import br.cadi.iam.domain.Role;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.people.api.dto.response.addresses.AddressDTO;
import br.cadi.people.api.dto.ParentDTO;
import br.cadi.people.api.dto.StudentGuardianDTO;
import br.cadi.people.api.dto.response.CollaboratorResponse;
import br.cadi.people.api.dto.response.ProfessorScheduleResponse;
import br.cadi.people.api.dto.response.StudentDataResponse;
import br.cadi.people.domain.entities.*;
import br.cadi.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final AuthService authService;
    private final EnrollmentRepository enrollmentRepository;
    private final RoleRepository roleRepository;
    private final StudentGuardianService studentGuardianService;

    @Transactional(readOnly = true)
    public Object getMe() {
        User user = authService.getPrincipal();
        Person p = personRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Dados pessoais não encontrados."));

        Role role = roleRepository.findByName(user.getRole().getName())
                .orElseThrow(() -> new EntityNotFoundException("Role não encontrada."));

        return switch (p) {
            case Collaborator c -> handleCollaborator(c, role, user);
            case Student s -> handleStudent(s);
            case Guardian g -> handleGuardian(g, role);
            default -> throw new IllegalStateException("Tipo de Person desconhecido: " + p.getClass().getSimpleName());
        };
    }

    private StudentDataResponse handleStudent(Student s) {
        List<Enrollment> enrollments = enrollmentRepository.findActiveEnrollmentsWithDetails(s.getId());

        List<LectureStudentResponse> lectures = enrollments.stream()
                .map(enr -> LectureStudentResponse.builder()
                        .schoolRoomId(enr.getSchoolroom().getId())
                        .lectureCurrentProfessorName(enr.getSchoolroom().getTeacher() != null ?
                                enr.getSchoolroom().getTeacher().getName() : "Sem professor")
                        .SchoolRoomName(enr.getSchoolroom().getName())
                        .build())
                .toList();

        return StudentDataResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .email(s.getEmail())
                .birthDate(s.getBirthDate())
                .phone(s.getPhone())
                .address(mapAddressToDTO(s.getAddress()))
                .guardiansNames(studentGuardianService.getGuardianNamesByStudent(s))
                .lectures(lectures)
                .build();
    }

    private ParentDTO handleGuardian(Guardian g, Role role) {
        List<StudentGuardian> familyTree = studentGuardianService.getFamilyTree(g.getId());

        List<StudentGuardianDTO> studentDtos = familyTree.stream()
                .map(sg -> {
                    Student student = sg.getStudent();
                    List<LectureStudentResponse> lectures = student.getEnrollments().stream()
                            .filter(Enrollment::getStatus)
                            .map(enr -> LectureStudentResponse.builder()
                                    .schoolRoomId(enr.getSchoolroom().getId())
                                    .lectureCurrentProfessorName(enr.getSchoolroom().getTeacher() != null ?
                                            enr.getSchoolroom().getTeacher().getName() : "Sem professor")
                                    .SchoolRoomName(enr.getSchoolroom().getName())
                                    .build())
                            .toList();

                    return StudentGuardianDTO.builder()
                            .id(student.getId())
                            .name(student.getName())
                            .email(student.getEmail())
                            .phone(student.getPhone())
                            .address(mapAddressToDTO(student.getAddress()))
                            .lectures(lectures)
                            .build();
                }).toList();

        return ParentDTO.builder()
                .cpf(g.getCpf())
                .email(g.getEmail())
                .name(g.getName())
                .birthDate(g.getBirthDate())
                .address(mapAddressToDTO(g.getAddress()))
                .phone(g.getPhone())
                .role(role.getName())
                .students(studentDtos)
                .build();
    }

    private Object handleCollaborator(Collaborator c, Role role, User user) {
        AddressDTO addressDTO = mapAddressToDTO(c.getAddress());

        if ("ROLE_PROFESSOR".equals(role.getName())) {
            return ProfessorScheduleResponse.builder()
                    .name(c.getName())
                    .email(c.getEmail())
                    .phone(c.getPhone())
                    .cpf(c.getCpf())
                    .birthDate(c.getBirthDate())
                    .address(addressDTO)
                    .role(role.getName())
                    .CollaboratorType(user.getRole().getName())
                    .collaboratorId(c.getId())
                    .infos(List.of())
                    .build();
        }

        return CollaboratorResponse.builder()
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .cpf(c.getCpf())
                .birthDate(c.getBirthDate())
                .address(addressDTO)
                .CollaboratorType(user.getRole().getName())
                .role(role.getName())
                .collaboratorId(c.getId())
                .build();
    }

    private AddressDTO mapAddressToDTO(Address addr) {
        if (addr == null) return null;
        return AddressDTO.builder()
                .street(addr.getStreet()).number(addr.getNumber().toString())
                .city(addr.getCity()).state(addr.getState()).zip(addr.getZipCode())
                .complement(addr.getComplement()).neighborhood(addr.getNeighborhood())
                .build();
    }
}