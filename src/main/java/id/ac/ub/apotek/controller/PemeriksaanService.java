package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * UC-10 / PC-10 reqSelesai: Sedang Dilayani menjadi Selesai beserta waktuSelesai dalam satu transaksi.
 * Notifikasi loket dibuat sesudah commit oleh NotifikasiLoketService agar kegagalannya tidak membatalkan status.
 */
@Service
public class PemeriksaanService {
    private final JadwalPraktikRepository jadwal;
    private final AntreanRepository antrean;
    private final CurrentUserService current;
    private final Clock clock;
    public PemeriksaanService(JadwalPraktikRepository jadwal, AntreanRepository antrean, CurrentUserService current, Clock clock) {
        this.jadwal=jadwal; this.antrean=antrean; this.current=current; this.clock=clock;
    }

    public record Hasil(String idJadwal, String idAntrean, int nomorAntrean, String nama, LocalDateTime waktuSelesai) {
        public String nomorTampilan() { return DashboardDokterService.nomor(nomorAntrean); }
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Hasil selesai(String idJadwal, String idAntrean) {
        var idDokter=current.requireDokterId();
        if(idJadwal==null || idJadwal.isBlank() || idJadwal.length()>10) throw new IllegalArgumentException("ID jadwal tidak valid.");
        if(idAntrean==null || idAntrean.isBlank() || idAntrean.length()>12) throw new IllegalArgumentException("ID antrean tidak valid.");
        // Urutan kunci kontrak: JadwalPraktik lalu Antrean, sama seperti panggilan/pembatalan/check-in.
        var sesi=jadwal.findForUpdate(idJadwal).orElseThrow(PemeriksaanService::tidakAda);
        if(!sesi.getDokter().getIdDokter().equals(idDokter)) throw tidakAda();
        var tiket=antrean.findForUpdate(idAntrean).orElseThrow(PemeriksaanService::tidakAda);
        if(!idJadwal.equals(tiket.getJadwal().getIdJadwal())) throw tidakAda();
        if(!"Sedang Dilayani".equals(tiket.getStatusAntrean()))
            throw new PemeriksaanException("Hanya pasien berstatus Sedang Dilayani yang dapat ditandai selesai.",tiket.getStatusAntrean(),tiket.getNomorAntrean());
        // Kolom waktuSelesai DATETIME; dibulatkan ke detik agar nilai sebelum/sesudah reload sama.
        var waktu=LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        tiket.setStatusAntrean("Selesai"); tiket.setWaktuSelesai(waktu);
        antrean.flush();
        return new Hasil(idJadwal,tiket.getIdAntrean(),tiket.getNomorAntrean(),tiket.getPasien().getNama(),waktu);
    }

    private static NoSuchElementException tidakAda() { return new NoSuchElementException("Antrean tidak ditemukan pada jadwal Anda."); }
}
