package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.KuotaVaksinasiService;
import id.ac.ub.apotek.security.*;
import jakarta.servlet.Filter;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class KuotaVaksinasiTests {
    static final String ID="QUOTATEST1",DATE="2026-11-27",TYPE="Vaksin Uji";
    @Autowired KuotaVaksinasiService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired KlinikUserDetailsService users;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO JadwalVaksinasi VALUES (?,? ,?,80,56,true)",ID,LocalDate.parse(DATE),TYPE);
        mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build(); auth("admin");
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void equalQuotaClosesRegistrationAndHigherQuotaReopensIt() {
        var full=save("56"); assertThat(full.sisaKuota()).isZero(); assertThat(full.status()).isEqualTo("Penuh"); assertThat(full.pendaftaranDibuka()).isFalse();
        var available=save("81"); assertThat(available.sisaKuota()).isEqualTo(25); assertThat(available.status()).isEqualTo("Tersedia"); assertThat(available.pendaftaranDibuka()).isTrue();
        assertThat(available.jumlahTerdaftar()).isEqualTo(56);
    }
    @Test void belowRegisteredCountIsRejectedWithoutChangingQuotaOrFlag() {
        assertThatThrownBy(()->save("55")).isInstanceOf(IllegalStateException.class).hasMessageContaining("peserta terdaftar");
        var result=service.pilih(DATE,TYPE,ID); assertThat(result.kuota()).isEqualTo(80); assertThat(result.pendaftaranDibuka()).isTrue();
    }
    @Test void zeroAndMaximumIntegerAreHandledWithoutOverflow() {
        jdbc.update("UPDATE JadwalVaksinasi SET jumlahTerdaftar=0 WHERE idJadwalVaksinasi=?",ID);
        assertThat(save("0").status()).isEqualTo("Penuh");
        assertThat(save("2147483647").sisaKuota()).isEqualTo(Integer.MAX_VALUE);
    }
    @Test void invalidQuotaFormatsAreRejected() {
        for(var invalid:Arrays.asList(null,"","-1","1.5","1e2","+80"," 80 ","2147483648","999999999999999"))
            assertThatThrownBy(()->save(invalid)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.pilih(DATE,TYPE,ID).kuota()).isEqualTo(80);
    }
    @Test void dateTypeMissingAndMismatchedIdAreRejected() {
        for(var invalid:Arrays.asList(null,"","2026-02-30","27-11-2026","2026-1-27"))
            assertThatThrownBy(()->service.cari(invalid,TYPE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari(DATE," ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.pilih(DATE,TYPE,"MISSING")).isInstanceOf(NoSuchElementException.class);
        var before=service.pilih(DATE,TYPE,ID);
        assertThatThrownBy(()->service.simpan(ID,"2026-11-28",TYPE,"90",before.versi())).isInstanceOf(IllegalStateException.class);
    }
    @Test void ambiguousDateAndVaccineRequireExplicitScheduleId() {
        jdbc.update("INSERT INTO JadwalVaksinasi VALUES ('QUOTATEST2',?,?,20,10,true)",LocalDate.parse(DATE),TYPE);
        assertThat(service.cari(DATE,TYPE)).hasSize(2);
        assertThatThrownBy(()->service.pilih(DATE,TYPE,null)).isInstanceOf(IllegalStateException.class);
        assertThat(service.pilih(DATE,TYPE,"QUOTATEST2").kuota()).isEqualTo(20);
    }
    @Test void staleFormAndReplayDoNotOverwriteNewQuota() {
        var original=service.pilih(DATE,TYPE,ID); service.simpan(ID,DATE,TYPE,"60",original.versi());
        assertThatThrownBy(()->service.simpan(ID,DATE,TYPE,"99",original.versi())).isInstanceOf(IllegalStateException.class).hasMessageContaining("berubah");
        assertThat(service.pilih(DATE,TYPE,ID).kuota()).isEqualTo(60);
    }
    @Test void serviceRejectsEveryNonAdminWhilePublicSnapshotHasNoPersonalData() {
        for(var role:List.of("pasien","loket","dokter")) {
            auth(role); assertThatThrownBy(()->service.pilih(DATE,TYPE,ID)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(()->service.simpan(ID,DATE,TYPE,"80","80:56:true")).isInstanceOf(AccessDeniedException.class);
        }
        SecurityContextHolder.clearContext(); assertThat(service.cari(DATE,TYPE)).hasSize(1);
    }
    @Test void anonymousPublicPageAndJsonAreReadOnlyAndPrivateRoutesAreProtected()throws Exception {
        SecurityContextHolder.clearContext();
        var page=get("/vaksinasi",null); assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getContentAsString()).contains("Ketersediaan vaksinasi");
        var json=get("/vaksinasi/data?tanggal="+DATE+"&jenisVaksin=Vaksin%20Uji",null);
        assertThat(json.getStatus()).isEqualTo(200); assertThat(json.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(json.getContentAsString()).contains("\"sisaKuota\":24").doesNotContain("nik","passwordHash","noRekamMedis");
        assertThat(get("/admin/vaksinasi",null).getStatus()).isEqualTo(302);
        for(var role:List.of("pasien","loket","dokter")) assertThat(get("/admin/vaksinasi",session(role)).getStatus()).isEqualTo(403);
    }
    @Test void pageRendersSelectedSummaryAndAmbiguousChoicesWithoutWriting()throws Exception {
        var session=session("admin"); var page=get(url(),session);
        assertThat(page.getStatus()).isEqualTo(200); assertThat(page.getContentAsString()).contains("Ringkasan layanan","80","56","24","Simpan perubahan","_csrf");
        jdbc.update("INSERT INTO JadwalVaksinasi VALUES ('QUOTATEST2',?,?,20,10,true)",LocalDate.parse(DATE),TYPE);
        var ambiguous=get(url(),session); assertThat(ambiguous.getStatus()).isEqualTo(200);
        assertThat(ambiguous.getContentAsString()).contains("Pilih QUOTATEST2").doesNotContain("id=\"kuota-form\"");
    }
    @Test void postChecksCsrfReturns303AndIgnoresClientRegisteredCount()throws Exception {
        var session=session("admin"); var page=get(url(),session);
        assertThat(post(session,null,"56","80:56:true").getStatus()).isEqualTo(403);
        var result=post(session,field(page.getContentAsString(),"_csrf"),"56","80:56:true");
        assertThat(result.getStatus()).isEqualTo(303); assertThat(result.getHeader("Location")).startsWith("/admin/vaksinasi?");
        assertThat(jdbc.queryForObject("SELECT pendaftaranDibuka FROM JadwalVaksinasi WHERE idJadwalVaksinasi=?",Boolean.class,ID)).isFalse();
        assertThat(post(session,field(page.getContentAsString(),"_csrf"),"100","80:56:true").getStatus()).isEqualTo(409);
    }
    private KuotaVaksinasiService.Layanan save(String quota) { return service.simpan(ID,DATE,TYPE,quota,service.pilih(DATE,TYPE,ID).versi()); }
    private String url() { return "/admin/vaksinasi?tanggal="+DATE+"&jenisVaksin=Vaksin%20Uji"; }
    private MockHttpServletResponse get(String path,MockHttpSession session)throws Exception {
        var req=MockMvcRequestBuilders.get(java.net.URI.create(path)); if(session!=null) req.session(session);
        return mvc.perform(req).andReturn().getResponse();
    }
    private MockHttpServletResponse post(MockHttpSession session,String csrf,String quota,String version)throws Exception {
        var req=MockMvcRequestBuilders.post("/admin/vaksinasi/kuota").session(session).param("tanggal",DATE).param("jenisVaksin",TYPE)
                .param("idJadwalVaksinasi",ID).param("kuotaBaru",quota).param("versi",version).param("jumlahTerdaftar","0").param("pendaftaranDibuka","true");
        if(csrf!=null) req.param("_csrf",csrf); return mvc.perform(req).andReturn().getResponse();
    }
    private String field(String html,String name) { var m=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]+)\"").matcher(html); assertThat(m.find()).isTrue(); return m.group(1); }
    private void auth(String name) { var u=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.getAuthorities())); }
    private MockHttpSession session(String name) { auth(name); var s=new MockHttpSession(); s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,SecurityContextHolder.getContext()); SecurityContextHolder.clearContext(); return s; }
}
