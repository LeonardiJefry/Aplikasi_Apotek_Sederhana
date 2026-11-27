package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.*;
import id.ac.ub.apotek.security.*;
import jakarta.servlet.Filter;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

/**
 * UC-10 / PC-10. Tidak memakai @Transactional karena notifikasi loket dibuat pada transaksi baru sesudah
 * status Selesai di-commit; fixture dibuat dan dihapus pada setiap tes.
 */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pemeriksaan-selesai;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test")
class PemeriksaanTests {
    static final String JADWAL="SLS-01", DILAYANI="SLSTICKET01", HADIR="SLSTICKET02", MENUNGGU="SLSTICKET03";
    static final String BASE="/dokter/antrean/"+JADWAL+"/";
    @Autowired JdbcTemplate jdbc;
    @Autowired PemeriksaanService pemeriksaan;
    @Autowired NotifikasiLoketService notifikasi;
    @Autowired DashboardDokterService dashboard;
    @Autowired PanggilanTests.MutableClock clock;
    @Autowired KlinikUserDetailsService users;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;

    @BeforeEach void setup() {
        clock.at("2026-11-27T09:00:00");
        jdbc.update("INSERT INTO JadwalPraktik (idJadwal,idDokter,idPoli,tanggal,sesi,jamMulai,jamSelesai,kuotaTotal,sisaKuota) VALUES (?,'DOK-001','POL-001','2026-11-27','Uji','08:00','14:00',5,2)",JADWAL);
        ticket(DILAYANI,1,"Sedang Dilayani","2026-11-27 08:10:00");
        ticket(HADIR,2,"Hadir/Menunggu","2026-11-27 08:20:00");
        ticket(MENUNGGU,3,"Menunggu",null);
        mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build();
    }
    @AfterEach void cleanup() {
        notifikasi.setSimulasiGagal(false); SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM Notifikasi WHERE idAntrean LIKE 'SLSTICKET%'");
        jdbc.update("DELETE FROM PanggilanAntrean WHERE idJadwal=?",JADWAL);
        jdbc.update("DELETE FROM RiwayatPelewatan WHERE idAntrean LIKE 'SLSTICKET%'");
        jdbc.update("DELETE FROM Antrean WHERE idJadwal=?",JADWAL);
        jdbc.update("DELETE FROM JadwalPraktik WHERE idJadwal=?",JADWAL);
    }

    @Test void finishSavesStatusTimeAndOnePendingNotificationThenNextPatientCanBeCalled() throws Exception {
        var dokter=session("dokter"); var page=page(dokter);
        assertThat(page).contains("Tekan setelah pemeriksaan selesai","name=\"idAntrean\" value=\""+DILAYANI+"\"");
        clock.at("2026-11-27T09:12:30.700");
        var result=postResult(BASE+"selesai",dokter,page,Map.of("idAntrean",DILAYANI,"waktuSelesai","2000-01-01T00:00:00","statusAntrean","Menunggu"));
        assertThat(result.getResponse().getStatus()).isEqualTo(303);
        assertThat(state(DILAYANI)).isEqualTo("Selesai");
        assertThat(finished(DILAYANI)).isEqualTo(LocalDateTime.parse("2026-11-27T09:12:30"));
        assertThat(notifications(DILAYANI)).containsExactly("Tertunda");
        assertThat((Map<String,Object>)result.getFlashMap()).containsEntry("selesaiNomor","A-001").containsEntry("statusNotifikasi","Tertunda").containsEntry("selesaiWaktu","09:12");

        var toast=mvc.perform(MockMvcRequestBuilders.get("/dokter/antrean?idJadwal="+JADWAL).session(dokter).flashAttrs(result.getFlashMap())).andReturn().getResponse().getContentAsString();
        assertThat(toast).contains("Pemeriksaan A-001 selesai","status tersimpan pukul 09:12","Terakhir diselesaikan","Panggil pasien");

        auth("dokter"); var s=dashboard.snapshot(JADWAL);
        assertThat(s.pelayanan()).isNull(); assertThat(s.aktif().selesai()).isEqualTo(1);
        assertThat(s.terakhir().pasien().idAntrean()).isEqualTo(DILAYANI); assertThat(s.terakhir().statusNotifikasi()).isEqualTo("Tertunda");
        // PC-10 langkah 5/7: antrean aktif diperbarui dan pasien berikutnya siap dipanggil melalui layanan UC-06.
        assertThat(s.panggilan().bolehPanggil()).isTrue(); assertThat(s.panggilan().berikutnya().idAntrean()).isEqualTo(HADIR);
        assertThat(s.aktif().menunggu()).isEqualTo(1);
    }

    @Test void repeatOrNonServingStatusIsRejectedWithoutChangingTimestampOrNotifications() throws Exception {
        var dokter=session("dokter"); var page=page(dokter);
        assertThat(post(BASE+"selesai",dokter,page,Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(303);
        clock.at("2026-11-27T09:30:00");
        var repeat=post(BASE+"selesai",dokter,page,Map.of("idAntrean",DILAYANI));
        assertThat(repeat.getStatus()).isEqualTo(409); assertThat(repeat.getContentAsString()).contains("Status pasien tidak dapat diselesaikan","A-001 berstatus Selesai");
        assertThat(finished(DILAYANI)).isEqualTo(LocalDateTime.parse("2026-11-27T09:00:00")); assertThat(notifications(DILAYANI)).hasSize(1);
        for(String id:List.of(HADIR,MENUNGGU)) {
            var invalid=post(BASE+"selesai",dokter,page,Map.of("idAntrean",id));
            assertThat(invalid.getStatus()).isEqualTo(409); assertThat(finished(id)).isNull(); assertThat(notifications(id)).isEmpty();
        }
        assertThat(state(HADIR)).isEqualTo("Hadir/Menunggu"); assertThat(state(MENUNGGU)).isEqualTo("Menunggu");
        assertThat(post(BASE+"selesai",dokter,page,Map.of()).getStatus()).isEqualTo(400);
    }

    @Test void otherDoctorWrongScheduleOtherRolesAndMissingCsrfCannotFinish() throws Exception {
        auth("dokter2"); assertThatThrownBy(()->pemeriksaan.selesai(JADWAL,DILAYANI)).isInstanceOf(NoSuchElementException.class);
        auth("dokter"); assertThatThrownBy(()->pemeriksaan.selesai(JADWAL,"ANT-DEMO0005")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(()->pemeriksaan.selesai("JDW-002","ANT-DEMO0005")).isInstanceOf(NoSuchElementException.class);
        for(String name:List.of("pasien","loket","admin")) { auth(name); assertThatThrownBy(()->pemeriksaan.selesai(JADWAL,DILAYANI)).isInstanceOf(AccessDeniedException.class); }
        var dokter2=session("dokter2");
        assertThat(post(BASE+"selesai",dokter2,page(dokter2),Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(404);
        for(String name:List.of("pasien","loket")) {
            var other=session(name); var login=mvc.perform(MockMvcRequestBuilders.get("/login").session(other)).andReturn().getResponse().getContentAsString();
            assertThat(post(BASE+"selesai",other,login,Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(403);
        }
        assertThat(mvc.perform(MockMvcRequestBuilders.post(BASE+"selesai").param("idAntrean",DILAYANI).session(session("dokter"))).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(state(DILAYANI)).isEqualTo("Sedang Dilayani"); assertThat(finished(DILAYANI)).isNull(); assertThat(notifications(DILAYANI)).isEmpty();
    }

    @Test void notificationFailureKeepsFinishedStatusAndRetryDoesNotDuplicate() throws Exception {
        notifikasi.setSimulasiGagal(true);
        var dokter=session("dokter"); var page=page(dokter);
        var result=postResult(BASE+"selesai",dokter,page,Map.of("idAntrean",DILAYANI));
        assertThat(result.getResponse().getStatus()).isEqualTo(303); assertThat((Map<String,Object>)result.getFlashMap()).containsEntry("statusNotifikasi","Gagal");
        assertThat(state(DILAYANI)).isEqualTo("Selesai"); assertThat(finished(DILAYANI)).isNotNull();
        assertThat(notifications(DILAYANI)).containsExactly("Gagal");
        var reloaded=mvc.perform(MockMvcRequestBuilders.get("/dokter/antrean?idJadwal="+JADWAL).session(dokter).flashAttrs(result.getFlashMap())).andReturn().getResponse();
        assertThat(reloaded.getStatus()).isEqualTo(200);
        assertThat(reloaded.getContentAsString()).contains("Status tersimpan, notifikasi tertunda","Kirim ulang notifikasi");

        page=page(dokter);
        assertThat(post(BASE+"notifikasi",dokter,page,Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(303);
        assertThat(notifications(DILAYANI)).containsExactly("Gagal");
        notifikasi.setSimulasiGagal(false);
        for(int i=0;i<2;i++) assertThat(post(BASE+"notifikasi",dokter,page,Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(303);
        assertThat(notifications(DILAYANI)).containsExactly("Tertunda");
        assertThat(page(dokter)).doesNotContain("Kirim ulang notifikasi");
        assertThat(post(BASE+"notifikasi",dokter,page,Map.of("idAntrean",HADIR)).getStatus()).isEqualTo(409);
        assertThat(notifications(HADIR)).isEmpty();
    }

    @Test void loketPanelReadsStoredNotificationWithoutWriting() throws Exception {
        var dokter=session("dokter");
        assertThat(post(BASE+"selesai",dokter,page(dokter),Map.of("idAntrean",DILAYANI)).getStatus()).isEqualTo(303);
        var loket=mvc.perform(MockMvcRequestBuilders.get("/loket/panggilan?idJadwal="+JADWAL).session(session("loket"))).andReturn().getResponse();
        assertThat(loket.getStatus()).isEqualTo(200);
        assertThat(loket.getContentAsString()).contains("Notifikasi ruang periksa","Pemeriksaan A-001 selesai","09:00 WIB","Tertunda");
        assertThat(notifications(DILAYANI)).containsExactly("Tertunda");
        assertThat(jdbc.queryForObject("SELECT waktuKirim FROM Notifikasi WHERE idAntrean=?",LocalDateTime.class,DILAYANI)).isNull();
        auth("dokter"); assertThatThrownBy(()->notifikasi.daftarUntukLoket(JADWAL)).isInstanceOf(AccessDeniedException.class);
        auth("loket"); assertThat(notifikasi.daftarUntukLoket(JADWAL)).extracting(NotifikasiLoketService.Item::statusKirim).containsExactly("Tertunda");
    }

    private void ticket(String id,int n,String status,String checkIn) {
        jdbc.update("INSERT INTO Antrean (idAntrean,idPasien,idJadwal,nomorAntrean,tglBooking,statusAntrean,waktuCheckIn) VALUES (?,'PAS-001',?,?,CURRENT_TIMESTAMP,?,?)",id,JADWAL,n,status,checkIn==null?null:LocalDateTime.parse(checkIn.replace(' ','T')));
    }
    private String state(String id) { return jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean=?",String.class,id); }
    private LocalDateTime finished(String id) { return jdbc.queryForObject("SELECT waktuSelesai FROM Antrean WHERE idAntrean=?",LocalDateTime.class,id); }
    private List<String> notifications(String id) { return jdbc.queryForList("SELECT statusKirim FROM Notifikasi WHERE idAntrean=?",String.class,id); }
    private void auth(String name) { var user=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); }
    private MockHttpSession session(String name) { var user=users.loadUserByUsername(name); var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities())); var session=new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context); return session; }
    private String page(MockHttpSession session) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get("/dokter/antrean?idJadwal="+JADWAL).session(session)).andReturn().getResponse().getContentAsString();
    }
    private MockHttpServletResponse post(String url,MockHttpSession session,String page,Map<String,String> fields) throws Exception { return postResult(url,session,page,fields).getResponse(); }
    private MvcResult postResult(String url,MockHttpSession session,String page,Map<String,String> fields) throws Exception {
        var token=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertThat(token.find()).isTrue();
        var request=MockMvcRequestBuilders.post(url).session(session).param("_csrf",token.group(1)); fields.forEach(request::param);
        return mvc.perform(request).andReturn();
    }
}
