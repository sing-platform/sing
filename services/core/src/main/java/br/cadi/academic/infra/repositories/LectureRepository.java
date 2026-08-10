package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findBySchoolroomId(Long schoolroomId);

    @Query("""
        SELECT COUNT(l) > 0 FROM Lecture l
        WHERE l.id = :lectureId
          AND l.schoolroom.teacher.id = :teacherId
    """)
    boolean isTeacherOfLecture(
            @Param("lectureId") Long lectureId,
            @Param("teacherId") Long teacherId
    );

    @Query("""
    SELECT l FROM Lecture l
    WHERE l.schoolroom.id = :schoolroomId
      AND l.dateTime >= :startOfDay
      AND l.dateTime < :endOfDay
""")
    Optional<Lecture> findBySchoolroomAndDate(
            @Param("schoolroomId") Long schoolroomId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    boolean existsBySchoolroomIdAndDateTime(Long schoolroomId, LocalDateTime date);

    @Query("""
    SELECT COUNT(l) > 0 FROM Lecture l
    WHERE l.schoolroom.id = :schoolroomId
      AND CAST(l.dateTime AS date) = CAST(:dateTime AS date)
      AND l.id <> :excludeId
""")
    //Valida se já existe um registro de frequência da turma para o dia consultado
    //OBS: conforme regra de negócio, não pode haver 2 Lectures registradas no mesmo dia para a mesma turma
    //para que não gere inconsistência de datas
    boolean existsAnotherLectureOnSameDay(
            @Param("schoolroomId") Long schoolroomId,
            @Param("dateTime") LocalDateTime dateTime,
            @Param("excludeId") Long excludeId
    );

    // Conta aulas registradas (com chamada) por turma num período
    @Query("""
    SELECT COUNT(l) FROM Lecture l
    WHERE l.schoolroom.id = :schoolRoomId
      AND l.dateTime >= :start
      AND l.dateTime < :end
""")
    long countRegisteredLectures(
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT l FROM Lecture l
    WHERE l.schoolroom.id = :roomId
      AND l.dateTime BETWEEN :start AND :end
    ORDER BY l.dateTime DESC
""")
    List<Lecture> findBySchoolroomIdAndPeriod(
            @Param("roomId") Long roomId,
            @Param("start")  LocalDateTime start,
            @Param("end")    LocalDateTime end
    );

}