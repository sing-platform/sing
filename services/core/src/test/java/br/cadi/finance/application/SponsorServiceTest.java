package br.cadi.finance.application;

import br.cadi.exception.EntityNotFoundException;
import br.cadi.finance.api.dto.request.SponsorCreateRequest;
import br.cadi.finance.domain.SponsorFrequency;
import br.cadi.finance.infra.repositories.SponsorRepository;
import br.cadi.people.infra.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SponsorServiceTest {

    @Mock
    private SponsorRepository sponsorRepository;
    @Mock
    private StudentRepository studentRepository;

    private SponsorService sponsorService;

    @BeforeEach
    void setUp() {
        sponsorService = new SponsorService(sponsorRepository, studentRepository);
    }

    @Test
    void createDeveLancarQuandoAlunoNaoExiste() {
        var request = new SponsorCreateRequest(
                999L,
                "Padrinho X",
                "x@x.com",
                "11999999999",
                new BigDecimal("50.00"),
                SponsorFrequency.MONTHLY,
                LocalDate.now()
        );
        org.mockito.Mockito.when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sponsorService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Aluno não encontrado");
    }
}
