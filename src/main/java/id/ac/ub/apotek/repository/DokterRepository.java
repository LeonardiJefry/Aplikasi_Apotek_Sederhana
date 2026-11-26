package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Dokter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DokterRepository extends JpaRepository<Dokter, String> {
    @org.springframework.data.jpa.repository.Query("select d from Dokter d join fetch d.poli order by d.nama, d.idDokter")
    java.util.List<Dokter> daftarDenganPoli();
}
