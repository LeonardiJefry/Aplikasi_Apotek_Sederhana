package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "RiwayatPelewatan")
public class RiwayatPelewatan {
    @Id
    @Column(name = "idRiwayatPelewatan", nullable = false, length = 12)
    private String idRiwayatPelewatan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idAntrean", nullable = false)
    private Antrean antrean;

    @Column(name = "waktuPelewatan", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime waktuPelewatan;

    @Column(name = "urutanPelewatan", nullable = false)
    private byte urutanPelewatan;

    @Column(name = "hasilPelewatan", nullable = false, length = 20)
    private String hasilPelewatan;

    protected RiwayatPelewatan() {}

    public RiwayatPelewatan(String idRiwayatPelewatan, Antrean antrean, LocalDateTime waktuPelewatan, byte urutanPelewatan, String hasilPelewatan) {
        this.idRiwayatPelewatan = idRiwayatPelewatan;
        this.antrean = antrean;
        this.waktuPelewatan = waktuPelewatan;
        this.urutanPelewatan = urutanPelewatan;
        this.hasilPelewatan = hasilPelewatan;
    }

    public String getIdRiwayatPelewatan() { return idRiwayatPelewatan; }
    public void setIdRiwayatPelewatan(String idRiwayatPelewatan) { this.idRiwayatPelewatan = idRiwayatPelewatan; }
    public Antrean getAntrean() { return antrean; }
    public void setAntrean(Antrean antrean) { this.antrean = antrean; }
    public LocalDateTime getWaktuPelewatan() { return waktuPelewatan; }
    public void setWaktuPelewatan(LocalDateTime waktuPelewatan) { this.waktuPelewatan = waktuPelewatan; }
    public byte getUrutanPelewatan() { return urutanPelewatan; }
    public void setUrutanPelewatan(byte urutanPelewatan) { this.urutanPelewatan = urutanPelewatan; }
    public String getHasilPelewatan() { return hasilPelewatan; }
    public void setHasilPelewatan(String hasilPelewatan) { this.hasilPelewatan = hasilPelewatan; }
}
