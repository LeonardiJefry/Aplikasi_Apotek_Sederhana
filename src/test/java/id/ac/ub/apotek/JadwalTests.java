package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.JadwalService;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JadwalTests {
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired KlinikUserDetailsService users;
    @Autowired JadwalService service;
    @Autowired Clock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(web).addFilters(security).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void initialPageOffersFiltersWithoutRunningAnUnfilteredSearch() throws Exception {
        var response = mvc.perform(get("/pasien/jadwal", "pasien")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("Jadwal Praktik &amp; Sisa Kuota", "Semua Poli", "Semua Dokter", "Poli Umum", "data-poli=\"POL-002\"", today());
        assertThat(response.getContentAsString()).doesNotContain("id=\"schedule-results\"");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test void filterRendersSelectedDoctorPoliSessionAndLiveRefresh() throws Exception {
        var response = mvc.perform(filter("/pasien/jadwal", "POL-001", "DOK-001")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("dr. Demo Satu", "08:00–14:00", "6 / 10", "Tersedia", "Pilih Jadwal", "/js/jadwal.js", "id=\"schedule-results\"");
        assertThat(response.getContentAsString()).doesNotContain("drg. Demo Dua</strong>");
    }

    @Test void doctorOnlyAndPoliOnlyReturnTheCorrectSchedule() {
        authenticate("pasien");
        assertThat(service.ambilJadwal(today(), null, "DOK-002")).extracting("idJadwal").containsExactly("JDW-002");
        assertThat(service.ambilJadwal(today(), "POL-001", null)).extracting("idJadwal").containsExactly("JDW-001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2026-02-30", "26-11-2026", "not-a-date"})
    void rejectsMissingOrInvalidDates(String date) throws Exception {
        var response = mvc.perform(get("/pasien/jadwal/data", "pasien").param("tanggal", date).param("idPoli", "POL-001")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("Tanggal kunjungan");
    }

    @Test void rejectsMissingUnknownAndMismatchedFiltersOnServer() throws Exception {
        assertThat(mvc.perform(get("/pasien/jadwal/data", "pasien").param("tanggal", today())).andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mvc.perform(filter("/pasien/jadwal/data", "UNKNOWN", "")).andReturn().getResponse().getStatus()).isEqualTo(400);
        var wrongDoctor = mvc.perform(filter("/pasien/jadwal/data", "POL-001", "DOK-002")).andReturn().getResponse();
        assertThat(wrongDoctor.getStatus()).isEqualTo(400);
        assertThat(wrongDoctor.getContentAsString()).contains("Dokter tidak sesuai");
        var invalidPage = mvc.perform(get("/pasien/jadwal", "pasien").param("tanggal", today())).andReturn().getResponse();
        assertThat(invalidPage.getStatus()).isEqualTo(400);
        assertThat(invalidPage.getContentAsString()).contains("Pilih poli atau dokter terlebih dahulu");
    }

    @Test void emptyResultsRenderAUsefulMessageAndEmptyJsonList() throws Exception {
        var date = LocalDate.now(clock).plusYears(1).toString();
        var request = get("/pasien/jadwal", "pasien").param("tanggal", date).param("idPoli", "POL-001");
        var page = mvc.perform(request).andReturn().getResponse();
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getContentAsString()).contains("Jadwal tidak tersedia");
        var json = mvc.perform(get("/pasien/jadwal/data", "pasien").param("tanggal", date).param("idPoli", "POL-001")).andReturn().getResponse();
        assertThat(json.getStatus()).isEqualTo(200);
        assertThat(json.getContentAsString()).contains("\"jadwal\":[]");
    }

    @Test void pollingReadsChangedQuotaAndFullScheduleCannotBeSelected() throws Exception {
        var initial = mvc.perform(filter("/pasien/jadwal/data", "POL-001", "")).andReturn().getResponse();
        assertThat(initial.getContentAsString()).contains("\"sisaKuota\":6", "\"status\":\"Tersedia\"");
        jdbc.update("UPDATE JadwalPraktik SET sisaKuota=0 WHERE idJadwal='JDW-001'");
        entityManager.clear();
        var fresh = mvc.perform(filter("/pasien/jadwal/data", "POL-001", "")).andReturn().getResponse();
        assertThat(fresh.getContentAsString()).contains("\"sisaKuota\":0", "\"status\":\"Penuh\"");
        assertThat(fresh.getHeader("Cache-Control")).isEqualTo("no-store");
        var full = mvc.perform(get("/pasien/jadwal/pilih", "pasien").param("idJadwal", "JDW-001")).andReturn().getResponse();
        assertThat(full.getStatus()).isEqualTo(409);
        assertThat(full.getContentAsString()).contains("Jadwal sudah penuh");
        var page = mvc.perform(filter("/pasien/jadwal", "POL-001", "")).andReturn().getResponse();
        assertThat(page.getContentAsString()).contains("disabled=\"disabled\"", "Penuh").doesNotContain("class=\"button primary schedule-choice\"");
    }

    @Test void selectingAvailableScheduleOnlyReadsAndUnknownScheduleReturns404() throws Exception {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class);
        var choice = mvc.perform(get("/pasien/jadwal/pilih", "pasien").param("idJadwal", "JDW-001")).andReturn().getResponse();
        assertThat(choice.getStatus()).isEqualTo(302);
        assertThat(choice.getHeader("Location")).isEqualTo("/pasien/booking?idJadwal=JDW-001");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-001'", Integer.class)).isEqualTo(6);
        assertThat(mvc.perform(get("/pasien/jadwal/pilih", "pasien").param("idJadwal", "UNKNOWN")).andReturn().getResponse().getStatus()).isEqualTo(404);
    }

    @Test void endpointsRejectOtherRolesAndServiceAlsoChecksPatientIdentity() throws Exception {
        for (var role : java.util.List.of("admin", "loket", "dokter")) {
            for (var path : java.util.List.of("/pasien/jadwal", "/pasien/jadwal/data", "/pasien/jadwal/pilih")) {
                assertThat(mvc.perform(get(path, role)).andReturn().getResponse().getStatus()).as(role + " " + path).isEqualTo(403);
            }
            authenticate(role);
            assertThatThrownBy(() -> service.ambilJadwal(today(), "POL-001", null)).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(mvc.perform(MockMvcRequestBuilders.get("/pasien/jadwal")).andReturn().getResponse().getStatus()).isEqualTo(302);
    }

    @Test void snapshotDoesNotExposePatientIdentities() throws Exception {
        var response = mvc.perform(filter("/pasien/jadwal/data", "POL-001", "")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"idJadwal\":\"JDW-001\"").doesNotContain("nik", "noRekamMedis", "idPasien", "passwordHash");
    }

    private MockHttpServletRequestBuilder filter(String path, String poli, String dokter) {
        return get(path, "pasien").param("tanggal", today()).param("idPoli", poli).param("idDokter", dokter);
    }
    private MockHttpServletRequestBuilder get(String path, String username) {
        var context = SecurityContextHolder.createEmptyContext();
        var user = users.loadUserByUsername(username);
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return MockMvcRequestBuilders.get(path).session(session);
    }
    private void authenticate(String username) {
        var user = users.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
    private String today() { return LocalDate.now(clock).toString(); }
}
