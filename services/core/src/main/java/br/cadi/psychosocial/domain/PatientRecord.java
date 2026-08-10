package br.cadi.psychosocial.domain;
import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Person;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "patient_record")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRecord extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notes", nullable = false)
    private String notes;

    @Column(name = "confidential_code", nullable = false)
    private String confidentialCode;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Person patient;

    @ManyToOne
    @JoinColumn(name = "psychologist_id", nullable = false)
    private Collaborator psychologist;

}
