package id.ac.ub.apotek.controller;

import java.time.*;

public final class CheckInPolicy {
    private CheckInPolicy() {}
    public static boolean waktuDibuka(LocalDateTime now, LocalDate date, LocalTime mulai, LocalTime selesai) {
        return now.toLocalDate().equals(date) && !now.isBefore(date.atTime(mulai).minusMinutes(30))
                && !now.isAfter(date.atTime(selesai));
    }
    public static boolean boleh(String status, LocalDateTime tercatat, LocalDateTime now,
                                LocalDate date, LocalTime mulai, LocalTime selesai) {
        return status.equals("Terdaftar") && tercatat == null && waktuDibuka(now, date, mulai, selesai);
    }
}
