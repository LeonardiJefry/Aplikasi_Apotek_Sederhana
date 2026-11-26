package id.ac.ub.apotek;

import id.ac.ub.apotek.boundary.PembatalanBoundary;
import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.repository.*;
import id.ac.ub.apotek.security.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.time.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PembatalanTests {
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired KlinikUserDetailsService users;
    @Autowired CurrentUserService current;
    @Autowired PembatalanService service;
    @Autowired AntreanRepository antrean;
    @Autowired JadwalPraktikRepository jadwal;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    MockMvc mvc;
    private static final String URL = "/pasien/antrean/ANT-DEMO0006/pembatalan";
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(web).addFilters(security).build(); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void previewAndReturningToStatusDoNotChangeTicketOrQuota() throws Exception {
        var session = session("pasien");
        var page = get(URL, session);
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(page.getContentAsString()).contains("cancel-dialog", "Batalkan Antrean?", "Ya, Batalkan", "method=\"post\"")
                .doesNotContain("name=\"idPasien\"");
        get("/pasien/antrean/ANT-DEMO0006", session);
        assertThat(status()).isEqualTo("Terdaftar"); assertThat(quota()).isEqualTo(9);
    }

    @Test void confirmedCancellationRestoresQuotaOnceAndUpdatesStatusAndScheduleSnapshots() throws Exception {
        var session = session("pasien"); var page = get(URL, session).getContentAsString();
        var response = post(URL, session, page, "ya");
        assertThat(response.getStatus()).isEqualTo(303);
        assertThat(response.getHeader("Location")).endsWith(URL + "/hasil");
        assertThat(status()).isEqualTo("Dibatalkan"); assertThat(quota()).isEqualTo(10);
        var result = get(URL + "/hasil", session);
        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getContentAsString()).contains("Antrean berhasil dibatalkan", "10 dari 10");
        assertThat(get("/pasien/antrean/ANT-DEMO0006/data", session).getContentAsString()).contains("\"statusAntrean\":\"Dibatalkan\"", "\"bolehBatal\":false");
        var date = jdbc.queryForObject("SELECT tanggal FROM JadwalPraktik WHERE idJadwal='JDW-003'", LocalDate.class);
        assertThat(get("/pasien/jadwal/data?tanggal=" + date + "&idPoli=POL-001", session).getContentAsString()).contains("\"sisaKuota\":10");
        assertThat(post(URL, session, page, "ya").getStatus()).isEqualTo(409);
        assertThat(quota()).isEqualTo(10);
        assertThat(get(URL, session).getStatus()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class)).isEqualTo(9);
    }

    @Test void csrfAndExplicitConfirmationAreRequiredAndSpoofedIdentityIsIgnored() throws Exception {
        var session = session("pasien"); var page = get(URL, session).getContentAsString();
        assertThat(mvc.perform(MockMvcRequestBuilders.post(URL).session(session).param("konfirmasi", "ya")).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(post(URL, session, page, "").getStatus()).isEqualTo(400);
        assertThat(post(URL, session, page, "tidak").getStatus()).isEqualTo(400);
        assertThat(quota()).isEqualTo(9);
        // Helper POST sengaja mengirim ID pasien/jadwal palsu; server tetap memakai sesi dan tiket di path.
        assertThat(post(URL, session, page, "ya").getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT idPasien FROM Antrean WHERE idAntrean='ANT-DEMO0006'", String.class)).isEqualTo("PAS-001");
        assertThat(quota()).isEqualTo(10);
    }

    @Test void anotherPatientsTicketAndUnknownTicketReturn404WithoutMutation() throws Exception {
        var other = session("pasien2"); var csrfPage = get("/login", other).getContentAsString();
        for (String id : java.util.List.of("ANT-DEMO0006", "UNKNOWN")) {
            var url = "/pasien/antrean/" + id + "/pembatalan";
            assertThat(get(url, other).getStatus()).isEqualTo(404);
            assertThat(post(url, other, csrfPage, "ya").getStatus()).isEqualTo(404);
            assertThat(get(url + "/hasil", other).getStatus()).isEqualTo(404);
        }
        assertThat(quota()).isEqualTo(9); assertThat(status()).isEqualTo("Terdaftar");
    }

    @Test void everyOtherRoleAndAnonymousAreRejected() throws Exception {
        assertThat(get(URL, new MockHttpSession()).getStatus()).isEqualTo(302);
        for (String user : java.util.List.of("admin", "loket", "dokter", "dokter2")) {
            var session = session(user); var page = get("/login", session).getContentAsString();
            assertThat(get(URL, session).getStatus()).isEqualTo(403);
            assertThat(post(URL, session, page, "ya").getStatus()).isEqualTo(403);
        }
        assertThat(quota()).isEqualTo(9);
    }

    @Test void allFourAllowedStatusesCanBeCancelled() {
        authenticate("pasien");
        for (String allowed : java.util.List.of("Terdaftar", "Menunggu", "Hadir/Menunggu", "Dilewati")) {
            reset(allowed, 9);
            assertThat(service.batalkan("ANT-DEMO0006", true)).isEqualTo("ANT-DEMO0006");
            assertThat(status()).isEqualTo("Dibatalkan"); assertThat(quota()).isEqualTo(10);
        }
    }

    @Test void servingAndAllFinalStatusesRejectCancellation() {
        authenticate("pasien");
        for (String forbidden : java.util.List.of("Sedang Dilayani", "Selesai", "Dibatalkan", "Tidak Hadir")) {
            reset(forbidden, 9);
            assertThatThrownBy(() -> service.batalkan("ANT-DEMO0006", true)).isInstanceOf(PembatalanException.class);
            assertThat(status()).isEqualTo(forbidden); assertThat(quota()).isEqualTo(9);
        }
    }

    @Test void sixtyOneMinutesWorksButSixtyMinutesOrLessAndPastSchedulesFail() {
        authenticate("pasien");
        var start = jdbc.queryForObject("SELECT tanggal FROM JadwalPraktik WHERE idJadwal='JDW-003'", LocalDate.class).atTime(8,0);
        at(start.minusMinutes(61)).batalkan("ANT-DEMO0006", true);
        assertThat(status()).isEqualTo("Dibatalkan"); assertThat(quota()).isEqualTo(10);
        for (LocalDateTime now : java.util.List.of(start.minusMinutes(60), start.minusMinutes(59), start.plusDays(1))) {
            reset("Terdaftar", 9);
            assertThatThrownBy(() -> at(now).batalkan("ANT-DEMO0006", true)).isInstanceOf(PembatalanException.class).hasMessageContaining("60 menit");
            assertThat(status()).isEqualTo("Terdaftar"); assertThat(quota()).isEqualTo(9);
        }
    }

    @Test void stalePreviewDoesNotAuthorizeCancellationAtTheDeadline() throws Exception {
        var session = session("pasien"); assertThat(get(URL, session).getStatus()).isEqualTo(200);
        authenticate("pasien");
        var start = jdbc.queryForObject("SELECT tanggal FROM JadwalPraktik WHERE idJadwal='JDW-003'", LocalDate.class).atTime(8,0);
        assertThatThrownBy(() -> at(start.minusMinutes(60)).batalkan("ANT-DEMO0006", true)).isInstanceOf(PembatalanException.class);
        assertThat(status()).isEqualTo("Terdaftar"); assertThat(quota()).isEqualTo(9);
    }

    @Test void quotaAtCapacityRejectsWholeOperationWithoutChangingStatus() {
        authenticate("pasien"); reset("Terdaftar", 10);
        assertThatThrownBy(() -> service.batalkan("ANT-DEMO0006", true)).isInstanceOf(PembatalanException.class).hasMessageContaining("kuota tidak konsisten");
        assertThat(status()).isEqualTo("Terdaftar"); assertThat(quota()).isEqualTo(10);
    }

    @Test void resultCannotClaimSuccessForUncancelledTicketAndOversizedIdIsRejected() throws Exception {
        var session = session("pasien");
        assertThat(get(URL + "/hasil", session).getStatus()).isEqualTo(409);
        assertThat(get(URL + "/hasil", session).getContentAsString()).doesNotContain("Antrean berhasil dibatalkan");
        assertThat(get("/pasien/antrean/TOO-LONG-TICKET-ID/pembatalan", session).getStatus()).isEqualTo(400);
    }

    @Test void saveFailureIsReportedAs503WithoutFalseSuccessOrDatabaseDetails() {
        var failed = mock(PembatalanService.class); var status = mock(StatusAntreanService.class);
        when(failed.batalkan("ANT-DEMO0006", true)).thenThrow(new DataAccessResourceFailureException("private-jdbc-url"));
        var boundary = new PembatalanBoundary(failed, status); var response = new MockHttpServletResponse(); var model = new ExtendedModelMap();
        boundary.batalkan("ANT-DEMO0006", "ya", response, model);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(model.get("pesanGalat").toString()).contains("gagal disimpan").doesNotContain("private-jdbc");
        assertThat(model).doesNotContainKey("berhasil");
    }

    private PembatalanService at(LocalDateTime time) {
        var zone = ZoneId.of("Asia/Jakarta"); return new PembatalanService(antrean, jadwal, current, Clock.fixed(time.atZone(zone).toInstant(), zone));
    }
    private void reset(String state, int quota) {
        jdbc.update("UPDATE Antrean SET statusAntrean=? WHERE idAntrean='ANT-DEMO0006'", state);
        jdbc.update("UPDATE JadwalPraktik SET sisaKuota=? WHERE idJadwal='JDW-003'", quota); em.clear();
    }
    private String status() { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0006'", String.class); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-003'", Integer.class); }
    private MockHttpServletResponse get(String url, MockHttpSession session) throws Exception { return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn().getResponse(); }
    private MockHttpServletResponse post(String url, MockHttpSession session, String page, String confirm) throws Exception {
        var csrf = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertThat(csrf.find()).isTrue();
        return mvc.perform(MockMvcRequestBuilders.post(url).session(session).param("_csrf", csrf.group(1)).param("konfirmasi", confirm)
                .param("idPasien", "PAS-002").param("idJadwal", "JDW-001")).andReturn().getResponse();
    }
    private void authenticate(String name) { var user = users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())); }
    private MockHttpSession session(String name) {
        var user = users.loadUserByUsername(name); var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        var session = new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context); return session;
    }
}
