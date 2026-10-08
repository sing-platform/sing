package br.sing.academic.domain.entities;

import br.sing.academic.domain.enums.DescriptiveReportHistoryAction;
import br.sing.common.domain.AuditableSoftDeleteEntity;
import br.sing.people.domain.entities.Collaborator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "descriptive_report_history")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DescriptiveReportHistory extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descriptive_report_id", nullable = false)
    private DescriptiveReport descriptiveReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private Collaborator actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DescriptiveReportHistoryAction action;

    @Column(name = "previous_content", length = 4000)
    private String previousContent;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
