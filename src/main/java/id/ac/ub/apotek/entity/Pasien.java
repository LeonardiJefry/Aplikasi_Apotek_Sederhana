package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Pasien")
public class Pasien {
    @Id
    @Column(name = "idPasien", nullable = false, length = 10)
    private String idPasien;

    @Column(name = "nik", nullable = false, length = 16, columnDefinition = "CHAR(16)")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) // PDM CHAR(16); divalidasi sama di H2/MySQL/PostgreSQL.
    private String nik;

    @Column(name = "noRekamMedis", nullable = false, length = 20)
    private String noRekamMedis;

    @Column(name = "nama", nullable = false, length = 100)
    private String nama;

    @Column(name = "tanggalLahir", nullable = false)
    private LocalDate tanggalLahir;

    protected Pasien() {}

    public Pasien(String idPasien, String nik, String noRekamMedis, String nama, LocalDate tanggalLahir) {
        this.idPasien = idPasien;
        this.nik = nik;
        this.noRekamMedis = noRekamMedis;
        this.nama = nama;
        this.tanggalLahir = tanggalLahir;
    }

    public String getIdPasien() { return idPasien; }
    public void setIdPasien(String idPasien) { this.idPasien = idPasien; }
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getNoRekamMedis() { return noRekamMedis; }
    public void setNoRekamMedis(String noRekamMedis) { this.noRekamMedis = noRekamMedis; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public LocalDate getTanggalLahir() { return tanggalLahir; }
    public void setTanggalLahir(LocalDate tanggalLahir) { this.tanggalLahir = tanggalLahir; }
}
