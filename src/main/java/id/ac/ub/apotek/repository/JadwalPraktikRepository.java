package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.JadwalPraktik;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JadwalPraktikRepository extends JpaRepository<JadwalPraktik, String> {
    java.util.List<JadwalPraktik> findByTanggal(java.time.LocalDate tanggal);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select j from JadwalPraktik j where j.idJadwal = :id")
    java.util.Optional<JadwalPraktik> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
