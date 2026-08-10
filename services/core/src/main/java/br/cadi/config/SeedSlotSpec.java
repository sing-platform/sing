package br.cadi.config;

import java.time.DayOfWeek;
import java.time.LocalTime;

record SeedSlotSpec(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
