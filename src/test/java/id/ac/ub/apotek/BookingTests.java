package id.ac.ub.apotek;

import id.ac.ub.apotek.boundary.BookingDraftStore;
import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.time.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingTests {
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired KlinikUserDetailsService users;
    @Autowired BookingService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(web).addFilters(security).build(); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void summaryUsesLoggedInPatientAndDoesNotWriteBeforeConfirmation() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking?idJadwal=JDW-003&idPasien=PAS-001", session);
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getContentAsString()).contains("Pasien Demo Dua", "DEMO-RM-002", "Lanjut ke Konfirmasi Data")
                .doesNotContain("DEMO-RM-001", "name=\"idPasien\"");
        assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session);
        assertThat(count()).isEqualTo(9);
        assertThat(quota()).isEqualTo(9);
    }

    @Test void confirmCreatesOwnedTicketAndReplayHasNoAdditionalEffects() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session).getContentAsString();
        var key = hidden(page, "bookingToken");
        var request = MockMvcRequestBuilders.post("/pasien/booking/konfirmasi").session(session)
                .param("bookingToken", key).param("_csrf", hidden(page, "_csrf"))
                .param("idPasien", "PAS-001").param("idJadwal", "JDW-004");
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(303);
        assertThat(response.getHeader("Location")).endsWith("/pasien/booking/tiket/" + key);
        assertThat(count()).isEqualTo(10);
        assertThat(quota()).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT idPasien FROM Antrean WHERE idAntrean=?", String.class, key)).isEqualTo("PAS-002");
        assertThat(jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?", String.class, key)).isEqualTo("Terdaftar");
        assertThat(jdbc.queryForObject("SELECT nomorAntrean FROM Antrean WHERE idAntrean=?", Integer.class, key)).isEqualTo(2);

        var replay = mvc.perform(request).andReturn().getResponse();
        assertThat(replay.getStatus()).isEqualTo(303);
        assertThat(replay.getHeader("Location")).isEqualTo(response.getHeader("Location"));
        assertThat(count()).isEqualTo(10);
        assertThat(quota()).isEqualTo(8);
        var receipt = get("/pasien/booking/tiket/" + key, session);
        assertThat(receipt.getContentAsString()).contains("Pasien Demo Dua", "DEMO-RM-002", "Terdaftar", "Lihat Status Pendaftaran");
        assertThat(get("/pasien/antrean/" + key, session).getStatus()).isEqualTo(200);
        assertThat(get("/pasien/booking/tiket/" + key, session("pasien")).getStatus()).isEqualTo(404);
        assertThat(get("/pasien/booking/tiket/" + key, session("admin")).getStatus()).isEqualTo(403);
    }

    @Test void quotaCanBecomeFullBetweenSummaryAndConfirmation() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session).getContentAsString();
        jdbc.update("UPDATE JadwalPraktik SET sisaKuota=0 WHERE idJadwal='JDW-003'");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) SELECT 'BK-ALT0001',idDokter,idPoli,DATEADD('DAY',1,tanggal),sesi,jamMulai,jamSelesai,2,2 FROM JadwalPraktik WHERE idJadwal='JDW-003'");
        em.clear();
        var response = post("/pasien/booking/konfirmasi", session, page);
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("Kuota penuh", "Sesi alternatif", "BK-ALT0001");
        assertThat(count()).isEqualTo(9);
        assertThat(quota()).isZero();
    }

    @Test void cancellingDraftDoesNotCancelOrCreateATicket() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session).getContentAsString();
        var cancelled = post("/pasien/booking/batal", session, page);
        assertThat(cancelled.getStatus()).isEqualTo(303);
        assertThat(cancelled.getHeader("Location")).endsWith("/pasien/jadwal");
        assertThat(post("/pasien/booking/konfirmasi", session, page).getStatus()).isEqualTo(400);
        assertThat(count()).isEqualTo(9);
        assertThat(quota()).isEqualTo(9);
    }

    @Test void staleCancelAfterConfirmationKeepsTheSavedBooking() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session).getContentAsString();
        var saved = post("/pasien/booking/konfirmasi", session, page);
        var cancel = post("/pasien/booking/batal", session, page);
        assertThat(cancel.getHeader("Location")).isEqualTo(saved.getHeader("Location"));
        assertThat(count()).isEqualTo(10);
        assertThat(quota()).isEqualTo(8);
    }

    @Test void draftCannotBeUsedInAnotherSessionEvenWithValidCsrf() throws Exception {
        var first = session("pasien");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", first).getContentAsString();
        var other = session("pasien2");
        var login = get("/login", other).getContentAsString();
        var response = mvc.perform(MockMvcRequestBuilders.post("/pasien/booking/konfirmasi").session(other)
                .param("bookingToken", hidden(page, "bookingToken")).param("_csrf", hidden(login, "_csrf"))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(count()).isEqualTo(9);
    }

    @Test void csrfAndRoleChecksProtectConfirmation() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/booking/konfirmasi?idJadwal=JDW-003", session).getContentAsString();
        assertThat(mvc.perform(MockMvcRequestBuilders.post("/pasien/booking/konfirmasi").session(session)
                .param("bookingToken", hidden(page, "bookingToken"))).andReturn().getResponse().getStatus()).isEqualTo(403);
        for (String role : java.util.List.of("admin", "loket", "dokter")) {
            var forbidden = session(role);
            assertThat(get("/pasien/booking?idJadwal=JDW-003", forbidden).getStatus()).isEqualTo(403);
            var csrf = hidden(get("/login", forbidden).getContentAsString(), "_csrf");
            assertThat(mvc.perform(MockMvcRequestBuilders.post("/pasien/booking/konfirmasi").session(forbidden)
                    .param("_csrf", csrf).param("bookingToken", "AAAAAAAAAAAA")).andReturn().getResponse().getStatus()).isEqualTo(403);
        }
        assertThat(count()).isEqualTo(9);
    }

    @Test void rejectsUnknownScheduleAndFinishedSession() throws Exception {
        var session = session("pasien2");
        assertThat(get("/pasien/booking?idJadwal=UNKNOWN", session).getStatus()).isEqualTo(404);
        assertThat(get("/pasien/booking?idJadwal=JDW-004", session).getStatus()).isEqualTo(409);
        assertThat(get("/pasien/booking?idJadwal=TOO-LONG-IDENTIFIER", session).getStatus()).isEqualTo(400);
        assertThat(get("/pasien/booking", session).getStatus()).isEqualTo(302);
        assertThat(count()).isEqualTo(9);
    }

    @Test void draftExpiresAtFifteenMinutesAndStorageIsBounded() {
        var instant = new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-11-26T00:00:00Z"));
        var clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return Clock.fixed(instant.get(), zone); }
            public Instant instant() { return instant.get(); }
        };
        var store = new BookingDraftStore(clock);
        var session = new MockHttpSession();
        var first = store.siapkan(session, "PAS-001", "J-0");
        assertThat(store.siapkan(session, "PAS-001", "J-0").token()).isEqualTo(first.token());
        instant.set(instant.get().plusSeconds(900));
        assertThatThrownBy(() -> store.ambil(session, first.token(), "PAS-001")).isInstanceOf(IllegalArgumentException.class);
        var oldest = store.siapkan(session, "PAS-001", "J-0");
        for (int i = 1; i <= 16; i++) store.siapkan(session, "PAS-001", "J-" + i);
        assertThatThrownBy(() -> store.ambil(session, oldest.token(), "PAS-001")).isInstanceOf(IllegalArgumentException.class);
    }

    private org.springframework.mock.web.MockHttpServletResponse get(String path, MockHttpSession session) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(path).session(session)).andReturn().getResponse();
    }
    private org.springframework.mock.web.MockHttpServletResponse post(String path, MockHttpSession session, String page) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path).session(session).param("bookingToken", hidden(page,"bookingToken"))
                .param("_csrf",hidden(page,"_csrf"))).andReturn().getResponse();
    }
    private MockHttpSession session(String username) {
        var user = users.loadUserByUsername(username);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
    private String hidden(String page, String name) {
        var matcher = Pattern.compile("name=\"" + name + "\"[^>]*value=\"([^\"]+)\"").matcher(page);
        assertThat(matcher.find()).as("hidden field " + name).isTrue();
        return matcher.group(1);
    }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-003'", Integer.class); }
}
