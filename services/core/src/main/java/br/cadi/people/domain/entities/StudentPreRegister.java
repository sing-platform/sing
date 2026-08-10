package br.cadi.people.domain.entities;

import br.cadi.academic.domain.entities.Workshop;
import br.cadi.iam.domain.User;
import br.cadi.people.domain.enums.Shift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_pre_register")
public class StudentPreRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "summoned_at")
    private LocalDateTime summonedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summoned_by")
    private User summonedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
