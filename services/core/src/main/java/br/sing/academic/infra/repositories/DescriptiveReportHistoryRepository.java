package br.sing.academic.infra.repositories;

import br.sing.academic.domain.entities.DescriptiveReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescriptiveReportHistoryRepository extends JpaRepository<DescriptiveReportHistory, Long> {
}
