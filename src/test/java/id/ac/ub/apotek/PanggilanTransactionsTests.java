package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:panggilan-race;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test")
public class PanggilanTransactionsTests {
    private static final String ID="CALLOCK001", TICKET="CALLRACE0001";
    @Autowired JdbcTemplate jdbc;
    @Autowired PanggilanService service;
    @Autowired KlinikUserDetailsService users;
    @Autowired PanggilanTests.MutableClock clock;
    String token;
    @BeforeEach void fixture() {
        clock.at("2026-11-27T08:00:00");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001','2026-11-27','Uji','08:00','14:00',1,0)",ID);
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES (?,'PAS-001',?,1,CURRENT_TIMESTAMP,'Dilewati')",TICKET,ID);
        for(int n=1;n<=2;n++) jdbc.update("INSERT INTO RiwayatPelewatan (idRiwayatPelewatan,idAntrean,waktuPelewatan,urutanPelewatan,hasilPelewatan) VALUES (?,?,?,?,'Dilewati')","CALLHIST000"+n,TICKET,LocalDateTime.of(2026,11,27,7,50+n),n);
        auth(); service.panggil(ID); token=service.snapshot(ID).idPanggilan(); clock.advance(120);
    }
    @AfterEach void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS uc06_fail_quota"); jdbc.execute("DROP TRIGGER IF EXISTS uc06_fail_call");
        jdbc.update("DELETE FROM PanggilanAntrean WHERE idJadwal=?",ID);
        jdbc.update("DELETE FROM RiwayatPelewatan WHERE idAntrean IN (SELECT idAntrean FROM Antrean WHERE idJadwal=?)",ID);
        jdbc.update("DELETE FROM Antrean WHERE idJadwal=?",ID); jdbc.update("DELETE FROM JadwalPraktik WHERE idJadwal=?",ID);
        SecurityContextHolder.clearContext();
    }
    @Test void simultaneousThirdSkipsRestoreOneQuotaAndOneHistoryEntry() throws Exception {
        assertThat(compete(()->service.lewati(ID,token,true),()->service.lewati(ID,token,true))).containsExactlyInAnyOrder("OK","CONFLICT");
        assertThat(status()).isEqualTo("Tidak Hadir"); assertThat(quota()).isEqualTo(1); assertThat(history()).isEqualTo(3);
    }
    @Test void attendanceRacingSkipHasOneWinnerAndNoMixedOutcome() throws Exception {
        assertThat(compete(()->service.hadir(ID,token),()->service.lewati(ID,token,true))).containsExactlyInAnyOrder("OK","CONFLICT");
        if(status().equals("Sedang Dilayani")) { assertThat(quota()).isZero(); assertThat(history()).isEqualTo(2); }
        else { assertThat(status()).isEqualTo("Tidak Hadir"); assertThat(quota()).isEqualTo(1); assertThat(history()).isEqualTo(3); }
    }
    @Test void concurrentCallsCreateOneTimerWithoutRestartingIt() throws Exception {
        jdbc.update("DELETE FROM PanggilanAntrean WHERE idJadwal=?",ID);
        assertThat(compete(()->service.panggil(ID),()->service.panggil(ID))).containsExactlyInAnyOrder("OK","CONFLICT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PanggilanAntrean WHERE idJadwal=?",Integer.class,ID)).isEqualTo(1);
        assertThat(service.snapshot(ID).sisaDetik()).isEqualTo(120); assertThat(history()).isEqualTo(2);
    }
    @Test void quotaFailureRollsBackHistoryStatusAndCallThenSameTokenCanRetry() {
        jdbc.execute("CREATE TRIGGER uc06_fail_quota BEFORE UPDATE ON JadwalPraktik FOR EACH ROW CALL \"id.ac.ub.apotek.PanggilanTransactionsTests$FailQuota\"");
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(DataAccessException.class);
        assertBefore(); jdbc.execute("DROP TRIGGER uc06_fail_quota"); service.lewati(ID,token,true);
        assertThat(status()).isEqualTo("Tidak Hadir"); assertThat(quota()).isEqualTo(1); assertThat(history()).isEqualTo(3);
    }
    @Test void failureStartingNextCallRollsBackWholeSkipIncludingQuota() {
        jdbc.update("UPDATE JadwalPraktik SET kuotaTotal=2 WHERE idJadwal=?",ID);
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('CALLRACE0002','PAS-002',?,2,CURRENT_TIMESTAMP,'Terdaftar')",ID);
        jdbc.execute("CREATE TRIGGER uc06_fail_call BEFORE UPDATE ON PanggilanAntrean FOR EACH ROW CALL \"id.ac.ub.apotek.PanggilanTransactionsTests$FailCall\"");
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(DataAccessException.class);
        assertBefore(); jdbc.execute("DROP TRIGGER uc06_fail_call"); service.lewati(ID,token,true);
        assertThat(status()).isEqualTo("Tidak Hadir"); assertThat(quota()).isEqualTo(1); assertThat(history()).isEqualTo(3);
        assertThat(service.snapshot(ID).dipanggil().idAntrean()).isEqualTo("CALLRACE0002");
    }
    private void assertBefore() { assertThat(status()).isEqualTo("Dilewati"); assertThat(quota()).isZero(); assertThat(history()).isEqualTo(2); assertThat(service.snapshot(ID).idPanggilan()).isEqualTo(token); assertThat(service.snapshot(ID).dipanggil()).isNotNull(); }
    private List<String> compete(Runnable one,Runnable two) throws Exception {
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->attempt(one,ready,start)); var b=pool.submit(()->attempt(two,ready,start));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown(); return List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }
    private String attempt(Runnable work,CountDownLatch ready,CountDownLatch start)throws Exception {
        auth(); ready.countDown();
        try { if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Barrier timeout"); work.run(); return "OK"; }
        catch(PanggilanException ex) { return "CONFLICT"; } finally { SecurityContextHolder.clearContext(); }
    }
    private void auth() { var user=users.loadUserByUsername("loket"); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal=?",Integer.class,ID); }
    private int history() { return jdbc.queryForObject("SELECT COUNT(*) FROM RiwayatPelewatan WHERE idAntrean=?",Integer.class,TICKET); }
    private String status() { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?",String.class,TICKET); }
    public static class FailQuota implements org.h2.api.Trigger {
        public void fire(Connection c,Object[] oldRow,Object[] newRow)throws SQLException { if(ID.equals(newRow[0])) throw new SQLException("Synthetic UC06 quota failure","45000"); }
    }
    public static class FailCall implements org.h2.api.Trigger {
        public void fire(Connection c,Object[] oldRow,Object[] newRow)throws SQLException { if(ID.equals(newRow[0])) throw new SQLException("Synthetic UC06 call failure","45000"); }
    }
}
