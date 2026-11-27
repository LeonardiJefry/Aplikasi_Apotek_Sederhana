package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.JadwalVaksinasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface JadwalVaksinasiRepository extends JpaRepository<JadwalVaksinasi, String> {
    List<JadwalVaksinasi> findByTanggalAndJenisVaksinOrderByIdJadwalVaksinasi(LocalDate tanggal, String jenisVaksin);
    @Query("select distinct j.jenisVaksin from JadwalVaksinasi j order by j.jenisVaksin")
    List<String> jenisVaksin();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JadwalVaksinasi j where j.idJadwalVaksinasi=:id")
    Optional<JadwalVaksinasi> findForUpdate(String id);
}
