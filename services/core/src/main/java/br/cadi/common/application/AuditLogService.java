package br.cadi.common.application;

import br.cadi.academic.api.dto.response.AttendanceAuditResponse;
import br.cadi.academic.domain.entities.Attendance;
import br.cadi.academic.infra.repositories.AttendanceRepository;
import br.cadi.common.domain.AuditLog;
import br.cadi.common.infra.AuditLogRepository;
import br.cadi.people.api.dto.response.TeacherAssignmentLogResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final AttendanceRepository attendanceRepository;

    public void logAudit(String action, String tableName, Long recordId,
                    Object oldData, Object newData, Long changedBy) {
        try {
            repository.save(AuditLog.builder()
                    .action(action)
                    .tableName(tableName)
                    .recordId(recordId)
                    .oldData(oldData != null ? objectMapper.writeValueAsString(oldData) : null)
                    .newData(newData != null ? objectMapper.writeValueAsString(newData) : null)
                    .changedBy(changedBy)
                    .changedAt(LocalDateTime.now())
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Falha ao gravar audit_log: {}", e.getMessage());
        }
    }

    public List<TeacherAssignmentLogResponse> findTeacherAssignments(Long schoolRoomId) {
        return repository
                .findByTableNameAndRecordIdAndActionOrderByChangedAtAsc(
                        "school_room", schoolRoomId, "UPDATE"
                )
                .stream()
                .filter(log -> {
                    if (log.getNewData() == null) return false;
                    try {
                        JsonNode node = objectMapper.readTree(log.getNewData());
                        JsonNode tid  = node.get("teacherId");
                        return tid != null && !tid.isNull();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(log -> {
                    Long teacherId = null;
                    try {
                        JsonNode node = objectMapper.readTree(log.getNewData());
                        teacherId = node.get("teacherId").asLong();
                    } catch (Exception ignored) {}

                    return new TeacherAssignmentLogResponse(
                            schoolRoomId,
                            teacherId,
                            null,
                            log.getChangedBy(),
                            log.getChangedAt()
                    );
                })
                .sorted(Comparator.comparing(
                        TeacherAssignmentLogResponse::changedAt).reversed()
                )
                .toList();
    }

    public Optional<LocalDateTime> findFirstTeacherAssignment(Long schoolRoomId) {
        return repository
                .findByTableNameAndRecordIdAndActionOrderByChangedAtAsc(
                        "school_room", schoolRoomId, "UPDATE"
                )
                .stream()
                .filter(log -> {
                    // checa se new_data tem teacherId não nulo
                    if (log.getNewData() == null) return false;
                    try {
                        JsonNode node = objectMapper.readTree(log.getNewData());
                        JsonNode tid  = node.get("teacherId");
                        return tid != null && !tid.isNull();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(AuditLog::getChangedAt)
                .findFirst();
    }

    // Tabela Log para Attendance
    public Optional<AttendanceAuditResponse> findLastAttendanceChange(Long attendanceId) {
        return repository
                .findTopByTableNameAndRecordIdOrderByChangedAtDesc(
                        "attendance",
                        attendanceId
                )
                .map(log -> new AttendanceAuditResponse(
                        attendanceId,
                        log.getAction(),
                        log.getChangedBy(),
                        log.getChangedAt()
                ));
    }

        public Optional<LocalDateTime> findLastEntityChange(String tableName, Long recordId) {
        return repository
            .findTopByTableNameAndRecordIdOrderByChangedAtDesc(
                tableName,
                recordId
            )
            .map(AuditLog::getChangedAt);
        }

    public  Optional<LocalDateTime>  findLastLectureAttendanceUpdate(Long lectureId) {
        List<Long> attendanceIds = attendanceRepository
                .findByLectureId(lectureId)
                .stream()
                .map(Attendance::getId)
                .toList();

        if (attendanceIds.isEmpty()) {
            return null;
        }

        return Optional.ofNullable(
                repository.findLastAttendanceChange(attendanceIds)
        );
    }

}