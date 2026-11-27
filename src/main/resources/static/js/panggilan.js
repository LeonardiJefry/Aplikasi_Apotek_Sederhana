(() => {
  'use strict';
  const root=document.getElementById('panggilan-workspace'); if(!root) return;
  const state=document.getElementById('call-refresh-state');
  const manual=document.getElementById('call-refresh'); manual.hidden=false;
  const dialog=document.getElementById('skip-dialog');
  const open=document.getElementById('skip-open');
  const close=document.getElementById('skip-close');
  let busy=false, submitting=false, timer, remaining=Number(root.dataset.remaining), sampled=performance.now();
  const supportsDialog=dialog && typeof dialog.showModal==='function';
  if(supportsDialog) {
    dialog.close(); dialog.dataset.modal='true'; open.hidden=false; close.hidden=false;
    document.getElementById('skip-back').hidden=true;
    open.addEventListener('click',()=>dialog.showModal());
    close.addEventListener('click',()=>{if(!submitting) dialog.close();});
    dialog.addEventListener('cancel',event=>{if(submitting) event.preventDefault();});
  }
  function stale(text) {
    state.className='alert danger'; state.textContent=text;
    for(const button of root.querySelectorAll('[data-call-action]')) button.disabled=true;
    if(open) open.disabled=true;
  }
  async function refresh() {
    if(busy || submitting) return;
    clearTimeout(timer);
    if(document.hidden) { timer=setTimeout(refresh,5000); return; }
    busy=true; manual.disabled=true;
    const controller=new AbortController(); const timeout=setTimeout(()=>controller.abort(),8000);
    try {
      const response=await fetch(root.dataset.apiUrl,{cache:'no-store',credentials:'same-origin',signal:controller.signal});
      if(response.redirected || response.status===401 || response.status===403) { stale('Sesi atau akses Anda berubah. Silakan masuk kembali.'); return; }
      if(!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('refresh');
      const data=await response.json();
      if(submitting) return;
      if(typeof data.versi!=='string' || typeof data.bolehLewati!=='boolean') throw new Error('snapshot');
      if(data.versi!==root.dataset.version) { window.location.reload(); return; }
      remaining=data.sisaDetik; sampled=performance.now();
      if(open) open.disabled=!data.bolehLewati;
      const skip=document.getElementById('skip-submit'); if(skip) skip.disabled=!data.bolehLewati;
      for(const form of root.querySelectorAll('.panggilan-action')) {
        const button=form.querySelector('[data-call-action]');
        if(form.action.endsWith('/hadir')) button.disabled=!(data.jamLayanan && data.dipanggil && !data.sedangDilayani);
        if(form.action.endsWith('/panggil')) button.disabled=!data.bolehPanggil;
      }
      state.className='help'; state.textContent=`Diperbarui ${data.sekarang} WIB · otomatis setiap 5 detik`;
    } catch { stale('Pembaruan gagal. Data terakhir belum diperbarui. Muat ulang atau coba lagi.'); }
    finally { clearTimeout(timeout); busy=false; manual.disabled=false; if(!submitting) timer=setTimeout(refresh,5000); }
  }
  const countdown=setInterval(()=>{
    const text=document.getElementById('call-wait'); if(!text) return;
    const seconds=Math.max(0,remaining-Math.floor((performance.now()-sampled)/1000));
    text.textContent=seconds>0?`Sisa batas tunggu: ${seconds} detik.`:'Batas tunggu 120 detik tercapai. Kelayakan tindakan diperiksa oleh server.';
  },1000);
  for(const form of root.querySelectorAll('.panggilan-action')) form.addEventListener('submit',event=>{
    if(submitting) {event.preventDefault(); return;}
    submitting=true; clearTimeout(timer);
    for(const button of root.querySelectorAll('[data-call-action]')) button.disabled=true;
    if(open) open.disabled=true; if(close) close.disabled=true;
  });
  manual.addEventListener('click',refresh);
  document.addEventListener('visibilitychange',()=>{if(!document.hidden) refresh();});
  window.addEventListener('pagehide',()=>{clearTimeout(timer); clearInterval(countdown);});
  window.addEventListener('pageshow',event=>{if(event.persisted) window.location.reload();});
  timer=setTimeout(refresh,5000);
})();
