package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.KuotaVaksinasiService;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
public class KuotaVaksinasiTransactionsTests {
    static final String ID="QUOTATX001",DATE="2026-11-27",TYPE="Vaksin Transaksi";
    @Autowired KuotaVaksinasiService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired KlinikUserDetailsService users;
    @BeforeEach void setup() { jdbc.update("INSERT INTO JadwalVaksinasi VALUES (?,?,?,80,56,true)",ID,LocalDate.parse(DATE),TYPE); auth(); }
    @AfterEach void cleanup() { jdbc.execute("DROP TRIGGER IF EXISTS uc07_fail_quota"); jdbc.update("DELETE FROM JadwalVaksinasi WHERE idJadwalVaksinasi=?",ID); SecurityContextHolder.clearContext(); }
    @Test void concurrentAdminEditsRejectOneStaleForm()throws Exception {
        String version=service.pilih(DATE,TYPE,ID).versi(); var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->attempt("56",version,ready,start)); var b=pool.submit(()->attempt("90",version,ready,start));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK","STALE");
        } finally { start.countDown(); }
        var result=service.pilih(DATE,TYPE,ID); assertThat(result.kuota()).isIn(56,90);
        assertThat(result.pendaftaranDibuka()).isEqualTo(result.sisaKuota()>0); assertThat(result.jumlahTerdaftar()).isEqualTo(56);
    }
    @Test void failedWriteRollsBackQuotaAndRegistrationStatusAndRetryWorks() {
        var version=service.pilih(DATE,TYPE,ID).versi();
        jdbc.execute("CREATE TRIGGER uc07_fail_quota BEFORE UPDATE ON JadwalVaksinasi FOR EACH ROW CALL \"id.ac.ub.apotek.KuotaVaksinasiTransactionsTests$FailWrite\"");
        assertThatThrownBy(()->service.simpan(ID,DATE,TYPE,"56",version)).isInstanceOf(DataAccessException.class);
        var before=service.pilih(DATE,TYPE,ID); assertThat(before.kuota()).isEqualTo(80); assertThat(before.pendaftaranDibuka()).isTrue();
        jdbc.execute("DROP TRIGGER uc07_fail_quota"); assertThat(service.simpan(ID,DATE,TYPE,"56",version).status()).isEqualTo("Penuh");
    }
    @Test void participantChangeInvalidatesOldFormEvenWhenQuotaStaysTheSame() {
        var before=service.pilih(DATE,TYPE,ID);
        jdbc.update("UPDATE JadwalVaksinasi SET jumlahTerdaftar=57 WHERE idJadwalVaksinasi=?",ID);
        assertThatThrownBy(()->service.simpan(ID,DATE,TYPE,"56",before.versi())).isInstanceOf(IllegalStateException.class);
        var now=service.pilih(DATE,TYPE,ID); assertThat(now.kuota()).isEqualTo(80); assertThat(now.jumlahTerdaftar()).isEqualTo(57);
    }
    private String attempt(String quota,String version,CountDownLatch ready,CountDownLatch start)throws Exception {
        try { auth(); ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Barrier timeout");
            try { service.simpan(ID,DATE,TYPE,quota,version); return "OK"; } catch(IllegalStateException ex) { return "STALE"; }
        } finally { SecurityContextHolder.clearContext(); }
    }
    private void auth() { var u=users.loadUserByUsername("admin"); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.getAuthorities())); }
    public static class FailWrite implements org.h2.api.Trigger {
        @Override public void fire(Connection c,Object[] oldRow,Object[] newRow)throws SQLException { if(ID.equals(newRow[0])) throw new SQLException("Synthetic UC07 write failure","45000"); }
    }
}
