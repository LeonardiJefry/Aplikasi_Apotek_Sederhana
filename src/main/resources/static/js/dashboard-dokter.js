(() => {
  'use strict';
  const root=document.getElementById('dokter-workspace'); if(!root) return;
  const live=document.getElementById('dk-live');
  const liveText=live && live.querySelector('span');
  const offline=document.getElementById('dk-offline');
  const manual=document.getElementById('dokter-refresh');
  const dialog=document.getElementById('skip-dialog');
  let busy=false, submitting=false, timer, remaining=Number(root.dataset.remaining), sampled=performance.now();

  // Dialog konfirmasi pelewatan (state Hi-Fi 53:856). Tanpa JavaScript, form noscript dipakai.
  if(dialog && typeof dialog.showModal==='function') {
    for(const opener of root.querySelectorAll('#skip-open,[data-skip-open]')) {
      opener.hidden=false;
      opener.addEventListener('click',()=>{ if(!opener.disabled) dialog.showModal(); });
    }
    document.getElementById('skip-close').addEventListener('click',()=>{ if(!submitting) dialog.close(); });
    dialog.addEventListener('cancel',event=>{ if(submitting) event.preventDefault(); });
  }

  function stale(text) {
    if(live) { live.classList.add('dk-live-stale'); liveText.textContent=text; }
    if(offline) offline.hidden=false;
    for(const button of root.querySelectorAll('[data-call-action]')) button.disabled=true;
  }
  async function refresh() {
    if(busy || submitting) return;
    clearTimeout(timer);
    if(document.hidden) { timer=setTimeout(refresh,5000); return; }
    busy=true; if(manual) manual.disabled=true;
    const controller=new AbortController(); const timeout=setTimeout(()=>controller.abort(),8000);
    try {
      const response=await fetch(root.dataset.apiUrl,{cache:'no-store',credentials:'same-origin',signal:controller.signal});
      if(response.redirected || response.status===401 || response.status===403) { stale('Sesi berakhir · masuk kembali'); return; }
      if(!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('refresh');
      const data=await response.json();
      if(submitting) return;
      if(typeof data.versi!=='string') throw new Error('snapshot');
      // Booking, check-in, panggilan atau perubahan status lain mengganti versi: muat ulang dari server.
      if(data.versi!==root.dataset.version || (offline && !offline.hidden)) { window.location.reload(); return; }
      const p=data.panggilan;
      if(p) {
        remaining=p.sisaDetik; sampled=performance.now();
        for(const skip of root.querySelectorAll('[data-skip]')) skip.disabled=!p.bolehLewati;
        for(const call of root.querySelectorAll('[data-call]')) call.disabled=!p.bolehPanggil;
      }
      if(live) { live.classList.remove('dk-live-stale'); liveText.textContent=`Diperbarui otomatis · ${data.sekarang.slice(0,5)}`; }
    } catch { stale('Pembaruan gagal · data lama'); }
    finally { clearTimeout(timeout); busy=false; if(manual) manual.disabled=false; if(!submitting) timer=setTimeout(refresh,5000); }
  }
  const countdown=setInterval(()=>{
    const text=document.getElementById('call-wait'); if(!text) return;
    const seconds=Math.max(0,remaining-Math.floor((performance.now()-sampled)/1000));
    text.textContent=seconds>0?`Sisa batas tunggu: ${seconds} detik.`:'Batas tunggu 120 detik tercapai. Kelayakan tindakan diperiksa oleh server.';
  },1000);
  for(const form of root.querySelectorAll('.dokter-action')) form.addEventListener('submit',event=>{
    if(submitting) { event.preventDefault(); return; }
    submitting=true; clearTimeout(timer);
    for(const button of root.querySelectorAll('[data-call-action]')) button.disabled=true;
  });
  if(manual) manual.addEventListener('click',refresh);
  document.addEventListener('visibilitychange',()=>{ if(!document.hidden) refresh(); });
  window.addEventListener('pagehide',()=>{ clearTimeout(timer); clearInterval(countdown); });
  window.addEventListener('pageshow',event=>{ if(event.persisted) window.location.reload(); });
  timer=setTimeout(refresh,5000);
})();
