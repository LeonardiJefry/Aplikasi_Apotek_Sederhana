package id.ac.ub.apotek;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","spring.datasource.url=jdbc:h2:mem:panggilan-servlet;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@Import(PanggilanTests.TimeFixture.class) @ActiveProfiles("test")
class PanggilanServletTests {
    @LocalServerPort int port;
    @Value("${klinik.demo.password}") String password;
    @Autowired JdbcTemplate jdbc;
    @Autowired PanggilanTests.MutableClock clock;
    @Test void realHttpCallWaitSkipAndReplayRemainConsistent()throws Exception {
        clock.at("2026-11-27T08:00:00");
        jdbc.update("UPDATE JadwalPraktik SET tanggal='2026-11-27' WHERE idJadwal='JDW-003'");
        try(var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build()) {
            var login=send(client,"/login",null).body();
            assertThat(send(client,"/login","username=loket&password="+encode(password)+"&_csrf="+encode(field(login,"_csrf"))).statusCode()).isEqualTo(302);
            String pageUrl="/loket/panggilan?idJadwal=JDW-003", action="/loket/panggilan/JDW-003/";
            var page=send(client,pageUrl,null); assertThat(page.statusCode()).isEqualTo(200);
            assertThat(send(client,action+"panggil","").statusCode()).isEqualTo(403);
            assertThat(send(client,action+"panggil","_csrf="+encode(field(page.body(),"_csrf"))).statusCode()).isEqualTo(303);
            var active=send(client,pageUrl,null); assertThat(active.body()).contains("Sedang dipanggil","120 detik");
            var token=field(active.body(),"idPanggilan"); var form="_csrf="+encode(field(active.body(),"_csrf"))+"&idPanggilan="+encode(token)+"&konfirmasi=ya";
            clock.advance(119); assertThat(send(client,action+"lewati",form).statusCode()).isEqualTo(409);
            clock.advance(1); assertThat(send(client,action+"lewati",form).statusCode()).isEqualTo(303);
            assertThat(send(client,action+"lewati",form).statusCode()).isEqualTo(409);
            assertThat(jdbc.queryForObject("SELECT statusAntrean FROM Antrean WHERE idAntrean='ANT-DEMO0006'",String.class)).isEqualTo("Dilewati");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RiwayatPelewatan WHERE idAntrean='ANT-DEMO0006'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-003'",Integer.class)).isEqualTo(9);
        } finally {
            jdbc.update("DELETE FROM PanggilanAntrean WHERE idJadwal='JDW-003'");
            jdbc.update("DELETE FROM RiwayatPelewatan WHERE idAntrean='ANT-DEMO0006'");
            jdbc.update("UPDATE Antrean SET statusAntrean='Terdaftar' WHERE idAntrean='ANT-DEMO0006'");
            jdbc.update("UPDATE JadwalPraktik SET tanggal='2026-11-28' WHERE idJadwal='JDW-003'");
        }
    }
    private HttpResponse<String> send(HttpClient client,String path,String form)throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(8));
        if(form==null) request.GET(); else request.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    private String field(String html,String name) { var match=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]+)\"").matcher(html); assertThat(match.find()).isTrue(); return match.group(1); }
    private String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
}
