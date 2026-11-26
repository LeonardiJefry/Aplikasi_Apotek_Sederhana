package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.*;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static id.ac.ub.apotek.controller.BookingException.Reason.*;

/** PC-02: jadwal -> antrean adalah urutan kunci bersama untuk seluruh penulisan. */
@Service
@Transactional(readOnly = true)
public class BookingService {
    private final JadwalPraktikRepository jadwal;
    private final AntreanRepository antrean;
    private final PasienRepository pasien;
    private final CurrentUserService current;
    private final Clock clock;

    public BookingService(JadwalPraktikRepository jadwal, AntreanRepository antrean,
                          PasienRepository pasien, CurrentUserService current, Clock clock) {
        this.jadwal = jadwal; this.antrean = antrean; this.pasien = pasien; this.current = current; this.clock = clock;
    }

    public record DataPasien(String nama, String noRekamMedis) {}
    public record Ringkasan(DataPasien pasien, JadwalDto jadwal) {}
    public record Tiket(String idAntrean, int nomorAntrean, String statusAntrean,
                        String waktuBooking, DataPasien pasien, JadwalDto jadwal) {}

    public Ringkasan buatRingkasan(String idJadwal) {
        var idPasien = current.requirePasienId();
        var selected = jadwal.findOneByIdJadwal(jadwalId(idJadwal))
                .orElseThrow(() -> new BookingException(NOT_FOUND, "Jadwal tidak ditemukan"));
        validasi(selected);
        return new Ringkasan(dataPasien(pasien.findById(idPasien)
                .orElseThrow(() -> new BookingException(NOT_FOUND, "Data pasien tidak ditemukan"))), JadwalDto.from(selected));
    }

    @Transactional
    public Tiket prosesBooking(String idJadwal, String idAntrean) {
        var idPasien = current.requirePasienId();
        var jadwalId = jadwalId(idJadwal);
        if (idAntrean == null || !idAntrean.matches("[A-Za-z0-9_-]{12}"))
            throw new BookingException(INVALID, "Data booking tidak lengkap. Silakan pilih jadwal kembali.");

        // Serialisasi semua penulis sesi sebelum membaca nomor berikutnya atau kuota.
        var selected = jadwal.findForUpdate(jadwalId)
                .orElseThrow(() -> new BookingException(NOT_FOUND, "Jadwal tidak ditemukan"));
        var existing = antrean.findForUpdate(idAntrean);
        if (existing.isPresent()) {
            pemilikDraft(existing.get(), idPasien, jadwalId);
            return dto(existing.get()); // Replay tetap aman walau kuota sudah nol / sesi sudah berakhir.
        }
        validasi(selected);
        var patient = pasien.findById(idPasien)
                .orElseThrow(() -> new BookingException(NOT_FOUND, "Data pasien tidak ditemukan"));
        int last = antrean.nomorTerakhir(jadwalId);
        if (last == Integer.MAX_VALUE)
            throw new BookingException(FULL, "Nomor antrean sesi sudah mencapai batas. Silakan pilih sesi lain.");

        var ticket = antrean.saveAndFlush(new Antrean(idAntrean, patient, selected, last + 1,
                LocalDateTime.now(clock), "Terdaftar", null, null));
        selected.setSisaKuota(selected.getSisaKuota() - 1);
        jadwal.flush(); // Kegagalan UPDATE juga me-rollback INSERT tiket di transaksi yang sama.
        return dto(ticket);
    }

    public Optional<Tiket> hasilDraft(String idJadwal, String idAntrean) {
        var idPasien = current.requirePasienId();
        return antrean.tiketPasien(idAntrean, idPasien).map(ticket -> {
            pemilikDraft(ticket, idPasien, jadwalId(idJadwal));
            return dto(ticket);
        });
    }

    public Tiket tiket(String idAntrean) {
        var idPasien = current.requirePasienId();
        if (idAntrean == null || idAntrean.length() > 12)
            throw new BookingException(NOT_FOUND, "Tiket tidak ditemukan");
        return antrean.tiketPasien(idAntrean, idPasien).map(this::dto)
                .orElseThrow(() -> new BookingException(NOT_FOUND, "Tiket tidak ditemukan"));
    }

    public List<JadwalDto> alternatif(String idJadwal) {
        current.requirePasienId();
        var id = jadwalId(idJadwal);
        var poli = jadwal.findById(id).map(j -> j.getPoli().getIdPoli()).orElse(null);
        var now = LocalDateTime.now(clock);
        return jadwal.alternatif(id, poli, now.toLocalDate(), now.toLocalTime(), PageRequest.of(0, 5))
                .stream().map(JadwalDto::from).toList();
    }

    private void validasi(JadwalPraktik selected) {
        var now = LocalDateTime.now(clock);
        if (!now.isBefore(selected.getTanggal().atTime(selected.getJamSelesai())))
            throw new BookingException(CLOSED, "Sesi praktik sudah berakhir. Silakan pilih jadwal lain.");
        if (selected.getSisaKuota() <= 0)
            throw new BookingException(FULL, "Kuota penuh. Silakan pilih sesi lain.");
    }

    private void pemilikDraft(Antrean ticket, String idPasien, String idJadwal) {
        if (!ticket.getPasien().getIdPasien().equals(idPasien) || !ticket.getJadwal().getIdJadwal().equals(idJadwal))
            throw new AccessDeniedException("Booking tidak dapat diakses");
    }

    private String jadwalId(String id) {
        if (id == null || id.isBlank() || id.strip().length() > 10)
            throw new BookingException(INVALID, "Data jadwal tidak valid. Silakan pilih jadwal kembali.");
        return id.strip();
    }

    private DataPasien dataPasien(Pasien p) { return new DataPasien(p.getNama(), p.getNoRekamMedis()); }
    private Tiket dto(Antrean a) {
        return new Tiket(a.getIdAntrean(), a.getNomorAntrean(), a.getStatusAntrean(),
                a.getTglBooking().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                dataPasien(a.getPasien()), JadwalDto.from(a.getJadwal()));
    }
}
