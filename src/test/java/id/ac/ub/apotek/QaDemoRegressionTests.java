package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/**
 * QA-01: regresi lintas peran pada demo H2 mengikuti "Cek demo" 1-5 di docs/PROJECT.md.
 * Satu alur berurutan dengan jam terkendali memakai layanan asli UC-01 sampai UC-10 dan identitas dari sesi.
 * Basis data H2 khusus kelas ini; tidak memakai @Transactional karena tiap tindakan melakukan commit sendiri.
 */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:qa-01-demo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test")
class QaDemoRegressionTests {
    static final String JADWAL="QA-JDW-01";
    @Autowired JdbcTemplate jdbc;
    @Autowired PanggilanTests.MutableClock clock;
    @Autowired KlinikUserDetailsService users;
    @Autowired JadwalService jadwal;
    @Autowired BookingService booking;
    @Autowired StatusAntreanService status;
    @Autowired PembatalanService pembatalan;
    @Autowired CheckInService checkIn;
    @Autowired PanggilanService panggilan;
    @Autowired DashboardDokterService dashboard;
    @Autowired PemeriksaanService pemeriksaan;
    @Autowired NotifikasiLoketService notifikasi;
    @Autowired KuotaVaksinasiService vaksinasi;
    @Autowired VerifikasiPasienService verifikasi;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void crossRoleDemoChecklistRunsEndToEnd() throws Exception {
        // Jadwal uji sesi pagi hari ini: kuota 2 agar perebutan slot terakhir dapat diuji.
        clock.at("2026-11-27T06:50:00");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001','2026-11-27','QA','08:00','12:00',2,2)",JADWAL);

        // Cek demo 1: jadwal, booking, perebutan slot terakhir, kuota diperbarui.
        auth("pasien");
        assertThat(sisa(jadwal.ambilJadwal("2026-11-27","POL-001",null))).isEqualTo(2);
        var tiketA=booking.prosesBooking(JADWAL,"QAA000000001");
        assertThat(tiketA.nomorAntrean()).isEqualTo(1);
        assertThat(sisa(jadwal.ambilJadwal("2026-11-27","POL-001",null))).isEqualTo(1);
        var hasil=rebutSlotTerakhir();
        assertThat(hasil.values()).containsOnlyOnce("OK").contains("FULL");
        assertThat(sisa(jadwal.ambilJadwal("2026-11-27","POL-001",null))).isZero();
        var pemenang=hasil.entrySet().stream().filter(e->e.getValue().equals("OK")).findFirst().orElseThrow().getKey();
        String idPemenang=pemenang.equals("pasien")?"QAR000000001":"QAR000000002";

        // Cek demo 2: status sendiri, isolasi pasien lain, batas pembatalan satu jam.
        auth("pasien"); assertThat(status.snapshot(tiketA.idAntrean()).dipilih().nomorAntrean()).isEqualTo(1);
        auth("pasien2"); assertThatThrownBy(()->status.snapshot(tiketA.idAntrean())).isInstanceOf(NoSuchElementException.class);
        clock.at("2026-11-27T06:58:00"); auth(pemenang);
        pembatalan.batalkan(idPemenang,true);
        assertThat(state(idPemenang)).isEqualTo("Dibatalkan"); assertThat(quota()).isEqualTo(1);
        clock.at("2026-11-27T07:00:00"); auth("pasien");
        assertThatThrownBy(()->pembatalan.batalkan(tiketA.idAntrean(),true)).isInstanceOf(PembatalanException.class);
        assertThat(state(tiketA.idAntrean())).isEqualTo("Terdaftar");
        auth("pasien2"); var tiketB=booking.prosesBooking(JADWAL,"QAB000000001");
        assertThat(tiketB.nomorAntrean()).isEqualTo(3); assertThat(quota()).isZero();

        // Cek demo 3: check-in, panggil, hadir; dokter melihat pasien Sedang Dilayani.
        clock.at("2026-11-27T07:29:59"); auth("pasien");
        assertThatThrownBy(()->checkIn.checkIn(tiketA.idAntrean())).isInstanceOf(CheckInException.class);
        clock.at("2026-11-27T07:30:00"); checkIn.checkIn(tiketA.idAntrean());
        assertThat(state(tiketA.idAntrean())).isEqualTo("Hadir/Menunggu");
        clock.at("2026-11-27T08:00:00"); auth("loket");
        panggilan.panggil(JADWAL); var call=panggilan.snapshot(JADWAL);
        assertThat(call.dipanggil().idAntrean()).isEqualTo(tiketA.idAntrean());
        clock.at("2026-11-27T08:00:10"); panggilan.hadir(JADWAL,call.idPanggilan());
        assertThat(state(tiketA.idAntrean())).isEqualTo("Sedang Dilayani");

        // Cek demo 4: dashboard dokter, Selesai, notifikasi loket dan jumlah menunggu diperbarui.
        clock.at("2026-11-27T08:15:00"); auth("dokter");
        var papan=dashboard.snapshot(JADWAL);
        assertThat(papan.pelayanan().pasien().idAntrean()).isEqualTo(tiketA.idAntrean());
        assertThat(papan.pelayanan().dipanggil()).isEqualTo("08:00"); assertThat(papan.pelayanan().durasiMenit()).isEqualTo(14);
        assertThat(papan.aktif().antrean()).hasSize(3); assertThat(papan.panggilan().bolehPanggil()).isFalse();
        auth("dokter2"); assertThatThrownBy(()->dashboard.snapshot(JADWAL)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(()->pemeriksaan.selesai(JADWAL,tiketA.idAntrean())).isInstanceOf(NoSuchElementException.class);
        auth("dokter"); var selesai=pemeriksaan.selesai(JADWAL,tiketA.idAntrean());
        assertThat(notifikasi.kirim(selesai.idAntrean())).isEqualTo("Tertunda");
        assertThat(notifikasi.kirim(selesai.idAntrean())).isEqualTo("Tertunda");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Notifikasi WHERE idAntrean=?",Integer.class,tiketA.idAntrean())).isEqualTo(1);
        assertThatThrownBy(()->pemeriksaan.selesai(JADWAL,tiketA.idAntrean())).isInstanceOf(PemeriksaanException.class);
        papan=dashboard.snapshot(JADWAL);
        assertThat(papan.aktif().selesai()).isEqualTo(1); assertThat(papan.terakhir().statusNotifikasi()).isEqualTo("Tertunda");
        assertThat(papan.panggilan().bolehPanggil()).isTrue(); assertThat(papan.panggilan().berikutnya().idAntrean()).isEqualTo(tiketB.idAntrean());
        auth("loket"); assertThat(notifikasi.daftarUntukLoket(JADWAL)).extracting(NotifikasiLoketService.Item::nomorTampilan).containsExactly("A-001");

        // Cek demo 3 lanjutan: dua pelewatan, panggil ulang, Tidak Hadir pada percobaan ketiga dan kuota kembali.
        for(int ke=1;ke<=3;ke++) {
            panggilan.panggil(JADWAL); var aktif=panggilan.snapshot(JADWAL);
            assertThat(aktif.dipanggil().idAntrean()).isEqualTo(tiketB.idAntrean());
            clock.advance(120); panggilan.lewati(JADWAL,aktif.idPanggilan(),true);
            assertThat(state(tiketB.idAntrean())).isEqualTo(ke==3?"Tidak Hadir":"Dilewati");
            assertThat(quota()).isEqualTo(ke==3?1:0);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RiwayatPelewatan WHERE idAntrean=?",Integer.class,tiketB.idAntrean())).isEqualTo(3);
        auth("dokter"); papan=dashboard.snapshot(JADWAL);
        assertThat(papan.aktif().menunggu()).isZero(); assertThat(papan.panggilan().berikutnya()).isNull();

        // Cek demo 5: kuota vaksin admin, pencarian/riwayat/verifikasi loket, peran keliru ditolak.
        auth("admin"); var vak=vaksinasi.pilih("2026-11-27","Influenza Demo",null);
        assertThat(vak.jumlahTerdaftar()).isEqualTo(56);
        assertThatThrownBy(()->vaksinasi.simpan(vak.idJadwalVaksinasi(),"2026-11-27","Influenza Demo","55",vak.versi())).isInstanceOf(IllegalStateException.class);
        var penuh=vaksinasi.simpan(vak.idJadwalVaksinasi(),"2026-11-27","Influenza Demo","56",vak.versi());
        assertThat(penuh.sisaKuota()).isZero(); assertThat(penuh.pendaftaranDibuka()).isFalse();
        auth("loket"); assertThat(verifikasi.cari("NIK","0000000000000001",0).getContent()).extracting(VerifikasiPasienService.Hasil::idPasien).containsExactly("PAS-001");
        var detail=verifikasi.detail("PAS-001",0,0);
        assertThat(detail.kunjungan().getContent()).extracting(VerifikasiPasienService.Riwayat::idAntrean).contains(tiketA.idAntrean());
        verifikasi.verifikasi("PAS-001",UUID.randomUUID().toString(),detail.sidikIdentitas(),true);
        assertThat(verifikasi.detail("PAS-001",0,0).terverifikasi()).isTrue();
        auth("pasien"); assertThatThrownBy(()->verifikasi.cari("NIK","0000000000000001",0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->dashboard.snapshot(null)).isInstanceOf(AccessDeniedException.class);
        auth("dokter"); assertThatThrownBy(()->vaksinasi.simpan(vak.idJadwalVaksinasi(),"2026-11-27","Influenza Demo","90",penuh.versi())).isInstanceOf(AccessDeniedException.class);
        auth("admin"); assertThatThrownBy(()->panggilan.panggil(JADWAL)).isInstanceOf(AccessDeniedException.class);
    }

    /** Dua pasien berebut slot terakhir pada saat bersamaan; hasil per username: OK atau alasan BookingException. */
    private Map<String,String> rebutSlotTerakhir() throws Exception {
        var mulai=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            var hasil=new ConcurrentHashMap<String,String>(); var tugas=new ArrayList<Future<?>>();
            for(var entry:Map.of("pasien","QAR000000001","pasien2","QAR000000002").entrySet()) tugas.add(pool.submit(()->{
                try { auth(entry.getKey()); mulai.await(); booking.prosesBooking(JADWAL,entry.getValue()); hasil.put(entry.getKey(),"OK"); }
                catch(BookingException ex) { hasil.put(entry.getKey(),ex.reason().name()); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { SecurityContextHolder.clearContext(); }
                return null;
            }));
            mulai.countDown();
            for(var t:tugas) t.get(20,TimeUnit.SECONDS);
            return hasil;
        } finally { pool.shutdownNow(); }
    }
    private int sisa(List<JadwalDto> daftar) { return daftar.stream().filter(j->j.idJadwal().equals(JADWAL)).findFirst().orElseThrow().sisaKuota(); }
    private String state(String id) { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?",String.class,id); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal=?",Integer.class,JADWAL); }
    private void auth(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
}
