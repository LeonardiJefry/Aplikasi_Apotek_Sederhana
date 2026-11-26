package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Notifikasi")
public class Notifikasi {
    @Id
    @Column(name = "idNotifikasi", nullable = false, length = 10)
    private String idNotifikasi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idAntrean", nullable = false)
    private Antrean antrean;

    @Column(name = "statusKirim", nullable = false, length = 20)
    private String statusKirim;

    @Column(name = "waktuKirim", nullable = true, columnDefinition = "DATETIME")
    private LocalDateTime waktuKirim;

    protected Notifikasi() {}

    public Notifikasi(String idNotifikasi, Antrean antrean, String statusKirim, LocalDateTime waktuKirim) {
        this.idNotifikasi = idNotifikasi;
        this.antrean = antrean;
        this.statusKirim = statusKirim;
        this.waktuKirim = waktuKirim;
    }

    public String getIdNotifikasi() { return idNotifikasi; }
    public void setIdNotifikasi(String idNotifikasi) { this.idNotifikasi = idNotifikasi; }
    public Antrean getAntrean() { return antrean; }
    public void setAntrean(Antrean antrean) { this.antrean = antrean; }
    public String getStatusKirim() { return statusKirim; }
    public void setStatusKirim(String statusKirim) { this.statusKirim = statusKirim; }
    public LocalDateTime getWaktuKirim() { return waktuKirim; }
    public void setWaktuKirim(LocalDateTime waktuKirim) { this.waktuKirim = waktuKirim; }
}
