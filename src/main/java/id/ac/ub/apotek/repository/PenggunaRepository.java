package id.ac.ub.apotek.repository;

import id.ac.ub.apotek.entity.Pengguna;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenggunaRepository extends JpaRepository<Pengguna, String> {
    java.util.Optional<Pengguna> findByUsername(String username);
}
