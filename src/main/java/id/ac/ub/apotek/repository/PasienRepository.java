package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Pasien;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasienRepository extends JpaRepository<Pasien, String> {
    org.springframework.data.domain.Page<Pasien> findByNamaContainingIgnoreCaseOrderByNamaAscIdPasienAsc(String nama,org.springframework.data.domain.Pageable halaman);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pasien p where p.idPasien=:id")
    java.util.Optional<Pasien> findForUpdate(String id);
    java.util.Optional<Pasien> findByNik(String nik);
    java.util.Optional<Pasien> findByNoRekamMedis(String noRekamMedis);
}
