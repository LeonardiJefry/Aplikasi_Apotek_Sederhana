package id.ac.ub.apotek.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Dipakai semua UC; ID pasien/dokter tidak diterima dari form sebagai identitas pelaku. */
@Service
public class CurrentUserService {
    public KlinikPrincipal requireUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof KlinikPrincipal user)) {
            throw new AccessDeniedException("Silakan masuk kembali");
        }
        return user;
    }

    public String requirePasienId() {
        var user = requireUser();
        if (!"PASIEN".equals(user.getPeran()) || user.getIdPasien() == null) {
            throw new AccessDeniedException("Akses pasien diperlukan");
        }
        return user.getIdPasien();
    }

    public String requireDokterId() {
        var user = requireUser();
        if (!"DOKTER".equals(user.getPeran()) || user.getIdDokter() == null) {
            throw new AccessDeniedException("Akses dokter diperlukan");
        }
        return user.getIdDokter();
    }
}
