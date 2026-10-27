const assert=require('node:assert/strict');
const F=require('../app/src/main/assets/features.js');
const now='2026-10-26';
assert.deepEqual(F.labels([' Computador ','computador','',null,'Cansaço','Cansac\u0327o']),['Computador','Cansaço']);
assert.equal(F.labels(Array.from({length:50},(_,i)=>'Gatilho '+i)).length,30);
assert.equal(F.labels(['a'.repeat(100)])[0].length,60);
assert.equal(F.validDate('2026-02-30'),false);
assert.equal(F.validDate('2024-02-29'),true);
assert.equal(F.move('2026-01-01',-1),'2025-12-31');
const entries={
 '2026-10-26':{status:'done',triggers:['Computador'],interrupt:'yes',coping:'helped',strategy:'Caminhar',distress:3},
 '2026-10-25':{status:'done',triggers:['computador'],distress:5},
 '2026-10-23':{status:'done',triggers:['Cansaço']},
 '2026-10-27':{status:'done'},
 '2026-13-01':{status:'done'},
 '2026-10-22':{}
};
let stats=F.progress(entries,now);
assert.equal(stats.registered,3);assert.equal(stats.done,3);assert.equal(stats.sequence,2);assert.equal(stats.pending,false);
const ledger=F.award({},stats,now);assert.equal(ledger.unlocked['done-3'],now);
assert.equal(ledger.unlocked['interrupted-1'],now);assert.equal(ledger.unlocked['helped-1'],now);
entries[now].status='slip';stats=F.progress(entries,now);
assert.equal(stats.done,2);assert.equal(stats.sequence,0);
assert.equal(F.award(ledger,stats,now).unlocked['done-3'],now,'A difficult day does not remove an earned badge');
delete entries[now].status;stats=F.progress(entries,now);assert.equal(stats.sequence,1);assert.equal(stats.pending,true);
delete entries['2026-10-25'];stats=F.progress(entries,now);assert.equal(stats.sequence,0);assert.equal(stats.pending,true);
assert.equal(F.progress(entries,now).registered,2,'Future/invalid dates and empty imports do not count');
assert.deepEqual(F.counts(['__proto__','__proto__','Computador','computador']),[['__proto__',2],['Computador',2]]);
const weekly=F.weekly(entries,now);assert.equal(weekly.rated,1);assert.equal(weekly.distress,3);assert.equal(weekly.helped,1);
const widget=F.widget(stats,{widgetEnabled:true,widgetMode:'total'});
assert.deepEqual(Object.keys(widget).sort(),['asOf','count','enabled','mode','pending']);
assert.equal(F.widget(stats,{widgetEnabled:false}).enabled,false);
assert.equal(F.widget(stats,{widgetEnabled:true,widgetMode:'sequence'}).pending,true);
const message=F.mailText('bug','Falha no botão',{version:'1.3.0',android:'15',entries});
assert.ok(message.includes('Falha no botão'));assert.ok(!message.includes('Caminhar'));assert.ok(!message.includes('Computador'));
assert.equal(F.cleanLedger({unlocked:{'unknown':'2026-10-26','done-3':'2026-10-27'}},now).unlocked['done-3'],undefined);
console.log('PASS: custom labels, date gaps/future dates, cumulative rewards, permanent milestones, weekly summary and widget/email data minimization.');
