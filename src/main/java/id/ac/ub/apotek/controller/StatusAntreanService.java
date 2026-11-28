package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.Antrean;
import id.ac.ub.apotek.repository.AntreanRepository;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static id.ac.ub.apotek.controller.StatusAntreanDto.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StatusAntreanService {
    private static final Set<String> AKHIR = Set.of("Selesai", "Dibatalkan", "Tidak Hadir");
    private static final Set<String> AKTIF = Set.of("Terdaftar", "Menunggu", "Hadir/Menunggu", "Dilewati", "Sedang Dilayani");
    private static final Set<String> BISA_BATAL = Set.of("Terdaftar", "Menunggu", "Hadir/Menunggu", "Dilewati");
    private final AntreanRepository antrean;
    private final CurrentUserService current;
    private final Clock clock;
    public StatusAntreanService(AntreanRepository antrean, CurrentUserService current, Clock clock) {
        this.antrean = antrean; this.current = current; this.clock = clock;
    }

    /** Beberapa query dibaca pada snapshot transaksi yang sama, tanpa mengunci/menulis kuota. */
    public Snapshot snapshot(String idAntrean) {
        var pasien = current.requirePasienId();
        if (idAntrean != null && (idAntrean.isBlank() || idAntrean.length() > 12))
            throw new IllegalArgumentException("ID tiket tidak valid");
        var tickets = antrean.daftarTiketPasien(pasien);
        var pilihan = tickets.stream().map(a -> new Pilihan(a.getIdAntrean(), a.getNomorAntrean(),
                a.getStatusAntrean(), a.getJadwal().getTanggal(), a.getJadwal().getPoli().getNamaPoli(),
                a.getJadwal().getSesi())).toList();
        var selected = idAntrean == null ? tickets.stream().findFirst()
                : tickets.stream().filter(a -> a.getIdAntrean().equals(idAntrean)).findFirst();
        if (idAntrean != null && selected.isEmpty()) throw new NoSuchElementException("Tiket tidak ditemukan");
        var now = LocalDateTime.now(clock);
        return new Snapshot(pilihan, selected.map(a -> detail(a, now)).orElse(null),
                now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private Detail detail(Antrean ticket, LocalDateTime now) {
        var jadwal = ticket.getJadwal();
        var rows = antrean.nomorStatusSesi(jadwal.getIdJadwal());
        boolean akhir = AKHIR.contains(ticket.getStatusAntrean());
        boolean dilayani = ticket.getStatusAntrean().equals("Sedang Dilayani");
        // Hitung entri aktif bernomor lebih kecil, bukan selisih nomor atau panjang seluruh antrean.
        Integer depan = akhir || dilayani ? null : (int) rows.stream()
                .filter(a -> a.getNomorAntrean() < ticket.getNomorAntrean() && AKTIF.contains(a.getStatusAntrean())).count();
        var sekarang = rows.stream().filter(a -> a.getStatusAntrean().equals("Sedang Dilayani"))
                .map(AntreanRepository.NomorStatus::getNomorAntrean).toList();
        var berikut = akhir ? List.<Baris>of() : rows.stream()
                .filter(a -> BISA_BATAL.contains(a.getStatusAntrean())).limit(5)
                .map(a -> new Baris(a.getNomorAntrean(), a.getStatusAntrean())).toList();
        var mulai = jadwal.getTanggal().atTime(jadwal.getJamMulai());
        var selesai = jadwal.getTanggal().atTime(jadwal.getJamSelesai());
        boolean batal = PembatalanPolicy.boleh(ticket.getStatusAntrean(), now, mulai);
        // UC-04/AD-04: tombol dan pop-up tersedia menurut status; batas 60 menit diperiksa setelah konfirmasi (alur 4a).
        boolean bukaBatal = PembatalanPolicy.statusDiizinkan(ticket.getStatusAntrean());
        boolean checkIn = CheckInPolicy.boleh(ticket.getStatusAntrean(), ticket.getWaktuCheckIn(), now,
                jadwal.getTanggal(), jadwal.getJamMulai(), jadwal.getJamSelesai());
        return new Detail(ticket.getIdAntrean(), ticket.getNomorAntrean(), ticket.getStatusAntrean(),
                JadwalDto.from(jadwal), sekarang, depan, berikut, akhir, batal, checkIn,
                keterangan(ticket.getStatusAntrean()), bukaBatal);
    }

    private String keterangan(String status) {
        return switch (status) {
            case "Terdaftar" -> "Booking Anda tercatat. Hadir sesuai jadwal dan lakukan check-in pada waktu yang ditentukan.";
            case "Menunggu", "Hadir/Menunggu" -> "Silakan menunggu giliran sesuai arahan petugas klinik.";
            case "Sedang Dilayani" -> "Anda sedang dilayani di ruang periksa.";
            case "Dilewati" -> "Nomor Anda dilewati. Hubungi petugas loket untuk arahan panggilan ulang.";
            case "Selesai" -> "Pemeriksaan telah selesai. Terima kasih atas kunjungan Anda.";
            case "Dibatalkan" -> "Pendaftaran ini telah dibatalkan.";
            case "Tidak Hadir" -> "Pendaftaran berakhir dengan status Tidak Hadir. Hubungi petugas bila memerlukan bantuan.";
            default -> "Hubungi petugas klinik untuk informasi antrean.";
        };
    }
}
