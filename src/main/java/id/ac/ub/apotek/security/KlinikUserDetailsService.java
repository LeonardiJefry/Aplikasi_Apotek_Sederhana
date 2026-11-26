package id.ac.ub.apotek.security;

import id.ac.ub.apotek.repository.PenggunaRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KlinikUserDetailsService implements UserDetailsService {
    private final PenggunaRepository pengguna;
    public KlinikUserDetailsService(PenggunaRepository pengguna) { this.pengguna = pengguna; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return pengguna.findByUsername(username).map(KlinikPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Akun tidak ditemukan"));
    }
}
