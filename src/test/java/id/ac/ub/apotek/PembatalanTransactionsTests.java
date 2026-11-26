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
public class PembatalanTransactionsTests {
    private static final String ID = "CATEST0001";
    private static final String TICKET = "CANCELTEST01";
    @Autowired JdbcTemplate jdbc;
    @Autowired PembatalanService service;
    @Autowired BookingService booking;
    @Autowired KlinikUserDetailsService users;
    @Autowired Clock clock;
    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001',?,'Uji','08:00','14:00',1,0)", ID, LocalDate.now(clock).plusDays(2));
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES (?,'PAS-001',?,1,?,'Terdaftar')", TICKET, ID, LocalDateTime.now(clock));
    }
    @AfterEach void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS uc04_fail_quota"); jdbc.update("DELETE FROM Antrean WHERE idJadwal=?", ID); jdbc.update("DELETE FROM JadwalPraktik WHERE idJadwal=?", ID); SecurityContextHolder.clearContext();
    }

    @Test void concurrentDuplicateCancellationRestoresOneQuota() throws Exception {
        var results = compete(() -> cancel(TICKET), () -> cancel(TICKET));
        assertThat(results).containsExactlyInAnyOrder("OK", "CONFLICT");
        assertThat(quota()).isEqualTo(1); assertThat(status()).isEqualTo("Dibatalkan");
    }
    @Test void concurrentDifferentCancellationsDoNotLoseQuotaIncrements() throws Exception {
        jdbc.update("UPDATE JadwalPraktik SET kuotaTotal=2 WHERE idJadwal=?", ID);
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('CANCELTEST02','PAS-001',?,2,?,'Terdaftar')", ID, LocalDateTime.now(clock));
        assertThat(compete(() -> cancel(TICKET), () -> cancel("CANCELTEST02"))).containsExactly("OK", "OK");
        assertThat(quota()).isEqualTo(2);
    }
    @Test void quotaFailureRollsBackCancelledStatusAndRetryRestoresExactlyOnce() {
        authenticate();
        jdbc.execute("CREATE TRIGGER uc04_fail_quota BEFORE UPDATE ON JadwalPraktik FOR EACH ROW CALL \"id.ac.ub.apotek.PembatalanTransactionsTests$FailQuotaUpdate\"");
        assertThatThrownBy(() -> service.batalkan(TICKET, true)).isInstanceOf(DataAccessException.class);
        assertThat(status()).isEqualTo("Terdaftar"); assertThat(quota()).isZero();
        jdbc.execute("DROP TRIGGER uc04_fail_quota"); service.batalkan(TICKET, true);
        assertThat(status()).isEqualTo("Dibatalkan"); assertThat(quota()).isEqualTo(1);
        assertThatThrownBy(() -> service.batalkan(TICKET, true)).isInstanceOf(PembatalanException.class);
        assertThat(quota()).isEqualTo(1);
    }
    @Test void bookingRacingCancellationKeepsQuotaAndActiveTicketsConsistent() throws Exception {
        var results = compete(() -> cancel(TICKET), () -> {
            try { booking.prosesBooking(ID, "NEWBOOKING01"); return "BOOKED"; }
            catch (BookingException ex) { return ex.reason().name(); }
        });
        assertThat(results.get(0)).isEqualTo("OK"); assertThat(results.get(1)).isIn("BOOKED", "FULL");
        assertThat(status()).isEqualTo("Dibatalkan");
        int active = jdbc.queryForObject("SELECT COUNT(*) FROM Antrean WHERE idJadwal=? AND statusAntrean='Terdaftar'", Integer.class, ID);
        assertThat(quota() + active).isEqualTo(1); assertThat(quota()).isBetween(0,1);
    }
    private String cancel(String ticket) { try { service.batalkan(ticket, true); return "OK"; } catch (PembatalanException ex) { return "CONFLICT"; } }
    private java.util.List<String> compete(Callable<String> one, Callable<String> two) throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> attempt(one, ready, start)); var second = pool.submit(() -> attempt(two, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally { start.countDown(); }
    }
    private String attempt(Callable<String> action, CountDownLatch ready, CountDownLatch start) throws Exception {
        try { authenticate(); ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Barrier timeout"); return action.call(); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private void authenticate() { var user = users.loadUserByUsername("pasien"); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal=?", Integer.class, ID); }
    private String status() { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?", String.class, TICKET); }
    public static class FailQuotaUpdate implements org.h2.api.Trigger {
        @Override public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            if (ID.equals(newRow[0])) throw new SQLException("Synthetic cancellation quota failure", "45000");
        }
    }
}
