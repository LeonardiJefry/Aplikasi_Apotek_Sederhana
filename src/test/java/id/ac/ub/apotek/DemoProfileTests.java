package id.ac.ub.apotek;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** Run the same authentication/seed checks using the actual runtime demo profile. */
@ActiveProfiles(value = "demo", inheritProfiles = false)
class DemoProfileTests extends ApotekApplicationTests {
    @Autowired DataSource dataSource;
    @Autowired Environment environment;

    @Test void demoUsesLocalMemoryDatabaseWithoutPrivateEnv() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:klinik-demo");
        }
        assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(environment.getProperty("spring.config.import")).isNull();
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Pengguna", Integer.class)).isEqualTo(6);
    }
}
