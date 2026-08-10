package br.cadi.config;

import br.cadi.academic.application.EnrollmentRules;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.people.domain.entities.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

final class SeedEnrollmentPlanner {

    private static final int MIN_PER_ROOM = 2;
    private static final int MAX_PER_STUDENT = 2;

    private SeedEnrollmentPlanner() {
    }

    static void enrollBalanced(
            List<SchoolRoom> rooms,
            List<Student> students,
            SchoolRoomRepository schoolRoomRepository,
            BiConsumer<Student, SchoolRoom> enrollAction
    ) {
        if (rooms.isEmpty() || students.isEmpty()) {
            return;
        }

        List<SchoolRoom> loadedRooms = rooms.stream()
                .map(r -> schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(r.getId()).orElse(r))
                .toList();

        List<SchoolRoom> manha = loadedRooms.stream().filter(SeedEnrollmentPlanner::isManhaRoom).toList();
        List<SchoolRoom> tarde = loadedRooms.stream().filter(SeedEnrollmentPlanner::isTardeRoom).toList();

        List<Student> vespertino = students.stream()
                .filter(s -> periodContains(s, "VESPERTINO"))
                .toList();
        List<Student> matutino = students.stream()
                .filter(s -> periodContains(s, "MATUTINO"))
                .toList();

        Map<Long, List<SchoolRoom>> enrolledByStudent = new HashMap<>();
        Map<Long, Integer> countByRoom = new HashMap<>();

        phaseMinimumCoverage(manha, vespertino, enrolledByStudent, countByRoom, enrollAction);
        phaseMinimumCoverage(tarde, matutino, enrolledByStudent, countByRoom, enrollAction);
        phaseSecondEnrollment(loadedRooms, students, enrolledByStudent, countByRoom, enrollAction);
    }

    private static void phaseMinimumCoverage(
            List<SchoolRoom> rooms,
            List<Student> eligibleStudents,
            Map<Long, List<SchoolRoom>> enrolledByStudent,
            Map<Long, Integer> countByRoom,
            BiConsumer<Student, SchoolRoom> enrollAction
    ) {
        if (rooms.isEmpty() || eligibleStudents.isEmpty()) {
            return;
        }
        int studentIdx = 0;
        for (SchoolRoom room : rooms) {
            int placed = 0;
            int attempts = 0;
            while (placed < MIN_PER_ROOM && attempts < eligibleStudents.size() * 2) {
                Student student = eligibleStudents.get(studentIdx % eligibleStudents.size());
                studentIdx++;
                attempts++;
                if (tryEnroll(student, room, enrolledByStudent, enrollAction)) {
                    placed++;
                    countByRoom.merge(room.getId(), 1, Integer::sum);
                }
            }
        }
    }

    private static void phaseSecondEnrollment(
            List<SchoolRoom> allRooms,
            List<Student> students,
            Map<Long, List<SchoolRoom>> enrolledByStudent,
            Map<Long, Integer> countByRoom,
            BiConsumer<Student, SchoolRoom> enrollAction
    ) {
        List<SchoolRoom> sortedRooms = new ArrayList<>(allRooms);
        for (Student student : students) {
            List<SchoolRoom> current = enrolledByStudent.getOrDefault(student.getId(), List.of());
            if (current.size() >= MAX_PER_STUDENT) {
                continue;
            }
            sortedRooms.sort(Comparator.comparingInt(r -> countByRoom.getOrDefault(r.getId(), 0)));
            for (SchoolRoom room : sortedRooms) {
                if (!roomMatchesPeriod(student, room)) {
                    continue;
                }
                if (tryEnroll(student, room, enrolledByStudent, enrollAction)) {
                    countByRoom.merge(room.getId(), 1, Integer::sum);
                    break;
                }
            }
        }
        for (Student student : students) {
            while (enrolledByStudent.getOrDefault(student.getId(), List.of()).size() < MAX_PER_STUDENT) {
                boolean added = false;
                sortedRooms.sort(Comparator.comparingInt(r -> countByRoom.getOrDefault(r.getId(), 0)));
                for (SchoolRoom room : sortedRooms) {
                    if (!roomMatchesPeriod(student, room)) {
                        continue;
                    }
                    if (tryEnroll(student, room, enrolledByStudent, enrollAction)) {
                        countByRoom.merge(room.getId(), 1, Integer::sum);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    break;
                }
            }
        }
    }

    private static boolean tryEnroll(
            Student student,
            SchoolRoom room,
            Map<Long, List<SchoolRoom>> enrolledByStudent,
            BiConsumer<Student, SchoolRoom> enrollAction
    ) {
        List<SchoolRoom> current = enrolledByStudent.computeIfAbsent(student.getId(), id -> new ArrayList<>());
        if (current.size() >= MAX_PER_STUDENT) {
            return false;
        }
        if (!roomMatchesPeriod(student, room)) {
            return false;
        }
        for (SchoolRoom existing : current) {
            if (existing.getId().equals(room.getId())) {
                return false;
            }
            if (schedulesConflict(existing, room)) {
                return false;
            }
        }
        enrollAction.accept(student, room);
        current.add(room);
        return true;
    }

    private static boolean schedulesConflict(SchoolRoom a, SchoolRoom b) {
        List<br.cadi.academic.domain.entities.SchoolRoomScheduleSlot> slotsA = a.getScheduleSlots();
        List<br.cadi.academic.domain.entities.SchoolRoomScheduleSlot> slotsB = b.getScheduleSlots();
        if (slotsA == null || slotsB == null || slotsA.isEmpty() || slotsB.isEmpty()) {
            return false;
        }
        return EnrollmentRules.schedulesOverlap(slotsA, slotsB);
    }

    private static boolean roomMatchesPeriod(Student student, SchoolRoom room) {
        String period = student.getPeriodStudies();
        if (period == null) {
            return true;
        }
        String p = period.toUpperCase();
        if (p.contains("VESPERTINO")) {
            return isManhaRoom(room);
        }
        if (p.contains("MATUTINO")) {
            return isTardeRoom(room);
        }
        return true;
    }

    private static boolean periodContains(Student student, String token) {
        return student.getPeriodStudies() != null
                && student.getPeriodStudies().toUpperCase().contains(token);
    }

    private static boolean isManhaRoom(SchoolRoom room) {
        return room.getName() != null && room.getName().endsWith(" - Manhã");
    }

    private static boolean isTardeRoom(SchoolRoom room) {
        return room.getName() != null && room.getName().endsWith(" - Tarde");
    }
}
