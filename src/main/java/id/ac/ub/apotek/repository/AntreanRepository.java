package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Antrean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AntreanRepository extends JpaRepository<Antrean, String> {
    interface Kehadiran {
        int getNomorAntrean();
        String getStatusAntrean();
        java.time.LocalDateTime getWaktuCheckIn();
    }
    @org.springframework.data.jpa.repository.Query("""
            select a.nomorAntrean as nomorAntrean,a.statusAntrean as statusAntrean,a.waktuCheckIn as waktuCheckIn
            from Antrean a where a.jadwal.idJadwal=:jadwal
              and a.statusAntrean in ('Terdaftar','Menunggu','Hadir/Menunggu','Dilewati')
            order by a.nomorAntrean
            """)
    java.util.List<Kehadiran> terdepanBelumDilayani(@org.springframework.data.repository.query.Param("jadwal") String idJadwal,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select a.jadwal.idJadwal from Antrean a where a.idAntrean = :id and a.pasien.idPasien = :pasien")
    java.util.Optional<String> jadwalTiketPasien(@org.springframework.data.repository.query.Param("id") String id,
            @org.springframework.data.repository.query.Param("pasien") String idPasien);

    @org.springframework.data.jpa.repository.Query("""
            select a from Antrean a join fetch a.jadwal j join fetch j.dokter join fetch j.poli
            where a.pasien.idPasien = :pasien
            order by case when a.statusAntrean in ('Selesai','Dibatalkan','Tidak Hadir') then 1 else 0 end,
                     j.tanggal, a.nomorAntrean desc, a.idAntrean
            """)
    java.util.List<Antrean> daftarTiketPasien(@org.springframework.data.repository.query.Param("pasien") String idPasien);

    /** Proyeksi antrean sesi tidak membawa identitas pasien atau ID tiket orang lain. */
    interface NomorStatus {
        int getNomorAntrean();
        String getStatusAntrean();
    }
    @org.springframework.data.jpa.repository.Query("""
            select a.nomorAntrean as nomorAntrean, a.statusAntrean as statusAntrean
            from Antrean a where a.jadwal.idJadwal = :jadwal order by a.nomorAntrean
            """)
    java.util.List<NomorStatus> nomorStatusSesi(@org.springframework.data.repository.query.Param("jadwal") String idJadwal);

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
