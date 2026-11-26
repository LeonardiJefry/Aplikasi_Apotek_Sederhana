package id.ac.ub.apotek;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaConstraintsTests {
    @Autowired JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {
        "UPDATE Pasien SET nik = 'ABC' WHERE idPasien = 'PAS-001'",
        "UPDATE Pasien SET nik = '0000000000000002' WHERE idPasien = 'PAS-001'",
        "UPDATE Pasien SET noRekamMedis = 'DEMO-RM-002' WHERE idPasien = 'PAS-001'",
        "UPDATE Dokter SET idPoli = 'MISSING' WHERE idDokter = 'DOK-001'",
        "UPDATE JadwalPraktik SET idDokter = 'MISSING' WHERE idJadwal = 'JDW-001'",
        "UPDATE JadwalPraktik SET idPoli = 'MISSING' WHERE idJadwal = 'JDW-001'",
        "UPDATE JadwalPraktik SET jamSelesai = jamMulai WHERE idJadwal = 'JDW-001'",
        "UPDATE JadwalPraktik SET kuotaTotal = -1 WHERE idJadwal = 'JDW-001'",
        "UPDATE JadwalPraktik SET sisaKuota = -1 WHERE idJadwal = 'JDW-001'",
        "UPDATE JadwalPraktik SET sisaKuota = 11 WHERE idJadwal = 'JDW-001'",
        "UPDATE Antrean SET nomorAntrean = 0 WHERE idAntrean = 'ANT-DEMO0001'",
        "UPDATE Antrean SET nomorAntrean = 2 WHERE idAntrean = 'ANT-DEMO0001'",
        "UPDATE Antrean SET idPasien = 'MISSING' WHERE idAntrean = 'ANT-DEMO0001'",
        "UPDATE Antrean SET idJadwal = 'MISSING' WHERE idAntrean = 'ANT-DEMO0001'",
        "UPDATE Antrean SET statusAntrean = 'INVALID' WHERE idAntrean = 'ANT-DEMO0001'",
        "UPDATE JadwalVaksinasi SET jumlahTerdaftar = 81 WHERE idJadwalVaksinasi = 'VAK-001'",
        "UPDATE JadwalVaksinasi SET kuota = -1 WHERE idJadwalVaksinasi = 'VAK-001'",
        "UPDATE Notifikasi SET statusKirim = 'INVALID' WHERE idNotifikasi = 'NTF-001'",
        "UPDATE Notifikasi SET idAntrean = 'MISSING' WHERE idNotifikasi = 'NTF-001'",
        "UPDATE RiwayatPelewatan SET urutanPelewatan = 0 WHERE idRiwayatPelewatan = 'RWT-DEMO0001'",
        "UPDATE RiwayatPelewatan SET urutanPelewatan = 2 WHERE idRiwayatPelewatan = 'RWT-DEMO0002'",
        "UPDATE RiwayatPelewatan SET hasilPelewatan = 'INVALID' WHERE idRiwayatPelewatan = 'RWT-DEMO0001'",
        "UPDATE RiwayatPelewatan SET idAntrean = 'MISSING' WHERE idRiwayatPelewatan = 'RWT-DEMO0001'",
        "UPDATE Pengguna SET peran = 'ADMIN' WHERE username = 'pasien'",
        "UPDATE Pengguna SET username = 'admin' WHERE username = 'loket'",
        "UPDATE Pengguna SET idPasien = 'MISSING' WHERE username = 'pasien'",
        "DELETE FROM Pasien WHERE idPasien = 'PAS-001'"
    })
    void databaseRejectsInvalidRelationsAndValues(String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
