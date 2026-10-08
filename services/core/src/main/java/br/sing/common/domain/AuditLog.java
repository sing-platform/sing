package br.sing.common.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Builder
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(nullable = false, length = 10)
    private String action; // INSERT, UPDATE, DELETE

    @Column(name = "old_data", columnDefinition = "JSON")
    private String oldData;

    @Column(name = "new_data", columnDefinition = "JSON")
    private String newData;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}