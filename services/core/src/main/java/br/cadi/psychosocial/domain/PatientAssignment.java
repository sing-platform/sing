package br.cadi.psychosocial.domain;

import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Vínculo entre um psicólogo e um aluno (paciente). Define quais alunos um
 * psicólogo acompanha — pré-requisito para visualizar dados clínicos e
 * registrar anotações. O psicólogo se auto-vincula a partir da lista de alunos.
 */
@Entity
@Table(name = "patient_assignment")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAssignment extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psychologist_id", nullable = false)
    private Collaborator psychologist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Student patient;

}
