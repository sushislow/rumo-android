(function(root){
  'use strict';
  const text=(value,max=60)=>typeof value==='string'?value.normalize('NFC').replace(/[\u0000-\u001f\u007f]/g,' ').trim().slice(0,max):'';
  const key=value=>value.toLocaleLowerCase('pt-BR');
  function labels(raw,max=30){
    const seen=new Set(),result=[];
    if(!Array.isArray(raw))return result;
    for(const value of raw){const label=text(value);if(label&&!seen.has(key(label))){seen.add(key(label));result.push(label);if(result.length>=max)break;}}
    return result;
  }
  function validDate(iso){
    if(typeof iso!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(iso))return false;
    const [y,m,d]=iso.split('-').map(Number),date=new Date(y,m-1,d,12);
    return date.getFullYear()===y&&date.getMonth()===m-1&&date.getDate()===d;
  }
  function move(iso,delta){const [y,m,d]=iso.split('-').map(Number),date=new Date(y,m-1,d+delta,12);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;}
  function meaningful(e){return !!e&&typeof e==='object'&&(e.status==='done'||e.status==='slip'||Number.isInteger(e.urge)||Number.isInteger(e.distress)||(Array.isArray(e.triggers)&&e.triggers.length>0)||!!e.updatedAt);}
  function progress(entries,now){
    const dates=Object.keys(entries||{}).filter(d=>validDate(d)&&d<=now&&meaningful(entries[d])).sort();
    const values=dates.map(d=>entries[d]);
    let sequence=0,cursor=entries?.[now]?.status==='done'?now:move(now,-1);
    if(entries?.[now]?.status!=='slip')while(entries?.[cursor]?.status==='done'){sequence++;cursor=move(cursor,-1);}
    return {registered:dates.length,done:values.filter(e=>e.status==='done').length,
      interrupted:values.filter(e=>e.interrupt==='yes').length,helped:values.filter(e=>e.coping==='helped').length,
      sequence,pending:!['done','slip'].includes(entries?.[now]?.status),asOf:now,lastRecorded:dates.length?dates[dates.length-1]:''};
  }
  const badges=[
    ...[1,7,14,30,60,90].map(n=>({id:'registered-'+n,metric:'registered',goal:n,title:n===1?'Primeiro passo':`${n} dias de registros`,description:'Dias diferentes em que você acompanhou seu caminho.'})),
    ...[3,7,14,30,60,90].map(n=>({id:'done-'+n,metric:'done',goal:n,title:`${n} dias de escolhas`,description:'Total de dias registrados sem consumo. Não precisam ser consecutivos.'})),
    ...[1,5,15].map(n=>({id:'interrupted-'+n,metric:'interrupted',goal:n,title:n===1?'Percebi e interrompi':`Interrupção praticada em ${n} dias`,description:'Dias em que você relatou interromper um impulso.'})),
    ...[1,5,15].map(n=>({id:'helped-'+n,metric:'helped',goal:n,title:n===1?'Uma alternativa ajudou':`Alternativas úteis em ${n} dias`,description:'Dias em que uma estratégia alternativa ajudou, segundo seu registro.'}))
  ];
  function cleanLedger(value,now){const unlocked={};for(const badge of badges){const date=value?.unlocked?.[badge.id];if(validDate(date)&&date<=now)unlocked[badge.id]=date;}return {version:1,unlocked};}
  function award(ledger,stats,now){const next=cleanLedger(ledger,now);for(const b of badges)if(stats[b.metric]>=b.goal&&!next.unlocked[b.id])next.unlocked[b.id]=now;return next;}
  function counts(values){const map=new Map();for(const value of values){const label=text(value,300),id=key(label);if(label){const item=map.get(id);map.set(id,item?[item[0],item[1]+1]:[label,1]);}}return [...map.values()].sort((a,b)=>b[1]-a[1]||a[0].localeCompare(b[0],'pt-BR'));}
  function weekly(entries,now){const days=Array.from({length:7},(_,i)=>move(now,i-6)),values=days.map(d=>entries?.[d]).filter(meaningful);return {
    registered:values.length,done:values.filter(e=>e.status==='done').length,interrupted:values.filter(e=>e.interrupt==='yes').length,
    helped:values.filter(e=>e.coping==='helped').length,triggers:counts(values.flatMap(e=>labels(e.triggers,20))),
    strategies:counts(values.filter(e=>e.coping==='helped').map(e=>text(e.strategy,300))),
    rated:values.filter(e=>Number.isInteger(e.distress)).length,
    distress:values.filter(e=>Number.isInteger(e.distress)).reduce((s,e)=>s+e.distress,0)
  };}
  function widget(stats,prefs){const mode=['total','registered','sequence'].includes(prefs.widgetMode)?prefs.widgetMode:'total';return {enabled:prefs.widgetEnabled===true,mode,count:mode==='sequence'?stats.sequence:mode==='registered'?stats.registered:stats.done,pending:mode==='sequence'&&stats.pending,asOf:stats.asOf};}
  function mailText(kind,message,info){const heading=kind==='bug'?'Relato de problema':'Sugestão';return `${heading} — Rumo\n\n${text(message,6000)}\n\nVersão do app: ${text(info?.version,30)||'1.3.0'}\nSistema: ${info?.android?'Android '+text(info.android,30):'Navegador'}\n\nNenhum registro do diário foi anexado automaticamente.`;}
  const api={text,labels,validDate,move,meaningful,progress,badges,cleanLedger,award,counts,weekly,widget,mailText};
  if(typeof module==='object'&&module.exports)module.exports=api;
  root.RumoFeatures=api;
})(typeof globalThis!=='undefined'?globalThis:this);
