package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.JadwalPraktik;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JadwalPraktikRepository extends JpaRepository<JadwalPraktik, String> {
    @org.springframework.data.jpa.repository.Query("""
            select j from JadwalPraktik j join fetch j.dokter join fetch j.poli
            where j.tanggal = :tanggal
              and (:idPoli is null or j.poli.idPoli = :idPoli)
              and (:idDokter is null or j.dokter.idDokter = :idDokter)
            order by j.jamMulai, j.idJadwal
            """)
    java.util.List<JadwalPraktik> cariJadwal(
            @org.springframework.data.repository.query.Param("tanggal") java.time.LocalDate tanggal,
            @org.springframework.data.repository.query.Param("idPoli") String idPoli,
            @org.springframework.data.repository.query.Param("idDokter") String idDokter);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dokter", "poli"})
    java.util.Optional<JadwalPraktik> findOneByIdJadwal(String idJadwal);

    java.util.List<JadwalPraktik> findByTanggal(java.time.LocalDate tanggal);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select j from JadwalPraktik j where j.idJadwal = :id")
    java.util.Optional<JadwalPraktik> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
