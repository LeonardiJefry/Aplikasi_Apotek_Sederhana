package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "JadwalVaksinasi")
public class JadwalVaksinasi {
    @Id
    @Column(name = "idJadwalVaksinasi", nullable = false, length = 10)
    private String idJadwalVaksinasi;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "jenisVaksin", nullable = false, length = 50)
    private String jenisVaksin;

    @Column(name = "kuota", nullable = false)
    private int kuota;

    @Column(name = "jumlahTerdaftar", nullable = false)
    private int jumlahTerdaftar;

    @Column(name = "pendaftaranDibuka", nullable = false, columnDefinition = "BOOLEAN")
    private boolean pendaftaranDibuka;

    protected JadwalVaksinasi() {}

    public JadwalVaksinasi(String idJadwalVaksinasi, LocalDate tanggal, String jenisVaksin, int kuota, int jumlahTerdaftar, boolean pendaftaranDibuka) {
        this.idJadwalVaksinasi = idJadwalVaksinasi;
        this.tanggal = tanggal;
        this.jenisVaksin = jenisVaksin;
        this.kuota = kuota;
        this.jumlahTerdaftar = jumlahTerdaftar;
        this.pendaftaranDibuka = pendaftaranDibuka;
    }

    public String getIdJadwalVaksinasi() { return idJadwalVaksinasi; }
    public void setIdJadwalVaksinasi(String idJadwalVaksinasi) { this.idJadwalVaksinasi = idJadwalVaksinasi; }
    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }
    public String getJenisVaksin() { return jenisVaksin; }
    public void setJenisVaksin(String jenisVaksin) { this.jenisVaksin = jenisVaksin; }
    public int getKuota() { return kuota; }
    public void setKuota(int kuota) { this.kuota = kuota; }
    public int getJumlahTerdaftar() { return jumlahTerdaftar; }
    public void setJumlahTerdaftar(int jumlahTerdaftar) { this.jumlahTerdaftar = jumlahTerdaftar; }
    public boolean isPendaftaranDibuka() { return pendaftaranDibuka; }
    public void setPendaftaranDibuka(boolean pendaftaranDibuka) { this.pendaftaranDibuka = pendaftaranDibuka; }
}
