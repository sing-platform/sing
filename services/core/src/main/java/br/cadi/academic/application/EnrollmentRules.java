package br.cadi.academic.application;

import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.SchoolRoomScheduleSlot;
import br.cadi.exception.BusinessException;
import br.cadi.people.domain.entities.Student;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * Regras de horário e período escolar para matrícula (UC013).
 * Pré-matrícula (E-4) fica fora até existir domínio de lista de espera.
 */
public final class EnrollmentRules {

    private static final LocalTime MIDDAY = LocalTime.NOON;
    private static final int MAX_ACTIVE_ENROLLMENTS = 2;

    private EnrollmentRules() {
    }

    public static int maxActiveEnrollments() {
        return MAX_ACTIVE_ENROLLMENTS;
    }

    public static boolean slotsOverlap(SchoolRoomScheduleSlot a, SchoolRoomScheduleSlot b) {
        if (a.getDayOfWeek() != b.getDayOfWeek()) {
            return false;
        }
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    public static boolean schedulesOverlap(
            Collection<SchoolRoomScheduleSlot> a,
            Collection<SchoolRoomScheduleSlot> b
    ) {
        for (SchoolRoomScheduleSlot x : a) {
            for (SchoolRoomScheduleSlot y : b) {
                if (slotsOverlap(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void assertPeriodStudiesAllow(Student student, SchoolRoom targetRoom) {
        String raw = student.getPeriodStudies();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String p = raw.trim().toUpperCase();
        List<SchoolRoomScheduleSlot> slots = targetRoom.getScheduleSlots();
        if (slots == null || slots.isEmpty()) {
            return;
        }
        String label = raw.trim();
        if (p.contains("MATUTINO")) {
            for (SchoolRoomScheduleSlot slot : slots) {
                if (slot.getStartTime().isBefore(MIDDAY)) {
                    throw new BusinessException(
                            "O aluno estuda no período da " + label
                                    + " na escola principal; selecione outra turma (horários após o período escolar).",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
        }
        if (p.contains("VESPERTINO")) {
            for (SchoolRoomScheduleSlot slot : slots) {
                if (!slot.getStartTime().isBefore(MIDDAY)) {
                    throw new BusinessException(
                            "O aluno estuda no período da " + label
                                    + " na escola principal; selecione outra turma (horários no período matutino).",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
        }
    }

    public static String conflictingRoomName(SchoolRoom target, Collection<SchoolRoom> others) {
        List<SchoolRoomScheduleSlot> targetSlots = target.getScheduleSlots();
        if (targetSlots == null || targetSlots.isEmpty()) {
            return null;
        }
        for (SchoolRoom other : others) {
            if (other.getId().equals(target.getId())) {
                continue;
            }
            List<SchoolRoomScheduleSlot> otherSlots = other.getScheduleSlots();
            if (otherSlots == null || otherSlots.isEmpty()) {
                continue;
            }
            if (schedulesOverlap(targetSlots, otherSlots)) {
                return other.getName();
            }
        }
        return null;
    }
}