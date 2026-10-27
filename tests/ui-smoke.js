/* Optional: npm install --no-save playwright; then node tests/ui-smoke.js.
   RUMO_CHROMIUM may point to an installed Chromium executable.
   This verifies the WebView UI with a mocked native bridge, not device biometrics. */
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.RUMO_CHROMIUM?{executablePath:process.env.RUMO_CHROMIUM}:{}),args:['--no-sandbox','--allow-file-access-from-files']});
 try {
  const context=await browser.newContext({viewport:{width:390,height:844},timezoneId:'America/Sao_Paulo'});
  await context.addInitScript(()=>{
   const d=new Date(),day=[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
   const legacy={};legacy[day]={status:'slip',urge:8,triggers:['Banheiro'],note:'registro legado privado',updatedAt:new Date().toISOString()};
   localStorage.setItem('rumo_diario_v1',JSON.stringify(legacy));window.__vault={};window.__calls={};
   window.RumoAndroid={
    getProtectedItem:k=>window.__vault[k]??null,setProtectedItem:(k,v)=>{window.__vault[k]=v;return true;},
    setProtectedBundle:s=>{Object.assign(window.__vault,JSON.parse(s));return true;},
    migrateStorage:s=>{for(const [k,v]of Object.entries(JSON.parse(s)))if(!(k in window.__vault))window.__vault[k]=v;return true;},
    getAppInfo:()=>JSON.stringify({version:'1.3.0',android:'15',encryptedStorage:true,pinEnabled:false,biometricAvailable:true,supportEmail:'teste@example.invalid'}),
    syncWidget:s=>{window.__calls.widget=JSON.parse(s);return true;},copyText:s=>{window.__calls.clipboard=s;},
    securitySettings:()=>{window.__calls.security=true;},addWidget:()=>{window.__calls.addWidget=true;},
    composeFeedback:(kind,text)=>{window.__calls.feedback={kind,text};},configureReminders:()=>{},saveFile:()=>{}
   };
  });
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  const url=pathToFileURL(path.join(__dirname,'../app/src/main/assets/index.html')).href;
  await page.goto(url);
  assert.equal(await page.evaluate(()=>window.__rumoBootError),undefined);
  assert.equal(await page.evaluate(()=>localStorage.getItem('rumo_diario_v1')),null);
  assert.ok(await page.evaluate(()=>window.__vault.rumo_diario_v1.includes('registro legado privado')));
  await page.locator('[data-action="today-checkin"]').first().click();
  await page.locator('input[name="status"][value="done"]').check();
  await page.locator('textarea[name="note"]').fill('rascunho que deve continuar');
  await page.locator('#new-trigger').fill('Computador à noite');await page.locator('[data-action="add-trigger"]').click();
  assert.equal(await page.locator('textarea[name="note"]').inputValue(),'rascunho que deve continuar');
  assert.ok(await page.locator('input[name="triggers"][value="Computador à noite"]').isChecked());
  await page.locator('#new-trigger').fill('<img src=x onerror=window.__testXss=1>');await page.locator('[data-action="add-trigger"]').click();
  assert.equal(await page.locator('.chips img').count(),0,'Custom labels must be rendered as text');
  assert.equal(await page.evaluate(()=>window.__testXss),undefined);
  await page.locator('#quick-mode').check();
  assert.equal(await page.locator('textarea[name="note"]').inputValue(),'rascunho que deve continuar');
  assert.equal(await page.locator('details:has(#strategy-used)').getAttribute('open'),null);
  await page.locator('details:has(#strategy-used)>summary').click();
  await page.locator('#strategy-used').fill('Caminhar por dez minutos');
  await page.locator('input[name="coping"][value="helped"]').check();
  await page.locator('input[name="interrupt"][value="yes"]').check();
  await page.locator('#checkin-form button[type="submit"]').click();
  const saved=await page.evaluate(()=>JSON.parse(window.__vault.rumo_diario_v1));
  const record=Object.values(saved)[0];assert.equal(record.strategy,'Caminhar por dez minutos');assert.ok(record.triggers.includes('Computador à noite'));assert.equal(record.status,'done');
  await page.locator('[data-tab="stats"]').click();assert.ok((await page.locator('#view').innerText()).includes('Computador à noite'));
  await page.locator('[data-tab="settings"]').click();
  await page.locator('#plan-settings>summary').click();await page.locator('#plan-trigger').fill('Estiver sozinho com o computador');await page.locator('#plan-action').fill('Levantar e caminhar');await page.locator('[data-action="save-plan"]').click();
  await page.locator('[data-tab="tools"]').click();assert.ok((await page.locator('.saved-plan').innerText()).includes('Levantar e caminhar'));
  await page.locator('[data-tab="settings"]').click();
  await page.locator('details:has(#widget-enabled)>summary').click();await page.locator('#widget-enabled').check();await page.locator('[data-action="save-widget"]').click();
  const widget=await page.evaluate(()=>window.__calls.widget);assert.equal(widget.count,1);assert.equal(widget.enabled,true);assert.ok(!JSON.stringify(widget).includes('rascunho'));
  await page.locator('details:has(#feedback-message)>summary').click();await page.locator('#feedback-kind').selectOption('bug');await page.locator('#feedback-message').fill('O botão não respondeu ao toque.');
  await page.locator('[data-action="send-feedback"]').click();assert.deepEqual(await page.evaluate(()=>window.__calls.feedback),{kind:'bug',text:'O botão não respondeu ao toque.'});
  await page.locator('[data-action="copy-feedback"]').click();const copied=await page.evaluate(()=>window.__calls.clipboard);assert.ok(copied.includes('O botão não respondeu'));assert.ok(!copied.includes('rascunho que deve continuar'));
  await page.locator('[data-action="security-settings"]').click();assert.equal(await page.evaluate(()=>window.__calls.security),true);
  for(const theme of ['oceano','violeta','floresta','areia']){await page.locator(`[data-action="choose-theme"][data-theme="${theme}"]`).click();assert.equal(await page.locator('html').getAttribute('data-theme'),theme);}
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
  if(process.env.RUMO_QA_DIR)await page.screenshot({path:path.join(process.env.RUMO_QA_DIR,'mobile-settings.png'),fullPage:true});
  await page.setViewportSize({width:1280,height:900});await page.locator('[data-tab="stats"]').click();
  if(process.env.RUMO_QA_DIR)await page.screenshot({path:path.join(process.env.RUMO_QA_DIR,'desktop-stats.png'),fullPage:true});
  assert.deepEqual(errors,[]);
  await context.close();
  const failure=await browser.newContext();
  await failure.addInitScript(()=>{
   localStorage.setItem('rumo_diario_v1','{"2026-10-25":{"status":"done","note":"preservar cópia anterior"}}');
   window.RumoAndroid={getProtectedItem:()=>null,setProtectedItem:()=>false,migrateStorage:()=>false};
  });
  const failed=await failure.newPage();await failed.goto(url);
  assert.ok(await failed.evaluate(()=>window.__rumoBootError));
  assert.ok((await failed.locator('#view').innerText()).includes('preservados'));
  assert.ok(await failed.evaluate(()=>localStorage.getItem('rumo_diario_v1').includes('preservar cópia anterior')));
  await failure.close();
  console.log('PASS: UI flows with native bridge, migration success/failure, draft retention, custom triggers, quick check-in, alternatives, plan, widget/email privacy, four themes and layouts.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
