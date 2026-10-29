/* Exercise the real home markup and event handlers with synthetic local data. */
const {JSDOM}=require('jsdom');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const base=path.join(__dirname,'../app/src/main/assets');
const html=fs.readFileSync(path.join(base,'index.html'),'utf8');
const date=offset=>{const d=new Date();d.setDate(d.getDate()+offset);return [d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');};
function boot(entries={},preferences={}){
 const dom=new JSDOM(html,{url:'https://appassets.androidplatform.net/assets/index.html',runScripts:'outside-only'}),w=dom.window;
 const vault={rumo_diario_v1:JSON.stringify(entries),rumo_preferencias_v1:JSON.stringify(preferences)};
 Object.assign(w,{scrollTo(){},TextEncoder,TextDecoder});
 w.RumoAndroid={getProtectedItem:k=>vault[k]??null,setProtectedItem:(k,v)=>{vault[k]=v;return true;},migrateStorage:()=>true,setProtectedBundle:s=>{Object.assign(vault,JSON.parse(s));return true;},getAppInfo:()=>JSON.stringify({pinEnabled:true}),syncWidget:()=>true};
 w.eval(fs.readFileSync(path.join(base,'features.js'),'utf8'));w.eval(html.match(/<script>([\s\S]*?)<\/script>/)[1]);
 assert.equal(w.__rumoBootError,undefined);
 return {w,vault,$:s=>w.document.querySelector(s),all:s=>[...w.document.querySelectorAll(s)],close:()=>w.close()};
}
(async()=>{
 let t=boot();
 assert.equal(t.all('.week-day').length,7);assert.equal(t.all('.week-day.done').length,0);
 assert.equal(t.$('.home-progress-value'),null);assert.match(t.$('.home-cta').textContent,/Fazer meu check-in/);
 assert.equal(t.$('#home-calendar').open,false);assert.ok(t.$('[data-tab=calendar]').getAttribute('aria-current')==='page');
 t.$('.home-cta').click();assert.ok(t.$('#checkin-form'));assert.equal(t.$('input[name=status]:checked').value,'none');
 t.$('[data-tab=calendar]').click();t.$('.home-help').click();assert.match(t.$('#view').textContent,/Ajuda para os momentos difíceis/);t.close();
 const entries={[date(0)]:{status:'slip',note:'preservar registro',updatedAt:new Date().toISOString()},[date(-1)]:{status:'done'},[date(-3)]:{status:null,note:'registro parcial'},[date(-12)]:{status:'done'}};
 t=boot(entries);const before=t.vault.rumo_diario_v1;
 assert.match(t.$('#home-title').textContent,/Seu caminhocontinua aqui/);
 assert.equal(t.$('.home-progress-value').textContent,'2');
 assert.equal(t.all('.week-day.done').length,1);assert.equal(t.all('.week-day.slip').length,1);assert.equal(t.all('.week-day.partial').length,1);
 t.$('#home-calendar').open=true;await new Promise(r=>setTimeout(r,0));t.$('[data-action=prev-month]').click();
 assert.equal(t.$('#home-calendar').open,true);assert.ok(t.all('.day[disabled]').every(e=>e.dataset.date>date(0)));
 t.$('.week-day.is-today').click();assert.equal(t.$('textarea[name=note]').value,'preservar registro');
 t.$('[data-tab=calendar]').click();t.$('.home-progress-link').click();assert.match(t.$('#view').textContent,/Sua evolução/);
 assert.equal(t.vault.rumo_diario_v1,before);t.close();
 t=boot({[date(0)]:{status:'done'}},{showCounter:false});assert.match(t.$('.home-cta').textContent,/Revisar meu check-in/);assert.equal(t.$('.home-progress'),null);t.close();
 t=boot({[date(0)]:{status:null}});assert.match(t.$('.home-state').textContent,/parcial/);assert.equal(t.$('.home-progress-value'),null);t.close();
 console.log('PASS: empty, partial and completed home states; cumulative count; counter preference; week/check-in/help/stats navigation; expanded calendar; existing records preserved.');
})().catch(error=>{console.error(error);process.exitCode=1;});
