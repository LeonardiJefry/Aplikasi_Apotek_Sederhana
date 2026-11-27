package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.RiwayatPelewatan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiwayatPelewatanRepository extends JpaRepository<RiwayatPelewatan, String> {
    @org.springframework.data.jpa.repository.Query("select r from RiwayatPelewatan r join fetch r.antrean a where a.jadwal.idJadwal=:jadwal order by r.waktuPelewatan desc, r.urutanPelewatan desc")
    java.util.List<RiwayatPelewatan> riwayatSesi(@org.springframework.data.repository.query.Param("jadwal") String idJadwal);
    @org.springframework.data.jpa.repository.Query("select coalesce(max(r.urutanPelewatan),0) from RiwayatPelewatan r where r.antrean.idAntrean=:tiket")
    int jumlahPelewatan(@org.springframework.data.repository.query.Param("tiket") String idAntrean);
}
