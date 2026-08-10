package br.cadi.people.infra.repositories;

import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {
    Optional<Collaborator> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user u JOIN FETCH c.address WHERE c.type = :type AND u.isActive = :active")
    List<Collaborator> findAllByTypeAndUserActive(@Param("type") CollaboratorType type, @Param("active") boolean active);

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user JOIN FETCH c.address WHERE c.type = :type")
    List<Collaborator> findAllByType(@Param("type") CollaboratorType type);

    /**
     * Professores titulares na UI: tipo {@link CollaboratorType#TEACHER} ou usuário com papel ROLE_PROFESSOR
     * (evita lista vazia quando o cadastro usa outro tipo de colaborador mas a role IAM é de professor).
     */
    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user u JOIN FETCH c.address JOIN FETCH u.role r "
            + "WHERE (c.type = :teacherType OR r.name = 'ROLE_PROFESSOR') AND u.isActive = true")
    List<Collaborator> findAllEligibleSchoolRoomTeachersActive(@Param("teacherType") CollaboratorType teacherType);

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user u JOIN FETCH c.address JOIN FETCH u.role r "
            + "WHERE c.type = :teacherType OR r.name = 'ROLE_PROFESSOR'")
    List<Collaborator> findAllEligibleSchoolRoomTeachersIncludingInactiveUsers(
            @Param("teacherType") CollaboratorType teacherType);

    @Query("SELECT c FROM Collaborator c JOIN FETCH c.user JOIN FETCH c.address WHERE c.id = :id AND c.type = :type")
    Optional<Collaborator> findByIdAndType(@Param("id") Long id, @Param("type") CollaboratorType type);

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user u JOIN FETCH c.address WHERE u.isActive = true")
    List<Collaborator> findAllActiveWithUserAndAddress();

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user JOIN FETCH c.address")
    List<Collaborator> findAllWithUserAndAddress();

    @Query("SELECT c FROM Collaborator c JOIN FETCH c.user JOIN FETCH c.address WHERE c.id = :id")
    Optional<Collaborator> findByIdWithUserAndAddress(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Collaborator c JOIN FETCH c.user u JOIN FETCH c.address "
            + "WHERE c.type = :type AND (:activeOnly = false OR u.isActive = true)")
    List<Collaborator> findAllByTypeFiltered(
            @Param("type") CollaboratorType type,
            @Param("activeOnly") boolean activeOnly);

    @Query("SELECT c.type, COUNT(c) FROM Collaborator c GROUP BY c.type")
    List<Object[]> countGroupByType();

    @Query("SELECT c.type, COUNT(c) FROM Collaborator c JOIN c.user u WHERE u.isActive = true GROUP BY c.type")
    List<Object[]> countActiveGroupByType();

    @Query("SELECT c FROM Collaborator c WHERE c.user.id = :userId")
    Optional<Collaborator> findByUserId(@Param("userId") Long userId);

}

