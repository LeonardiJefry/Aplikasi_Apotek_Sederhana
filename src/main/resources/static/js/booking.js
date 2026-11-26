(() => {
  'use strict';
  const form = document.getElementById('booking-confirm');
  if (!form) return;
  form.addEventListener('submit', () => {
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = 'Memproses booking…';
  });
  window.addEventListener('pageshow', () => {
    const button = form.querySelector('button[type="submit"]');
    button.disabled = false;
    button.textContent = 'Konfirmasi Booking';
  });
})();
