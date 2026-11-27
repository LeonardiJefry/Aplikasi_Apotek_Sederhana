"""Verify packaged demo startup, login, role isolation and a fresh JVM restart.

Run after Maven verify: python scripts/smoke_demo.py target/apotek-0.0.1-SNAPSHOT.jar
Uses only the Python standard library, a temporary working directory, and demo data.
"""
import html
import http.cookiejar
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def client():
    return urllib.request.build_opener(
        urllib.request.ProxyHandler({}), NoRedirect(),
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def request(browser, base, path, fields=None):
    data = None if fields is None else urllib.parse.urlencode(fields).encode()
    try:
        response = browser.open(base + path, data=data, timeout=3)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.code, response.headers, response.read().decode("utf-8")


def expect_status(response, expected, action):
    code, headers, _ = response
    if code != expected:
        raise AssertionError(
            f"{action}: expected HTTP {expected}, got HTTP {code}; "
            f"Location={headers.get('Location', '(none)')}")
    return response


def csrf(page):
    match = re.search(r'name="_csrf"[^>]*value="([^"]+)"', page)
    if not match:
        raise AssertionError("CSRF hidden input missing")
    return html.unescape(match.group(1))


def login(browser, base, username, password):
    code, _, page = request(browser, base, "/login")
    assert code == 200, f"Login page returned {code}"
    return request(browser, base, "/login", {
        "username": username, "password": password, "_csrf": csrf(page)})


def check_roles(base, password):
    areas = {"pasien": "/pasien", "pasien2": "/pasien", "loket": "/loket",
             "admin": "/admin", "dokter": "/dokter", "dokter2": "/dokter"}
    anonymous = client()
    expect_status(request(anonymous, base, "/pasien"), 302, "Anonymous patient page")
    expect_status(request(anonymous, base, "/login", {"username": "pasien"}),
                  403, "Login without CSRF token")
    expect_status(request(anonymous, base, "/pasien"), 302,
                  "Rejected login must leave user unauthenticated")
    code, headers, _ = login(anonymous, base, "pasien", "wrong-password")
    assert code == 302 and headers["Location"].endswith("/login?error")
    for username, area in areas.items():
        browser = client()
        code, headers, _ = login(browser, base, username, password)
        assert code == 302 and not headers["Location"].endswith("/login?error"), username
        code, _, page = request(browser, base, area)
        assert code == 200 and "Akun aktif" in page and username in page, username
        for other in set(areas.values()) - {area}:
            expect_status(request(browser, base, other), 403, f"{username}: {other}")
        expect_status(request(browser, base, "/logout", {}), 403,
                      f"{username}: logout without CSRF token")
        expect_status(request(browser, base, area), 200,
                      f"{username}: rejected logout must keep the session")
        assert request(browser, base, "/logout", {"_csrf": csrf(page)})[0] == 302
        assert request(browser, base, area)[0] == 302
        print(f"PASS login, cross-role rejection, CSRF and logout: {username}")


def check_schedule(base, password):
    browser = client()
    login(browser, base, "pasien", password)
    _, _, page = expect_status(request(browser, base, "/pasien/jadwal"), 200, "Schedule page")
    date = re.search(r'id="tanggal"[^>]*value="([0-9-]+)"', page)
    if not date:
        raise AssertionError("Schedule page did not render the default visit date")
    query = urllib.parse.urlencode({"tanggal": date.group(1), "idPoli": "POL-001"})
    _, headers, body = expect_status(request(browser, base, "/pasien/jadwal/data?" + query),
                                     200, "Schedule snapshot")
    rows = json.loads(body)["jadwal"]
    assert len(rows) == 1 and rows[0]["idJadwal"] == "JDW-001"
    assert rows[0]["sisaKuota"] == 6 and rows[0]["status"] == "Tersedia"
    assert headers.get("Cache-Control") == "no-store"
    assert not any(key in rows[0] for key in ("idPasien", "nik", "passwordHash"))
    expect_status(request(browser, base, "/pasien/jadwal/data"), 400, "Missing schedule filters")
    expect_status(request(browser, base, "/pasien/jadwal/pilih?idJadwal=UNKNOWN"),
                  404, "Unknown schedule selection")
    _, headers, _ = expect_status(request(browser, base, "/pasien/jadwal/pilih?idJadwal=JDW-001"),
                                 302, "Schedule handoff to booking")
    assert headers["Location"].endswith("/pasien/booking?idJadwal=JDW-001")
    print("PASS UC-01: default filters, live snapshot, validation, privacy and booking handoff")


def check_booking(base, password):
    browser = client()
    login(browser, base, "pasien2", password)
    _, _, page = expect_status(request(browser, base, "/pasien/booking?idJadwal=JDW-003"),
                               200, "Booking summary")
    assert "Pasien Demo Dua" in page and "DEMO-RM-002" in page and "DEMO-RM-001" not in page
    date = re.search(r'id="booking-tanggal"[^>]*value="([0-9-]+)"', page).group(1)
    _, _, page = expect_status(request(browser, base, "/pasien/booking/konfirmasi?idJadwal=JDW-003"),
                               200, "Final confirmation")
    key = re.search(r'name="bookingToken"[^>]*value="([^"]+)"', page).group(1)
    form = {"bookingToken": key, "_csrf": csrf(page), "idPasien": "PAS-001", "idJadwal": "JDW-004"}
    _, headers, _ = expect_status(request(browser, base, "/pasien/booking/konfirmasi", form),
                                  303, "Create booking")
    location = headers["Location"]
    path = urllib.parse.urlsplit(location).path
    assert path.endswith("/pasien/booking/tiket/" + key)
    _, _, ticket = expect_status(request(browser, base, path), 200, "Own booking ticket")
    assert "Pasien Demo Dua" in ticket and "Terdaftar" in ticket and date in ticket
    _, replay_headers, _ = expect_status(request(browser, base, "/pasien/booking/konfirmasi", form),
                                         303, "Replay confirmation")
    assert replay_headers["Location"] == location
    query = urllib.parse.urlencode({"tanggal": date, "idPoli": "POL-001"})
    _, _, body = expect_status(request(browser, base, "/pasien/jadwal/data?" + query), 200, "Quota after booking")
    assert json.loads(body)["jadwal"][0]["sisaKuota"] == 8, "Booking/replay must consume one quota"
    expect_status(request(browser, base, "/pasien/antrean/" + key), 200, "UC-03 status handoff")
    other = client()
    login(other, base, "pasien", password)
    expect_status(request(other, base, path), 404, "Other patient cannot read ticket")
    cancel = client()
    login(cancel, base, "pasien2", password)
    _, _, page = expect_status(request(cancel, base, "/pasien/booking/konfirmasi?idJadwal=JDW-003"),
                               200, "Cancellation draft")
    cancel_key = re.search(r'name="bookingToken"[^>]*value="([^"]+)"', page).group(1)
    expect_status(request(cancel, base, "/pasien/booking/batal", {"bookingToken": cancel_key, "_csrf": csrf(page)}),
                  303, "Cancel before confirmation")
    _, _, body = expect_status(request(browser, base, "/pasien/jadwal/data?" + query), 200, "Quota after draft cancellation")
    assert json.loads(body)["jadwal"][0]["sisaKuota"] == 8
    print("PASS UC-02: summary, confirmation, owner identity, ticket, replay, quota, cancellation and privacy")


def check_queue_status(base, password):
    browser = client()
    login(browser, base, "pasien2", password)
    _, _, page = expect_status(request(browser, base, "/pasien/antrean"), 200, "Patient queue page")
    assert "Status Antrean" in page and "Daftar Antrean Berikutnya" in page
    _, headers, body = expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0004/data?idPasien=PAS-001"),
                                     200, "Own queue snapshot")
    snapshot = json.loads(body)
    ticket = snapshot["dipilih"]
    assert ticket["idAntrean"] == "ANT-DEMO0004" and ticket["nomorAntrean"] == 4
    assert ticket["nomorSedangDilayani"] == [1] and ticket["jumlahDiDepan"] == 3
    assert ticket["statusAntrean"] == "Menunggu" and not ticket["statusAkhir"]
    assert headers.get("Cache-Control") == "no-store"
    assert not any(value in body for value in ("Pasien Demo", "DEMO-RM", '"nik":', '"passwordHash":', "ANT-DEMO0001", "ANT-DEMO0003"))
    expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0001/data"), 404, "Other patient's queue snapshot")
    expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0001"), 404, "Other patient's queue page")
    expect_status(request(browser, base, "/pasien/antrean/INVALID-LONG-TICKET/data"), 400, "Invalid queue ticket ID")
    _, _, body = expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0008/data"), 200, "Final ticket snapshot")
    final = json.loads(body)["dipilih"]
    assert final["statusAntrean"] == "Dibatalkan" and final["statusAkhir"]
    assert final["jumlahDiDepan"] is None and final["berikutnya"] == []
    assert not final["bolehBatal"] and not final["bolehCheckIn"]
    admin = client()
    login(admin, base, "admin", password)
    expect_status(request(admin, base, "/pasien/antrean/ANT-DEMO0004/data"), 403, "Admin cannot access patient queue")
    print("PASS UC-03: patient tickets, same-session position, serving number, final status, privacy and role rejection")


def check_cancellation(base, password):
    browser = client()
    login(browser, base, "pasien", password)
    url = "/pasien/antrean/ANT-DEMO0006/pembatalan"
    _, _, page = expect_status(request(browser, base, url), 200, "Cancellation preview")
    assert "Batalkan Antrean?" in page and "Ya, Batalkan" in page
    expect_status(request(browser, base, url + "/hasil"), 409, "Uncancelled ticket cannot claim success")
    expect_status(request(browser, base, url, {"konfirmasi": "ya"}), 403, "Cancellation requires CSRF")
    token = csrf(page)
    expect_status(request(browser, base, url, {"_csrf": token}), 400, "Explicit cancellation confirmation required")
    _, _, body = expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0006/data"), 200, "Status before cancellation")
    before = json.loads(body)["dipilih"]
    assert before["statusAntrean"] == "Terdaftar" and before["jadwal"]["sisaKuota"] == 8
    other = client()
    login(other, base, "pasien2", password)
    _, _, other_page = request(other, base, "/login")
    expect_status(request(other, base, url, {"_csrf": csrf(other_page), "konfirmasi": "ya"}), 404, "Other patient cannot cancel ticket")
    form = {"_csrf": token, "konfirmasi": "ya", "idPasien": "PAS-002", "idJadwal": "JDW-001"}
    _, headers, _ = expect_status(request(browser, base, url, form), 303, "Cancel own registration")
    assert headers["Location"].endswith(url + "/hasil")
    _, _, result = expect_status(request(browser, base, url + "/hasil"), 200, "Cancellation result")
    assert "Antrean berhasil dibatalkan" in result
    expect_status(request(browser, base, url, form), 409, "Repeated cancellation rejected")
    _, _, body = expect_status(request(browser, base, "/pasien/antrean/ANT-DEMO0006/data"), 200, "Status after cancellation")
    after = json.loads(body)["dipilih"]
    assert after["statusAntrean"] == "Dibatalkan" and after["jadwal"]["sisaKuota"] == 9
    assert not after["bolehBatal"] and after["jumlahDiDepan"] is None
    query = urllib.parse.urlencode({"tanggal": after["jadwal"]["tanggal"], "idPoli": "POL-001"})
    _, _, body = expect_status(request(browser, base, "/pasien/jadwal/data?" + query), 200, "Restored quota in UC-01")
    assert json.loads(body)["jadwal"][0]["sisaKuota"] == 9
    print("PASS UC-04: explicit confirmation, CSRF, ownership, cancellation, one quota refund, replay rejection and UC-01/03 updates")


def check_check_in(base, password):
    browser = client()
    login(browser, base, "pasien", password)
    url = "/pasien/antrean/ANT-DEMO0006/check-in"
    _, _, page = expect_status(request(browser, base, url), 409, "Future-day check-in rejected")
    token = csrf(page)
    expect_status(request(browser, base, url, {}), 403, "Check-in requires CSRF")
    expect_status(request(browser, base, url, {"_csrf": token}), 409, "Future-day POST rejected")
    expect_status(request(browser, base, url + "/hasil"), 409, "Check-in result cannot claim false success")
    _, _, body = expect_status(request(browser, base, url + "/data"), 200, "Read future check-in window")
    future = json.loads(body)["tiket"]
    assert future["statusAntrean"] == "Terdaftar" and future["waktuCheckIn"] is None and not future["bolehCheckIn"]
    other = client()
    login(other, base, "pasien2", password)
    expect_status(request(other, base, url + "/data"), 404, "Other patient cannot read check-in")
    own_url = "/pasien/antrean/ANT-DEMO0005/check-in"
    _, _, body = expect_status(request(other, base, own_url + "/data"), 200, "Today's check-in window")
    today = json.loads(body)["tiket"]
    code, _, own_page = request(other, base, own_url)
    assert code in (200, 409)
    form = {"_csrf": csrf(own_page), "idPasien": "PAS-001", "waktuCheckIn": "2000-01-01T00:00:00"}
    code, _, _ = request(other, base, own_url, form)
    assert code in (303, 409), f"Unexpected check-in response {code}"
    _, _, body = expect_status(request(other, base, own_url + "/data"), 200, "Check-in state after request")
    after = json.loads(body)["tiket"]
    assert after["jadwal"]["sisaKuota"] == today["jadwal"]["sisaKuota"]
    if code == 303:
        assert after["statusAntrean"] == "Hadir/Menunggu" and after["waktuCheckIn"]
        expect_status(request(other, base, own_url, form), 409, "Repeated check-in rejected")
        print("PASS UC-05 real clock: check-in succeeded within today's window")
    else:
        assert after["statusAntrean"] == "Terdaftar" and after["waktuCheckIn"] is None
        assert not after["bolehCheckIn"], "A valid open-window check-in must succeed"
        print("PASS UC-05 real clock: today's window closed; status and timestamp unchanged")
    loket = client()
    login(loket, base, "loket", password)
    expect_status(request(loket, base, "/loket/check-in"), 200, "Counter warning page")
    _, headers, body = expect_status(request(loket, base, "/loket/check-in/data"), 200, "Counter warning snapshot")
    assert isinstance(json.loads(body)["peringatan"], list) and headers.get("Cache-Control") == "no-store"
    assert not any(key in body for key in ('"idPasien":', '"idAntrean":', '"nik":', '"noRekamMedis":'))
    expect_status(request(browser, base, "/loket/check-in/data"), 403, "Patient cannot read counter warnings")
    expect_status(request(loket, base, url + "/data"), 403, "Counter cannot read patient's check-in")
    print("PASS UC-05: real-time window, CSRF, ownership, no quota change and counter warnings")


def check_calls(base, password):
    counter = client()
    login(counter, base, "loket", password)
    page_url = "/loket/panggilan?idJadwal=JDW-003"
    _, _, page = expect_status(request(counter, base, page_url), 200, "Counter call page")
    assert "Panggilan antrean" in page and "Panggil antrean berikutnya" in page
    _, headers, body = expect_status(request(counter, base, "/loket/panggilan/data?idJadwal=JDW-003"), 200, "Future session call snapshot")
    data = json.loads(body)
    assert data["dipanggil"] is None and not data["jamLayanan"] and not data["bolehPanggil"]
    assert headers.get("Cache-Control") == "no-store"
    assert not any(key in body for key in ('"nik":', '"passwordHash":', '"tanggalLahir":'))
    action = "/loket/panggilan/JDW-003/panggil"
    expect_status(request(counter, base, action, {}), 403, "Calling requires CSRF")
    expect_status(request(counter, base, action, {"_csrf": csrf(page)}), 409, "Future session cannot be called")
    for username in ("pasien", "admin", "dokter"):
        other = client()
        login(other, base, username, password)
        expect_status(request(other, base, page_url), 403, "Other role cannot open counter call page")
        expect_status(request(other, base, "/loket/panggilan/data?idJadwal=JDW-003"), 403, "Other role cannot read counter call snapshot")
    print("PASS UC-06: call page, future-session rejection, read-only snapshot, CSRF and role isolation")


def check_vaccination_quota(base, password):
    admin = client()
    login(admin, base, "admin", password)
    _, _, today_page = expect_status(request(admin, base, "/admin/vaksinasi"), 200, "Quota filter")
    date = re.search(r'name="tanggal"[^>]*value="([0-9-]+)"', today_page).group(1)
    query = urllib.parse.urlencode({"tanggal": date, "jenisVaksin": "Influenza Demo", "idJadwalVaksinasi": "VAK-001"})
    page_url = "/admin/vaksinasi?" + query
    public_url = "/vaksinasi/data?" + query
    anonymous = client()
    def snapshot():
        _, headers, body = expect_status(request(anonymous, base, public_url), 200, "Public vaccine availability")
        assert headers.get("Cache-Control") == "no-store"
        assert not any(key in body for key in ('"nik":', '"nama":', '"passwordHash":'))
        return json.loads(body)["layanan"][0]
    initial = snapshot()
    assert initial["kuota"] == 80 and initial["jumlahTerdaftar"] == 56 and initial["sisaKuota"] == 24
    def change(value, expected, version=None):
        _, _, page = expect_status(request(admin, base, page_url), 200, "Selected quota form")
        data = {"_csrf": csrf(page), "tanggal": date, "jenisVaksin": "Influenza Demo", "idJadwalVaksinasi": "VAK-001",
                "kuotaBaru": str(value), "versi": version or snapshot()["versi"], "jumlahTerdaftar": "0"}
        return expect_status(request(admin, base, "/admin/vaksinasi/kuota", data), expected, "Quota change")
    expect_status(request(admin, base, "/admin/vaksinasi/kuota", {}), 403, "Quota CSRF required")
    change(55, 409)
    assert snapshot()["kuota"] == 80
    change(56, 303)
    full = snapshot()
    assert full["status"] == "Penuh" and full["sisaKuota"] == 0 and not full["pendaftaranDibuka"]
    change(90, 409, initial["versi"])
    assert snapshot()["kuota"] == 56
    change(80, 303)
    assert snapshot()["sisaKuota"] == 24 and snapshot()["pendaftaranDibuka"]
    expect_status(request(anonymous, base, "/vaksinasi?" + query), 200, "Public availability page")
    for username in ("pasien", "loket", "dokter"):
        other = client()
        login(other, base, username, password)
        expect_status(request(other, base, page_url), 403, "Other role cannot manage quota")
    print("PASS UC-07: admin quota, minimum validation, closure/reopening, stale form, public availability, CSRF and roles")


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python scripts/smoke_demo.py PATH_TO_BOOT_JAR")
    jar = Path(sys.argv[1]).resolve(strict=True)
    logs = Path("target").resolve()
    logs.mkdir(exist_ok=True)
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if java_home else "java"
    password = "demo-klinik-2026"  # Public synthetic fixture, never a private credential.
    base = "http://127.0.0.1:18080"
    # Exercise packaged demo defaults without inheriting a developer's DB/profile overrides.
    demo_env = {key: value for key, value in os.environ.items()
                if not key.upper().startswith(("SPRING_", "DB_", "DEMO_"))
                and key.upper() not in {"APP_PORT", "SESSION_COOKIE_SECURE"}}
    for run in (1, 2):
        log_path = logs / f"demo-smoke-{run}.log"
        with tempfile.TemporaryDirectory(prefix="klinik-demo-") as directory, log_path.open("w", encoding="utf-8") as log:
            process = subprocess.Popen([
                java, "-jar", str(jar), "--spring.profiles.active=demo",
                "--server.port=18080", "--server.address=127.0.0.1",
                f"--klinik.demo.password={password}"], cwd=directory, env=demo_env,
                stdout=log, stderr=subprocess.STDOUT)
            try:
                deadline = time.monotonic() + 45
                while True:
                    if process.poll() is not None:
                        raise RuntimeError(f"Demo startup failed (exit {process.returncode}); see {log_path}")
                    if time.monotonic() >= deadline:
                        raise TimeoutError(f"Demo startup timed out; see {log_path}")
                    # Require this process's startup marker before using the fixed local port.
                    if "Started ApotekApplication" in log_path.read_text(encoding="utf-8", errors="replace"):
                        try:
                            code, headers, _ = login(client(), base, "pasien", password)
                            if code == 302 and not headers["Location"].endswith("/login?error"):
                                break
                        except (OSError, urllib.error.URLError):
                            pass
                    time.sleep(0.5)
                check_roles(base, password)
                check_schedule(base, password)
                check_booking(base, password)
                check_queue_status(base, password)
                check_check_in(base, password)
                check_cancellation(base, password)
                check_calls(base, password)
                check_vaccination_quota(base, password)
                print(f"PASS demo startup {run}: fresh working directory, H2, synthetic accounts")
            finally:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    main()
