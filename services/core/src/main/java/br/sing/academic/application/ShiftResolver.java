package br.sing.academic.application;

import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.SchoolRoomScheduleSlot;
import br.sing.people.domain.enums.Shift;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ShiftResolver {

    private static final LocalTime NOON = LocalTime.NOON;
    private static final LocalTime EVENING_START = LocalTime.of(18, 0);

    private ShiftResolver() {
    }

    public static Shift fromStartTime(LocalTime startTime) {
        if (startTime == null) {
            return Shift.EVENING;
        }
        if (startTime.isBefore(NOON)) {
            return Shift.MORNING;
        }
        if (startTime.isBefore(EVENING_START)) {
            return Shift.AFTERNOON;
        }
        return Shift.EVENING;
    }

    public static Set<Shift> shiftsFromRoom(SchoolRoom room) {
        List<SchoolRoomScheduleSlot> slots = room.getScheduleSlots();
        if (slots == null || slots.isEmpty()) {
            return Set.of();
        }
        EnumSet<Shift> shifts = EnumSet.noneOf(Shift.class);
        for (SchoolRoomScheduleSlot slot : slots) {
            if (slot.getStartTime() != null) {
                shifts.add(fromStartTime(slot.getStartTime()));
            }
        }
        return shifts;
    }

    public static boolean roomMatchesShift(SchoolRoom room, Shift shift) {
        return shiftsFromRoom(room).contains(shift);
    }
}
