// Bedrock (Geyser + Floodgate) multi-world check on mm-rig: kills, boss trophy, armor hits, lore reaches the Bedrock client, EliteMobs item untouched. TEST RIG ONLY.
const bp=require('/workspace/tips-tests/node_modules/bedrock-protocol');const C=require('./common.js');const {L,cons,sleep,insp}=C;
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('  PASS '+w)}else{fail++;L('  FAIL '+w)}};
const U=process.env.MMB||'MkBedW1';const N='.'+U;
const W=[['overworld','minecraft:overworld',1210,100,1210],['nether','minecraft:the_nether',50,100,50],['end','minecraft:the_end',130,100,130],['em_id_test_dungeon','minecraft:em_id_test_dungeon',70,-50,70],['em_adventurers_guild','minecraft:em_adventurers_guild',310,120,310]];
const c=bp.createClient({host:'127.0.0.1',port:19975,username:U,offline:true,version:'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
c.on('error',e=>{L('ERR',e.message);L(`RESULT pass=${pass} fail=${fail} aborted=1`);process.exit(4)});c.on('kick',p=>L('KICK',JSON.stringify(p)));
let seen='';const js=o=>{try{return JSON.stringify(o,(k,v)=>typeof v==='bigint'?v.toString():v)}catch(e){return ''}};
for(const ev of ['inventory_slot','inventory_content','mob_equipment','mob_armor_equipment'])c.on(ev,p=>{seen+=js(p).replace(/§./g,'')+'\n';if(seen.length>4e6)seen=seen.slice(-2e6)});
let dimAcks=0;c.on('change_dimension',p=>{dimAcks++;setTimeout(()=>{try{c.queue('player_action',{runtime_entity_id:c.entityId||0n,action:'dimension_change_ack',position:{x:0,y:0,z:0},result_position:{x:0,y:0,z:0},face:0})}catch(e){L('   (ack: '+e.message+')')}},1500)});
const ex=(w,s,ms=500)=>cons(`execute in ${w[1]} run ${s}`,ms,true);
setTimeout(()=>{L('TIMEOUT');L(`RESULT pass=${pass} fail=${fail}`);process.exit(3)},900000);
c.once('spawn',async()=>{
 L('=== Bedrock multi-world check ('+N+') '+new Date().toString());await sleep(4000);
 for(const g of ['gamemode survival '+N,'minecraft:effect give '+N+' minecraft:regeneration 1200 4 true','gamerule spawn_mobs false'])await cons(g,300,true);
 const only=process.env.ONLY?process.env.ONLY.split(','):null;
 for(const w of W){if(only&&!only.includes(w[0]))continue;const lbl=w[0];L('--- '+lbl);const [x,y,z]=[w[2],w[3],w[4]];
  await ex(w,`minecraft:fill ${x-5} ${y-1} ${z-5} ${x+5} ${y-1} ${z+5} minecraft:stone`,400);await ex(w,`minecraft:fill ${x-5} ${y} ${z-5} ${x+5} ${y+4} ${z+5} minecraft:air`,400);
  await ex(w,`minecraft:tp ${N} ${x}.5 ${y} ${z}.5 -90 0`,6000);
  let o=await cons(`execute as ${N} run data get entity @s Dimension`,600,true);
  for(let k=0;k<4&&!o.includes(w[1]);k++){L('   (dimension change still pending, re-tp '+(k+1)+')');await ex(w,`minecraft:tp ${N} ${x}.5 ${y} ${z}.5 -90 0`,8000);o=await cons(`execute as ${N} run data get entity @s Dimension`,600,true)}
  ok(o.includes(w[1]),`${lbl}: Bedrock player is in ${w[1]}`);
  await ex(w,`minecraft:kill @e[type=!player,type=!item_frame,distance=..40,x=${x},y=${y},z=${z}]`,400);
  if(lbl==='em_adventurers_guild'){
   await cons(`em loot give ${N} ag_adventurer_axe.yml`,1500,true);await cons(`minecraft:item replace entity ${N} weapon.mainhand from entity ${N} hotbar.0`,400,true);
   const r=await insp(N,'hand');L('   '+r.raw);ok(r.excluded===true||/excluded=true/.test(r.raw),`${lbl}: EliteMobs axe in a Bedrock hand is left alone (excluded)`);
   ok(!/Crafted by|Kills:|Slain:/.test(r.raw),`${lbl}: no maker's mark lines added to the EliteMobs axe`);
   await sleep(1500);ok(/Adventurer|adventurer/.test(seen),`${lbl}: EliteMobs axe lore reaches the Bedrock client`);
   await cons(`minecraft:clear ${N}`,300,true);continue}
  seen='';await cons(`minecraft:item replace entity ${N} weapon.mainhand with minecraft:iron_sword`,500,true);await cons(`minecraft:item replace entity ${N} armor.head with minecraft:iron_helmet`,500,true);await sleep(1500);
  // mob kill
  await ex(w,`minecraft:summon zombie ${x+1}.5 ${y} ${z}.5 {NoAI:1b,Health:1f,PersistenceRequired:1b}`,1500);await cons(`mmrig attack ${N} zombie`,1500,true);
  // boss kill (wither, low health)
  await ex(w,`minecraft:summon wither ${x+2}.5 ${y} ${z}.5 {NoAI:1b,Health:2f,Invul:0}`,1500);await cons(`mmrig attack ${N} wither`,2500,true);
  // armor hit
  await ex(w,`minecraft:summon husk ${x+3}.5 ${y} ${z+3}.5 {NoAI:1b,PersistenceRequired:1b,Tags:["mmbh"]}`,800);
  await cons(`minecraft:damage ${N} 1 minecraft:mob_attack by @e[tag=mmbh,limit=1]`,800,true);
  await sleep(5500);const r=await insp(N,'hand');L('   sword: '+r.raw);const h=await insp(N,'head');L('   helmet: '+h.raw);
  ok(/mob=1\b/.test(r.raw)||/mob=2\b/.test(r.raw),`${lbl}: Bedrock kills counted on the sword`);
  ok(/WITHER=1/.test(r.raw)&&/Slain: Wither/.test(r.raw),`${lbl}: Bedrock boss kill gives "Slain: Wither"`);
  {const m=h.raw.match(/hits=(\d+)/);ok(m&&+m[1]>=1&&/Hits blocked: [1-9]/.test(h.raw),`${lbl}: Bedrock armor counts the hit (lore redraw is throttled by design)`)}
  await sleep(1500);{const km=r.raw.match(/Kills: (\d+)/);ok(km&&seen.includes('Kills: '+km[1])&&/Slain: Wither/.test(seen),`${lbl}: the Bedrock client receives the lore lines`)}
  ok((r.raw.match(/Slain:/g)||[]).length===1,`${lbl}: one Slain line only`);
  for(const t of ['zombie','wither','husk','item','experience_orb'])await ex(w,`minecraft:kill @e[type=${t},distance=..20,x=${x},y=${y},z=${z}]`,250);
  await cons(`minecraft:clear ${N}`,300,true)}
 L('   dimension changes seen by client: '+dimAcks);
 L(`RESULT pass=${pass} fail=${fail}`);setTimeout(()=>process.exit(fail?1:0),1500);try{c.close()}catch(e){}});
