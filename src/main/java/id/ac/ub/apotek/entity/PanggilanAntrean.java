package id.ac.ub.apotek.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Metadata UC-06; status panggilan terpisah dari delapan status tiket bersama. */
@Entity
@Table(name = "PanggilanAntrean")
public class PanggilanAntrean {
    @Id @Column(name="idJadwal", length=10, nullable=false)
    private String idJadwal;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="idAntrean", nullable=false)
    private Antrean antrean;
    @Column(name="idPanggilan", length=36, nullable=false, unique=true)
    private String idPanggilan;
    @Column(name="waktuPanggil", nullable=false, columnDefinition="DATETIME(6)")
    private LocalDateTime waktuPanggil;
    @Column(name="hasilPanggilan", length=20, nullable=false)
    private String hasilPanggilan;
    @Column(name="waktuTutup", columnDefinition="DATETIME(6)")
    private LocalDateTime waktuTutup;
    @Column(name="idPengguna", length=10, nullable=false)
    private String idPengguna;
    protected PanggilanAntrean() {}
    public PanggilanAntrean(String idJadwal) { this.idJadwal=idJadwal; }
    public void mulai(Antrean tiket, String token, LocalDateTime waktu, String pelaku) {
        antrean=tiket; idPanggilan=token; waktuPanggil=waktu.truncatedTo(ChronoUnit.MICROS); idPengguna=pelaku;
        hasilPanggilan="Menunggu"; waktuTutup=null;
    }
    public void tutup(String hasil, LocalDateTime waktu) { hasilPanggilan=hasil; waktuTutup=waktu.truncatedTo(ChronoUnit.MICROS); }
    public String getIdJadwal() { return idJadwal; }
    public Antrean getAntrean() { return antrean; }
    public String getIdPanggilan() { return idPanggilan; }
    public LocalDateTime getWaktuPanggil() { return waktuPanggil; }
    public String getHasilPanggilan() { return hasilPanggilan; }
    public LocalDateTime getWaktuTutup() { return waktuTutup; }
    public String getIdPengguna() { return idPengguna; }
    public boolean menunggu() { return "Menunggu".equals(hasilPanggilan); }
}
