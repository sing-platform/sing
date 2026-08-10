package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.DescriptiveReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescriptiveReportHistoryRepository extends JpaRepository<DescriptiveReportHistory, Long> {
}
