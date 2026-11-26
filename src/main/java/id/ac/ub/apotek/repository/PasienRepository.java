package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Pasien;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasienRepository extends JpaRepository<Pasien, String> {
    java.util.Optional<Pasien> findByNik(String nik);
    java.util.Optional<Pasien> findByNoRekamMedis(String noRekamMedis);
}
