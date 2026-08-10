package br.cadi.people.application;

import br.cadi.academic.infra.repositories.AttendanceRepository;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.exception.UserCreationConflict;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.iam.infra.UserRepository;
import br.cadi.people.api.dto.request.AddressRequest;
import br.cadi.people.api.dto.request.GuardianRequest;
import br.cadi.people.api.dto.request.StudentRequest;
import br.cadi.people.infra.repositories.AddressRepository;
import br.cadi.people.infra.repositories.GuardianRepository;
import br.cadi.people.infra.repositories.StudentGuardianRepository;
import br.cadi.people.infra.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private AttendanceRepository attendanceRepository;

    private StudentService service;

    @BeforeEach
    void setUp() {
        service = new StudentService(
                studentRepository,
                guardianRepository,
                studentGuardianRepository,
                userRepository,
                roleRepository,
                addressRepository,
                passwordEncoder,
                enrollmentRepository,
                attendanceRepository
        );
    }

    private AddressRequest addressDto() {
        return new AddressRequest("12345-678", "Rua A", 100, "Centro", "São Paulo", "SP", "");
    }

    private GuardianRequest guardianDto(String email) {
        return new GuardianRequest(
                "Maria Responsável",
                email,
                "senha123",
                "55566677788",
                "11988887777",
                LocalDate.of(1985, 5, 5),
                "MAE",
                true,
                addressDto()
        );
    }

    private StudentRequest studentRequest(String studentEmail, String guardianEmail) {
        return new StudentRequest(
                "João Aluno",
                studentEmail,
                "senha123",
                "11122233344",
                "11999998888",
                LocalDate.of(2010, 1, 1),
                "1-ef",
                "EMEF Central",
                "MATUTINO",
                addressDto(),
                List.of(guardianDto(guardianEmail)),
                null, null, null, null, null, null
        );
    }

    @Test
    void createDeveLancar409QuandoEmailDoAlunoJaCadastrado() {
        when(userRepository.existsByEmail("joao@cadi.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(studentRequest("joao@cadi.com", "maria@cadi.com")))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("E-mail já cadastrado");

        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createDeveLancar409QuandoEmailDoResponsavelJaCadastrado() {
        when(userRepository.existsByEmail("joao@cadi.com")).thenReturn(false);
        when(userRepository.existsByEmail("maria@cadi.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(studentRequest("joao@cadi.com", "maria@cadi.com")))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("E-mail já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createDeveLancar409QuandoEmailRepetidoNaPropriaRequisicao() {
        assertThatThrownBy(() -> service.create(studentRequest("joao@cadi.com", "JOAO@cadi.com")))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("E-mail duplicado na requisição");

        verify(userRepository, never()).save(any());
    }
}
