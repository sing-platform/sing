package br.sing.people.application;

import br.sing.people.domain.entities.Guardian;
import br.sing.people.domain.entities.Student;
import br.sing.people.domain.entities.StudentGuardian;
import br.sing.people.infra.repositories.StudentGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link StudentGuardianService}.
 *
 * <p>Service de leitura simples — apenas delega ao repositório.
 */
@ExtendWith(MockitoExtension.class)
class StudentGuardianServiceTest {

    @Mock
    private StudentGuardianRepository repository;

    private StudentGuardianService service;

    @BeforeEach
    void setUp() {
        service = new StudentGuardianService(repository);
    }

    @Test
    void getFamilyTreeDeveDelegarAoRepositorio() {
        StudentGuardian sg = StudentGuardian.builder().id(1L).build();
        when(repository.findFamilyTreeByGuardianId(7L)).thenReturn(List.of(sg));

        List<StudentGuardian> result = service.getFamilyTree(7L);

        assertThat(result).hasSize(1).contains(sg);
    }

    @Test
    void getFamilyTreeDeveRetornarVazioQuandoSemRegistros() {
        when(repository.findFamilyTreeByGuardianId(99L)).thenReturn(List.of());

        assertThat(service.getFamilyTree(99L)).isEmpty();
    }

    @Test
    void getGuardianNamesByStudentDeveExtrairNomesDosGuardians() {
        Student student = Student.builder().build();

        Guardian g1 = Guardian.builder().isPrimary(true).build();
        g1.setName("Pai");
        Guardian g2 = Guardian.builder().isPrimary(false).build();
        g2.setName("Mãe");

        StudentGuardian sg1 = StudentGuardian.builder().guardian(g1).build();
        StudentGuardian sg2 = StudentGuardian.builder().guardian(g2).build();

        when(repository.findAllByStudent(student)).thenReturn(List.of(sg1, sg2));

        List<String> names = service.getGuardianNamesByStudent(student);

        assertThat(names).containsExactly("Pai", "Mãe");
    }

    @Test
    void getGuardianNamesByStudentDeveRetornarVazioQuandoStudentSemGuardians() {
        Student student = Student.builder().build();
        when(repository.findAllByStudent(student)).thenReturn(List.of());

        assertThat(service.getGuardianNamesByStudent(student)).isEmpty();
    }
}
