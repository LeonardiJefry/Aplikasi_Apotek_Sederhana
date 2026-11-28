(() => {
  'use strict';
  const root = document.getElementById('queue-workspace');
  if (!root) return;
  const state = document.getElementById('queue-refresh-state');
  const refreshButton = document.getElementById('queue-refresh');
  refreshButton.hidden = false;
  const number = value => String(value).padStart(3, '0');
  const set = (id, text) => { document.getElementById(id).textContent = text; };
  let busy = false;
  let timer;
  function disableActions() {
    for (const link of root.querySelectorAll('.queue-action')) {
      link.removeAttribute('href'); link.setAttribute('aria-disabled', 'true'); link.tabIndex = -1;
    }
  }
  function stale(message) {
    state.className = 'alert danger'; state.textContent = message;
    disableActions();
  }
  function action(id, allowed, suffix) {
    const link = document.getElementById(id);
    link.setAttribute('aria-disabled', String(!allowed));
    link.tabIndex = allowed ? 0 : -1;
    if (allowed) link.href = `${root.dataset.baseUrl}/${encodeURIComponent(root.dataset.ticketId)}/${suffix}`;
    else link.removeAttribute('href');
  }
  function draw(snapshot) {
    const detail = snapshot.dipilih;
    if (!root.dataset.ticketId && detail) { window.location.reload(); return; }
    if (!detail) return;
    if (detail.idAntrean !== root.dataset.ticketId || !Array.isArray(detail.nomorSedangDilayani)
        || !Array.isArray(detail.berikutnya) || !Array.isArray(snapshot.tiket)) throw new Error('snapshot');
    set('queue-own-number', number(detail.nomorAntrean));
    set('queue-status', detail.statusAntrean);
    document.getElementById('queue-status').className = `badge ${detail.statusAkhir ? 'queue-final' : detail.statusAntrean === 'Sedang Dilayani' ? 'queue-serving' : 'queue-waiting'}`;
    set('queue-current-number', detail.nomorSedangDilayani.length ? detail.nomorSedangDilayani.map(number).join(', ') : '—');
    set('queue-ahead', detail.jumlahDiDepan === null ? '—' : `${detail.jumlahDiDepan} orang`);
    set('queue-notice', detail.keterangan);
    set('queue-poli', detail.jadwal.poli); set('queue-doctor', detail.jadwal.dokter);
    set('queue-date', detail.jadwal.tanggal);
    set('queue-session', `${detail.jadwal.sesi} | ${detail.jadwal.jamMulai}–${detail.jadwal.jamSelesai} WIB`);
    const rows = document.createDocumentFragment();
    for (const row of detail.berikutnya) {
      const tr = document.createElement('tr');
      const no = document.createElement('td'); no.textContent = number(row.nomorAntrean);
      const status = document.createElement('td');
      const badge = document.createElement('span'); badge.className = 'badge queue-waiting'; badge.textContent = row.statusAntrean;
      status.append(badge);
      const estimate = document.createElement('td'); estimate.textContent = '—';
      tr.append(no, status, estimate); rows.append(tr);
    }
    document.getElementById('queue-rows').replaceChildren(rows);
    const empty = document.getElementById('queue-no-rows');
    empty.hidden = detail.berikutnya.length > 0;
    empty.textContent = detail.statusAkhir ? 'Pendaftaran ini telah berakhir.' : 'Belum ada antrean menunggu pada sesi ini.';
    const select = document.getElementById('queue-ticket');
    // Pertahankan pilihan yang belum disubmit ketika polling memperbarui label/status tiket.
    const pendingChoice = snapshot.tiket.some(ticket => ticket.idAntrean === select.value) ? select.value : detail.idAntrean;
    const options = document.createDocumentFragment();
    for (const ticket of snapshot.tiket) {
      const option = document.createElement('option'); option.value = ticket.idAntrean;
      option.textContent = `${ticket.tanggal} | ${ticket.poli} | ${ticket.sesi} | ${number(ticket.nomorAntrean)} | ${ticket.statusAntrean}`;
      option.selected = ticket.idAntrean === pendingChoice; options.append(option);
    }
    select.replaceChildren(options);
    action('queue-cancel', detail.bolehBukaBatal, 'pembatalan');
    action('queue-checkin', detail.bolehCheckIn, 'check-in');
  }
  async function refresh() {
    if (busy) return;
    clearTimeout(timer);
    if (document.hidden) { timer = setTimeout(refresh, 3000); return; }
    busy = true; refreshButton.disabled = true;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(root.dataset.apiUrl, {cache:'no-store', credentials:'same-origin', signal:controller.signal});
      if (response.redirected || response.status === 401 || response.status === 403) {
        stale('Sesi atau akses Anda berubah. Silakan masuk kembali untuk memperbarui antrean.'); return;
      }
      if (response.status === 404) { stale('Tiket tidak tersedia. Kembali ke antrean saya untuk memilih tiket lain.'); return; }
      if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('refresh');
      const snapshot = await response.json();
      if (!Array.isArray(snapshot.tiket) || typeof snapshot.diperbaruiPada !== 'string'
          || !Object.hasOwn(snapshot, 'dipilih')) throw new Error('snapshot');
      draw(snapshot);
      state.className = 'help';
      state.textContent = `Diperbarui ${snapshot.diperbaruiPada} WIB · otomatis setiap 5 detik`;
    } catch { stale('Pembaruan gagal. Data terakhir belum diperbarui. Silakan coba lagi.'); }
    finally {
      clearTimeout(timeout); busy = false; refreshButton.disabled = false;
      timer = setTimeout(refresh, 3000);
    }
  }
  refreshButton.addEventListener('click', refresh);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
  window.addEventListener('pagehide', () => clearTimeout(timer));
  window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
  timer = setTimeout(refresh, 3000);
})();
