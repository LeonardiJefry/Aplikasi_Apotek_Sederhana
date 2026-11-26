package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PembatalanService {
    private final AntreanRepository antrean;
    private final JadwalPraktikRepository jadwal;
    private final CurrentUserService current;
    private final Clock clock;
    public PembatalanService(AntreanRepository antrean, JadwalPraktikRepository jadwal, CurrentUserService current, Clock clock) {
        this.antrean = antrean; this.jadwal = jadwal; this.current = current; this.clock = clock;
    }

    @Transactional
    public String batalkan(String idAntrean, boolean dikonfirmasi) {
        var pasien = current.requirePasienId();
        if (idAntrean == null || idAntrean.isBlank() || idAntrean.length() > 12)
            throw new IllegalArgumentException("ID tiket tidak valid");
        if (!dikonfirmasi) throw new IllegalArgumentException("Konfirmasi pembatalan diperlukan.");
        // Baca ID skalar agar entity tiket tidak masuk persistence context sebelum kunci jadwal.
        var idJadwal = antrean.jadwalTiketPasien(idAntrean, pasien).orElseThrow(this::tidakAda);
        var sesi = jadwal.findForUpdate(idJadwal).orElseThrow(this::tidakAda);
        var tiket = antrean.findForUpdate(idAntrean).orElseThrow(this::tidakAda);
        if (!tiket.getPasien().getIdPasien().equals(pasien)) throw tidakAda();
        if (!tiket.getJadwal().getIdJadwal().equals(idJadwal))
            throw new PembatalanException("Jadwal tiket berubah. Silakan muat ulang status antrean.");
        if (!PembatalanPolicy.statusDiizinkan(tiket.getStatusAntrean()))
            throw new PembatalanException(tiket.getStatusAntrean().equals("Dibatalkan")
                    ? "Antrean sudah dibatalkan. Kuota tidak dikembalikan lagi."
                    : "Antrean tidak dapat dibatalkan dari status " + tiket.getStatusAntrean() + ".");
        // Waktu diambil setelah menunggu seluruh kunci, bukan dari ringkasan/form klien.
        if (!PembatalanPolicy.waktuDiizinkan(LocalDateTime.now(clock), sesi.getTanggal().atTime(sesi.getJamMulai())))
            throw new PembatalanException("Pembatalan ditolak. Waktu tersisa harus lebih dari 60 menit sebelum jam praktik.");
        if (sesi.getSisaKuota() >= sesi.getKuotaTotal())
            throw new PembatalanException("Data kuota tidak konsisten. Silakan hubungi petugas loket.");
        tiket.setStatusAntrean("Dibatalkan");
        antrean.flush();
        sesi.setSisaKuota(sesi.getSisaKuota() + 1);
        jadwal.flush(); // Kegagalan pengembalian kuota me-rollback perubahan status tiket.
        return tiket.getIdAntrean();
    }
    private NoSuchElementException tidakAda() { return new NoSuchElementException("Tiket tidak ditemukan"); }
}
