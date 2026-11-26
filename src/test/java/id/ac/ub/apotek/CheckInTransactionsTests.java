package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.sql.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:checkin-concurrent;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(CheckInTransactionsTests.ClockFixture.class)
@ActiveProfiles("test")
public class CheckInTransactionsTests {
    @TestConfiguration(proxyBeanMethods=false)
    public static class ClockFixture {
        @Bean @Primary Clock testCheckInClock() { return Clock.fixed(Instant.parse("2026-11-27T01:00:00Z"),ZoneId.of("Asia/Jakarta")); }
    }
    @Autowired CheckInService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired KlinikUserDetailsService users;
    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES ('CKTEST0001','DOK-001','POL-001','2026-11-27','Uji','08:00','14:00',1,0)");
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('CHECKINTST01','PAS-001','CKTEST0001',1,CURRENT_TIMESTAMP,'Terdaftar')");
    }
    @AfterEach void cleanup() { jdbc.execute("DROP TRIGGER IF EXISTS uc05_fail_attendance"); jdbc.update("DELETE FROM Antrean WHERE idJadwal='CKTEST0001'"); jdbc.update("DELETE FROM JadwalPraktik WHERE idJadwal='CKTEST0001'"); SecurityContextHolder.clearContext(); }
    @Test void twoConcurrentCheckInsHaveOneWinnerAndOneImmutableTimestamp() throws Exception {
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<String> action=()-> { try { authenticate(); ready.countDown(); if(!start.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Barrier timeout"); try {service.checkIn("CHECKINTST01");return "OK";}catch(CheckInException ex){return "CONFLICT";} } finally {SecurityContextHolder.clearContext();} };
            var first=pool.submit(action); var second=pool.submit(action); assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(java.util.List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK","CONFLICT");
        } finally {start.countDown();}
        assertThat(state()).isEqualTo("Hadir/Menunggu"); assertThat(recorded()).isEqualTo(LocalDateTime.of(2026,11,27,8,0));
        assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='CKTEST0001'",Integer.class)).isZero();
    }
    @Test void writeFailureRollsBackStatusAndTimeThenRetryWorks() {
        authenticate(); jdbc.execute("CREATE TRIGGER uc05_fail_attendance BEFORE UPDATE ON Antrean FOR EACH ROW CALL \"id.ac.ub.apotek.CheckInTransactionsTests$FailAttendance\"");
        assertThatThrownBy(()->service.checkIn("CHECKINTST01")).isInstanceOf(DataAccessException.class);
        assertThat(state()).isEqualTo("Terdaftar"); assertThat(recorded()).isNull();
        jdbc.execute("DROP TRIGGER uc05_fail_attendance"); service.checkIn("CHECKINTST01");
        assertThat(state()).isEqualTo("Hadir/Menunggu"); assertThat(recorded()).isNotNull();
    }
    @Test void repeatedActionKeepsFirstAttendanceTime() {
        authenticate(); service.checkIn("CHECKINTST01"); var original=recorded();
        assertThatThrownBy(()->service.checkIn("CHECKINTST01")).isInstanceOf(CheckInException.class); assertThat(recorded()).isEqualTo(original);
    }
    private void authenticate(){var user=users.loadUserByUsername("pasien");SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()));}
    private String state(){return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='CHECKINTST01'",String.class);}
    private LocalDateTime recorded(){return jdbc.queryForObject("SELECT waktuCheckIn FROM Antrean WHERE idAntrean='CHECKINTST01'",LocalDateTime.class);}
    public static class FailAttendance implements org.h2.api.Trigger {
        @Override public void fire(Connection c,Object[] oldRow,Object[] newRow)throws SQLException { if("CHECKINTST01".equals(newRow[0]))throw new SQLException("Synthetic attendance update failure","45000"); }
    }
}
