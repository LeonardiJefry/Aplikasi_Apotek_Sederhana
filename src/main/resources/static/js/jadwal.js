(() => {
  'use strict';
  const form = document.getElementById('schedule-filter');
  if (!form) return;
  const poli = form.elements.idPoli;
  const dokter = form.elements.idDokter;
  function filterDoctors() {
    for (const option of dokter.options) {
      const allowed = !option.value || !poli.value || option.dataset.poli === poli.value;
      option.disabled = !allowed;
      option.hidden = !allowed;
      if (!allowed && option.selected) dokter.value = '';
    }
    poli.setCustomValidity('');
  }
  poli.addEventListener('change', filterDoctors);
  dokter.addEventListener('change', () => poli.setCustomValidity(''));
  form.addEventListener('submit', event => {
    poli.setCustomValidity(poli.value || dokter.value ? '' : 'Pilih poli atau dokter terlebih dahulu');
    if (!form.reportValidity()) event.preventDefault();
  });
  filterDoctors();

  const results = document.getElementById('schedule-results');
  if (!results) return;
  const tbody = document.getElementById('schedule-rows');
  const state = document.getElementById('refresh-state');
  const manual = document.getElementById('refresh-schedule');
  manual.hidden = false;
  let timer;
  let busy = false;
  let lastRows = null;
  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  function draw(rows) {
    const focused = document.activeElement?.dataset.choiceId;
    const fragment = document.createDocumentFragment();
    for (const item of rows) {
      const row = element('tr');
      const doctorCell = element('td');
      const doctor = element('div', undefined, 'schedule-doctor');
      const initials = item.dokter.replace(/^drg?\.\s*/i, '').split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]).join('').toUpperCase();
      const avatar = element('span', initials, 'schedule-avatar');
      avatar.setAttribute('aria-hidden', 'true');
      const name = element('div');
      name.append(element('strong', item.dokter), element('small', item.spesialisasi));
      doctor.append(avatar, name);
      doctorCell.append(doctor);
      row.append(doctorCell);
      for (const value of [item.poli, item.sesi, `${item.jamMulai}–${item.jamSelesai}`, `${item.sisaKuota} / ${item.kuotaTotal}`]) row.append(element('td', value));
      const available = item.sisaKuota > 0;
      const badgeCell = element('td');
      badgeCell.append(element('span', item.status, `badge ${available ? 'schedule-available' : 'schedule-full'}`));
      const actionCell = element('td');
      if (available) {
        const choice = element('a', 'Pilih Jadwal', 'button primary schedule-choice');
        const url = new URL(results.dataset.choiceUrl, window.location.href);
        url.searchParams.set('idJadwal', item.idJadwal);
        choice.href = url.href;
        choice.dataset.choiceId = item.idJadwal;
        actionCell.append(choice);
      } else {
        const full = element('button', 'Penuh');
        full.type = 'button';
        full.disabled = true;
        actionCell.append(full);
      }
      row.append(badgeCell, actionCell);
      fragment.append(row);
    }
    tbody.replaceChildren(fragment);
    if (focused) Array.from(tbody.querySelectorAll('[data-choice-id]')).find(n => n.dataset.choiceId === focused)?.focus();
    document.getElementById('schedule-empty').hidden = rows.length > 0;
    document.getElementById('result-count').textContent = rows.length;
    document.getElementById('available-count').textContent = rows.filter(j => j.sisaKuota > 0).length;
    document.getElementById('full-count').textContent = rows.filter(j => j.sisaKuota === 0).length;
  }
  function stale(message) {
    state.textContent = message;
    state.className = 'alert danger';
    for (const link of tbody.querySelectorAll('a.schedule-choice')) {
      link.removeAttribute('href');
      link.setAttribute('aria-disabled', 'true');
      link.tabIndex = -1;
    }
    // Force a redraw after recovery, restoring only choices in a fresh snapshot.
    lastRows = null;
  }
  async function refresh() {
    if (busy) return;
    clearTimeout(timer);
    if (document.hidden) { timer = setTimeout(refresh, 5000); return; }
    busy = true;
    manual.disabled = true;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(results.dataset.apiUrl, { cache: 'no-store', credentials: 'same-origin', signal: controller.signal });
      if (response.redirected || response.status === 401 || response.status === 403) {
        stale('Sesi atau akses Anda berubah. Silakan masuk kembali untuk memperbarui jadwal.');
        return;
      }
      if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('refresh');
      const snapshot = await response.json();
      if (!Array.isArray(snapshot.jadwal)) throw new Error('snapshot');
      const signature = JSON.stringify(snapshot.jadwal);
      if (signature !== lastRows) { draw(snapshot.jadwal); lastRows = signature; }
      state.className = 'help';
      state.textContent = `Diperbarui ${snapshot.diperbaruiPada} WIB · otomatis setiap 5 detik`;
    } catch {
      stale('Pembaruan gagal. Data terakhir belum diperbarui. Coba lagi sebelum memilih jadwal.');
    } finally {
      clearTimeout(timeout);
      busy = false;
      manual.disabled = false;
      timer = setTimeout(refresh, 5000);
    }
  }
  manual.addEventListener('click', refresh);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
  window.addEventListener('pagehide', () => clearTimeout(timer));
  window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
  timer = setTimeout(refresh, 5000);
})();
