package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.Antrean;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CheckInService {
    private final AntreanRepository antrean;
    private final JadwalPraktikRepository jadwal;
    private final CurrentUserService current;
    private final Clock clock;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public CheckInService(AntreanRepository antrean, JadwalPraktikRepository jadwal, CurrentUserService current, Clock clock) {
        this.antrean = antrean; this.jadwal = jadwal; this.current = current; this.clock = clock;
    }
    public record Tiket(String idAntrean, int nomorAntrean, String statusAntrean, JadwalDto jadwal,
                        String waktuCheckIn, String batasBuka, String batasTutup, boolean jendelaDibuka, boolean bolehCheckIn) {
        public String nomorTampilan() { return String.format(Locale.ROOT, "%03d", nomorAntrean); }
    }
    public record Panel(Tiket tiket, String sekarang) {}

    public Panel panel(String id) {
        var pasien = current.requirePasienId(); var now = LocalDateTime.now(clock);
        Optional<Antrean> tiket;
        if (id == null) tiket = antrean.daftarTiketPasien(pasien).stream()
                .filter(a -> a.getStatusAntrean().equals("Terdaftar") && a.getJadwal().getTanggal().equals(now.toLocalDate())).findFirst();
        else { validasiId(id); tiket = antrean.tiketPasien(id, pasien); if (tiket.isEmpty()) throw tidakAda(); }
        return new Panel(tiket.map(a -> dto(a, now)).orElse(null), now.format(TIME));
    }

    @Transactional
    public String checkIn(String id) {
        var pasien = current.requirePasienId(); validasiId(id);
        var idJadwal = antrean.jadwalTiketPasien(id, pasien).orElseThrow(this::tidakAda);
        var sesi = jadwal.findForUpdate(idJadwal).orElseThrow(this::tidakAda);
        var tiket = antrean.findForUpdate(id).orElseThrow(this::tidakAda);
        if (!tiket.getPasien().getIdPasien().equals(pasien)) throw tidakAda();
        if (!tiket.getJadwal().getIdJadwal().equals(idJadwal)) throw new CheckInException("Jadwal tiket berubah. Silakan muat ulang antrean.");
        if (!tiket.getStatusAntrean().equals("Terdaftar") || tiket.getWaktuCheckIn() != null)
            throw new CheckInException("Check-in ditolak. Tiket harus berstatus Terdaftar dan belum pernah check-in.");
        var now = LocalDateTime.now(clock); // Baca waktu setelah memperoleh kedua kunci.
        if (!CheckInPolicy.waktuDibuka(now, sesi.getTanggal(), sesi.getJamMulai(), sesi.getJamSelesai()))
            throw new CheckInException("Check-in belum dapat dilakukan. Hanya pada hari kunjungan, mulai 30 menit sebelum praktik sampai jam selesai.");
        tiket.setStatusAntrean("Hadir/Menunggu"); tiket.setWaktuCheckIn(now); antrean.flush();
        return tiket.getIdAntrean();
    }

    private Tiket dto(Antrean a, LocalDateTime now) {
        var j = a.getJadwal();
        return new Tiket(a.getIdAntrean(), a.getNomorAntrean(), a.getStatusAntrean(), JadwalDto.from(j),
                a.getWaktuCheckIn() == null ? null : a.getWaktuCheckIn().format(TIME),
                j.getTanggal().atTime(j.getJamMulai()).minusMinutes(30).format(TIME),
                j.getTanggal().atTime(j.getJamSelesai()).format(TIME),
                CheckInPolicy.waktuDibuka(now,j.getTanggal(),j.getJamMulai(),j.getJamSelesai()),
                CheckInPolicy.boleh(a.getStatusAntrean(),a.getWaktuCheckIn(),now,j.getTanggal(),j.getJamMulai(),j.getJamSelesai()));
    }
    private void validasiId(String id) { if (id == null || id.isBlank() || id.length() > 12) throw new IllegalArgumentException("ID tiket tidak valid"); }
    private NoSuchElementException tidakAda() { return new NoSuchElementException("Tiket tidak ditemukan"); }
}
