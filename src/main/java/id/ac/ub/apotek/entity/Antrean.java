package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Antrean")
public class Antrean {
    @Id
    @Column(name = "idAntrean", nullable = false, length = 12)
    private String idAntrean;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPasien", nullable = false)
    private Pasien pasien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idJadwal", nullable = false)
    private JadwalPraktik jadwal;

    @Column(name = "nomorAntrean", nullable = false)
    private int nomorAntrean;

    @Column(name = "tglBooking", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime tglBooking;

    @Column(name = "statusAntrean", nullable = false, length = 30)
    private String statusAntrean;

    @Column(name = "waktuCheckIn", nullable = true, columnDefinition = "DATETIME")
    private LocalDateTime waktuCheckIn;

    @Column(name = "waktuSelesai", nullable = true, columnDefinition = "DATETIME")
    private LocalDateTime waktuSelesai;

    protected Antrean() {}

    public Antrean(String idAntrean, Pasien pasien, JadwalPraktik jadwal, int nomorAntrean, LocalDateTime tglBooking, String statusAntrean, LocalDateTime waktuCheckIn, LocalDateTime waktuSelesai) {
        this.idAntrean = idAntrean;
        this.pasien = pasien;
        this.jadwal = jadwal;
        this.nomorAntrean = nomorAntrean;
        this.tglBooking = tglBooking;
        this.statusAntrean = statusAntrean;
        this.waktuCheckIn = waktuCheckIn;
        this.waktuSelesai = waktuSelesai;
    }

    public String getIdAntrean() { return idAntrean; }
    public void setIdAntrean(String idAntrean) { this.idAntrean = idAntrean; }
    public Pasien getPasien() { return pasien; }
    public void setPasien(Pasien pasien) { this.pasien = pasien; }
    public JadwalPraktik getJadwal() { return jadwal; }
    public void setJadwal(JadwalPraktik jadwal) { this.jadwal = jadwal; }
    public int getNomorAntrean() { return nomorAntrean; }
    public void setNomorAntrean(int nomorAntrean) { this.nomorAntrean = nomorAntrean; }
    public LocalDateTime getTglBooking() { return tglBooking; }
    public void setTglBooking(LocalDateTime tglBooking) { this.tglBooking = tglBooking; }
    public String getStatusAntrean() { return statusAntrean; }
    public void setStatusAntrean(String statusAntrean) { this.statusAntrean = statusAntrean; }
    public LocalDateTime getWaktuCheckIn() { return waktuCheckIn; }
    public void setWaktuCheckIn(LocalDateTime waktuCheckIn) { this.waktuCheckIn = waktuCheckIn; }
    public LocalDateTime getWaktuSelesai() { return waktuSelesai; }
    public void setWaktuSelesai(LocalDateTime waktuSelesai) { this.waktuSelesai = waktuSelesai; }
}
