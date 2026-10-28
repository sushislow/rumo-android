/* DOM-level UI tests; a device is still needed for native PIN, biometrics and Google account selection.
   npm install --prefix /tmp/rumo-ui-tests jsdom@26.1.0 --no-audit --no-fund
   NODE_PATH=/tmp/rumo-ui-tests/node_modules node tests/security-ui.cjs */
const {JSDOM}=require('jsdom'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{webcrypto}=require('node:crypto');
const base=path.join(__dirname,'../app/src/main/assets');
const html=fs.readFileSync(path.join(base,'index.html'),'utf8');
const dom=new JSDOM(html,{url:'https://appassets.androidplatform.net/assets/index.html',runScripts:'outside-only',pretendToBeVisual:true});
const w=dom.window,vault={},files=[];
Object.defineProperty(w,'crypto',{value:webcrypto});Object.assign(w,{TextEncoder,TextDecoder,Blob,scrollTo(){},confirm:()=>true});
w.RumoAndroid={getProtectedItem:k=>vault[k]??null,setProtectedItem:(k,v)=>{vault[k]=v;return true;},migrateStorage:s=>{Object.assign(vault,JSON.parse(s));return true;},setProtectedBundle:s=>{Object.assign(vault,JSON.parse(s));return true;},getAppInfo:()=>JSON.stringify({version:'1.4.0-beta',encryptedStorage:true}),syncWidget:()=>true,saveFile:(...args)=>files.push(args)};
w.eval(fs.readFileSync(path.join(base,'features.js'),'utf8'));w.eval(html.match(/<script>([\s\S]*?)<\/script>/)[1]);
const $=s=>w.document.querySelector(s),click=s=>$(s).click(),submit=()=>$('.password-card').dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
async function until(check){for(let i=0;i<60;i++){if(check())return;await new Promise(resolve=>setTimeout(resolve,25));}throw Error('UI operation did not finish');}
(async()=>{
 assert.equal(w.__rumoBootError,undefined);click('[data-tab=settings]');assert.equal(w.document.querySelectorAll('.theme-choice').length,8);
 for(const theme of ['oceano','violeta','floresta','areia','aurora','grafite','ceu','terracota']){click('[data-action=choose-theme][data-theme='+theme+']');assert.equal(w.document.documentElement.dataset.theme,theme);assert.equal(JSON.parse(vault.rumo_preferencias_v1).theme,theme);}
 click('[data-action=export-secure]');assert.ok($('.password-overlay'));assert.equal($('#backup-password').type,'password');assert.equal($('#backup-confirm').type,'password');
 $('#backup-reveal').checked=true;$('#backup-reveal').dispatchEvent(new w.Event('change'));assert.equal($('#backup-password').type,'text');
 $('#backup-password').value='short';$('#backup-confirm').value='different';submit();assert.ok($('.password-error').textContent.includes('10'));assert.equal(files.length,0);
 $('#backup-password').value='synthetic-backup-password';$('#backup-confirm').value='mismatch-password';submit();assert.ok($('.password-error').textContent.includes('conferem'));assert.equal(files.length,0);
 $('#backup-confirm').value='synthetic-backup-password';submit();await until(()=>files.length===1);assert.equal($('.password-overlay'),null);assert.equal(JSON.parse(files[0][2]).encrypted,true);assert.ok(!files[0][2].includes('synthetic-backup-password'));
 click('[data-action=export-secure]');click('.password-card [data-cancel]');await new Promise(resolve=>setTimeout(resolve,0));assert.equal($('.password-overlay'),null);assert.equal(files.length,1);
 click('[data-action=export-secure]');$('#backup-password').value='must be cleared';w.RumoDismissSensitiveDialogs();assert.equal($('.password-overlay'),null);assert.equal(w.document.querySelector('input[type=password]'),null);assert.equal(files.length,1);
 console.log('PASS: 8 themes, masked/revealed passwords, length/mismatch validation, encrypted backup, cancellation, and clearing on native background lock.');
 dom.window.close();
})().catch(error=>{console.error(error);dom.window.close();process.exitCode=1;});
