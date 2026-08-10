package br.cadi.people.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "guardian")
@PrimaryKeyJoinColumn(name = "person_id") // Liga com a tabela Person
@Getter
@Setter // Adicionado para permitir que o Update funcione no Service
@NoArgsConstructor // O JPA exige um construtor vazio
@AllArgsConstructor
@SuperBuilder // Permite usar o Builder com campos da classe Person
public class Guardian extends Person {

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    // --- Relacionamentos ---

    @OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<StudentGuardian> studentGuardians;
}