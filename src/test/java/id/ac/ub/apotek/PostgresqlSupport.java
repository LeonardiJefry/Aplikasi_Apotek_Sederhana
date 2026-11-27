package id.ac.ub.apotek;

import java.sql.DriverManager;
import java.util.*;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * DB-01: konfigurasi uji PostgreSQL (lokal/CI). Tes PostgreSQL hanya berjalan bila KLINIK_PG_URL diisi, misalnya
 * jdbc:postgresql://127.0.0.1:55432/klinik, dengan KLINIK_PG_USER/KLINIK_PG_PASSWORD. Setiap kelas memakai skema
 * baru agar seed sintetis selalu segar; skema dihapus sesudah kelas selesai. Profil "test" tetap aktif untuk seed.
 */
final class PostgresqlSupport {
    static final String ENV="KLINIK_PG_URL";
    private PostgresqlSupport() {}

    static String skemaBaru(String nama) {
        return ("t_"+nama+"_"+UUID.randomUUID().toString().substring(0,8)).toLowerCase(Locale.ROOT);
    }

    static void daftarkan(DynamicPropertyRegistry r,String skema) {
        String url=System.getenv(ENV);
        r.add("spring.datasource.url",()->url+(url.contains("?")?"&":"?")+"currentSchema="+skema);
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.datasource.username",PostgresqlSupport::user);
        r.add("spring.datasource.password",PostgresqlSupport::password);
        r.add("spring.datasource.hikari.maximum-pool-size",()->"10");
        r.add("spring.flyway.locations",()->"classpath:db/migration-postgresql");
        r.add("spring.flyway.schemas",()->skema);
        r.add("spring.flyway.default-schema",()->skema);
        r.add("spring.flyway.create-schemas",()->"true");
        r.add("spring.jpa.properties.hibernate.default_schema",()->skema);
    }

    static void hapus(String skema) throws Exception {
        try(var c=DriverManager.getConnection(System.getenv(ENV),user(),password()); var s=c.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS "+skema+" CASCADE");
        }
    }
    private static String user() { return Objects.requireNonNullElse(System.getenv("KLINIK_PG_USER"),"postgres"); }
    private static String password() { return Objects.requireNonNullElse(System.getenv("KLINIK_PG_PASSWORD"),""); }
}
