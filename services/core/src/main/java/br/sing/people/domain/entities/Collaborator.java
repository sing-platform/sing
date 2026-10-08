package br.sing.people.domain.entities;

import br.sing.people.domain.enums.CollaboratorType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter @Setter
@Table(name = "collaborator")
@PrimaryKeyJoinColumn(name = "person_id")
public class Collaborator extends Person{

    @Column(name = "education_level", length = 50, nullable = false)
    private String educationLevel;

    @Column(name = "education_cert_type", length = 100)
    private String educationCertType;

    @Column(name = "institution_cert")
    private String institutionCert;

    @Column(name = "year_conclusion")
    private Integer yearConclusion;

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CollaboratorType type;

}

//todo: adicionar campos de salario. trocar type por especificação de funcao do funcionario.
//todo: configurar constrain na tabela de colaborador para pegar role de User