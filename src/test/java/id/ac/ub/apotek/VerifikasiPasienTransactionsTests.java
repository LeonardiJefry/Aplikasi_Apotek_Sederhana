package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.VerifikasiPasienService;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.sql.*;
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
public class VerifikasiPasienTransactionsTests {
    static final String ID="VERIFYTX01";
    @Autowired VerifikasiPasienService service;
    @Autowired KlinikUserDetailsService users;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO Pasien VALUES (?,'9999999999999999','VERIFY-TX-RM','Pasien Audit Uji','2000-01-01')",ID); auth();
    }
    @AfterEach void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS uc08_fail_audit"); jdbc.update("DELETE FROM VerifikasiPasien WHERE idPasien=?",ID);
        jdbc.update("DELETE FROM Pasien WHERE idPasien=?",ID); SecurityContextHolder.clearContext();
    }
    @Test void simultaneousDuplicateSubmissionCreatesOneAuditWithSameTime()throws Exception {
        var before=service.detail(ID,0,0); var token=UUID.randomUUID().toString();
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var one=pool.submit(()->attempt(token,before.sidikIdentitas(),ready,start));
            var two=pool.submit(()->attempt(token,before.sidikIdentitas(),ready,start));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(one.get(10,TimeUnit.SECONDS)).isEqualTo(two.get(10,TimeUnit.SECONDS));
        } finally { start.countDown(); }
        assertThat(count()).isEqualTo(1);
    }
    @Test void auditInsertFailureLeavesNoVerificationAndCanBeRetried() {
        var d=service.detail(ID,0,0); var token=UUID.randomUUID().toString();
        jdbc.execute("CREATE TRIGGER uc08_fail_audit BEFORE INSERT ON VerifikasiPasien FOR EACH ROW CALL \"id.ac.ub.apotek.VerifikasiPasienTransactionsTests$FailWrite\"");
        assertThatThrownBy(()->service.verifikasi(ID,token,d.sidikIdentitas(),true)).isInstanceOf(DataAccessException.class);
        assertThat(count()).isZero(); assertThat(service.detail(ID,0,0).terverifikasi()).isFalse();
        jdbc.execute("DROP TRIGGER uc08_fail_audit"); service.verifikasi(ID,token,d.sidikIdentitas(),true);
        assertThat(count()).isEqualTo(1); assertThat(service.detail(ID,0,0).terverifikasi()).isTrue();
    }
    @Test void repeatedReviewCreatesNewEventWhileExistingRecordRemainsImmutable() {
        var d=service.detail(ID,0,0);
        var first=service.verifikasi(ID,UUID.randomUUID().toString(),d.sidikIdentitas(),true);
        service.verifikasi(ID,UUID.randomUUID().toString(),d.sidikIdentitas(),true);
        assertThat(count()).isEqualTo(2);
        var read=service.verifikasi(ID,first.idVerifikasi(),d.sidikIdentitas(),true);
        assertThat(read).isEqualTo(first); assertThat(count()).isEqualTo(2);
    }
    private VerifikasiPasienService.Audit attempt(String token,String sidik,CountDownLatch ready,CountDownLatch start)throws Exception {
        try { auth(); ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Barrier timeout"); return service.verifikasi(ID,token,sidik,true); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM VerifikasiPasien WHERE idPasien=?",Integer.class,ID); }
    private void auth() { var u=users.loadUserByUsername("loket"); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.getAuthorities())); }
    public static class FailWrite implements org.h2.api.Trigger {
        @Override public void fire(Connection c,Object[] oldRow,Object[] newRow)throws SQLException { if(ID.equals(newRow[1])) throw new SQLException("Synthetic UC08 audit failure","45000"); }
    }
}
