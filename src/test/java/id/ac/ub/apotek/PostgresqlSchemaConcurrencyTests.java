package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

/**
 * DB-01: skema PostgreSQL, Row Level Security, constraint PDM dan regresi transaksi penguncian/kuota/panggilan/selesai
 * dengan beberapa koneksi bersamaan. Berjalan hanya bila KLINIK_PG_URL diisi (lokal atau CI).
 */
@SpringBootTest @Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named=PostgresqlSupport.ENV, matches=".+")
class PostgresqlSchemaConcurrencyTests {
    static final String SKEMA=PostgresqlSupport.skemaBaru("skema");
    @DynamicPropertySource static void pg(DynamicPropertyRegistry r) { PostgresqlSupport.daftarkan(r,SKEMA); }
    @AfterAll static void hapusSkema() throws Exception { PostgresqlSupport.hapus(SKEMA); }

    @Autowired JdbcTemplate jdbc;
    @Autowired PanggilanTests.MutableClock clock;
    @Autowired KlinikUserDetailsService users;
    @Autowired BookingService booking;
    @Autowired PanggilanService panggilan;
    @Autowired PemeriksaanService pemeriksaan;

    @BeforeEach void jam() { clock.at("2026-11-27T07:00:00"); }
    @AfterEach void bersih() { SecurityContextHolder.clearContext(); }

    @Test void migrationsCreateTwelveTablesInPrivateSchemaWithRowLevelSecurity() {
        assertThat(jdbc.queryForObject("SELECT max(version::int) FROM flyway_schema_history WHERE success",Integer.class)).isEqualTo(6);
        var tabel=jdbc.queryForList("""
                SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname=? AND c.relkind='r' AND c.relname<>'flyway_schema_history' AND c.relrowsecurity
                ORDER BY c.relname""",String.class,SKEMA);
        assertThat(tabel).containsExactly("antrean","demodataset","dokter","jadwalpraktik","jadwalvaksinasi","notifikasi",
                "panggilanantrean","pasien","pengguna","poli","riwayatpelewatan","verifikasipasien");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename='pasien'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema=? AND table_name='panggilanantrean' AND column_name='waktupanggil'",String.class,SKEMA))
                .isEqualTo("timestamp without time zone");
        assertThat(jdbc.queryForObject("SELECT datetime_precision FROM information_schema.columns WHERE table_schema=? AND table_name='panggilanantrean' AND column_name='waktupanggil'",Integer.class,SKEMA)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pengguna",Integer.class)).isEqualTo(6);
    }

    @Test void pdmConstraintsAreEnforcedByPostgresql() {
        assertThatThrownBy(()->jdbc.update("INSERT INTO Pasien (idPasien,nik,noRekamMedis,nama,tanggalLahir) VALUES ('PG-X1','12345678901234AB','PG-RM-X1','Uji','1990-01-01')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("UPDATE Antrean SET statusAntrean='Sedang Diperiksa' WHERE idAntrean='ANT-DEMO0004'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("UPDATE JadwalPraktik SET sisaKuota=kuotaTotal+1 WHERE idJadwal='JDW-001'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('PG-DUPLIKAT','PAS-001','JDW-001',1,CURRENT_TIMESTAMP,'Terdaftar')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO Notifikasi (idNotifikasi,idAntrean,statusKirim) VALUES ('PG-N1','ANT-DEMO0007','Dibaca')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0004'",String.class)).isEqualTo("Menunggu");
    }

    @Test void concurrentBookingNeverOversellsQuotaOrDuplicatesNumbers() throws Exception {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sisaKuota,kuotaTotal,sesi,jamMulai,jamSelesai) VALUES ('PG-RACE1','DOK-001','POL-001','2026-11-27',3,3,'Race','08:00','14:00')");
        var hasil=bersamaan(8,i->{ auth(i%2==0?"pasien":"pasien2"); booking.prosesBooking("PG-RACE1",String.format("PGRACE%06d",i)); });
        assertThat(hasil.stream().filter("OK"::equals)).hasSize(3);
        assertThat(hasil.stream().filter("BookingException"::equals)).hasSize(5);
        assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='PG-RACE1'",Integer.class)).isZero();
        assertThat(jdbc.queryForList("SELECT nomorAntrean FROM Antrean WHERE idJadwal='PG-RACE1' ORDER BY nomorAntrean",Integer.class)).containsExactly(1,2,3);
    }

    @Test void concurrentCallAndFinishSucceedExactlyOnce() throws Exception {
        clock.at("2026-11-27T09:00:00");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sisaKuota,kuotaTotal,sesi,jamMulai,jamSelesai) VALUES ('PG-CALL1','DOK-001','POL-001','2026-11-27',0,3,'Race','08:00','14:00')");
        for(int n=1;n<=3;n++) jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES (?,'PAS-001','PG-CALL1',?,CURRENT_TIMESTAMP,?)",
                "PGCALL00000"+n,n,n==1?"Sedang Dilayani":"Hadir/Menunggu");
        var selesai=bersamaan(4,i->{ auth("dokter"); pemeriksaan.selesai("PG-CALL1","PGCALL000001"); });
        assertThat(selesai).containsOnlyOnce("OK").contains("PemeriksaanException");
        var waktu=jdbc.queryForObject("SELECT waktuSelesai FROM Antrean WHERE idAntrean='PGCALL000001'",java.time.LocalDateTime.class);
        assertThat(waktu).isEqualTo(java.time.LocalDateTime.parse("2026-11-27T09:00:00"));
        var panggil=bersamaan(4,i->{ auth(i%2==0?"loket":"dokter"); panggilan.panggil("PG-CALL1"); });
        assertThat(panggil).containsOnlyOnce("OK").contains("PanggilanException");
        assertThat(jdbc.queryForObject("SELECT idAntrean FROM PanggilanAntrean WHERE idJadwal='PG-CALL1'",String.class)).isEqualTo("PGCALL000002");
    }

    interface Aksi { void jalankan(int i) throws Exception; }
    /** Menjalankan aksi pada beberapa thread sekaligus; hasil per thread: OK atau nama kelas exception. */
    private List<String> bersamaan(int jumlah,Aksi aksi) throws Exception {
        var mulai=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(jumlah);
        try {
            var tugas=new ArrayList<Future<String>>();
            for(int i=0;i<jumlah;i++) { int ke=i; tugas.add(pool.submit(()->{
                try { mulai.await(); aksi.jalankan(ke); return "OK"; }
                catch(Exception ex) { return ex.getClass().getSimpleName(); }
                finally { SecurityContextHolder.clearContext(); }
            })); }
            mulai.countDown();
            var hasil=new ArrayList<String>(); for(var t:tugas) hasil.add(t.get(30,TimeUnit.SECONDS));
            return hasil;
        } finally { pool.shutdownNow(); }
    }
    private void auth(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
}
