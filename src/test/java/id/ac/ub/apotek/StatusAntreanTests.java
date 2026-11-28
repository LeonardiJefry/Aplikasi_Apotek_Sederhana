package id.ac.ub.apotek;

import id.ac.ub.apotek.boundary.StatusAntreanBoundary;
import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.repository.AntreanRepository;
import id.ac.ub.apotek.security.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.time.*;
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
class StatusAntreanTests {
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired KlinikUserDetailsService users;
    @Autowired CurrentUserService current;
    @Autowired StatusAntreanService service;
    @Autowired AntreanRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(web).addFilters(security).build(); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void pageAndSnapshotUseOwnTicketAndSameSessionQueueOnly() throws Exception {
        var session = session("pasien2");
        var page = get("/pasien/antrean/ANT-DEMO0004", session);
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(page.getContentAsString()).contains("Status Antrean", "004", "001", "3 orang", "Detail Kunjungan", "Daftar Antrean Berikutnya");
        var api = get("/pasien/antrean/ANT-DEMO0004/data?idPasien=PAS-001", session);
        assertThat(api.getStatus()).isEqualTo(200);
        assertThat(api.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(api.getContentAsString()).contains("\"jumlahDiDepan\":3", "\"nomorSedangDilayani\":[1]")
                .doesNotContain("\"nik\":", "\"passwordHash\":", "DEMO-RM", "Pasien Demo", "PAS-001", "ANT-DEMO0001", "ANT-DEMO0003", "ANT-DEMO0006");
    }

    @Test void defaultAndSelectorNeverSelectAnotherPatientsTicket() throws Exception {
        authenticate("pasien2");
        var snapshot = service.snapshot(null);
        assertThat(snapshot.tiket()).extracting(StatusAntreanDto.Pilihan::idAntrean)
                .containsExactly("ANT-DEMO0004", "ANT-DEMO0002", "ANT-DEMO0005", "ANT-DEMO0009", "ANT-DEMO0008");
        assertThat(snapshot.dipilih().idAntrean()).isEqualTo("ANT-DEMO0004");
        var page = get("/pasien/antrean?tiket=ANT-DEMO0005", session("pasien2"));
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getContentAsString()).contains("Poli Gigi", "data-ticket-id=\"ANT-DEMO0005\"");
    }

    @Test void countsActiveSmallerNumbersInsteadOfSubtractingNumbersAndExcludesFinalStatuses() {
        authenticate("pasien2");
        update("ANT-DEMO0002", "Dibatalkan"); update("ANT-DEMO0003", "Tidak Hadir");
        assertThat(service.snapshot("ANT-DEMO0004").dipilih().jumlahDiDepan()).isEqualTo(1);
        update("ANT-DEMO0001", "Selesai");
        var detail = service.snapshot("ANT-DEMO0004").dipilih();
        assertThat(detail.jumlahDiDepan()).isZero();
        assertThat(detail.nomorSedangDilayani()).isEmpty();
    }

    @Test void queueDoesNotMixDoctorsDatesOrSessions() {
        authenticate("pasien2");
        var otherSession = service.snapshot("ANT-DEMO0005").dipilih();
        assertThat(otherSession.nomorSedangDilayani()).isEmpty();
        assertThat(otherSession.jumlahDiDepan()).isZero();
        assertThat(otherSession.berikutnya()).extracting(StatusAntreanDto.Baris::nomorAntrean).containsExactly(1);
        authenticate("pasien");
        var tomorrow = service.snapshot("ANT-DEMO0006").dipilih();
        assertThat(tomorrow.nomorSedangDilayani()).isEmpty();
        assertThat(tomorrow.jumlahDiDepan()).isZero();
    }

    @Test void refreshedSnapshotReflectsChangesWithoutRebookingOrQuotaWrites() {
        authenticate("pasien2");
        assertThat(service.snapshot("ANT-DEMO0004").dipilih().jumlahDiDepan()).isEqualTo(3);
        update("ANT-DEMO0001", "Selesai"); update("ANT-DEMO0002", "Sedang Dilayani");
        var changed = service.snapshot("ANT-DEMO0004").dipilih();
        assertThat(changed.nomorSedangDilayani()).containsExactly(2);
        assertThat(changed.jumlahDiDepan()).isEqualTo(2);
        update("ANT-DEMO0004", "Dibatalkan");
        var ended = service.snapshot("ANT-DEMO0004").dipilih();
        assertThat(ended.statusAkhir()).isTrue();
        assertThat(ended.jumlahDiDepan()).isNull();
        assertThat(ended.berikutnya()).isEmpty();
        assertThat(ended.bolehBatal()).isFalse();
        assertThat(ended.bolehCheckIn()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-001'", Integer.class)).isEqualTo(6);
    }

    @Test void allFinalStatusesShowFinalStateAndNoWaitingPosition() throws Exception {
        for (var pair : java.util.Map.of("ANT-DEMO0007", "pasien", "ANT-DEMO0008", "pasien2", "ANT-DEMO0009", "pasien2").entrySet()) {
            authenticate(pair.getValue());
            var detail = service.snapshot(pair.getKey()).dipilih();
            assertThat(detail.statusAkhir()).isTrue();
            assertThat(detail.jumlahDiDepan()).isNull();
            assertThat(detail.berikutnya()).isEmpty();
            assertThat(detail.bolehBatal() || detail.bolehCheckIn()).isFalse();
            assertThat(get("/pasien/antrean/" + pair.getKey(), session(pair.getValue())).getStatus()).isEqualTo(200);
        }
    }

    @Test void skippedTicketRemainsActiveAndServingTicketIsNotWaiting() {
        authenticate("pasien");
        var skipped = service.snapshot("ANT-DEMO0003").dipilih();
        assertThat(skipped.statusAkhir()).isFalse();
        assertThat(skipped.jumlahDiDepan()).isEqualTo(2);
        assertThat(skipped.keterangan()).contains("panggilan ulang");
        var serving = service.snapshot("ANT-DEMO0001").dipilih();
        assertThat(serving.statusAntrean()).isEqualTo("Sedang Dilayani");
        assertThat(serving.jumlahDiDepan()).isNull();
        assertThat(serving.bolehBatal() || serving.bolehCheckIn()).isFalse();
    }

    @Test void emptyPatientHasExplicitEmptyPageAndNullSelectedSnapshot() throws Exception {
        jdbc.update("INSERT INTO Pasien (idPasien,nik,noRekamMedis,nama,tanggalLahir) VALUES ('PAS-EMPTY','0000000000000099','TEST-EMPTY','Pasien Kosong','2000-01-01')");
        jdbc.update("INSERT INTO Pengguna (idPengguna,username,passwordHash,peran,idPasien,aktif) SELECT 'USR-EMPTY','empty',passwordHash,'PASIEN','PAS-EMPTY',true FROM Pengguna WHERE username='pasien'");
        var empty = session("empty");
        var response = get("/pasien/antrean", empty);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("Belum ada tiket antrean").doesNotContain("id=\"queue-own-number\"");
        assertThat(get("/pasien/antrean/data", empty).getContentAsString()).contains("\"tiket\":[]", "\"dipilih\":null");
    }

    @Test void unknownAndOtherOwnedTicketsAreIndistinguishableAndInvalidIdIsRejected() throws Exception {
        var patient = session("pasien2");
        for (String id : java.util.List.of("ANT-DEMO0001", "UNKNOWN")) {
            var page = get("/pasien/antrean/" + id, patient);
            assertThat(page.getStatus()).isEqualTo(404);
            assertThat(page.getContentAsString()).contains("Tiket tidak ditemukan").doesNotContain("Pasien Demo Satu");
            assertThat(get("/pasien/antrean/" + id + "/data", patient).getStatus()).isEqualTo(404);
            assertThat(get("/pasien/antrean/" + id + "/pembatalan", patient).getStatus()).isEqualTo(404);
        }
        assertThat(get("/pasien/antrean/TOO-LONG-TICKET-ID/data", patient).getStatus()).isEqualTo(400);
    }

    @Test void anonymousAndEveryOtherRoleCannotReadPageSnapshotOrActionSeams() throws Exception {
        assertThat(get("/pasien/antrean", new MockHttpSession()).getStatus()).isEqualTo(302);
        for (String user : java.util.List.of("admin", "loket", "dokter", "dokter2")) {
            for (String path : java.util.List.of("/pasien/antrean", "/pasien/antrean/data", "/pasien/antrean/ANT-DEMO0004", "/pasien/antrean/ANT-DEMO0004/data", "/pasien/antrean/ANT-DEMO0004/check-in"))
                assertThat(get(path, session(user)).getStatus()).as(user + " " + path).isEqualTo(403);
            authenticate(user);
            assertThatThrownBy(() -> service.snapshot(null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }

    @Test void actionEligibilityUsesStrictSixtyMinuteAndInclusiveCheckInBounds() {
        authenticate("pasien");
        LocalDate date = jdbc.queryForObject("SELECT tanggal FROM JadwalPraktik WHERE idJadwal='JDW-003'", LocalDate.class);
        assertThat(at(date.atTime(6,59)).snapshot("ANT-DEMO0006").dipilih().bolehBatal()).isTrue();
        assertThat(at(date.atTime(7,0)).snapshot("ANT-DEMO0006").dipilih().bolehBatal()).isFalse();
        // AD-04 alur D1/D2: tombol tetap dapat dibuka pada batas 60 menit; penolakan terjadi setelah konfirmasi.
        assertThat(at(date.atTime(7,0)).snapshot("ANT-DEMO0006").dipilih().bolehBukaBatal()).isTrue();
        assertThat(at(date.atTime(7,0)).snapshot("ANT-DEMO0001").dipilih().bolehBukaBatal()).isFalse();
        for (LocalTime time : java.util.List.of(LocalTime.of(7,30), LocalTime.of(14,0)))
            assertThat(at(date.atTime(time)).snapshot("ANT-DEMO0006").dipilih().bolehCheckIn()).isTrue();
        for (LocalDateTime time : java.util.List.of(date.atTime(7,29,59), date.atTime(14,0,1), date.minusDays(1).atTime(8,0)))
            assertThat(at(time).snapshot("ANT-DEMO0006").dipilih().bolehCheckIn()).isFalse();
    }

    @Test void actionInformationChecksOwnershipAndEligibilityWithoutMutation() throws Exception {
        var patient = session("pasien");
        var info = get("/pasien/antrean/ANT-DEMO0006/pembatalan", patient);
        assertThat(info.getStatus()).isEqualTo(200);
        assertThat(info.getContentAsString()).contains("Batalkan Antrean?", "Ya, Batalkan");
        assertThat(get("/pasien/antrean/ANT-DEMO0001/pembatalan", patient).getStatus()).isEqualTo(409);
        assertThat(get("/pasien/antrean/ANT-DEMO0006/check-in", patient).getStatus()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class)).isEqualTo(9);
    }

    @Test void waitingListIsBoundedAndMultipleServingNumbersAreNotHidden() {
        authenticate("pasien2"); update("ANT-DEMO0002", "Sedang Dilayani");
        for (int n = 5; n <= 10; n++)
            jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES (?,'PAS-001','JDW-001',?,CURRENT_TIMESTAMP,'Menunggu')", String.format("QTEST%07d", n), n);
        em.clear();
        var detail = service.snapshot("ANT-DEMO0004").dipilih();
        assertThat(detail.nomorSedangDilayani()).containsExactly(1, 2);
        assertThat(detail.berikutnya()).hasSize(5);
        assertThat(detail.berikutnya()).extracting(StatusAntreanDto.Baris::nomorAntrean).containsExactly(3,4,5,6,7);
    }

    @Test void databaseErrorsReturn503WithoutLeakingInternalDetails() {
        var failed = mock(StatusAntreanService.class);
        when(failed.snapshot(null)).thenThrow(new DataAccessResourceFailureException("private-db-connection"));
        var boundary = new StatusAntreanBoundary(failed);
        var response = boundary.data(null);
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().toString()).contains("belum dapat dimuat").doesNotContain("private-db");
        var servletResponse = new MockHttpServletResponse(); var model = new ExtendedModelMap();
        assertThat(boundary.halaman(null, null, model, servletResponse)).isEqualTo("antreanpasien/status");
        assertThat(servletResponse.getStatus()).isEqualTo(503);
        assertThat(model.get("pesanGalat").toString()).doesNotContain("private-db");
    }

    private StatusAntreanService at(LocalDateTime now) {
        var zone = ZoneId.of("Asia/Jakarta");
        return new StatusAntreanService(repository, current, Clock.fixed(now.atZone(zone).toInstant(), zone));
    }
    private void update(String ticket, String status) { jdbc.update("UPDATE Antrean SET statusAntrean=? WHERE idAntrean=?", status, ticket); em.clear(); }
    private MockHttpServletResponse get(String url, MockHttpSession session) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn().getResponse();
    }
    private void authenticate(String username) {
        var user = users.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
    private MockHttpSession session(String username) {
        var user = users.loadUserByUsername(username);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context); return session;
    }
}
