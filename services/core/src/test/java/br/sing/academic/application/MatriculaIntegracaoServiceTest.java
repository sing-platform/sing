package br.sing.academic.application;

import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.MatriculaIntegracao;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.academic.infra.repositories.MatriculaIntegracaoRepository;
import br.sing.people.domain.entities.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculaIntegracaoServiceTest {

    @Mock
    private MatriculaIntegracaoRepository repository;

    @Test
    void duasMatriculasNascemComEventIdDiferenteEVencimentoFixo() {
        when(repository.save(any(MatriculaIntegracao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MatriculaIntegracaoService service = new MatriculaIntegracaoService(repository, new N2Properties());

        MatriculaIntegracao primeira = service.registrar(matricula(9001L, LocalDate.of(2026, 11, 5)));
        MatriculaIntegracao segunda = service.registrar(matricula(9002L, LocalDate.of(2026, 11, 5)));

        assertThat(primeira.getEventId()).isNotEqualTo(segunda.getEventId());
        assertThat(primeira.getEventId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(primeira.getEnvelope()).contains(primeira.getEventId());
        assertThat(segunda.getEnvelope()).contains("\"matriculaId\":9002");
        assertThat(primeira.getVencimento()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(primeira.getValorCentavos()).isEqualTo(15000);
        assertThat(primeira.getPublicacao()).isEqualTo("pendente");
        assertThat(primeira.getCobranca()).isEqualTo("pendente");
    }

    private static Enrollment matricula(Long id, LocalDate data) {
        Student aluno = Student.builder().id(501L).name("Aluno Exemplo Fictício").build();
        Workshop oficina = Workshop.builder().id(12L).name("Oficina Exemplo de Teatro").build();
        SchoolRoom turma = SchoolRoom.builder().id(77L).name("Turma Exemplo Manhã").workshop(oficina).build();
        return Enrollment.builder()
                .id(id)
                .student(aluno)
                .schoolroom(turma)
                .enrollmentDate(data)
                .status(true)
                .build();
    }
}
