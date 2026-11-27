package id.ac.ub.apotek;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import id.ac.ub.apotek.demo.DemoDataService;
import id.ac.ub.apotek.controller.AntreanAccessService;
import id.ac.ub.apotek.security.KlinikUserDetailsService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.servlet.Filter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ApotekApplicationTests {
    @org.springframework.beans.factory.annotation.Value("${klinik.demo.password}")
    String demoPassword;
    MockMvc mvc;
    @Autowired WebApplicationContext web;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilter;
    @BeforeEach void setupMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(web).addFilters(securityFilter).build();
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DemoDataService demo;
    @Autowired PasswordEncoder encoder;
    @Autowired KlinikUserDetailsService users;
    @Autowired AntreanAccessService access;

    @Test void anonymousRedirectAndLoginTemplate() throws Exception {
        var client = client();
        assertThat(get(client, "/").getStatus()).isEqualTo(302);
        var login = get(client, "/login");
        assertThat(login.getStatus()).isEqualTo(200);
        assertThat(login.getContentAsString()).contains("Klinik Leonardo", "name=\"_csrf\"", "href=\"/css/app.css\"").doesNotContain("th:href");
        assertThat(get(client, "/css/app.css").getStatus()).isEqualTo(200);
    }

    @Test void anonymousCanRenderForbiddenPageWithoutLogin() throws Exception {
        var response = get(client(), "/akses-ditolak");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getContentAsString()).contains("Halaman ini tidak tersedia untuk akun Anda.");
    }

    @Test void forbiddenPageAcceptsForwardedPostAndKeepsForbiddenStatus() throws Exception {
        var client = client();
        var token = csrf(get(client, "/login").getContentAsString());
        var request = MockMvcRequestBuilders.post("/akses-ditolak")
                .with(forward -> {
                    forward.setDispatcherType(jakarta.servlet.DispatcherType.FORWARD);
                    return forward;
                })
                .param("_csrf", token).session(client.session);
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getContentAsString()).contains("Halaman ini tidak tersedia untuk akun Anda.");
    }

    @Test void allRolesLoginAndRejectEveryOtherRoleArea() throws Exception {
        var roles = Map.of("pasien", "/pasien", "loket", "/loket", "admin", "/admin", "dokter", "/dokter");
        for (var entry : roles.entrySet()) {
            var client = client();
            login(client, entry.getKey(), demoPassword);
            var root = get(client, "/");
            assertThat(root.getStatus()).isEqualTo(302);
            assertThat(root.getHeader("Location")).endsWith(entry.getValue());
            var home = get(client, entry.getValue());
            assertThat(home.getStatus()).isEqualTo(200);
            assertThat(home.getContentAsString()).contains("Akun aktif", entry.getKey());
            for (String area : roles.values()) {
                if (!area.equals(entry.getValue())) {
                    assertThat(get(client, area).getStatus()).as(entry.getKey()+" cannot enter "+area).isEqualTo(403);
                    assertThat(get(client, area + "/future-feature").getStatus()).isEqualTo(403);
                }
            }
            assertThat(get(client, "/unconfigured-route").getStatus()).isEqualTo(403);
        }
    }

    @Test void invalidPasswordCsrfAndLogout() throws Exception {
        var client = client();
        assertThat(post(client, "/login", "username=pasien&password=wrong").getStatus()).isEqualTo(403);
        var response = login(client, "pasien", "wrong");
        assertThat(response.getHeader("Location")).endsWith("/login?error");
        assertThat(get(client, "/login?error").getContentAsString()).contains("Nama pengguna atau kata sandi salah");
        login(client, "pasien", demoPassword);
        assertThat(post(client, "/logout", "").getStatus()).isEqualTo(403);
        var token = csrf(get(client, "/pasien").getContentAsString());
        assertThat(post(client, "/logout", "_csrf=" + encode(token)).getStatus()).isEqualTo(302);
        assertThat(get(client, "/pasien").getStatus()).isEqualTo(302);
    }

    @Test void identitiesAndOwnershipComeFromAuthenticatedSession() {
        try {
            authenticate("pasien");
            assertThat(access.requirePasienTicket("ANT-DEMO0001").idJadwal()).isEqualTo("JDW-001");
            assertThatThrownBy(() -> access.requirePasienTicket("ANT-DEMO0002")).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> access.requireDokterTicket("ANT-DEMO0001")).isInstanceOf(AccessDeniedException.class);
            authenticate("dokter");
            assertThat(access.requireDokterTicket("ANT-DEMO0001").nomorAntrean()).isEqualTo(1);
            assertThatThrownBy(() -> access.requireDokterTicket("ANT-DEMO0005")).isInstanceOf(AccessDeniedException.class);
            authenticate("dokter2");
            assertThat(access.requireDokterTicket("ANT-DEMO0005").idJadwal()).isEqualTo("JDW-002");
            authenticate("pasien2");
            assertThat(access.requirePasienTicket("ANT-DEMO0002").nomorAntrean()).isEqualTo(2);
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void disabledAccountCannotLogin() throws Exception {
        jdbc.update("UPDATE Pengguna SET aktif = false WHERE username = 'pasien2'");
        try {
            var response = login(client(), "pasien2", demoPassword);
            assertThat(response.getHeader("Location")).endsWith("/login?error");
        } finally { jdbc.update("UPDATE Pengguna SET aktif = true WHERE username = 'pasien2'"); }
    }

    @Test void migrationAndSeedCanRunAgainWithoutChanges() {
        var before = counts();
        assertThat(before).containsEntry("Pasien", 2).containsEntry("Poli", 2).containsEntry("Dokter", 2)
                .containsEntry("JadwalPraktik", 4).containsEntry("Antrean", 9).containsEntry("Pengguna", 6);
        var hash = jdbc.queryForObject("SELECT passwordHash FROM Pengguna WHERE username='pasien'", String.class);
        assertThat(hash).startsWith("$2a$");
        assertThat(encoder.matches(demoPassword, hash)).isTrue();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        demo.seed(); demo.seed();
        assertThat(counts()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT passwordHash FROM Pengguna WHERE username='pasien'", String.class)).isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM JadwalPraktik j WHERE j.sisaKuota <> j.kuotaTotal - (SELECT COUNT(*) FROM Antrean a WHERE a.idJadwal=j.idJadwal AND a.statusAntrean NOT IN ('Dibatalkan','Tidak Hadir'))", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT statusAntrean) FROM Antrean", Integer.class)).isEqualTo(8);
    }

    private Map<String,Integer> counts() {
        var counts = new LinkedHashMap<String,Integer>();
        for (String table : List.of("Pasien","Poli","Dokter","JadwalPraktik","Antrean","JadwalVaksinasi","Notifikasi","RiwayatPelewatan","Pengguna","PanggilanAntrean")) {
            counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
        return counts;
    }
    private void authenticate(String username) {
        var user = users.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
    private Browser client() { return new Browser(); }
    private static class Browser { MockHttpSession session; }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String csrf(String html) {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as("rendered CSRF hidden input").isTrue();
        return matcher.group(1);
    }
    private MockHttpServletResponse login(Browser client, String username, String password) throws Exception {
        String token = csrf(get(client, "/login").getContentAsString());
        return post(client, "/login", "username="+encode(username)+"&password="+encode(password)+"&_csrf="+encode(token));
    }
    private MockHttpServletResponse get(Browser browser, String path) throws Exception {
        var request = MockMvcRequestBuilders.get(path);
        if (browser.session != null) request.session(browser.session);
        var result = mvc.perform(request).andReturn();
        browser.session = (MockHttpSession) result.getRequest().getSession(false);
        return result.getResponse();
    }
    private MockHttpServletResponse post(Browser browser, String path, String form) throws Exception {
        var request = MockMvcRequestBuilders.post(path);
        if (browser.session != null) request.session(browser.session);
        if (!form.isEmpty()) {
            for (String pair : form.split("&")) {
                String[] parts = pair.split("=", 2);
                request.param(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        var result = mvc.perform(request).andReturn();
        browser.session = (MockHttpSession) result.getRequest().getSession(false);
        return result.getResponse();
    }
}
