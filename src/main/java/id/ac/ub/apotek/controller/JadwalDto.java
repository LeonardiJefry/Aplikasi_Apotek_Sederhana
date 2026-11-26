package id.ac.ub.apotek.controller;

import id.ac.ub.apotek.entity.JadwalPraktik;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Snapshot publik jadwal; tidak memuat identitas pasien atau entity JPA. */
public record JadwalDto(String idJadwal, String idDokter, String dokter, String spesialisasi,
        String idPoli, String poli, LocalDate tanggal, String sesi, String jamMulai,
        String jamSelesai, int kuotaTotal, int sisaKuota, String status) {
    public static JadwalDto from(JadwalPraktik jadwal) {
        var jam = DateTimeFormatter.ofPattern("HH:mm");
        return new JadwalDto(jadwal.getIdJadwal(), jadwal.getDokter().getIdDokter(),
                jadwal.getDokter().getNama(), jadwal.getDokter().getSpesialisasi(),
                jadwal.getPoli().getIdPoli(), jadwal.getPoli().getNamaPoli(), jadwal.getTanggal(),
                jadwal.getSesi(), jadwal.getJamMulai().format(jam), jadwal.getJamSelesai().format(jam),
                jadwal.getKuotaTotal(), jadwal.getSisaKuota(),
                jadwal.getSisaKuota() == 0 ? "Penuh" : "Tersedia");
    }

    public boolean tersedia() { return sisaKuota > 0; }
    public String inisial() {
        return java.util.Arrays.stream(dokter.replaceFirst("(?i)^drg?\\.\\s*", "").split("\\s+"))
                .filter(s -> !s.isBlank()).limit(2).map(s -> s.substring(0, 1))
                .collect(java.util.stream.Collectors.joining()).toUpperCase(java.util.Locale.ROOT);
    }
}
