package br.sing.academic.domain.entities;
import br.sing.common.domain.AuditableSoftDeleteEntity;
import br.sing.common.domain.Trackable;
import br.sing.people.domain.entities.Collaborator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Getter
@Entity
@Table(name = "schoolroom")
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchoolRoom extends AuditableSoftDeleteEntity implements Trackable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String schedule;

    @Column(name = "classroom", nullable = false)
    private String classroom;

    @Column(name = "capacity")
    private Integer capacity;

    @ManyToOne
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Collaborator teacher;

    @OneToMany(mappedBy = "schoolroom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Lecture> lectures;

    @OneToMany(mappedBy = "schoolroom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Enrollment> enrollments;

    @Builder.Default
    @OneToMany(mappedBy = "schoolroom", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchoolRoomScheduleSlot> scheduleSlots = new ArrayList<>();

    @Override
    public Long getId() { return id; }

}
