package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
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
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:panggilan-core;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test") @Transactional
class PanggilanTests {
    static final String ID="CALLTEST01",ONE="CALLTICKET01",TWO="CALLTICKET02";
    @Autowired JdbcTemplate jdbc;
    @Autowired PanggilanService service;
    @Autowired MutableClock clock;
    @Autowired KlinikUserDetailsService users;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;
    @BeforeEach void setup() {
        clock.at("2026-11-27T08:00:00");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001','2026-11-27','Uji','08:00','14:00',2,0)",ID);
        ticket(ONE,1); ticket(TWO,2); auth("loket");
        mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void snapshotAndPageAreReadOnlyAndShowEmptyCallWithCandidate() throws Exception {
        var s=service.snapshot(ID); assertThat(s.dipanggil()).isNull(); assertThat(s.bolehPanggil()).isTrue();
        assertThat(s.berikutnya().nomorAntrean()).isEqualTo(1);
        var page=get("/loket/panggilan?idJadwal="+ID,session("loket"));
        assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(page.getContentAsString()).contains("Panggilan antrean","Panggil antrean berikutnya");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PanggilanAntrean WHERE idJadwal=?",Integer.class,ID)).isZero();
        assertThat(state(ONE)).isEqualTo("Terdaftar");
    }
    @Test void callUsesLowestNumberAndReloadKeepsOriginalTimeAndToken() {
        service.panggil(ID); var first=service.snapshot(ID);
        assertThat(first.dipanggil().idAntrean()).isEqualTo(ONE); assertThat(first.sisaDetik()).isEqualTo(120);
        assertThat(state(ONE)).isEqualTo("Terdaftar"); clock.at("2026-11-27T08:01:00"); em.clear();
        var again=service.snapshot(ID); assertThat(again.idPanggilan()).isEqualTo(first.idPanggilan());
        assertThat(again.waktuPanggil()).isEqualTo(first.waktuPanggil()); assertThat(again.sisaDetik()).isEqualTo(60);
        assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(PanggilanException.class);
        assertThat(service.snapshot(ID).waktuPanggil()).isEqualTo(first.waktuPanggil());
    }
    @Test void skipRejects119SecondsAndAcceptsExactly120ThenCallsNextAtomically() {
        service.panggil(ID); String token=service.snapshot(ID).idPanggilan();
        clock.at("2026-11-27T08:01:59"); assertThat(service.snapshot(ID).bolehLewati()).isFalse();
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        assertThat(history(ONE)).isZero(); assertThat(state(ONE)).isEqualTo("Terdaftar");
        clock.at("2026-11-27T08:02:00"); assertThat(service.snapshot(ID).bolehLewati()).isTrue();
        service.lewati(ID,token,true);
        assertThat(state(ONE)).isEqualTo("Dilewati"); assertThat(history(ONE)).isEqualTo(1); assertThat(quota()).isZero();
        var next=service.snapshot(ID); assertThat(next.dipanggil().idAntrean()).isEqualTo(TWO);
        assertThat(next.idPanggilan()).isNotEqualTo(token); assertThat(next.sisaDetik()).isEqualTo(120);
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        assertThat(history(ONE)).isEqualTo(1); assertThat(history(TWO)).isZero();
    }
    @Test void presentClosesCallAndCannotBeSkippedOrRepeated() {
        service.panggil(ID); var token=service.snapshot(ID).idPanggilan();
        clock.at("2026-11-27T08:00:30"); service.hadir(ID,token);
        assertThat(state(ONE)).isEqualTo("Sedang Dilayani"); assertThat(service.snapshot(ID).dipanggil()).isNull();
        assertThat(service.snapshot(ID).sedangDilayani()).isTrue();
        assertThatThrownBy(()->service.hadir(ID,token)).isInstanceOf(PanggilanException.class);
        assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(PanggilanException.class);
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        assertThat(history(ONE)).isZero(); assertThat(quota()).isZero();
    }
    @Test void fractionalCallTimeSurvivesDatabaseReloadWithoutRoundingTimerTo121() {
        clock.at("2026-11-27T08:00:00.750123400"); service.panggil(ID); em.clear();
        var s=service.snapshot(ID); assertThat(s.sisaDetik()).isEqualTo(120);
        assertThat(LocalDateTime.parse(s.waktuPanggil())).isEqualTo(LocalDateTime.parse("2026-11-27T08:00:00.750123"));
        clock.at("2026-11-27T08:02:00.750122"); assertThat(service.snapshot(ID).bolehLewati()).isFalse();
        assertThat(service.snapshot(ID).sisaDetik()).isEqualTo(1);
        assertThatThrownBy(()->service.lewati(ID,s.idPanggilan(),true)).isInstanceOf(PanggilanException.class);
        clock.at("2026-11-27T08:02:00.750123"); assertThat(service.snapshot(ID).bolehLewati()).isTrue();
        service.lewati(ID,s.idPanggilan(),true); assertThat(state(ONE)).isEqualTo("Dilewati");
    }
    @Test void immediatePresenceDoesNotFailBecauseStoredCallTimeRoundedIntoFuture() {
        clock.at("2026-11-27T08:00:00.750123400"); service.panggil(ID); em.clear();
        var token=service.snapshot(ID).idPanggilan(); service.hadir(ID,token); em.clear();
        assertThat(state(ONE)).isEqualTo("Sedang Dilayani");
        assertThat(jdbc.queryForObject("SELECT waktuTutup FROM PanggilanAntrean WHERE idJadwal=?",LocalDateTime.class,ID))
                .isEqualTo(LocalDateTime.parse("2026-11-27T08:00:00.750123"));
    }
    @Test void thirdSkipMarksNoShowAndRestoresQuotaOnlyOnce() {
        loneSlot();
        for(int count=1;count<=3;count++) {
            service.panggil(ID); var token=service.snapshot(ID).idPanggilan();
            clock.advance(120); service.lewati(ID,token,true);
            assertThat(history(ONE)).isEqualTo(count);
            assertThat(state(ONE)).isEqualTo(count==3?"Tidak Hadir":"Dilewati");
            assertThat(quota()).isEqualTo(count==3?1:0);
            assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        }
        assertThat(service.snapshot(ID).berikutnya()).isNull();
        assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(PanggilanException.class);
        assertThat(jdbc.queryForObject("SELECT hasilPelewatan FROM RiwayatPelewatan WHERE idAntrean=? AND urutanPelewatan=3",String.class,ONE)).isEqualTo("Tidak Hadir");
    }
    @Test void skippedPatientGetsNextChanceAfterInterveningServiceCompletes() {
        ticket("CALLTICKET03",3); service.panggil(ID); var token=service.snapshot(ID).idPanggilan(); clock.advance(120); service.lewati(ID,token,true);
        var second=service.snapshot(ID); assertThat(second.dipanggil().idAntrean()).isEqualTo(TWO);
        service.hadir(ID,second.idPanggilan()); assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(PanggilanException.class);
        // Simulasi hasil UC-10 pada fixture; UC-06 tidak menyediakan aksi Selesai.
        jdbc.update("UPDATE Antrean SET statusAntrean='Selesai' WHERE idAntrean=?",TWO); em.clear();
        service.panggil(ID); assertThat(service.snapshot(ID).dipanggil().idAntrean()).isEqualTo(ONE);
        service.hadir(ID,service.snapshot(ID).idPanggilan()); assertThat(state(ONE)).isEqualTo("Sedang Dilayani");
    }
    @Test void outsideHoursDifferentDayAndEndedSessionRejectWithoutWrites() {
        for(String when:List.of("2026-11-27T07:59:59","2026-11-27T14:00:01","2026-11-28T08:00:00")) {
            clock.at(when); assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(PanggilanException.class);
        }
        clock.at("2026-11-27T14:00:00"); service.panggil(ID); var token=service.snapshot(ID).idPanggilan();
        clock.advance(120); assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        assertThat(state(ONE)).isEqualTo("Terdaftar"); assertThat(history(ONE)).isZero();
    }
    @Test void confirmationAndCurrentCallTokenAreMandatory() {
        service.panggil(ID); var token=service.snapshot(ID).idPanggilan(); clock.advance(120);
        assertThatThrownBy(()->service.lewati(ID,token,false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.lewati(ID,"invalid",true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.hadir(ID,UUID.randomUUID().toString())).isInstanceOf(PanggilanException.class);
        assertThat(history(ONE)).isZero(); assertThat(state(ONE)).isEqualTo("Terdaftar");
    }
    @Test void quotaInconsistencyRejectsThirdSkipWithoutChangingHistoryOrCall() {
        loneSlot();
        for(int n=0;n<2;n++) { service.panggil(ID); var token=service.snapshot(ID).idPanggilan(); clock.advance(120); service.lewati(ID,token,true); }
        service.panggil(ID); var token=service.snapshot(ID).idPanggilan(); clock.advance(120);
        jdbc.update("UPDATE JadwalPraktik SET sisaKuota=1 WHERE idJadwal=?",ID); em.clear();
        assertThatThrownBy(()->service.lewati(ID,token,true)).isInstanceOf(PanggilanException.class);
        assertThat(history(ONE)).isEqualTo(2); assertThat(state(ONE)).isEqualTo("Dilewati");
        assertThat(service.snapshot(ID).idPanggilan()).isEqualTo(token); assertThat(service.snapshot(ID).dipanggil()).isNotNull();
    }
    @Test void serviceLimitsDoctorToOwnScheduleAndRejectsOtherRoles() {
        for(String name:List.of("pasien","pasien2","admin")) { auth(name); assertThatThrownBy(()->service.snapshot(ID)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class); assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class); }
        auth("dokter2"); assertThatThrownBy(()->service.snapshot(ID)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(()->service.panggil(ID)).isInstanceOf(NoSuchElementException.class);
        auth("dokter"); service.panggil(ID); assertThat(service.snapshot(ID).dipanggil()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT idPengguna FROM PanggilanAntrean WHERE idJadwal=?",String.class,ID)).isEqualTo("USR-005");
    }
    @Test void endpointsEnforceRoleAndCsrfAndIgnoreForgedIdentityOrWaitTime() throws Exception {
        String base="/loket/panggilan"; var loket=session("loket");
        assertThat(get(base+"?idJadwal="+ID,new MockHttpSession()).getStatus()).isEqualTo(302);
        for(String name:List.of("pasien","admin","dokter","dokter2")) {
            var other=session(name); assertThat(get(base,other).getStatus()).isEqualTo(403);
            assertThat(get(base+"/data?idJadwal="+ID,other).getStatus()).isEqualTo(403);
            assertThat(post(base+"/"+ID+"/panggil",other,get("/login",other).getContentAsString(),Map.of()).getStatus()).isEqualTo(403);
        }
        assertThat(mvc.perform(MockMvcRequestBuilders.post(base+"/"+ID+"/panggil").session(loket)).andReturn().getResponse().getStatus()).isEqualTo(403);
        var page=get(base+"?idJadwal="+ID,loket).getContentAsString();
        assertThat(post(base+"/"+ID+"/panggil",loket,page,Map.of("idPengguna","USR-004","idAntrean",TWO)).getStatus()).isEqualTo(303);
        auth("loket"); var snapshot=service.snapshot(ID); assertThat(snapshot.dipanggil().idAntrean()).isEqualTo(ONE);
        assertThat(jdbc.queryForObject("SELECT idPengguna FROM PanggilanAntrean WHERE idJadwal=?",String.class,ID)).isEqualTo("USR-003");
        page=get(base+"?idJadwal="+ID,loket).getContentAsString();
        assertThat(post(base+"/"+ID+"/lewati",loket,page,Map.of("idPanggilan",snapshot.idPanggilan(),"konfirmasi","ya","waktuPanggil","2000-01-01T00:00:00")).getStatus()).isEqualTo(409);
        assertThat(history(ONE)).isZero();
        clock.advance(120);
        assertThat(post(base+"/"+ID+"/lewati",loket,page,Map.of("idPanggilan",snapshot.idPanggilan(),"konfirmasi","ya")).getStatus()).isEqualTo(303);
    }
    @Test void jsonContainsOnlyAuthorizedOperationalDataAndErrorsAreExplicit() throws Exception {
        var session=session("loket");
        var data=get("/loket/panggilan/data?idJadwal="+ID,session);
        assertThat(data.getStatus()).isEqualTo(200); assertThat(data.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(data.getContentAsString()).contains("Pasien Demo Satu","DEMO-RM-001").doesNotContain("passwordHash","nik","tanggalLahir");
        assertThat(get("/loket/panggilan?idJadwal=TOO-LONG-SCHEDULE",session).getStatus()).isEqualTo(400);
        assertThat(get("/loket/panggilan?idJadwal=MISSING",session).getStatus()).isEqualTo(404);
    }
    private void loneSlot() { jdbc.update("DELETE FROM Antrean WHERE idAntrean=?",TWO); jdbc.update("UPDATE JadwalPraktik SET kuotaTotal=1 WHERE idJadwal=?",ID); em.clear(); }
    private void ticket(String id,int n) { jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES (?,'PAS-001',?,?,CURRENT_TIMESTAMP,'Terdaftar')",id,ID,n); }
    private String state(String id) { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?",String.class,id); }
    private int quota() { return jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal=?",Integer.class,ID); }
    private int history(String id) { return jdbc.queryForObject("SELECT COUNT(*) FROM RiwayatPelewatan WHERE idAntrean=?",Integer.class,id); }
    private void auth(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
    private MockHttpSession session(String name) { var user=users.loadUserByUsername(name); var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); var session=new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context); return session; }
    private MockHttpServletResponse get(String url,MockHttpSession session)throws Exception { return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn().getResponse(); }
    private MockHttpServletResponse post(String url,MockHttpSession session,String page,Map<String,String> fields)throws Exception {
        var token=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertThat(token.find()).isTrue();
        var request=MockMvcRequestBuilders.post(url).session(session).param("_csrf",token.group(1)); fields.forEach(request::param);
        return mvc.perform(request).andReturn().getResponse();
    }
    @TestConfiguration(proxyBeanMethods=false) public static class TimeFixture {
        @Bean @Primary MutableClock callClock() { return new MutableClock(); }
    }
    public static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant=new AtomicReference<>(Instant.parse("2026-11-27T01:00:00Z"));
        public void at(String local) { instant.set(LocalDateTime.parse(local).atZone(getZone()).toInstant()); }
        public void advance(long seconds) { instant.updateAndGet(i->i.plusSeconds(seconds)); }
        public ZoneId getZone() { return ZoneId.of("Asia/Jakarta"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(),zone); }
        public Instant instant() { return instant.get(); }
    }
}
