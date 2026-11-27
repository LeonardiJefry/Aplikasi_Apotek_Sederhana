package id.ac.ub.apotek;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.*;

/** DB-01: UC-10 Selesai, notifikasi loket dan simulasi kegagalan dijalankan ulang pada PostgreSQL. */
@EnabledIfEnvironmentVariable(named=PostgresqlSupport.ENV, matches=".+")
class PostgresqlPemeriksaanTests extends PemeriksaanTests {
    static final String SKEMA=PostgresqlSupport.skemaBaru("selesai");
    @DynamicPropertySource static void pg(DynamicPropertyRegistry r) { PostgresqlSupport.daftarkan(r,SKEMA); }
    @AfterAll static void hapusSkema() throws Exception { PostgresqlSupport.hapus(SKEMA); }
}
