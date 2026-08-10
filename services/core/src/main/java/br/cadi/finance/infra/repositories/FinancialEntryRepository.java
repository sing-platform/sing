package br.cadi.finance.infra.repositories;

import br.cadi.finance.domain.EntryDirection;
import br.cadi.finance.domain.EntryStatus;
import br.cadi.finance.domain.FinancialEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, Long> {

    @EntityGraph(attributePaths = {"sponsor", "fiscalDocument"})
    @Query("""
            SELECT e FROM FinancialEntry e
            WHERE (:start IS NULL OR e.dueDate >= :start)
              AND (:finish IS NULL OR e.dueDate <= :finish)
              AND (:direction IS NULL OR e.direction = :direction)
              AND (:status IS NULL OR e.status = :status)
              AND (:category IS NULL OR LOWER(e.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND e.isActive = true
            ORDER BY e.dueDate DESC, e.id DESC
            """)
    List<FinancialEntry> search(
            @Param("start") LocalDate start,
            @Param("finish") LocalDate finish,
            @Param("direction") EntryDirection direction,
            @Param("status") EntryStatus status,
            @Param("category") String category
    );

    @EntityGraph(attributePaths = {"sponsor", "fiscalDocument"})
    List<FinancialEntry> findBySponsorIdOrderByDueDateDesc(Long sponsorId);

    @Query("""
            SELECT e FROM FinancialEntry e
            WHERE e.isActive = true
              AND e.status = :status
              AND e.dueDate < :referenceDate
            """)
    List<FinancialEntry> findOverdue(@Param("status") EntryStatus status, @Param("referenceDate") LocalDate referenceDate);

    boolean existsBySponsorIdAndDueDateAndIsActiveTrue(Long sponsorId, LocalDate dueDate);
}
