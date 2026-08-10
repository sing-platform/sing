package br.cadi.common.infra;

import br.cadi.common.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findTopByTableNameAndRecordIdOrderByChangedAtDesc(
            String tableName, Long recordId
    );

    // Busca UPDATE para atribuição de professores-turmas
    List<AuditLog> findByTableNameAndRecordIdAndActionOrderByChangedAtAsc(
            String tableName, Long recordId, String action
    );

    @Query("""
    SELECT MAX(al.changedAt)
    FROM AuditLog al
    WHERE al.tableName = 'attendance'
    AND al.recordId IN :attendanceIds
""")
    LocalDateTime findLastAttendanceChange(
            @Param("attendanceIds") List<Long> attendanceIds
    );

}
