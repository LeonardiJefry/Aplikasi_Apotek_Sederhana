package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import java.sql.*;
import java.time.*;
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

@SpringBootTest
@ActiveProfiles("test")
public class BookingTransactionsTests {
    private static final String ID = "BKTEST0001";
    @Autowired JdbcTemplate jdbc;
    @Autowired BookingService service;
    @Autowired KlinikUserDetailsService users;
    @Autowired Clock clock;

    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001',?,'Uji','08:00','14:00',1,1)", ID, LocalDate.now(clock).plusDays(2));
    }
    @AfterEach void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS uc02_fail_quota");
        jdbc.update("DELETE FROM Antrean WHERE idJadwal=?", ID);
        jdbc.update("DELETE FROM JadwalPraktik WHERE idJadwal=?", ID);
        SecurityContextHolder.clearContext();
    }

    @Test void twoPatientsCompetingForLastSlotHaveExactlyOneWinner() throws Exception {
        var results = compete("pasien", "BKTESTKEY001", "pasien2", "BKTESTKEY002");
        assertThat(results).filteredOn(r -> r.equals("OK")).hasSize(1);
        assertThat(results).filteredOn(r -> r.equals("FULL")).hasSize(1);
        assertThat(count()).isEqualTo(1);
        assertThat(quota()).isZero();
    }

    @Test void simultaneousReplayOfSameDraftCreatesOnlyOneTicket() throws Exception {
        var results = compete("pasien2", "BKTESTKEY001", "pasien2", "BKTESTKEY001");
        assertThat(results).containsExactly("OK", "OK");
        assertThat(count()).isEqualTo(1);
        assertThat(quota()).isZero();
        assertThat(jdbc.queryForObject("SELECT nomorAntrean FROM Antrean WHERE idJadwal=?", Integer.class, ID)).isEqualTo(1);
    }

    @Test void quotaWriteFailureRollsBackAnAlreadyInsertedTicketAndRetryWorks() {
        authenticate("pasien2");
        jdbc.execute("CREATE TRIGGER uc02_fail_quota BEFORE UPDATE ON JadwalPraktik FOR EACH ROW CALL \"id.ac.ub.apotek.BookingTransactionsTests$FailQuotaUpdate\"");
        assertThatThrownBy(() -> service.prosesBooking(ID, "BKTESTKEY001")).isInstanceOf(DataAccessException.class);
        assertThat(count()).isZero();
        assertThat(quota()).isEqualTo(1);
        jdbc.execute("DROP TRIGGER uc02_fail_quota");
        var retry = service.prosesBooking(ID, "BKTESTKEY001");
        assertThat(retry.nomorAntrean()).isEqualTo(1);
        assertThat(count()).isEqualTo(1);
        assertThat(quota()).isZero();
    }

    @Test void nextNumberNeverReusesCancelledOrFinishedTicketNumbers() {
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('BKOLDCANCEL1','PAS-001',?,7,?,'Dibatalkan')", ID, LocalDateTime.now(clock));
        authenticate("pasien2");
        var result = service.prosesBooking(ID, "BKTESTKEY001");
        assertThat(result.nomorAntrean()).isEqualTo(8);
        assertThat(result.statusAntrean()).isEqualTo("Terdaftar");
        assertThat(result.jadwal().sisaKuota()).isZero();
    }

    private java.util.List<String> compete(String firstUser, String firstKey, String secondUser, String secondKey) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> attempt(firstUser, firstKey, ready, start));
            var second = pool.submit(() -> attempt(secondUser, secondKey, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }
    private String attempt(String user, String key, CountDownLatch ready, CountDownLatch start) throws Exception {
        try {
            authenticate(user); ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timeout");
            try { service.prosesBooking(ID, key); return "OK"; }
            catch (BookingException ex) { return ex.reason().name(); }
        } finally { SecurityContextHolder.clearContext(); }
    }
    private void authenticate(String username) {
        var user = users.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM Antrean WHERE idJadwal=?", Integer.class, ID); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal=?", Integer.class, ID); }

    public static class FailQuotaUpdate implements org.h2.api.Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            if (ID.equals(newRow[0])) throw new SQLException("Synthetic quota update failure", "45000");
        }
    }
}
