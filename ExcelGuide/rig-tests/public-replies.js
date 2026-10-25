const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const T0=Date.now(); const ts=()=>((Date.now()-T0)/1000).toFixed(1)+'s';
function join(name){return new Promise((res,rej)=>{const b=mf.createBot({host:'127.0.0.1',port:26181,username:name,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;const ow=b._client.write.bind(b._client);b._client.write=(n,p)=>{if(n==='position'||n==='look'||n==='position_look')return;return ow(n,p)};b.msgs=[];
 b.on('message',(m,pos)=>{if(pos==='game_info')return;const t=m.toString(); b.msgs.push({at:Date.now(),t}); console.log(ts(),'['+name+' sees]',t.slice(0,220));});
 b.once('spawn',()=>setTimeout(()=>res(b),1500)); b.on('kicked',r=>{console.log('KICK',name);fail++;}); b.on('error',e=>rej(e));});}
const mode=process.argv[2];
let pass=0,fail=0; const ok=(c,w)=>{c?pass++:fail++; console.log((c?'PASS ':'FAIL ')+w)};
const guide=(b,since)=>b.msgs.filter(m=>m.at>=since && /Valley Guide/.test(m.t)).map(m=>m.t);
(async()=>{
 if(mode==='pub'){
  const a=await join('RigPubA'), c=await join('RigPubC'), w=await join('RigWatch');
  await sleep(3000); const t=Date.now();
  console.log(ts(),'> RigPubA: how do i make a potion?'); a.chat('how do i make a potion?');
  console.log(ts(),'> RigPubC: how do you find a stronghold?'); c.chat('how do you find a stronghold?');
  await sleep(38000);
  ok(guide(a,t).length+guide(c,t).length+guide(w,t).length===0,'nothing from the Guide in the first 38s');
  await sleep(16000);
  for (const [b,n] of [[a,'A'],[c,'C'],[w,'watcher']]){
    const g=guide(b,t).join(' | ');
    ok(/nether wart/i.test(g),n+' sees the website-knowledge answer (public)');
    ok(/STUBANSWER/.test(g),n+' sees the AI (stub) answer (public)');
  }
  const all=guide(w,t).join(' | ');
  ok(!/RigPub|RigWatch/.test(all),'public Guide lines carry no player names');
  ok(!/[0-9a-f]{8}-[0-9a-f]{4}-/.test(all),'public Guide lines carry no UUIDs');
  await sleep(20000);
  ok(guide(w,t).length===2,'exactly two public Guide lines (no repeats)');
  [a,c,w].forEach(x=>x.quit());
 } else if(mode==='thx'){
  const b=await join('RigThxB'), ans=await join('RigThxAns'), w=await join('RigWatch');
  await sleep(3000); const t=Date.now();
  console.log(ts(),'> RigThxB: how do i tame a fox?'); b.chat('how do i tame a fox?');
  await sleep(10000);
  console.log(ts(),'> RigThxAns: crouch near the fox holding sweet berries until it trusts you'); ans.chat('crouch near the fox holding sweet berries until it trusts you');
  await sleep(5000);
  console.log(ts(),'> RigThxB: thanks that worked'); b.chat('thanks that worked');
  await sleep(45000);
  ok(guide(w,t).length===0,'held: the Guide stayed out after a player answered and got thanks');
  console.log(ts(),'waiting for the peer window to close (no /guide review used)');
  await sleep(300000);
  b.quit(); ans.quit();
  const e=await join('RigThxE'); await sleep(3000); const t2=Date.now();
  console.log(ts(),'> RigThxE: how do you tame a fox?'); e.chat('how do you tame a fox?');
  await sleep(38000); ok(guide(e,t2).length===0,'reuse: nothing in the first 38s');
  await sleep(16000);
  const ge=guide(e,t2).join(' | '), gw=guide(w,t2).join(' | ');
  ok(/A player said: crouch near the fox/.test(ge),'reuse: new asker gets the thanked answer, never reviewed');
  ok(/A player said: crouch near the fox/.test(gw),'reuse: posted publicly (watcher sees it)');
  ok(!/RigThx|RigWatch/.test(gw),'reuse: no player names');
  e.quit(); w.quit();
 }
 console.log('RESULT '+pass+' pass, '+fail+' fail'); setTimeout(()=>process.exit(fail?1:0),800);
})();
