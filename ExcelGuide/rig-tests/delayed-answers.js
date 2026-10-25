const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const T0=Date.now(); const ts=()=>((Date.now()-T0)/1000).toFixed(1)+'s';
function join(name){return new Promise((res,rej)=>{const b=mf.createBot({host:'127.0.0.1',port:26181,username:name,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;const ow=b._client.write.bind(b._client);b._client.write=(n,p)=>{if(n==='position'||n==='look'||n==='position_look')return;return ow(n,p)};b.msgs=[];
 b.on('message',(m,pos)=>{if(pos==='game_info')return;const t=m.toString(); b.msgs.push({at:Date.now(),t}); console.log(ts(),'['+name+' sees]',t.slice(0,220));});
 b.once('spawn',()=>setTimeout(()=>res(b),1500)); b.on('kicked',r=>{console.log('KICK',name);fail++;}); b.on('error',e=>rej(e));});}
const mode=process.argv[2];
let pass=0,fail=0; const ok=(c,w)=>{c?pass++:fail++; console.log((c?'PASS ':'FAIL ')+w)};
const guideLines=(b,since)=>b.msgs.filter(m=>m.at>=since && !/^<\w+>/.test(m.t) && !/joined the game|left the game/.test(m.t));
(async()=>{
 if(mode==='ac'){
  const a=await join('RigAskA'), c=await join('RigAskC');
  await sleep(3000);
  const t=Date.now();
  console.log(ts(),'> RigAskA: how do i make a potion?'); a.chat('how do i make a potion?');
  console.log(ts(),'> RigAskC: how do you tame an axolotl?'); c.chat('how do you tame an axolotl?');
  await sleep(38000);
  ok(guideLines(a,t).length===0,'A: nothing from the Guide in the first 38s');
  ok(guideLines(c,t).length===0,'C: nothing from the Guide in the first 38s');
  await sleep(16000);
  const ga=guideLines(a,t), gc=guideLines(c,t);
  ok(ga.length>=1,'A: got a delayed answer'); if(ga.length) console.log('   A delay = '+((ga[0].at-t)/1000).toFixed(1)+'s');
  ok(gc.length>=1 && /STUBANSWER/.test(gc.map(x=>x.t).join(' ')),'C: AI (stub) answer delivered'); if(gc.length) console.log('   C delay = '+((gc[0].at-t)/1000).toFixed(1)+'s');
  ok(!/STUBANSWER/.test(ga.map(x=>x.t).join(' ')) && !gc.some(x=>ga.length && x.t===ga[0].t),'A and C each saw only their own private answer');
  await sleep(30000);
  ok(guideLines(a,t).length===ga.length && guideLines(c,t).length===gc.length,'each answered once (no repeat in the next 30s)');
  [a,c].forEach(x=>x.quit());
 } else if(mode==='b'){
  const b=await join('RigAskB'), bans=await join('RigAnsB');
  await sleep(3000); const t=Date.now();
  console.log(ts(),'> RigAskB: how do i tame a fox?'); b.chat('how do i tame a fox?');
  await sleep(12000);
  console.log(ts(),'> RigAnsB: crouch near the fox holding sweet berries until it trusts you'); bans.chat('crouch near the fox holding sweet berries until it trusts you');
  await sleep(5000);
  console.log(ts(),'> RigAskB: thanks that worked'); b.chat('thanks that worked');
  await sleep(45000);
  ok(guideLines(b,t).length===0,'B (held): the Guide stayed out after a player answered');
  ok(guideLines(bans,t).length===0,'B (held): the answerer saw nothing from the Guide');
  [b,bans].forEach(x=>x.quit());
 } else if(mode==='e'){
  const e=await join('RigAskE'); await sleep(3000); const t=Date.now();
  console.log(ts(),'> RigAskE: how do you tame a fox?'); e.chat('how do you tame a fox?');
  await sleep(38000); ok(guideLines(e,t).length===0,'E: nothing in the first 38s');
  await sleep(16000);
  const g=guideLines(e,t).map(x=>x.t).join(' ');
  ok(/A player said: crouch near the fox/.test(g),'E: the approved player answer is reused in plain chat');
  ok(!/RigAns|RigAsk/.test(g),'E: no player names');
  e.quit();
 } else if(mode==='d'){
  const d=await join('RigAskD'); await sleep(3000); const t=Date.now();
  console.log(ts(),'> RigAskD: how do you find a stronghold?'); d.chat('how do you find a stronghold?');
  await sleep(56000);
  const g=guideLines(d,t); ok(g.length>=1 && /don.t know/i.test(g.map(x=>x.t).join(' ')),'D (AI failing): the standard non-answer');
  if(g.length) console.log('   D delay = '+((g[0].at-t)/1000).toFixed(1)+'s');
  d.quit();
 }
 console.log('RESULT '+pass+' pass, '+fail+' fail'); setTimeout(()=>process.exit(fail?1:0),800);
})();
