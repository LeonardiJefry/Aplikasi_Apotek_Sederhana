package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Audit append-only; identitas petugas dan waktu selalu dari server. */
@Entity @Table(name="VerifikasiPasien")
public class VerifikasiPasien {
    @Id @Column(name="idVerifikasi",length=36,nullable=false) private String idVerifikasi;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="idPasien",nullable=false) private Pasien pasien;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="idPetugas",nullable=false) private Pengguna petugas;
    @Column(name="waktuVerifikasi",nullable=false,columnDefinition="DATETIME(6)") private LocalDateTime waktuVerifikasi;
    @Column(name="statusVerifikasi",length=20,nullable=false) private String statusVerifikasi;
    @Column(name="sidikIdentitas",length=64,nullable=false,columnDefinition="CHAR(64)") private String sidikIdentitas;
    protected VerifikasiPasien() {}
    public VerifikasiPasien(String id,Pasien pasien,Pengguna petugas,LocalDateTime waktu,String sidik) {
        this.idVerifikasi=id; this.pasien=pasien; this.petugas=petugas;
        this.waktuVerifikasi=waktu.truncatedTo(ChronoUnit.MICROS); this.sidikIdentitas=sidik; this.statusVerifikasi="Terverifikasi";
    }
    public String getIdVerifikasi() { return idVerifikasi; }
    public Pasien getPasien() { return pasien; }
    public Pengguna getPetugas() { return petugas; }
    public LocalDateTime getWaktuVerifikasi() { return waktuVerifikasi; }
    public String getStatusVerifikasi() { return statusVerifikasi; }
    public String getSidikIdentitas() { return sidikIdentitas; }
}
