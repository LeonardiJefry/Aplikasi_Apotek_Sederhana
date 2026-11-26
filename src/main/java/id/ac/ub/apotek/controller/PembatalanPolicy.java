package id.ac.ub.apotek.controller;

import java.time.LocalDateTime;
import java.util.Set;

/** Satu aturan kelayakan untuk tampilan UC-03 dan penulisan UC-04. */
public final class PembatalanPolicy {
    private static final Set<String> STATUS = Set.of("Terdaftar", "Menunggu", "Hadir/Menunggu", "Dilewati");
    private PembatalanPolicy() {}
    public static boolean statusDiizinkan(String status) { return STATUS.contains(status); }
    public static boolean waktuDiizinkan(LocalDateTime sekarang, LocalDateTime mulai) { return sekarang.plusMinutes(60).isBefore(mulai); }
    public static boolean boleh(String status, LocalDateTime sekarang, LocalDateTime mulai) {
        return statusDiizinkan(status) && waktuDiizinkan(sekarang, mulai);
    }
}
