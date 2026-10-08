package br.sing.people.domain.entities;

import br.sing.academic.domain.entities.Attendance;
import br.sing.academic.domain.entities.Enrollment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;


@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@PrimaryKeyJoinColumn(name = "person_id")
@Table(name = "student")
public class Student extends Person{


    @Column(length = 50, nullable = false)
    private String grade;

    @Column(name = "current_school", nullable = false)
    private String currentSchool;

    @Column(name = "period_studies", nullable = false)
    private String periodStudies; //ex: MATUTITNO ou VESPERTINO

    @Column(name = "chronic_illness", length = 2000)
    private String chronicIllness;

    @Column(length = 2000)
    private String allergy;

    @Column(length = 2000)
    private String medicine;

    @Column(length = 2000)
    private String deficiency;

    @Column(name = "follow_up", length = 2000)
    private String followUp;

    @Column(length = 2000)
    private String surgery;


    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<StudentGuardian> guardians = new HashSet<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<Attendance> attendances = new HashSet<>();
}
