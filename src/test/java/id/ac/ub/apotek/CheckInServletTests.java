package id.ac.ub.apotek;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","spring.datasource.url=jdbc:h2:mem:checkin-servlet;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@Import(CheckInTransactionsTests.ClockFixture.class)
@ActiveProfiles("test")
class CheckInServletTests {
    @LocalServerPort int port;
    @Value("${klinik.demo.password}") String password;
    @Autowired JdbcTemplate jdbc;
    @Test void realHttpCheckInSucceedsAndReplayDoesNotOverwriteTime() throws Exception {
        jdbc.update("UPDATE JadwalPraktik SET tanggal='2026-11-27' WHERE idJadwal='JDW-003'");
        try(var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build()) {
            var login=send(client,"/login",null).body();
            assertThat(send(client,"/login","username=pasien&password="+encode(password)+"&_csrf="+encode(csrf(login))).statusCode()).isEqualTo(302);
            var url="/pasien/antrean/ANT-DEMO0006/check-in";
            var preview=send(client,url,null); assertThat(preview.statusCode()).isEqualTo(200);
            assertThat(send(client,url,"").statusCode()).isEqualTo(403);
            var form="_csrf="+encode(csrf(preview.body()));
            var checked=send(client,url,form); assertThat(checked.statusCode()).isEqualTo(303);
            assertThat(send(client,url+"/hasil",null).body()).contains("Check-in berhasil","Hadir/Menunggu","2026-11-27 08:00:00");
            assertThat(send(client,url,form).statusCode()).isEqualTo(409);
            assertThat(jdbc.queryForObject("SELECT waktuCheckIn FROM Antrean WHERE idAntrean='ANT-DEMO0006'",LocalDateTime.class)).isEqualTo(LocalDateTime.of(2026,11,27,8,0));
            assertThat(jdbc.queryForObject("SELECT sisaKuota FROM JadwalPraktik WHERE idJadwal='JDW-003'",Integer.class)).isEqualTo(9);
        } finally {
            jdbc.update("UPDATE Antrean SET statusAntrean='Terdaftar',waktuCheckIn=NULL WHERE idAntrean='ANT-DEMO0006'");
            jdbc.update("UPDATE JadwalPraktik SET tanggal='2026-11-28' WHERE idJadwal='JDW-003'");
        }
    }
    private HttpResponse<String> send(HttpClient client,String path,String form)throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(8));
        if(form==null)request.GET();else request.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(String page){var match=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page);assertThat(match.find()).isTrue();return match.group(1);}
    private String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
