package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.*;

/** Model bersama sesuai PDM; perubahan skema wajib melalui migrasi. */
@Entity
@Table(name = "Poli")
public class Poli {
    @Id
    @Column(name = "idPoli", nullable = false, length = 10)
    private String idPoli;

    @Column(name = "namaPoli", nullable = false, length = 50)
    private String namaPoli;

    protected Poli() {}

    public Poli(String idPoli, String namaPoli) {
        this.idPoli = idPoli;
        this.namaPoli = namaPoli;
    }

    public String getIdPoli() { return idPoli; }
    public void setIdPoli(String idPoli) { this.idPoli = idPoli; }
    public String getNamaPoli() { return namaPoli; }
    public void setNamaPoli(String namaPoli) { this.namaPoli = namaPoli; }
}
