package id.ac.ub.apotek;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/** No ActiveProfiles: checks the same defaults used by a new clone. */
@SpringBootTest
class DefaultDemoProfileTests {
    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;

    @Test void defaultStartsWithSyntheticH2DataAndNoPrivateEnv() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getDefaultProfiles()).containsExactly("demo");
        assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:klinik-demo;");
        assertThat(environment.getProperty("spring.config.import")).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Pengguna", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Antrean", Integer.class)).isEqualTo(9);
    }
}
