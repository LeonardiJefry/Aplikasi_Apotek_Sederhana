package id.ac.ub.apotek;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.*;

/** DB-01: alur QA-01 (Cek demo 1-5) dijalankan ulang pada PostgreSQL dengan migrasi db/migration-postgresql. */
@EnabledIfEnvironmentVariable(named=PostgresqlSupport.ENV, matches=".+")
class PostgresqlQaDemoTests extends QaDemoRegressionTests {
    static final String SKEMA=PostgresqlSupport.skemaBaru("qa");
    @DynamicPropertySource static void pg(DynamicPropertyRegistry r) { PostgresqlSupport.daftarkan(r,SKEMA); }
    @AfterAll static void hapusSkema() throws Exception { PostgresqlSupport.hapus(SKEMA); }
}
