package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Antrean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AntreanRepository extends JpaRepository<Antrean, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Antrean a where a.idAntrean = :id")
    java.util.Optional<Antrean> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);

    @org.springframework.data.jpa.repository.Query("select coalesce(max(a.nomorAntrean), 0) from Antrean a where a.jadwal.idJadwal = :id")
    int nomorTerakhir(@org.springframework.data.repository.query.Param("id") String idJadwal);

    @org.springframework.data.jpa.repository.Query("""
            select a from Antrean a join fetch a.pasien join fetch a.jadwal j
            join fetch j.dokter join fetch j.poli
            where a.idAntrean = :id and a.pasien.idPasien = :pasien
            """)
    java.util.Optional<Antrean> tiketPasien(@org.springframework.data.repository.query.Param("id") String id,
            @org.springframework.data.repository.query.Param("pasien") String idPasien);

    java.util.Optional<Antrean> findByIdAntreanAndPasien_IdPasien(String idAntrean, String idPasien);
    java.util.Optional<Antrean> findByIdAntreanAndJadwal_Dokter_IdDokter(String idAntrean, String idDokter);
}
