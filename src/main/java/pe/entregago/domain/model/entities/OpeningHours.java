package pe.entregago.domain.model.entities;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Set;

public record OpeningHours(Set<DayOfWeek> days, LocalTime opensAt, LocalTime closesAt) {
    public OpeningHours {
        days = Set.copyOf(days);
        if (days.isEmpty() || opensAt == null || closesAt == null || !opensAt.isBefore(closesAt))
            throw new IllegalArgumentException("opening hours must contain days and a valid time range");
    }
    public boolean contains(ZonedDateTime time) {
        return days.contains(time.getDayOfWeek()) && !time.toLocalTime().isBefore(opensAt)
                && time.toLocalTime().isBefore(closesAt);
    }
}
