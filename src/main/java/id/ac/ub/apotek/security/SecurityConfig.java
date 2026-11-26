package id.ac.ub.apotek.security;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean Clock clinicClock() { return Clock.system(ZoneId.of("Asia/Jakarta")); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/fonts/**", "/error", "/akses-ditolak").permitAll()
                .requestMatchers("/").authenticated()
                .requestMatchers("/pasien", "/pasien/**").hasRole("PASIEN")
                .requestMatchers("/loket", "/loket/**").hasRole("PETUGAS_LOKET")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/dokter", "/dokter/**").hasRole("DOKTER")
                .anyRequest().denyAll())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true)
                        .failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/akses-ditolak"))
                .build(); // CSRF dan proteksi session fixation tetap aktif.
    }
}
