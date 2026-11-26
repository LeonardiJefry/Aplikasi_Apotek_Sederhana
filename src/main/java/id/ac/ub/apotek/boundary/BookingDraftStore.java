package id.ac.ub.apotek.boundary;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Draft terikat sesi, pasien dan jadwal. Token juga menjadi PK tiket untuk retry atomik. */
@Component
public class BookingDraftStore {
    private static final String KEY = BookingDraftStore.class.getName();
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    public BookingDraftStore(Clock clock) { this.clock = clock; }
    public record Draft(String token, String idPasien, String idJadwal, Instant dibuat) {}

    public Draft siapkan(HttpSession session, String idPasien, String idJadwal) {
        synchronized (session) {
            var drafts = drafts(session);
            var same = drafts.values().stream().filter(d -> d.idPasien().equals(idPasien) && d.idJadwal().equals(idJadwal)).findFirst();
            if (same.isPresent()) return same.get();
            if (drafts.size() >= 16) drafts.remove(drafts.keySet().iterator().next());
            String token;
            do {
                var chars = new StringBuilder();
                for (int i = 0; i < 12; i++) chars.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
                token = chars.toString();
            } while (drafts.containsKey(token));
            var draft = new Draft(token, idPasien, idJadwal, clock.instant());
            drafts.put(token, draft);
            return draft;
        }
    }

    public Draft ambil(HttpSession session, String token, String idPasien) {
        synchronized (session) {
            var draft = drafts(session).get(token);
            if (draft == null || !draft.idPasien().equals(idPasien))
                throw new IllegalArgumentException("Konfirmasi tidak valid atau kedaluwarsa. Silakan pilih jadwal kembali.");
            return draft;
        }
    }

    public void hapus(HttpSession session, Draft draft) {
        synchronized (session) { drafts(session).remove(draft.token()); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Draft> drafts(HttpSession session) {
        var map = (Map<String, Draft>) session.getAttribute(KEY);
        if (map == null) { map = new LinkedHashMap<>(); session.setAttribute(KEY, map); }
        var now = clock.instant();
        map.values().removeIf(d -> !now.isBefore(d.dibuat().plus(Duration.ofMinutes(15))));
        return map;
    }
}
