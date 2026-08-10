package br.cadi.academic.domain.entities;

import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.people.domain.entities.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "enrollment")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "schoolroom_id", nullable = false)
    private SchoolRoom schoolroom;

    @Column(nullable = false)
    private LocalDate enrollmentDate;

    @Builder.Default
    private Boolean status = true;
}
