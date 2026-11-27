package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Notifikasi;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotifikasiRepository extends JpaRepository<Notifikasi, String> {
    /** Satu notifikasi per tiket; bila data lama ganda, baris dengan ID terkecil dipakai. */
    Optional<Notifikasi> findFirstByAntrean_IdAntreanOrderByIdNotifikasi(String idAntrean);

    /**
     * Panel loket: tiket Selesai pada satu jadwal beserta status kirim (null bila belum tercatat).
     * Kolom: idAntrean, nomorAntrean, nama pasien, waktuSelesai, statusKirim. Tanpa NIK atau data akun.
     */
    @Query("""
            select a.idAntrean, a.nomorAntrean, p.nama, a.waktuSelesai,
                   (select min(n.statusKirim) from Notifikasi n where n.antrean = a)
            from Antrean a join a.pasien p
            where a.jadwal.idJadwal = :jadwal and a.statusAntrean = 'Selesai'
            order by a.waktuSelesai desc, a.nomorAntrean desc
            """)
    List<Object[]> selesaiPadaJadwal(@Param("jadwal") String idJadwal);
}
