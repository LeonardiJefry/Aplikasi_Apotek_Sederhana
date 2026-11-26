(() => {
  'use strict';
  const dialog = document.getElementById('cancel-dialog');
  if (!dialog || typeof dialog.showModal !== 'function') return;
  const open = document.getElementById('cancel-open');
  const close = document.getElementById('cancel-close');
  const submit = document.getElementById('cancel-submit');
  let busy = false;
  dialog.close(); dialog.dataset.modal = 'true';
  open.hidden = false; close.hidden = false; document.getElementById('cancel-back').hidden = true;
  open.addEventListener('click', () => dialog.showModal());
  close.addEventListener('click', () => { if (!busy) dialog.close(); });
  dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  document.getElementById('cancel-form').addEventListener('submit', () => {
    busy = true; submit.disabled = true; close.disabled = true; submit.textContent = 'Memproses pembatalan…';
  });
  window.addEventListener('pageshow', () => {
    busy = false; submit.disabled = false; close.disabled = false; submit.textContent = 'Ya, Batalkan';
  });
})();
