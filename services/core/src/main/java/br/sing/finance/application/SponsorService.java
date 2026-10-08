package br.sing.finance.application;

import br.sing.exception.EntityNotFoundException;
import br.sing.finance.api.dto.request.SponsorCreateRequest;
import br.sing.finance.api.dto.request.SponsorUpdateRequest;
import br.sing.finance.api.dto.response.SponsorResponse;
import br.sing.finance.domain.Sponsor;
import br.sing.finance.infra.repositories.SponsorRepository;
import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<SponsorResponse> listAll() {
        return sponsorRepository.findAllByOrderByNameAsc()
                .stream()
                .map(SponsorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SponsorResponse getById(Long id) {
        return SponsorResponse.from(findOrThrow(id));
    }

    @Transactional
    public SponsorResponse create(SponsorCreateRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));

        Sponsor sponsor = Sponsor.builder()
                .student(student)
                .name(request.name().trim())
                .email(request.email())
                .phone(request.phone())
                .amount(request.amount())
                .frequency(request.frequency())
                .startDate(request.startDate())
                .lastGeneratedDate(null)
                .build();

        return SponsorResponse.from(sponsorRepository.save(sponsor));
    }

    @Transactional
    public SponsorResponse update(Long id, SponsorUpdateRequest request) {
        Sponsor sponsor = findOrThrow(id);
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));

        sponsor.setStudent(student);
        sponsor.setName(request.name().trim());
        sponsor.setEmail(request.email());
        sponsor.setPhone(request.phone());
        sponsor.setAmount(request.amount());
        sponsor.setFrequency(request.frequency());
        sponsor.setStartDate(request.startDate());
        sponsor.setActive(request.active());
        if (!request.active()) {
            sponsor.setDeactivatedAt(java.time.LocalDateTime.now());
        } else {
            sponsor.setDeactivatedAt(null);
            sponsor.setDeactivatedBy(null);
        }

        return SponsorResponse.from(sponsorRepository.save(sponsor));
    }

    @Transactional
    public void delete(Long id) {
        Sponsor sponsor = findOrThrow(id);
        sponsor.setActive(false);
        sponsor.setDeactivatedAt(java.time.LocalDateTime.now());
        sponsorRepository.save(sponsor);
    }

    @Transactional(readOnly = true)
    public List<Sponsor> findActiveForBilling() {
        return sponsorRepository.findAllByIsActiveTrue();
    }

    @Transactional
    public void saveAll(List<Sponsor> sponsors) {
        sponsorRepository.saveAll(sponsors);
    }

    @Transactional(readOnly = true)
    public Sponsor findEntityOrThrow(Long id) {
        return findOrThrow(id);
    }

    private Sponsor findOrThrow(Long id) {
        return sponsorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Padrinho não encontrado"));
    }
}
