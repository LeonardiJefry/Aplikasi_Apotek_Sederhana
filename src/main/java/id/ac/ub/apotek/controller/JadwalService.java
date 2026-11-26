package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.CurrentUserService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PC-01: validasi filter dan snapshot kuota yang dibaca ulang dari database. */
@Service
@Transactional(readOnly = true)
public class JadwalService {
    private final JadwalPraktikRepository jadwal;
    private final PoliRepository poli;
    private final DokterRepository dokter;
    private final CurrentUserService current;

    public JadwalService(JadwalPraktikRepository jadwal, PoliRepository poli,
                         DokterRepository dokter, CurrentUserService current) {
        this.jadwal = jadwal; this.poli = poli; this.dokter = dokter; this.current = current;
    }

    public record PilihanPoli(String id, String nama) {}
    public record PilihanDokter(String id, String nama, String idPoli) {}
    public record PilihanFilter(List<PilihanPoli> poli, List<PilihanDokter> dokter) {}

    public PilihanFilter pilihanFilter() {
        current.requirePasienId();
        return new PilihanFilter(
                poli.findAll(Sort.by("namaPoli")).stream().map(p -> new PilihanPoli(p.getIdPoli(), p.getNamaPoli())).toList(),
                dokter.daftarDenganPoli().stream().map(d -> new PilihanDokter(d.getIdDokter(), d.getNama(), d.getPoli().getIdPoli())).toList());
    }

    public List<JadwalDto> ambilJadwal(String tanggal, String idPoli, String idDokter) {
        current.requirePasienId();
        var date = tanggal(tanggal);
        var poliId = id(idPoli);
        var dokterId = id(idDokter);
        if (poliId == null && dokterId == null)
            throw new IllegalArgumentException("Pilih poli atau dokter terlebih dahulu");
        if (poliId != null && !poli.existsById(poliId))
            throw new IllegalArgumentException("Poli tidak ditemukan");
        if (dokterId != null) {
            var selected = dokter.findById(dokterId)
                    .orElseThrow(() -> new IllegalArgumentException("Dokter tidak ditemukan"));
            if (poliId != null && !poliId.equals(selected.getPoli().getIdPoli()))
                throw new IllegalArgumentException("Dokter tidak sesuai dengan poli yang dipilih");
        }
        return jadwal.cariJadwal(date, poliId, dokterId).stream().map(JadwalDto::from).toList();
    }

    /** Titik sambung baca-saja ke UC-02; tidak membuat tiket atau mengubah kuota. */
    public JadwalDto pilihJadwal(String idJadwal) {
        current.requirePasienId();
        var selected = jadwal.findOneByIdJadwal(java.util.Optional.ofNullable(id(idJadwal)).orElse(""))
                .orElseThrow(() -> new NoSuchElementException("Jadwal tidak ditemukan"));
        if (selected.getSisaKuota() == 0)
            throw new IllegalStateException("Jadwal sudah penuh. Silakan pilih jadwal lain.");
        return JadwalDto.from(selected);
    }

    private LocalDate tanggal(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Tanggal kunjungan wajib dipilih");
        try {
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("format", value, 0);
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Tanggal kunjungan tidak valid. Gunakan YYYY-MM-DD.");
        }
    }

    private String id(String value) {
        if (value == null || value.isBlank()) return null;
        var normalized = value.strip();
        if (normalized.length() > 10) throw new IllegalArgumentException("Pilihan poli, dokter, atau jadwal tidak valid");
        return normalized;
    }
}
