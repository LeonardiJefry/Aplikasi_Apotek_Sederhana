package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.VerifikasiPasien;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface VerifikasiPasienRepository extends JpaRepository<VerifikasiPasien,String> {
    @Query("select v from VerifikasiPasien v join fetch v.petugas where v.pasien.idPasien=:pasien order by v.waktuVerifikasi desc,v.idVerifikasi desc")
    List<VerifikasiPasien> terbaru(String pasien,Pageable halaman);
}
