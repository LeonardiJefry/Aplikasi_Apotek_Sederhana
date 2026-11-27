package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

/** UC-09 memakai dataset sintetis profil test (hari ini 2026-11-27 dari MutableClock) dan fixture tambahan yang di-rollback. */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:dashboard-dokter;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test") @Transactional
class DashboardDokterTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired DashboardDokterService service;
    @Autowired PanggilanTests.MutableClock clock;
    @Autowired KlinikUserDetailsService users;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;

    @BeforeEach void setup() {
        clock.at("2026-11-27T09:00:00");
        mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void doctorSeesOnlyOwnTodayScheduleGroupedBySessionWithExactWaitingCount() {
        auth("dokter"); var s=service.snapshot(null);
        assertThat(s.dokter()).isEqualTo("dr. Demo Satu"); assertThat(s.tanggal()).hasToString("2026-11-27");
        assertThat(s.sesi()).extracting(g->g.jadwal().idJadwal()).containsExactly("JDW-001");
        var pagi=s.sesi().getFirst();
        assertThat(pagi.antrean()).extracting(DashboardDokterService.Baris::nomorAntrean).containsExactly(1,2,3,4);
        // PC-09: hitungan utama hanya status Menunggu; Hadir/Menunggu terpisah.
        assertThat(pagi.menunggu()).isEqualTo(1); assertThat(pagi.hadirMenunggu()).isEqualTo(1);
        assertThat(pagi.sedangDilayani()).isEqualTo(1); assertThat(pagi.dipilih()).isTrue(); assertThat(pagi.berlangsung()).isTrue();
        assertThat(pagi.dilewati()).isEqualTo(1); assertThat(pagi.terdaftar()).isEqualTo(4); assertThat(s.aktif()).isEqualTo(pagi);
        assertThat(s.tanggalTampilan()).isEqualTo("Jumat, 27 Nov 2026"); assertThat(s.inisial()).isEqualTo("DS");
        // Seed menandai A-001 Sedang Dilayani tanpa metadata panggilan; waktu dipanggil/durasi tidak dikarang.
        assertThat(s.pelayanan().pasien().nomorTampilan()).isEqualTo("A-001"); assertThat(s.pelayanan().dipanggil()).isNull();
        assertThat(s.panggilan().jadwal().idJadwal()).isEqualTo("JDW-001"); assertThat(s.panggilan().sedangDilayani()).isTrue();

        auth("dokter2"); var other=service.snapshot(null);
        assertThat(other.sesi()).extracting(g->g.jadwal().idJadwal()).containsExactly("JDW-002");
        assertThat(other.sesi().getFirst().antrean()).extracting(DashboardDokterService.Baris::idAntrean).containsExactly("ANT-DEMO0005");
    }

    @Test void multipleSessionsAreGroupedAndEmptySessionIsReported() throws Exception {
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES ('DSHSORE01','DOK-001','POL-001','2026-11-27','Sore','15:00','18:00',5,5)");
        auth("dokter"); var s=service.snapshot(null);
        assertThat(s.sesi()).extracting(g->g.jadwal().sesi()).containsExactly("Pagi","Sore");
        assertThat(s.sesi().get(1).antrean()).isEmpty(); assertThat(s.sesi().get(1).dipilih()).isFalse();
        clock.at("2026-11-27T15:30:00"); var later=service.snapshot(null);
        // Setelah sesi pagi berakhir, sesi berikutnya yang masih berjalan dipilih otomatis.
        assertThat(later.panggilan().jadwal().idJadwal()).isEqualTo("DSHSORE01");
        assertThat(service.snapshot("JDW-001").panggilan().jadwal().idJadwal()).isEqualTo("JDW-001");
        var page=get("/dokter/antrean?idJadwal=DSHSORE01",session("dokter")).getContentAsString();
        assertThat(page).contains("Sesi Pagi","Sesi Sore","Belum ada pasien dalam antrean","Memantau otomatis","Belum ada pasien yang dilayani");
    }

    @Test void noScheduleTodayShowsEmptyState() throws Exception {
        clock.at("2026-11-30T09:00:00"); auth("dokter2");
        var s=service.snapshot(null); assertThat(s.sesi()).isEmpty(); assertThat(s.panggilan()).isNull(); assertThat(s.totalMenunggu()).isZero();
        var page=get("/dokter/antrean",session("dokter2"));
        assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getContentAsString()).contains("Tidak ada jadwal praktik hari ini");
    }

    @Test void bookingAndCheckInChangeTheSnapshotVersion() {
        auth("dokter2"); var before=service.snapshot(null);
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean) VALUES ('DSHBOOK0001','PAS-001','JDW-002',2,CURRENT_TIMESTAMP,'Terdaftar')");
        em.clear(); var booked=service.snapshot(null);
        assertThat(booked.versi()).isNotEqualTo(before.versi());
        assertThat(booked.sesi().getFirst().antrean()).extracting(DashboardDokterService.Baris::nomorAntrean).containsExactly(1,2);
        jdbc.update("UPDATE Antrean SET statusAntrean='Hadir/Menunggu',waktuCheckIn='2026-11-27 08:55:00' WHERE idAntrean='DSHBOOK0001'");
        em.clear(); var checkedIn=service.snapshot(null);
        assertThat(checkedIn.versi()).isNotEqualTo(booked.versi()); assertThat(checkedIn.totalHadirMenunggu()).isEqualTo(1);
        assertThat(checkedIn.sesi().getFirst().antrean().get(1).waktuCheckIn()).isEqualTo("08:55");
    }

    @Test void otherDoctorsOrOtherDaysScheduleIsNotFoundAndOtherRolesDenied() {
        auth("dokter");
        assertThatThrownBy(()->service.snapshot("JDW-002")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(()->service.snapshot("JDW-003")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(()->service.snapshot("TOO-LONG-SCHEDULE")).isInstanceOf(IllegalArgumentException.class);
        for(String name:List.of("pasien","loket","admin")) { auth(name); assertThatThrownBy(()->service.snapshot(null)).isInstanceOf(AccessDeniedException.class); }
    }

    @Test void endpointsEnforceRoleAndReturnPrivateNoStoreData() throws Exception {
        assertThat(get("/dokter/antrean",new MockHttpSession()).getStatus()).isEqualTo(302);
        for(String name:List.of("pasien","loket","admin")) {
            var other=session(name);
            assertThat(get("/dokter/antrean",other).getStatus()).isEqualTo(403);
            assertThat(get("/dokter/antrean/data",other).getStatus()).isEqualTo(403);
        }
        var dokter=session("dokter");
        var page=get("/dokter/antrean",dokter);
        assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(page.getContentAsString()).contains("Antrean ruang periksa","Jumat, 27 Nov 2026","Poli Umum · Sesi Pagi","Pasien saat ini","A-001","DEMO-RM-002","Sisa antrean","Daftar antrean","4 pasien");
        var data=get("/dokter/antrean/data",dokter);
        assertThat(data.getStatus()).isEqualTo(200); assertThat(data.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(data.getContentAsString()).contains("\"versi\"","JDW-001").doesNotContain("JDW-002","passwordHash","\"nik\"","tanggalLahir");
        assertThat(get("/dokter/antrean/data?idJadwal=JDW-002",dokter).getStatus()).isEqualTo(404);
        assertThat(get("/dokter/antrean?idJadwal=TOO-LONG-SCHEDULE",dokter).getStatus()).isEqualTo(400);
    }

    @Test void doctorCallsPresentAndSkipThroughSharedCallServiceWithCsrf() throws Exception {
        clock.at("2026-11-27T13:00:00"); var dokter2=session("dokter2");
        var page=get("/dokter/antrean",dokter2).getContentAsString();
        assertThat(mvc.perform(MockMvcRequestBuilders.post("/dokter/antrean/JDW-002/panggil").session(dokter2)).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(post("/dokter/antrean/JDW-001/panggil",dokter2,page,Map.of()).getStatus()).isEqualTo(404);
        assertThat(post("/dokter/antrean/JDW-002/panggil",dokter2,page,Map.of("idPengguna","USR-003")).getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT idPengguna FROM PanggilanAntrean WHERE idJadwal='JDW-002'",String.class)).isEqualTo("USR-006");
        assertThat(post("/dokter/antrean/JDW-002/panggil",dokter2,page,Map.of()).getStatus()).isEqualTo(409);

        auth("dokter2"); var token=service.snapshot("JDW-002").panggilan().idPanggilan(); assertThat(token).isNotNull();
        page=get("/dokter/antrean",dokter2).getContentAsString();
        assertThat(page).contains("Pasien hadir, mulai periksa");
        assertThat(post("/dokter/antrean/JDW-002/lewati",dokter2,page,Map.of("idPanggilan",token,"konfirmasi","ya")).getStatus()).isEqualTo(409);
        assertThat(post("/dokter/antrean/JDW-002/hadir",dokter2,page,Map.of("idPanggilan",token)).getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0005'",String.class)).isEqualTo("Sedang Dilayani");
        assertThat(post("/dokter/antrean/JDW-002/hadir",dokter2,page,Map.of("idPanggilan",token)).getStatus()).isEqualTo(409);
        assertThat(post("/dokter/antrean/JDW-002/selesai",dokter2,page,Map.of()).getStatus()).isEqualTo(400);
    }

    @Test void currentPatientShowsCallTimeDurationAndFinishTimeFromStoredData() {
        clock.at("2026-11-27T13:00:00"); auth("dokter2");
        var call=panggilanService(); call.panggil("JDW-002"); var token=service.snapshot(null).panggilan().idPanggilan();
        clock.at("2026-11-27T13:01:00"); call.hadir("JDW-002",token);
        clock.at("2026-11-27T13:15:30"); em.clear();
        var s=service.snapshot(null);
        assertThat(s.pelayanan().pasien().idAntrean()).isEqualTo("ANT-DEMO0005");
        assertThat(s.pelayanan().dipanggil()).isEqualTo("13:00"); assertThat(s.pelayanan().durasiMenit()).isEqualTo(14);
        assertThat(s.panggilan().bolehPanggil()).isFalse(); assertThat(s.panggilan().sedangDilayani()).isTrue();
        jdbc.update("UPDATE Antrean SET statusAntrean='Selesai',waktuSelesai='2026-11-27 13:20:00' WHERE idAntrean='ANT-DEMO0005'"); em.clear();
        var done=service.snapshot(null); assertThat(done.pelayanan()).isNull(); assertThat(done.aktif().selesai()).isEqualTo(1);
        assertThat(done.aktif().antrean().getFirst().waktuSelesai()).isEqualTo("13:20");
    }

    @Autowired PanggilanService panggilanServiceBean;
    private PanggilanService panggilanService() { return panggilanServiceBean; }

    private void auth(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
    private MockHttpSession session(String name) { var user=users.loadUserByUsername(name); var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); var session=new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context); return session; }
    private MockHttpServletResponse get(String url,MockHttpSession session) throws Exception { return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn().getResponse(); }
    private MockHttpServletResponse post(String url,MockHttpSession session,String page,Map<String,String> fields) throws Exception {
        var token=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertThat(token.find()).isTrue();
        var request=MockMvcRequestBuilders.post(url).session(session).param("_csrf",token.group(1)); fields.forEach(request::param);
        return mvc.perform(request).andReturn().getResponse();
    }
}
