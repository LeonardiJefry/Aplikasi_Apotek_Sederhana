package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Antrean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AntreanRepository extends JpaRepository<Antrean, String> {
    java.util.Optional<Antrean> findByIdAntreanAndPasien_IdPasien(String idAntrean, String idPasien);
    java.util.Optional<Antrean> findByIdAntreanAndJadwal_Dokter_IdDokter(String idAntrean, String idDokter);
}
