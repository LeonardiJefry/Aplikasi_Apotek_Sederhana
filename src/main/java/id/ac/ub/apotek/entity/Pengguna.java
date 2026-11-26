package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Pengguna")
public class Pengguna {
    @Id
    @Column(name = "idPengguna", nullable = false, length = 10)
    private String idPengguna;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "passwordHash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "peran", nullable = false, length = 20)
    private String peran;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "idPasien", nullable = true)
    private Pasien pasien;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "idDokter", nullable = true)
    private Dokter dokter;

    @Column(name = "aktif", nullable = false, columnDefinition = "BOOLEAN")
    private boolean aktif;

    protected Pengguna() {}

    public Pengguna(String idPengguna, String username, String passwordHash, String peran, Pasien pasien, Dokter dokter, boolean aktif) {
        this.idPengguna = idPengguna;
        this.username = username;
        this.passwordHash = passwordHash;
        this.peran = peran;
        this.pasien = pasien;
        this.dokter = dokter;
        this.aktif = aktif;
    }

    public String getIdPengguna() { return idPengguna; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getPeran() { return peran; }
    public Pasien getPasien() { return pasien; }
    public Dokter getDokter() { return dokter; }
    public boolean isAktif() { return aktif; }
}
