package id.ac.ub.apotek;

import id.ac.ub.apotek.controller.VerifikasiPasienService;
import id.ac.ub.apotek.security.*;
import jakarta.servlet.Filter;
import jakarta.persistence.EntityManager;
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
class VerifikasiPasienTests {
    @Autowired VerifikasiPasienService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired KlinikUserDetailsService users;
    @Autowired EntityManager em;
    @Autowired Clock clock;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;
    @BeforeEach void setup() { auth("loket"); mvc=MockMvcBuilders.webAppContextSetup(web).addFilters(security).build(); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void searchesNikRecordNumberAndPartialNameWithoutLeakingFullNikInResults() {
        assertThat(service.cari("NIK","0000000000000001",0).getContent()).extracting(VerifikasiPasienService.Hasil::idPasien).containsExactly("PAS-001");
        assertThat(service.cari("NO_RM"," DEMO-RM-002 ",0).getContent()).extracting(VerifikasiPasienService.Hasil::idPasien).containsExactly("PAS-002");
        var results=service.cari("NAMA","pAsIeN DeMo",0); assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).allSatisfy(p->assertThat(p.nikTersamar()).contains("••••").doesNotContain("000000000000000"));
    }
    @Test void nameSearchEscapesSqlWildcardsAndHandlesEmptyResults() {
        assertThat(service.cari("NAMA","%%",0)).isEmpty(); assertThat(service.cari("NAMA","__",0)).isEmpty();
        assertThat(service.cari("NAMA","Tidak Ada",0)).isEmpty();
        assertThat(service.cari("NO_RM","' OR '1'='1",0)).isEmpty();
    }
    @Test void validatesSearchTypeKeywordAndPage() {
        for(var word:Arrays.asList(null,"","123","123456789012345a")) assertThatThrownBy(()->service.cari("NIK",word,0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari("X","Demo",0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari("NAMA","a",0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari("NAMA","a".repeat(101),0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari("NO_RM","x".repeat(21),0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.cari("NAMA","Demo",-1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void searchPaginationHasStableOrderAndAllowsExplicitSelection() {
        for(int i=0;i<23;i++) jdbc.update("INSERT INTO Pasien VALUES (?,?,?,?,?)",String.format("PAGE%03d",i),String.format("999999999999%04d",i),"PAGE-RM-"+i,"Pagination Demo "+String.format("%02d",i),LocalDate.of(2000,1,1));
        var first=service.cari("NAMA","Pagination Demo",0); var last=service.cari("NAMA","Pagination Demo",1);
        assertThat(first.getTotalElements()).isEqualTo(23); assertThat(first.getContent()).hasSize(20); assertThat(last.getContent()).hasSize(3);
        assertThat(first.getContent()).doesNotContainAnyElementsOf(last.getContent());
    }
    @Test void patientHistoryIsSeparateAndCompletedVisitsContainOnlySelesai() {
        var d=service.detail("PAS-001",0,0);
        assertThat(d.pasien().noRekamMedis()).isEqualTo("DEMO-RM-001"); assertThat(d.pendaftaran().getTotalElements()).isEqualTo(4);
        assertThat(d.pendaftaran().getContent()).extracting(VerifikasiPasienService.Riwayat::idAntrean).doesNotContain("ANT-DEMO0002","ANT-DEMO0008","ANT-DEMO0009");
        assertThat(d.kunjungan().getContent()).extracting(VerifikasiPasienService.Riwayat::idAntrean).containsExactly("ANT-DEMO0007");
        assertThat(service.detail("PAS-002",0,0).kunjungan()).isEmpty();
        assertThatThrownBy(()->service.detail("MISSING",0,0)).isInstanceOf(NoSuchElementException.class);
    }
    @Test void verificationPersistsSessionActorTimeAndReplayReturnsSameEvent() {
        var before=service.detail("PAS-001",0,0); String token=UUID.randomUUID().toString();
        var start=LocalDateTime.now(clock).minusSeconds(1); var saved=service.verifikasi("PAS-001",token,before.sidikIdentitas(),true);
        em.flush(); em.clear(); var again=service.verifikasi("PAS-001",token,before.sidikIdentitas(),true);
        assertThat(again).isEqualTo(saved); assertThat(saved.idPetugas()).isEqualTo("USR-003"); assertThat(saved.petugas()).isEqualTo("loket");
        assertThat(LocalDateTime.parse(saved.waktu())).isAfter(start).isBefore(LocalDateTime.now(clock).plusSeconds(1));
        assertThat(service.detail("PAS-001",0,0).terverifikasi()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM VerifikasiPasien WHERE idVerifikasi=?",Integer.class,token)).isEqualTo(1);
        assertThat(service.detail("PAS-002",0,0).verifikasi()).isEmpty();
    }
    @Test void mismatchedPatientChangedIdentityAndMissingConfirmationAreRejected() {
        var d=service.detail("PAS-001",0,0); String token=UUID.randomUUID().toString();
        assertThatThrownBy(()->service.verifikasi("PAS-001",token,d.sidikIdentitas(),false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.verifikasi("PAS-002",token,d.sidikIdentitas(),true)).isInstanceOf(IllegalStateException.class);
        jdbc.update("UPDATE Pasien SET nama='Nama Berubah' WHERE idPasien='PAS-001'"); em.clear();
        assertThatThrownBy(()->service.verifikasi("PAS-001",token,d.sidikIdentitas(),true)).isInstanceOf(IllegalStateException.class);
        assertThat(service.detail("PAS-001",0,0).verifikasi()).isEmpty();
    }
    @Test void changedIdentityAfterVerificationRequiresNewReviewAndLeavesAuditIntact() {
        var d=service.detail("PAS-001",0,0); service.verifikasi("PAS-001",UUID.randomUUID().toString(),d.sidikIdentitas(),true);
        jdbc.update("UPDATE Pasien SET nama='Nama Terbaru' WHERE idPasien='PAS-001'"); em.clear();
        var changed=service.detail("PAS-001",0,0); assertThat(changed.terverifikasi()).isFalse(); assertThat(changed.verifikasi()).hasSize(1);
    }
    @Test void roleRestrictionAppliesToAllServiceOperations() {
        for(var name:List.of("pasien","admin","dokter")) { auth(name);
            assertThatThrownBy(()->service.cari("NAMA","Demo",0)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(()->service.detail("PAS-001",0,0)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(()->service.verifikasi("PAS-001",null,null,true)).isInstanceOf(AccessDeniedException.class);
        }
    }
    @Test void pagesAreReadOnlyNoStoreAndDoNotAutoSelectMultipleResults()throws Exception {
        var session=session("loket"); var results=get("/loket/pasien?jenis=NAMA&kataKunci=Demo",session);
        assertThat(results.getStatus()).isEqualTo(200); assertThat(results.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(results.getContentAsString()).contains("2 pasien ditemukan","PAS-001","PAS-002").doesNotContain("0000000000000001","name=\"idVerifikasi\"");
        var detail=get("/loket/pasien/PAS-001",session); assertThat(detail.getStatus()).isEqualTo(200);
        assertThat(detail.getContentAsString()).contains("Riwayat kunjungan","Riwayat pendaftaran","DEMO-RM-001","Verifikasi identitas").doesNotContain("passwordHash","ANT-DEMO0008");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM VerifikasiPasien",Integer.class)).isZero();
    }
    @Test void postRequiresCsrfConfirmationAndIgnoresForgedActorAndTimestamp()throws Exception {
        var session=session("loket"); var page=get("/loket/pasien/PAS-001",session).getContentAsString();
        assertThat(post(session,page,false,true).getStatus()).isEqualTo(403);
        assertThat(post(session,page,true,false).getStatus()).isEqualTo(400);
        assertThat(post(session,page,true,true).getStatus()).isEqualTo(303);
        assertThat(post(session,page,true,true).getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM VerifikasiPasien",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT idPetugas FROM VerifikasiPasien",String.class)).isEqualTo("USR-003");
        assertThat(jdbc.queryForObject("SELECT waktuVerifikasi FROM VerifikasiPasien",LocalDateTime.class)).isNotEqualTo(LocalDateTime.parse("1990-01-01T00:00:00"));
    }
    @Test void anonymousAndOtherRolesCannotSearchOrReadDetail()throws Exception {
        assertThat(get("/loket/pasien",null).getStatus()).isEqualTo(302);
        for(var name:List.of("pasien","admin","dokter")) {
            var session=session(name); assertThat(get("/loket/pasien?jenis=NAMA&kataKunci=Demo",session).getStatus()).isEqualTo(403);
            assertThat(get("/loket/pasien/PAS-001",session).getStatus()).isEqualTo(403);
        }
    }
    private MockHttpServletResponse get(String path,MockHttpSession session)throws Exception { var req=MockMvcRequestBuilders.get(java.net.URI.create(path)); if(session!=null) req.session(session); return mvc.perform(req).andReturn().getResponse(); }
    private MockHttpServletResponse post(MockHttpSession session,String page,boolean csrf,boolean confirmed)throws Exception {
        var req=MockMvcRequestBuilders.post("/loket/pasien/PAS-001/verifikasi").session(session).param("idVerifikasi",field(page,"idVerifikasi"))
            .param("sidikIdentitas",field(page,"sidikIdentitas")).param("idPetugas","USR-004").param("waktuVerifikasi","1990-01-01T00:00:00");
        if(csrf) req.param("_csrf",field(page,"_csrf")); if(confirmed) req.param("konfirmasi","ya"); return mvc.perform(req).andReturn().getResponse();
    }
    private String field(String html,String name) { var m=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]+)\"").matcher(html); assertThat(m.find()).isTrue(); return m.group(1); }
    private void auth(String name) { var u=users.loadUserByUsername(name); SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,u.getAuthorities())); }
    private MockHttpSession session(String name) { auth(name); var s=new MockHttpSession(); s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,SecurityContextHolder.getContext()); SecurityContextHolder.clearContext(); return s; }
}
