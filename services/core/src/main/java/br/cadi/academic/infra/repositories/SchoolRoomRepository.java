package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SchoolRoomRepository extends JpaRepository<SchoolRoom, Long> {
    Optional<SchoolRoom> findByName(String name);

    List<SchoolRoom> findByTeacherIdAndIsActiveTrue(Long teacherId);

    List<SchoolRoom> findByWorkshop_Id(Long workshopId);

    @EntityGraph(attributePaths = {"scheduleSlots", "teacher", "workshop"})
    List<SchoolRoom> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"scheduleSlots", "teacher", "workshop"})
    @Override
    List<SchoolRoom> findAll();

    @EntityGraph(attributePaths = {"scheduleSlots", "teacher", "workshop"})
    @Query("SELECT s FROM SchoolRoom s WHERE s.id = :id")
    Optional<SchoolRoom> findByIdWithScheduleSlotsAndTeacher(@Param("id") Long id);

    boolean existsByNameIgnoreCaseAndWorkshopIdAndIsActiveTrue(String name, Long workshopId);

    boolean existsByNameIgnoreCaseAndWorkshopIdAndIdNotAndIsActiveTrue(String name, Long workshopId, Long id);

    List<SchoolRoom> findByTeacherId(Long teacherId);

    /**
     * Sem {@code scheduleSlots} no graph: fetch de coleção + {@link Page} quebra paginação no Hibernate (500).
     * Slots são carregados sob demanda em {@code toResponse} dentro da transação.
     */
    @EntityGraph(attributePaths = {"teacher", "workshop"})
    @Query("SELECT sr FROM SchoolRoom sr WHERE sr.teacher.id = :teacherId AND (:active IS NULL OR sr.isActive = :active)")
    Page<SchoolRoom> pageByTeacherIdAndActiveOptional(
            @Param("teacherId") Long teacherId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    List<SchoolRoom> getSchoolRoomByEnrollments(List<Enrollment> enrollments);

    @Query("SELECT DISTINCT sr FROM SchoolRoom sr " +
            "JOIN FETCH sr.workshop w " +
            "LEFT JOIN FETCH sr.scheduleSlots " +
            "LEFT JOIN FETCH sr.enrollments e " +
            "LEFT JOIN FETCH e.student s " +
            "LEFT JOIN FETCH s.user " +
            "WHERE sr.teacher.id = :teacherId")
    List<SchoolRoom> findFullSchedulesByTeacherId(@Param("teacherId") Long teacherId);

    // Busca turmas e horários
    @Query("SELECT DISTINCT sr FROM SchoolRoom sr " +
            "JOIN FETCH sr.workshop " +
            "JOIN FETCH sr.scheduleSlots " +
            "WHERE sr.teacher.id = :teacherId")
    List<SchoolRoom> findWithSchedulesByTeacherId(@Param("teacherId") Long teacherId);

    // Busca turmas e matrículas (o Hibernate mescla com os dados já na sessão)
    @Query("SELECT DISTINCT sr FROM SchoolRoom sr " +
            "JOIN FETCH sr.enrollments e " +
            "JOIN FETCH e.student " +
            "WHERE sr.teacher.id = :teacherId")
    List<SchoolRoom> findWithEnrollmentsByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Carrega turmas por ID com oficina, professor e slots (evita DISTINCT + múltiplos JOIN FETCH,
     * que em alguns drivers/versões do Hibernate gerava falha em runtime).
     */
    @EntityGraph(attributePaths = {"workshop", "teacher", "scheduleSlots"})
    List<SchoolRoom> findByIdIn(Collection<Long> ids);

}
