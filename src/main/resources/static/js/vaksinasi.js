(() => {
  'use strict';
  const input = document.querySelector('#kuotaBaru');
  const preview = document.querySelector('#kuota-preview');
  if (input && preview) input.addEventListener('input', () => {
    const count = Number(input.dataset.peserta);
    if (!input.validity.valid || input.value === '') {
      preview.textContent = `Kuota harus bilangan bulat, minimal ${count}.`;
      return;
    }
    const left = Number(input.value) - count;
    preview.textContent = `Pratinjau belum disimpan: sisa ${left}, status ${left === 0 ? 'Penuh' : 'Tersedia'}.`;
  });
  // Pilihan filter baru harus dimuat dahulu; jangan menyimpan kuota layanan lama tanpa sengaja.
  const filters = document.querySelector('#vaksinasi-cari');
  const save = document.querySelector('#kuota-form button[type="submit"]');
  if (filters && save) filters.addEventListener('change', () => {
    save.disabled = true;
    preview.textContent = 'Pilihan layanan berubah. Tekan Muat layanan sebelum menyimpan.';
  });
  const panel = document.querySelector('.vaksinasi-summary[data-publik="true"]');
  const sync = document.querySelector('#vaksinasi-sync');
  if (!panel || !sync || !panel.dataset.jenis) return;
  let busy = false;
  const refresh = async () => {
    if (busy || document.hidden) return;
    busy = true;
    try {
      const query = new URLSearchParams({tanggal: panel.dataset.tanggal, jenisVaksin: panel.dataset.jenis});
      const response = await fetch(`/vaksinasi/data?${query}`, {cache: 'no-store', signal: AbortSignal.timeout(8000)});
      if (!response.ok) throw new Error('snapshot unavailable');
      const data = await response.json();
      const cards = Array.from(panel.querySelectorAll('[data-layanan]'));
      if (!Array.isArray(data.layanan) || data.layanan.length !== cards.length) throw new Error('schedule changed');
      for (const card of cards) {
        const item = data.layanan.find(row => row.idJadwalVaksinasi === card.dataset.layanan);
        if (!item) throw new Error('schedule changed');
        card.querySelector('[data-kuota]').textContent = item.kuota;
        card.querySelector('[data-peserta]').textContent = item.jumlahTerdaftar;
        card.querySelector('[data-sisa]').textContent = item.sisaKuota;
        const badge = card.querySelector('[data-status]');
        badge.textContent = item.status;
        badge.className = `badge ${item.pendaftaranDibuka ? 'schedule-available' : 'schedule-full'}`;
        card.querySelector('progress').max = Math.max(item.kuota, 1);
        card.querySelector('progress').value = item.jumlahTerdaftar;
        card.querySelector('[data-terisi]').textContent = `${item.jumlahTerdaftar} dari ${item.kuota} kuota sudah terisi`;
        card.querySelector('[data-pendaftaran]').textContent = item.pendaftaranDibuka ? `${item.sisaKuota} peserta masih dapat mendaftar pada layanan ini.` : 'Pendaftaran ditutup.';
      }
      sync.textContent = 'Ketersediaan terbaru · diperbarui otomatis setiap 5 detik.';
    } catch (_) {
      sync.textContent = 'Pembaruan gagal. Data terakhir belum diperbarui; muat ulang layanan atau coba lagi.';
    } finally { busy = false; }
  };
  refresh();
  window.setInterval(refresh, 5000);
})();
