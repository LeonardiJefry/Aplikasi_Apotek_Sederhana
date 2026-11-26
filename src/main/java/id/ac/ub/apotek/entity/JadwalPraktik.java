package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "JadwalPraktik")
public class JadwalPraktik {
    @Id
    @Column(name = "idJadwal", nullable = false, length = 10)
    private String idJadwal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idDokter", nullable = false)
    private Dokter dokter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPoli", nullable = false)
    private Poli poli;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "sesi", nullable = false, length = 30)
    private String sesi;

    @Column(name = "jamMulai", nullable = false, columnDefinition = "TIME")
    private LocalTime jamMulai;

    @Column(name = "jamSelesai", nullable = false, columnDefinition = "TIME")
    private LocalTime jamSelesai;

    @Column(name = "kuotaTotal", nullable = false)
    private int kuotaTotal;

    @Column(name = "sisaKuota", nullable = false)
    private int sisaKuota;

    protected JadwalPraktik() {}

    public JadwalPraktik(String idJadwal, Dokter dokter, Poli poli, LocalDate tanggal, String sesi, LocalTime jamMulai, LocalTime jamSelesai, int kuotaTotal, int sisaKuota) {
        this.idJadwal = idJadwal;
        this.dokter = dokter;
        this.poli = poli;
        this.tanggal = tanggal;
        this.sesi = sesi;
        this.jamMulai = jamMulai;
        this.jamSelesai = jamSelesai;
        this.kuotaTotal = kuotaTotal;
        this.sisaKuota = sisaKuota;
    }

    public String getIdJadwal() { return idJadwal; }
    public void setIdJadwal(String idJadwal) { this.idJadwal = idJadwal; }
    public Dokter getDokter() { return dokter; }
    public void setDokter(Dokter dokter) { this.dokter = dokter; }
    public Poli getPoli() { return poli; }
    public void setPoli(Poli poli) { this.poli = poli; }
    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }
    public String getSesi() { return sesi; }
    public void setSesi(String sesi) { this.sesi = sesi; }
    public LocalTime getJamMulai() { return jamMulai; }
    public void setJamMulai(LocalTime jamMulai) { this.jamMulai = jamMulai; }
    public LocalTime getJamSelesai() { return jamSelesai; }
    public void setJamSelesai(LocalTime jamSelesai) { this.jamSelesai = jamSelesai; }
    public int getKuotaTotal() { return kuotaTotal; }
    public void setKuotaTotal(int kuotaTotal) { this.kuotaTotal = kuotaTotal; }
    public int getSisaKuota() { return sisaKuota; }
    public void setSisaKuota(int sisaKuota) { this.sisaKuota = sisaKuota; }
}
