package id.ac.ub.apotek.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** DTO UC-03: tiket sendiri dan nomor/status publik dalam satu sesi praktik. */
public final class StatusAntreanDto {
    private StatusAntreanDto() {}
    public record Pilihan(String idAntrean, int nomorAntrean, String statusAntrean,
                          LocalDate tanggal, String poli, String sesi) {
        public String label() {
            return tanggal + " | " + poli + " | " + sesi + " | " + nomor(nomorAntrean) + " | " + statusAntrean;
        }
    }
    public record Baris(int nomorAntrean, String statusAntrean) {
        public String nomorTampilan() { return nomor(nomorAntrean); }
    }
    public record Detail(String idAntrean, int nomorAntrean, String statusAntrean, JadwalDto jadwal,
                         List<Integer> nomorSedangDilayani, Integer jumlahDiDepan, List<Baris> berikutnya,
                         boolean statusAkhir, boolean bolehBatal, boolean bolehCheckIn, String keterangan,
                         boolean bolehBukaBatal) {
        public String nomorTampilan() { return nomor(nomorAntrean); }
        public String nomorSekarang() {
            return nomorSedangDilayani.isEmpty() ? "—" : nomorSedangDilayani.stream()
                    .map(StatusAntreanDto::nomor).collect(Collectors.joining(", "));
        }
        public String sisaTampilan() { return jumlahDiDepan == null ? "—" : jumlahDiDepan + " orang"; }
    }
    public record Snapshot(List<Pilihan> tiket, Detail dipilih, String diperbaruiPada) {}
    private static String nomor(int nomor) { return String.format(Locale.ROOT, "%03d", nomor); }
}
