// EA 0.1.6 restart check (rig only): dragon state across restarts, never-won vs killed, + forceload console check.
const T=require('./lib');const {L,cons,sleep,join,since,con}=T;const fs=require('fs');
const R=[];const P=(ok,name,detail)=>{const s=(ok?'PASS ':'FAIL ')+name+(detail?' | '+detail:'');R.push(s);L(s)};
const E='world_the_end';const PHASE=process.env.PHASE;
const dstate=async()=>{const o=await cons('rigdragon '+E+' state',800,true);const l=o.split('\n').find(x=>/RIGDRAGON/.test(x))||'';L('  '+l.slice(0,300));return l};
const status=async()=>{const o=await cons('attunement status',900,true);const l=o.split('\n').find(x=>/Arena locks/.test(x))||o;const m=l.match(/world_the_end: [^.]*/);L('  status: '+(m?m[0]:l.slice(-120)));return l};
const fcheck=async(label)=>{await cons('execute in minecraft:the_end run forceload add -128 -128 127 127',2000);await sleep(8000);
  const o=await cons('execute in minecraft:the_end if entity @e[type=minecraft:ender_dragon]',1200);await cons('execute in minecraft:the_end run forceload remove all',1500);return o};
const pre=async()=>{const o=await cons('execute in minecraft:the_end if entity @e[type=minecraft:ender_dragon]',1200);return o};
(async()=>{L('=== EA 0.1.6 restart check PHASE='+PHASE+' '+new Date().toString());
if(PHASE==='spawn'){ // fresh End: a player arrives, dragon spawns, player leaves
  await cons('difficulty peaceful',300,true);
  const A=await join('Ra'+(Date.now()%1000));const a=A.username;await cons('gamemode creative '+a,300,true);
  await cons(`rigpos ${a} ${E} 100.5 49 0.5 90 0`,1500,true);await sleep(5000);const ds=await dstate();
  P(/live=1/.test(ds)&&/prevKilled=false/.test(ds),'fresh End: dragon spawned, never killed');
  const st=await status();P(/dragon ALIVE/.test(st),'status ALIVE with a live never-killed dragon');
  await cons(`rigpos ${a} world 0.5 100 0.5 0 0`,1500,true);A.quit();await sleep(3000);
}
if(PHASE==='afterrestart-alive'){ // after restart, no players, never-killed dragon unloaded
  let ds=await dstate();const st=await status();P(/dragon ALIVE/.test(st),'restart, no players, never-killed: status ALIVE (by design)',ds.slice(0,200));
  const p0=await pre();P(/Test failed/.test(p0),'plain entity check without forceload: does NOT see the unloaded dragon (false negative)',p0.slice(-80));
  const o=await fcheck();P(/Test passed\. Count: 1/i.test(o),'forceload check finds the live dragon (count 1)',o.slice(-120));
  ds=await dstate();const st2=await status();P(/dragon ALIVE/.test(st2),'after forceload remove: still ALIVE');
}
if(PHASE==='kill'){ // a player returns and the dragon is killed through the BossTrial flip
  const A=await join('Rk'+(Date.now()%1000));const a=A.username;await cons('gamemode creative '+a,300,true);
  await cons(`rigpos ${a} ${E} 100.5 49 0.5 90 0`,1500,true);await sleep(5000);await dstate();
  await cons('execute in minecraft:the_end run kill @e[type=minecraft:end_crystal]',800,true);
  const hit=async n=>{for(let i=0;i<n;i++)await cons('execute in minecraft:the_end run damage @e[type=minecraft:ender_dragon,limit=1] 240 minecraft:player_attack by '+a,550,true)};
  await hit(9);await sleep(4500);await hit(20);let ds='';let dead=false;
  for(let i=0;i<20&&!dead;i++){await sleep(2000);ds=await dstate();dead=/live=0/.test(ds)&&/bar=false/.test(ds)&&/prevKilled=true/.test(ds);}
  P(dead,'dragon killed: live=0, bar=false, prevKilled=true',ds.slice(0,200));
  let st=await status();P(/dragon NONE/.test(st),'status NONE right after the kill');
  await cons(`rigpos ${a} world 0.5 100 0.5 0 0`,1500,true);A.quit();await sleep(20000);
  st=await status();P(/dragon NONE/.test(st),'status NONE with the End empty (20 s later)');
}
if(PHASE==='afterrestart-dead'){ // after restart with the dragon dead, no players
  let ds=await dstate();let st=await status();P(/dragon NONE/.test(st),'restart, no players, killed dragon: status NONE (early)',ds.slice(0,200));
  L('  waiting 20 s past the 300-tick empty-world window');await sleep(20000);
  ds=await dstate();st=await status();P(/dragon NONE/.test(st),'restart, no players, killed dragon: status NONE (later)',ds.slice(0,200));
  const o=await fcheck();P(/Test failed/.test(o),'forceload check finds no dragon',o.slice(-120));
  st=await status();P(/dragon NONE/.test(st),'after forceload remove: still NONE');
}
L('SUMMARY '+PHASE+': '+R.filter(x=>x.startsWith('PASS')).length+'/'+R.length+' PASS');process.exit(0)})().catch(e=>{L('ERR '+e.stack);process.exit(1)});
