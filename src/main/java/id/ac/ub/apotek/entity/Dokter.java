package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Dokter")
public class Dokter {
    @Id
    @Column(name = "idDokter", nullable = false, length = 10)
    private String idDokter;

    @Column(name = "nama", nullable = false, length = 100)
    private String nama;

    @Column(name = "spesialisasi", nullable = false, length = 100)
    private String spesialisasi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPoli", nullable = false)
    private Poli poli;

    protected Dokter() {}

    public Dokter(String idDokter, String nama, String spesialisasi, Poli poli) {
        this.idDokter = idDokter;
        this.nama = nama;
        this.spesialisasi = spesialisasi;
        this.poli = poli;
    }

    public String getIdDokter() { return idDokter; }
    public void setIdDokter(String idDokter) { this.idDokter = idDokter; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getSpesialisasi() { return spesialisasi; }
    public void setSpesialisasi(String spesialisasi) { this.spesialisasi = spesialisasi; }
    public Poli getPoli() { return poli; }
    public void setPoli(Poli poli) { this.poli = poli; }
}
