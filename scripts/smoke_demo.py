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
