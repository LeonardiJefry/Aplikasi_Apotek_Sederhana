(() => {
  'use strict';
  const own = document.getElementById('checkin-workspace');
  const warnings = document.getElementById('checkin-warnings');
  const root = own || warnings;
  if (!root) return;
  const state = document.getElementById('checkin-refresh-state');
  const manual = document.getElementById('checkin-refresh'); manual.hidden = false;
  const submit = document.getElementById('checkin-submit');
  const hiddenWarnings = new Set();
  let busy = false; let submitting = false; let timer;
  const set = (id,text) => { document.getElementById(id).textContent = text; };
  function stale(message) { state.className = 'alert danger'; state.textContent = message; if (submit) submit.disabled = true; }
  function drawOwn(snapshot) {
    const t = snapshot.tiket;
    if (!t || t.idAntrean !== root.dataset.ticketId || typeof t.bolehCheckIn !== 'boolean') throw new Error('snapshot');
    set('checkin-status',t.statusAntrean); set('checkin-number',String(t.nomorAntrean).padStart(3,'0'));
    set('checkin-now',`Waktu server ${snapshot.sekarang} WIB`);
    set('checkin-window',`${t.batasBuka} — ${t.batasTutup} WIB`);
    set('checkin-window-state',t.jendelaDibuka ? 'Check-in dibuka' : 'Di luar jendela check-in');
    const recorded = document.getElementById('checkin-recorded'); recorded.hidden = !t.waktuCheckIn;
    recorded.textContent = t.waktuCheckIn ? `Waktu check-in: ${t.waktuCheckIn} WIB` : '';
    document.getElementById('checkin-step').classList.toggle('step-done',Boolean(t.waktuCheckIn));
    set('checkin-step-note',t.waktuCheckIn ? 'Kehadiran telah tercatat' : 'Konfirmasi saat tiba di klinik');
    set('checkin-eligibility',t.bolehCheckIn ? 'Check-in dapat dilakukan sekarang.' : 'Check-in belum dapat dilakukan dari waktu atau status tiket ini.');
    submit.disabled = submitting || !t.bolehCheckIn;
    const error = document.getElementById('checkin-error'); if (error && (t.bolehCheckIn || t.waktuCheckIn)) error.hidden = true;
  }
  function dismiss(button) {
    button.hidden = false;
    button.addEventListener('click',() => { const card = button.closest('[data-warning-key]'); hiddenWarnings.add(card.dataset.warningKey); card.hidden = true; document.getElementById('checkin-warning-restore').hidden = false; });
  }
  if (warnings) {
    for (const button of warnings.querySelectorAll('.warning-dismiss')) dismiss(button);
    document.getElementById('checkin-warning-restore').addEventListener('click',() => { hiddenWarnings.clear(); for (const card of warnings.querySelectorAll('[data-warning-key]')) card.hidden = false; document.getElementById('checkin-warning-restore').hidden = true; });
  }
  function drawWarnings(snapshot) {
    if (!Array.isArray(snapshot.peringatan)) throw new Error('snapshot');
    const currentKeys = new Set(snapshot.peringatan.map(w=>`${w.idJadwal}:${w.nomorAntrean}`));
    for (const key of hiddenWarnings) if (!currentKeys.has(key)) hiddenWarnings.delete(key);
    const fragment = document.createDocumentFragment();
    for (const w of snapshot.peringatan) {
      const card = document.createElement('article'); card.className = 'card checkin-warning'; card.dataset.warningKey = `${w.idJadwal}:${w.nomorAntrean}`; card.hidden = hiddenWarnings.has(card.dataset.warningKey);
      for (const [tag,text] of [['h2',`${String(w.nomorAntrean).padStart(3,'0')} belum check-in`],['p',`${w.poli} | ${w.dokter} | ${w.sesi} | ${w.tanggal}`],['p','Giliran pasien hampir tiba. Peringatan akan hilang setelah kehadirannya tercatat.']]) { const el = document.createElement(tag); el.textContent = text; card.append(el); }
      const button = document.createElement('button'); button.type = 'button'; button.className = 'outline'; button.textContent = 'Tutup peringatan'; dismiss(button); card.append(button); fragment.append(card);
    }
    document.getElementById('checkin-warning-list').replaceChildren(fragment);
    document.getElementById('checkin-warning-empty').hidden = snapshot.peringatan.length > 0;
    document.getElementById('checkin-warning-restore').hidden = hiddenWarnings.size === 0;
  }
  async function refresh() {
    if (busy || submitting) return;
    clearTimeout(timer);
    if (document.hidden) { timer = setTimeout(refresh,5000); return; }
    busy = true; manual.disabled = true;
    const controller = new AbortController(); const timeout = setTimeout(()=>controller.abort(),8000);
    try {
      const response = await fetch(root.dataset.apiUrl,{cache:'no-store',credentials:'same-origin',signal:controller.signal});
      if (response.redirected || response.status === 401 || response.status === 403) { stale('Sesi atau akses Anda berubah. Silakan masuk kembali.'); return; }
      if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('refresh');
      const snapshot = await response.json();
      if (own) drawOwn(snapshot); else drawWarnings(snapshot);
      state.className = 'help'; state.textContent = `Diperbarui ${snapshot.sekarang || snapshot.diperbaruiPada} WIB · otomatis setiap 5 detik`;
    } catch { stale('Pembaruan gagal. Data terakhir belum diperbarui. Silakan coba lagi.'); }
    finally { clearTimeout(timeout); busy = false; manual.disabled = false; if (!submitting) timer = setTimeout(refresh,5000); }
  }
  document.getElementById('checkin-form')?.addEventListener('submit',()=>{submitting = true; clearTimeout(timer); submit.disabled = true; submit.textContent = 'Memproses check-in…';});
  manual.addEventListener('click',refresh);
  document.addEventListener('visibilitychange',()=>{if (!document.hidden) refresh();});
  window.addEventListener('pagehide',()=>clearTimeout(timer));
  window.addEventListener('pageshow',event=>{if(event.persisted){submitting=false; if(submit) submit.textContent='Check-in sekarang'; refresh();}});
  timer = setTimeout(refresh,5000);
})();
