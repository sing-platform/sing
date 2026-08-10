package br.cadi.people.domain.entities;

import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.people.domain.enums.FamilyTie;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "student_guardian")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class StudentGuardian extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_tie", nullable = false, length = 50)
    private FamilyTie familyTie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;
}
