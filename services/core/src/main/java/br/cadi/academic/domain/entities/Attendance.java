package br.cadi.academic.domain.entities;
import br.cadi.academic.domain.enums.AttendanceStatus;
import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.common.domain.Trackable;
import br.cadi.people.domain.entities.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends AuditableSoftDeleteEntity implements Trackable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Boolean present;

    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    /**
     * Documento (atestado/justificativa) que originou esta justificativa.
     * Permite rollback automatico caso o documento seja excluido ou re-aprovado.
     */
    @Column(name = "justified_by_document_id")
    private Long justifiedByDocumentId;

    public void applyStatus(AttendanceStatus newStatus) {
        this.status = newStatus;
        this.present = newStatus == AttendanceStatus.PRESENT;
    }

}
