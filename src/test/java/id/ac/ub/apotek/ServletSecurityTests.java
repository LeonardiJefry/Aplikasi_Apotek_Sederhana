package id.ac.ub.apotek;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** Real servlet forwards retain POST; MockMvc alone does not follow those forwards. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1")
@ActiveProfiles("test")
class ServletSecurityTests {
    @LocalServerPort int port;
    @Value("${klinik.demo.password}") String password;

    @Test void csrfRejectionRenders403WithoutRedirectOrChangingSession() throws Exception {
        try (var client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5)).build()) {
            send(client, "/login", null);
            var rejectedLogin = send(client, "/login", "username=pasien&password=" + encode(password));
            assertForbidden(rejectedLogin);
            assertThat(send(client, "/pasien", null).statusCode()).isEqualTo(302);

            var token = csrf(send(client, "/login", null).body());
            var login = send(client, "/login", "username=pasien&password=" + encode(password)
                    + "&_csrf=" + encode(token));
            assertThat(login.statusCode()).isEqualTo(302);
            assertThat(login.headers().firstValue("Location").orElseThrow()).doesNotEndWith("/login?error");
            var home = send(client, "/pasien", null);
            assertThat(home.statusCode()).isEqualTo(200);
            assertThat(home.body()).contains("Akun aktif");

            assertForbidden(send(client, "/admin", null));
            assertForbidden(send(client, "/logout", ""));
            assertThat(send(client, "/pasien", null).statusCode()).isEqualTo(200);
            var logout = send(client, "/logout", "_csrf=" + encode(csrf(home.body())));
            assertThat(logout.statusCode()).isEqualTo(302);
            assertThat(send(client, "/pasien", null).statusCode()).isEqualTo(302);
        }
    }

    private HttpResponse<String> send(HttpClient client, String path, String form) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5));
        if (form == null) request.GET();
        else request.header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertForbidden(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("Halaman ini tidak tersedia untuk akun Anda.");
    }

    private String csrf(String page) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page);
        assertThat(match.find()).isTrue();
        return match.group(1);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
