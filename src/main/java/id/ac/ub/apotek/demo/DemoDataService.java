package id.ac.ub.apotek.demo;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dataset sintetis dipasang sekali, atomik, dan tidak mereset perubahan fitur pada restart. */
@Service
@Profile({"demo", "test", "mysql"})
@ConditionalOnProperty(name = "klinik.demo.enabled", havingValue = "true")
public class DemoDataService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String password;

    public DemoDataService(JdbcTemplate jdbc, PasswordEncoder encoder,
                           Clock clock, @Value("${klinik.demo.password:}") String password) {
        this.jdbc = jdbc; this.encoder = encoder;
        this.clock = clock; this.password = password;
    }

    @Transactional
    public void seed() {
        // Kunci baris yang sudah disediakan V2, termasuk saat dua instance startup bersamaan.
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT seeded FROM DemoDataset WHERE id = 'demo-v1' FOR UPDATE", Boolean.class))) return;
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_PASSWORD harus minimal 12 karakter dan maksimal 72 byte");
        }
        var today = LocalDate.now(clock);
        var previous = today.minusDays(1);
        jdbc.update("INSERT INTO Poli (idPoli,namaPoli) VALUES (?,?)", "POL-001", "Poli Umum");
        jdbc.update("INSERT INTO Poli (idPoli,namaPoli) VALUES (?,?)", "POL-002", "Poli Gigi");
        jdbc.update("INSERT INTO Dokter (idDokter,nama,spesialisasi,idPoli) VALUES (?,?,?,?)",
                "DOK-001", "dr. Demo Satu", "Dokter Umum", "POL-001");
        jdbc.update("INSERT INTO Dokter (idDokter,nama,spesialisasi,idPoli) VALUES (?,?,?,?)",
                "DOK-002", "drg. Demo Dua", "Dokter Gigi", "POL-002");
        jdbc.update("INSERT INTO Pasien (idPasien,nik,noRekamMedis,nama,tanggalLahir) VALUES (?,?,?,?,?)",
                "PAS-001", "0000000000000001", "DEMO-RM-001", "Pasien Demo Satu", LocalDate.of(2000,1,1));
        jdbc.update("INSERT INTO Pasien (idPasien,nik,noRekamMedis,nama,tanggalLahir) VALUES (?,?,?,?,?)",
                "PAS-002", "0000000000000002", "DEMO-RM-002", "Pasien Demo Dua", LocalDate.of(2001,2,2));
        schedule("JDW-001", "DOK-001", "POL-001", today, "Pagi", "08:00", "14:00", 10, 6);
        schedule("JDW-002", "DOK-002", "POL-002", today, "Siang", "13:00", "17:00", 5, 4);
        schedule("JDW-003", "DOK-001", "POL-001", today.plusDays(1), "Pagi", "08:00", "14:00", 10, 9);
        schedule("JDW-004", "DOK-001", "POL-001", previous, "Pagi", "08:00", "14:00", 10, 9);
        ticket("ANT-DEMO0001", "PAS-001", "JDW-001", 1, "Sedang Dilayani", today, true, false);
        ticket("ANT-DEMO0002", "PAS-002", "JDW-001", 2, "Hadir/Menunggu", today, true, false);
        ticket("ANT-DEMO0003", "PAS-001", "JDW-001", 3, "Dilewati", today, true, false);
        ticket("ANT-DEMO0004", "PAS-002", "JDW-001", 4, "Menunggu", today, false, false);
        ticket("ANT-DEMO0005", "PAS-002", "JDW-002", 1, "Terdaftar", today, false, false);
        ticket("ANT-DEMO0006", "PAS-001", "JDW-003", 1, "Terdaftar", today, false, false);
        ticket("ANT-DEMO0007", "PAS-001", "JDW-004", 1, "Selesai", previous, true, true);
        ticket("ANT-DEMO0008", "PAS-002", "JDW-004", 2, "Dibatalkan", previous, false, false);
        ticket("ANT-DEMO0009", "PAS-002", "JDW-004", 3, "Tidak Hadir", previous, false, false);
        jdbc.update("INSERT INTO RiwayatPelewatan (idRiwayatPelewatan,idAntrean,waktuPelewatan,urutanPelewatan,hasilPelewatan) VALUES (?,?,?,?,?)",
                "RWT-DEMO0001", "ANT-DEMO0003", today.atTime(8,15), 1, "Dilewati");
        for (int i = 1; i <= 3; i++) {
            jdbc.update("INSERT INTO RiwayatPelewatan (idRiwayatPelewatan,idAntrean,waktuPelewatan,urutanPelewatan,hasilPelewatan) VALUES (?,?,?,?,?)",
                    "RWT-DEMO000" + (i+1), "ANT-DEMO0009", previous.atTime(8,i*5), i,
                    i == 3 ? "Tidak Hadir" : "Dilewati");
        }
        jdbc.update("INSERT INTO Notifikasi (idNotifikasi,idAntrean,statusKirim,waktuKirim) VALUES (?,?,?,?)",
                "NTF-001", "ANT-DEMO0007", "Terkirim", previous.atTime(9,0));
        jdbc.update("INSERT INTO JadwalVaksinasi (idJadwalVaksinasi,tanggal,jenisVaksin,kuota,jumlahTerdaftar,pendaftaranDibuka) VALUES (?,?,?,?,?,?)",
                "VAK-001", today, "Influenza Demo", 80, 56, true);
        jdbc.update("INSERT INTO JadwalVaksinasi (idJadwalVaksinasi,tanggal,jenisVaksin,kuota,jumlahTerdaftar,pendaftaranDibuka) VALUES (?,?,?,?,?,?)",
                "VAK-002", today, "Hepatitis Demo", 10, 10, false);
        account("USR-001", "pasien", "PASIEN", "PAS-001", null);
        account("USR-002", "pasien2", "PASIEN", "PAS-002", null);
        account("USR-003", "loket", "PETUGAS_LOKET", null, null);
        account("USR-004", "admin", "ADMIN", null, null);
        account("USR-005", "dokter", "DOKTER", null, "DOK-001");
        account("USR-006", "dokter2", "DOKTER", null, "DOK-002");
        jdbc.update("UPDATE DemoDataset SET seeded = true WHERE id = 'demo-v1'");
    }

    private void account(String id, String username, String role, String pasien, String dokter) {
        jdbc.update("INSERT INTO Pengguna (idPengguna,username,passwordHash,peran,idPasien,idDokter,aktif) VALUES (?,?,?,?,?,?,?)",
                id, username, encoder.encode(password), role, pasien, dokter, true);
    }
    private void schedule(String id, String dokter, String poli, LocalDate date, String sesi,
                          String mulai, String selesai, int total, int remaining) {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,?,?,?,?,?,?,?,?)",
                id, dokter, poli, date, sesi, LocalTime.parse(mulai), LocalTime.parse(selesai), total, remaining);
    }
    private void ticket(String id, String pasien, String jadwal, int number, String status,
                        LocalDate date, boolean arrived, boolean done) {
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean,waktuCheckIn,waktuSelesai) VALUES (?,?,?,?,?,?,?,?)",
                id, pasien, jadwal, number, date.minusDays(1).atTime(10,0), status,
                arrived ? date.atTime(8,0) : null, done ? date.atTime(9,0) : null);
    }
}
