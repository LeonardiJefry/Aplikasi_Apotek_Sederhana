package id.ac.ub.apotek;

import id.ac.ub.apotek.boundary.CheckInBoundary;
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
import org.springframework.context.annotation.Import;
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

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:checkin-core;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(CheckInTransactionsTests.ClockFixture.class)
@ActiveProfiles("test")
@Transactional
class CheckInTests {
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired KlinikUserDetailsService users;
    @Autowired CurrentUserService current;
    @Autowired CheckInService service;
    @Autowired LoketCheckInService loket;
    @Autowired AntreanRepository antrean;
    @Autowired JadwalPraktikRepository jadwal;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired Clock clock;
    private static final String URL="/pasien/antrean/ANT-DEMO0006/check-in";
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build();
        jdbc.update("UPDATE JadwalPraktik SET tanggal=? WHERE idJadwal='JDW-003'",LocalDate.now(clock)); em.clear();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void previewIsReadOnlyAndUsesOwnTicket() throws Exception {
        var session=session("pasien"); var page=get(URL,session);
        assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(page.getContentAsString()).contains("Check-in kedatangan","Check-in sekarang","2026-11-27","07:30:00");
        assertThat(get("/pasien/check-in",session).getStatus()).isEqualTo(200);
        assertThat(state()).isEqualTo("Terdaftar"); assertThat(recorded()).isNull(); assertThat(quota()).isEqualTo(9);
    }
    @Test void postRecordsAttendanceWithoutChangingQuotaNumberOrBookingAndResultIsReloadSafe() throws Exception {
        var session=session("pasien"); var page=get(URL,session).getContentAsString();
        var booking=jdbc.queryForObject("SELECT tglBooking FROM Antrean WHERE idAntrean='ANT-DEMO0006'",LocalDateTime.class);
        var response=post(URL,session,page);
        assertThat(response.getStatus()).isEqualTo(303); assertThat(response.getHeader("Location")).endsWith(URL+"/hasil");
        assertThat(state()).isEqualTo("Hadir/Menunggu"); assertThat(recorded()).isEqualTo(LocalDateTime.now(clock));
        assertThat(quota()).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT nomorAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0006'",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT tglBooking FROM Antrean WHERE idAntrean='ANT-DEMO0006'",LocalDateTime.class)).isEqualTo(booking);
        assertThat(get(URL+"/hasil",session).getContentAsString()).contains("Check-in berhasil","Hadir/Menunggu","2026-11-27 08:00:00");
        assertThat(get("/pasien/antrean/ANT-DEMO0006/data",session).getContentAsString()).contains("\"statusAntrean\":\"Hadir/Menunggu\"","\"bolehCheckIn\":false");
    }
    @Test void repeatedPostRejectsWithoutReplacingOriginalTimestamp() throws Exception {
        var session=session("pasien"); var page=get(URL,session).getContentAsString();
        assertThat(post(URL,session,page).getStatus()).isEqualTo(303); var first=recorded();
        assertThat(post(URL,session,page).getStatus()).isEqualTo(409); assertThat(recorded()).isEqualTo(first); assertThat(quota()).isEqualTo(9);
    }
    @Test void csrfIsRequired() throws Exception {
        assertThat(mvc.perform(MockMvcRequestBuilders.post(URL).session(session("pasien"))).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(state()).isEqualTo("Terdaftar"); assertThat(recorded()).isNull();
    }
    @Test void anotherPatientsTicketAndUnknownTicketAre404OnEverySurface() throws Exception {
        var other=session("pasien2"); var page=get("/login",other).getContentAsString();
        for (String id:java.util.List.of("ANT-DEMO0006","UNKNOWN")) {
            var url="/pasien/antrean/"+id+"/check-in";
            assertThat(get(url,other).getStatus()).isEqualTo(404); assertThat(post(url,other,page).getStatus()).isEqualTo(404);
            assertThat(get(url+"/data",other).getStatus()).isEqualTo(404); assertThat(get(url+"/hasil",other).getStatus()).isEqualTo(404);
        }
        assertThat(recorded()).isNull();
    }
    @Test void rolesAreCheckedAtEndpointsAndService() throws Exception {
        assertThat(get(URL,new MockHttpSession()).getStatus()).isEqualTo(302);
        for(String user:java.util.List.of("admin","loket","dokter","dokter2")) {
            var session=session(user); var page=get("/login",session).getContentAsString();
            assertThat(get(URL,session).getStatus()).isEqualTo(403); assertThat(post(URL,session,page).getStatus()).isEqualTo(403);
            assertThat(get(URL+"/data",session).getStatus()).isEqualTo(403);
            authenticate(user); assertThatThrownBy(()->service.panel(null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        for(String user:java.util.List.of("pasien","pasien2","admin","dokter")) {
            assertThat(get("/loket/check-in",session(user)).getStatus()).isEqualTo(403);
            assertThat(get("/loket/check-in/data",session(user)).getStatus()).isEqualTo(403);
            authenticate(user); assertThatThrownBy(()->loket.snapshot()).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }
    @Test void bothInclusiveBoundsWork() {
        authenticate("pasien");
        for(LocalTime time:java.util.List.of(LocalTime.of(7,30),LocalTime.of(14,0))) {
            reset("Terdaftar",null); at(LocalDate.now(clock).atTime(time)).checkIn("ANT-DEMO0006");
            assertThat(recorded()).isEqualTo(LocalDate.now(clock).atTime(time)); assertThat(state()).isEqualTo("Hadir/Menunggu");
        }
    }
    @Test void outsideBoundsAndDifferentDaysRejectWithoutTimestampOrQuotaChanges() {
        authenticate("pasien"); var today=LocalDate.now(clock);
        for(LocalDateTime time:java.util.List.of(today.atTime(7,29,59,999000000),today.atTime(14,0,0,1000000),today.minusDays(1).atTime(8,0),today.plusDays(1).atTime(8,0))) {
            assertThatThrownBy(()->at(time).checkIn("ANT-DEMO0006")).isInstanceOf(CheckInException.class);
            assertThat(state()).isEqualTo("Terdaftar"); assertThat(recorded()).isNull(); assertThat(quota()).isEqualTo(9);
        }
        assertThat(CheckInPolicy.waktuDibuka(today.minusDays(1).atTime(23,50),today,LocalTime.of(0,10),LocalTime.of(1,0))).isFalse();
    }
    @Test void nonRegisteredStatusesAndExistingTimestampReject() {
        authenticate("pasien");
        for(String status:java.util.List.of("Menunggu","Hadir/Menunggu","Dilewati","Sedang Dilayani","Selesai","Dibatalkan","Tidak Hadir")) {
            reset(status,null); assertThatThrownBy(()->service.checkIn("ANT-DEMO0006")).isInstanceOf(CheckInException.class);
            assertThat(state()).isEqualTo(status); assertThat(recorded()).isNull();
        }
        var original=LocalDateTime.now(clock).minusMinutes(5); reset("Terdaftar",original);
        assertThatThrownBy(()->service.checkIn("ANT-DEMO0006")).isInstanceOf(CheckInException.class); assertThat(recorded()).isEqualTo(original);
    }
    @Test void timeAndStatusAreRecheckedAfterPreview() throws Exception {
        var session=session("pasien"); var page=get(URL,session).getContentAsString();
        jdbc.update("UPDATE JadwalPraktik SET jamMulai='06:00',jamSelesai='07:59' WHERE idJadwal='JDW-003'"); em.clear();
        assertThat(post(URL,session,page).getStatus()).isEqualTo(409);
        assertThat(state()).isEqualTo("Terdaftar"); assertThat(recorded()).isNull(); assertThat(quota()).isEqualTo(9);
    }
    @Test void emptyTodayInvalidIdAndForgedSuccessHaveExplicitStates() throws Exception {
        var session=session("pasien"); assertThat(get(URL+"/hasil",session).getStatus()).isEqualTo(409);
        assertThat(get("/pasien/antrean/TOO-LONG-TICKET-ID/check-in",session).getStatus()).isEqualTo(400);
        reset("Dibatalkan",null);
        var empty=get("/pasien/check-in",session); assertThat(empty.getStatus()).isEqualTo(200);
        assertThat(empty.getContentAsString()).contains("Tidak ada antrean Terdaftar hari ini");
    }
    @Test void loketWarningIsPrivateAndDisappearsAfterAttendance() throws Exception {
        authenticate("loket"); assertThat(loket.snapshot().peringatan()).extracting(LoketCheckInService.Peringatan::idJadwal).containsExactly("JDW-003");
        var data=get("/loket/check-in/data",session("loket")); assertThat(data.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(data.getContentAsString()).contains("\"nomorAntrean\":1").doesNotContain("\"idPasien\"","\"idAntrean\"","DEMO-RM","Pasien Demo","\"nik\"");
        authenticate("pasien"); service.checkIn("ANT-DEMO0006"); authenticate("loket"); assertThat(loket.snapshot().peringatan()).isEmpty();
    }
    @Test void warningThresholdCountsOnlyFirstTwoPendingActiveTicketsAndRespectsTime() {
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean,waktuCheckIn) VALUES ('CKWARN000002','PAS-002','JDW-003',2,CURRENT_TIMESTAMP,'Hadir/Menunggu',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('CKWARN000003','PAS-002','JDW-003',3,CURRENT_TIMESTAMP,'Terdaftar')"); em.clear();
        authenticate("loket"); assertThat(loket.snapshot().peringatan()).extracting(LoketCheckInService.Peringatan::nomorAntrean).containsExactly(1);
        jdbc.update("UPDATE Antrean SET statusAntrean='Selesai' WHERE idAntrean='ANT-DEMO0006'"); em.clear();
        assertThat(loket.snapshot().peringatan()).extracting(LoketCheckInService.Peringatan::nomorAntrean).containsExactly(3);
        var late=new LoketCheckInService(antrean,jadwal,current,Clock.fixed(LocalDate.now(clock).atTime(20,0).atZone(clock.getZone()).toInstant(),clock.getZone()));
        assertThat(late.snapshot().peringatan()).isEmpty();
    }
    @Test void databaseFailuresNeverReportSuccessOrLeakConnectionDetails() {
        var failed=mock(CheckInService.class); when(failed.checkIn("ANT-DEMO0006")).thenThrow(new DataAccessResourceFailureException("private-jdbc-url"));
        var boundary=new CheckInBoundary(failed,mock(LoketCheckInService.class)); var response=new MockHttpServletResponse(); var model=new ExtendedModelMap();
        boundary.checkIn("ANT-DEMO0006",response,model); assertThat(response.getStatus()).isEqualTo(503);
        assertThat(model.get("pesanGalat").toString()).contains("gagal disimpan").doesNotContain("private-jdbc"); assertThat(model).doesNotContainKey("berhasil");
    }
    private CheckInService at(LocalDateTime time) { return new CheckInService(antrean,jadwal,current,Clock.fixed(time.atZone(clock.getZone()).toInstant(),clock.getZone())); }
    private void reset(String state,LocalDateTime recorded) { jdbc.update("UPDATE Antrean SET statusAntrean=?,waktuCheckIn=? WHERE idAntrean='ANT-DEMO0006'",state,recorded); em.clear(); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-003'",Integer.class); }
    private String state() { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0006'",String.class); }
    private LocalDateTime recorded() { return jdbc.queryForObject("SELECT waktuCheckIn FROM Antrean WHERE idAntrean='ANT-DEMO0006'",LocalDateTime.class); }
    private MockHttpServletResponse get(String url,MockHttpSession session)throws Exception { return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn().getResponse(); }
    private MockHttpServletResponse post(String url,MockHttpSession session,String page)throws Exception {
        var csrf=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertThat(csrf.find()).isTrue();
        return mvc.perform(MockMvcRequestBuilders.post(url).session(session).param("_csrf",csrf.group(1)).param("idPasien","PAS-002").param("idJadwal","JDW-001").param("waktuCheckIn","2000-01-01T00:00:00")).andReturn().getResponse();
    }
    private void authenticate(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
    private MockHttpSession session(String name) { var user=users.loadUserByUsername(name); var ctx=SecurityContextHolder.createEmptyContext(); ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); var session=new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,ctx); return session; }
}
