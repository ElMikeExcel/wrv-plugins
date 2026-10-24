// ExcelMakersMark 0.1.0 multi-world run (mm-rig, TEST RIG ONLY) with real EliteMobs (MagmaGuy) 10.9.10 loaded.
// Worlds: overworld, Nether, End, em_adventurers_guild (loaded by EliteMobs), em_id_test_dungeon (EliteMobs-style name, rig-made).
// Per world: craft + sign, mob kill, armor hits, boss trophy with two players (every weapon that hit gets credit).
// EliteMobs worlds also: real EliteMobs items (no lines added, EM lore not doubled, anvil + grindstone), a real EliteMobs custom boss.
// Overworld also: real enchanting table on a marked sword, Warden and Ender Dragon-in-overworld guard.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,say,P,Q,insp,items,winOpen,put,take,close,has}=C;
const fs=require('fs');const CFG='/workspace/mm-rig/plugins/ExcelMakersMark/config.yml';
const rep=(n,slot,item)=>cons(`minecraft:item replace entity ${n} ${slot} with ${item}`,400,true);
const att=(n,t)=>cons(`mmrig attack ${n} ${t}`,300,true);
const W=[['overworld','minecraft:overworld',1200,100,1200],['nether','minecraft:the_nether',40,100,40],['end','minecraft:the_end',120,100,120],
 ['em_adventurers_guild','minecraft:em_adventurers_guild',300,120,300],['em_id_test_dungeon','minecraft:em_id_test_dungeon',60,-50,60]];
const only=process.env.WORLDS?process.env.WORLDS.split(','):null;
const ex=(w,c,ms=400)=>cons(`execute in ${w[1]} run ${c}`,ms,true);
const alive=async(w,sel)=>/Test passed/.test(await cons(`execute in ${w[1]} if entity ${sel}`,500,true));
const dim=async n=>{const o=await cons(`data get entity ${n} Dimension`,500,true);const m=o.match(/"([^"]+)"/);return m?m[1]:o.slice(-80)};
const LP={SelectedItem:'SelectedItem','armor.head':'equipment.head'};
const loreOf=async(n,slot)=>{const o=await cons(`data get entity ${n} ${LP[slot]||slot}.components."minecraft:lore"`,700,true);const m=o.match(/entity data: (.*)/);return m?m[1]:'NONE:'+o.slice(-120)};
const plat=async w=>{const [_,d,x,y,z]=w;await ex(w,`forceload add ${x-8} ${z-8} ${x+8} ${z+8}`,800);await ex(w,`fill ${x-6} ${y-1} ${z-6} ${x+6} ${y-1} ${z+6} minecraft:stone`,500);
  await ex(w,`fill ${x-6} ${y} ${z-6} ${x+6} ${y+5} ${z+6} minecraft:air`,500);};
const BOTS={};// a real client sends "player loaded" after a dimension change; the bot must too, or vanilla keeps the player invulnerable
const tpw=async(n,w,dx=0,dz=0,yaw=-90)=>{await ex(w,`minecraft:tp ${n} ${w[2]+dx}.5 ${w[3]} ${w[4]+dz}.5 ${yaw} 0`,2000);try{BOTS[n]._client.write('player_loaded',{})}catch(e){L('   (player_loaded: '+e.message+')')}await sleep(400)};
const sumw=(w,type,dx,dz,extra='')=>ex(w,`minecraft:summon ${type} ${w[2]+dx}.5 ${w[3]} ${w[4]+dz}.5 {NoAI:1b,PersistenceRequired:1b${extra}}`,600);
const openBlk=async(b,w,kind,blk,dx,dz)=>{await ex(w,`setblock ${w[2]+dx} ${w[3]} ${w[4]+dz} ${blk}`,300);const p=winOpen(b);
  await cons(`mmrig open ${b.username} ${kind} ${w[2]+dx} ${w[3]} ${w[4]+dz}`,300,true);const win=await p;await sleep(500);return win};
async function craftSword(b,w){await cons('minecraft:clear '+b.username+' iron_sword',300,true);await cons(`minecraft:give ${b.username} iron_ingot 2`,300,true);await cons(`minecraft:give ${b.username} stick 1`,300,true);
  const win=await openBlk(b,w,'craft','crafting_table',2,0);if(!win){L('   (no crafting window)');return null}
  await put(b,win,'iron_ingot',2,true);await put(b,win,'iron_ingot',5,true);await put(b,win,'stick',8);await sleep(400);await take(b,win,0);await close(b);
  const s=items(b,'iron_sword');return s.length?s[0]:null}
const hold=async(b,slot)=>{if(slot==null)return;if(slot<9){b.setQuickBarSlot(slot);await sleep(300);return}
  await cons(`minecraft:item replace entity ${b.username} hotbar.0 from entity ${b.username} inventory.${slot-9}`,300,true);await cons(`minecraft:item replace entity ${b.username} inventory.${slot-9} with air`,300,true);b.setQuickBarSlot(0);await sleep(300)};
(async()=>{
L('=== ExcelMakersMark 0.1.0 multi-world run '+new Date().toString()+' (P='+P+' Q='+Q+')');
let a=await join(P);let b=await join(Q);BOTS[P]=a;BOTS[Q]=b;
for(const c of ['gamerule spawn_mobs false','gamerule advance_time false','difficulty easy','gamemode survival '+P,'gamemode survival '+Q,'minecraft:clear '+P,'minecraft:clear '+Q,
  'minecraft:effect clear '+P,'minecraft:effect clear '+Q,'minecraft:effect give '+P+' minecraft:regeneration 1200 4 true','minecraft:effect give '+Q+' minecraft:regeneration 1200 4 true',
  // rig-only: EliteMobs can turn a summoned test husk into a high-level elite whose 1-damage hit kills a 20 HP bot (R3 run 5:19 PM); give the bots 1000 HP
  'minecraft:attribute '+P+' minecraft:max_health base set 1000','minecraft:attribute '+Q+' minecraft:max_health base set 1000','minecraft:effect give '+P+' minecraft:instant_health 1 20 true','minecraft:effect give '+Q+' minecraft:instant_health 1 20 true'])await cons(c,300,true);
await cons('nick '+P+' ~Crafty',400,true);await say(a,'/mark ok');a.setQuickBarSlot(0);b.setQuickBarSlot(0);
const keep={};
for(const w of W){if(only&&!only.includes(w[0]))continue;const lbl=w[0];
  L('--- '+lbl);await plat(w);await tpw(P,w);await tpw(Q,w,0,2);await sleep(800);
  // rig-only: mineflayer via ViaVersion never finishes a dimension change, so vanilla keeps it invulnerable; a relog in place clears that (real clients send player_loaded)
  if(w[1]!=='minecraft:overworld'){a.quit();b.quit();await sleep(3000);a=await join(P);b=await join(Q);BOTS[P]=a;BOTS[Q]=b;a.setQuickBarSlot(0);b.setQuickBarSlot(0);await sleep(1500);L('   (relogged bots in '+w[1]+')')}
  const d=await dim(P);ok(d===w[1],`${lbl}: player is in ${w[1]} (got ${d})`);
  for(const t of ['husk','zombie','wither','ender_dragon','warden','elder_guardian','item'])await ex(w,`minecraft:kill @e[type=${t},distance=..40,x=${w[2]},y=${w[3]},z=${w[4]}]`,250);
  await sumw(w,'husk',5,5,'');await sleep(1500);const mobsOk=await alive(w,`@e[type=husk,x=${w[2]},y=${w[3]},z=${w[4]},distance=..12]`);
  await ex(w,`minecraft:kill @e[type=husk,distance=..40,x=${w[2]},y=${w[3]},z=${w[4]}]`,300);
  if(!mobsOk)L(`   INFO ${lbl}: a summoned husk was removed at once (EliteMobs keeps this hub world mob-free), so mob kills, mob hits and bosses cannot happen here; checked craft, PvP hit and EliteMobs items instead`);
  // A craft + sign
  await cons('minecraft:clear '+P,300,true);const ss=await craftSword(a,w);await hold(a,ss);let r=await insp(P,'hand');L('   craft: '+r.raw);
  ok(r.name==='Crafty'&&has(r,/^Crafted by Crafty$/)&&r.lore.length===1,`${lbl}: crafted iron sword signed "Crafted by Crafty" (one line)`);
  if(mobsOk){
  // B mob kill
  await sumw(w,'husk',1,0,',Health:1f');await att(P,'husk');await sleep(900);r=await insp(P,'hand');
  // rig-only: EliteMobs sometimes turns the summoned husk into an elite with more health (R4 End 5:38 PM); keep hitting it (up to 8 more hits)
  for(let k=0;k<8&&r.mob===0;k++){await att(P,'husk');await sleep(700);r=await insp(P,'hand')}
  ok(r.mob===1&&has(r,/^Kills: 1$/)&&has(r,/^Crafted by Crafty$/),`${lbl}: mob kill counted: "Kills: 1"`);
  // C armor hits (two hits from a mob, then a screen open to redraw)
  await rep(P,'armor.head','iron_helmet');await sumw(w,'husk',3,3,'');await sleep(800);a._client.write('player_loaded',{});await sleep(500);
  for(let i=0;i<2;i++){await cons(`execute in ${w[1]} positioned ${w[2]} ${w[3]} ${w[4]} run minecraft:damage ${P} 1 minecraft:mob_attack by @e[type=husk,limit=1,sort=nearest]`,700,true)}
  await sleep(5500);let h=await insp(P,'head');L('   helmet: '+h.raw);ok(h.hits===2&&has(h,/^Hits blocked: 2$/),`${lbl}: armor counts 2 hits: "Hits blocked: 2"`);
  await ex(w,`minecraft:kill @e[type=husk,distance=..40,x=${w[2]},y=${w[3]},z=${w[4]}]`,300);
  // D boss with two players: P crafted sword, Q stone axe
  const boss={overworld:'warden',nether:'wither',end:'ender_dragon',em_adventurers_guild:'wither',em_id_test_dungeon:'elder_guardian'}[lbl];
  const label={warden:'Warden',wither:'Wither',ender_dragon:'Ender Dragon',elder_guardian:'Elder Guardian'}[boss];
  const KEY={warden:'WARDEN',wither:'WITHER',ender_dragon:'ENDER_DRAGON',elder_guardian:'ELDER_GUARDIAN'}[boss];
  await rep(Q,'weapon.mainhand','stone_axe');
  const extra=boss==='wither'?',Health:20f,Invul:0':boss==='ender_dragon'?',Health:20f,DragonPhase:10':',Health:24f';
  await sumw(w,boss,2,1,extra);await sleep(900);
  for(let i=0;i<16&&await alive(w,`@e[type=${boss},x=${w[2]},y=${w[3]},z=${w[4]},distance=..30]`);i++){await att(i%2?Q:P,boss);await sleep(1100)}
  await sleep(1500);const dead=!(await alive(w,`@e[type=${boss},x=${w[2]},y=${w[3]},z=${w[4]},distance=..30]`));
  r=await insp(P,'hand');const q=await insp(Q,'hand');L('   P: '+r.raw);L('   Q: '+q.raw);
  ok(dead,`${lbl}: ${label} killed`);
  ok(new RegExp(KEY+'=1').test(r.slain)&&has(r,new RegExp('^Slain: '+label+'$')),`${lbl}: P sword credited "Slain: ${label}"`);
  ok(new RegExp(KEY+'=1').test(q.slain)&&has(q,new RegExp('^Slain: '+label+'$')),`${lbl}: Q axe also credited (it hit the ${label} too)`);
  ok(r.lore.filter(x=>/^Crafted by/.test(x)).length===1&&r.lore.filter(x=>/^Kills/.test(x)).length===1&&r.lore.filter(x=>/^Slain/.test(x)).length===1,`${lbl}: no doubled lines on the sword`);
  keep[lbl]=r;
  } else {
    // hub world: a PvP hit from Q. If the world blocks it, a blocked hit must not count.
    await rep(P,'armor.head','iron_helmet');a._client.write('player_loaded',{});b._client.write('player_loaded',{});await sleep(500);const h0=await cons(`data get entity ${P} Health`,400,true);
    await cons(`minecraft:damage ${P} 1 minecraft:player_attack by ${Q}`,800,true);await sleep(5500);const h=await insp(P,'head');const h1=await cons(`data get entity ${P} Health`,400,true);
    const hurt=(h0.match(/data: ([\d.]+)/)||[])[1]!==(h1.match(/data: ([\d.]+)/)||[])[1];L('   PvP hit applied='+hurt+' helmet: '+h.raw);
    ok(hurt?(h.hits===1&&has(h,/^Hits blocked: 1$/)):(h.hits===0&&!h.lore.length),`${lbl}: PvP hit ${hurt?'counted once':'was blocked by the world and not counted'}`);
    keep[lbl]=r;
  }
  // E EliteMobs items and boss (EliteMobs worlds)
  if(/^em_/.test(lbl)){
    await cons('minecraft:clear '+P,300,true);await cons(`em loot give ${P} ag_adventurer_axe.yml`,1200,true);await cons(`em loot give ${P} ag_adventurer_helmet.yml`,1200,true);await sleep(800);
    let ax=items(a,'iron_axe').concat(items(a,'diamond_axe'),items(a,'netherite_axe'),items(a,'stone_axe'),items(a,'golden_axe'),items(a,'wooden_axe'));
    const inv=a.inventory.items().map(i=>i.name).join(',');L('   EM give -> '+inv);
    const axSlot=a.inventory.items().find(i=>/_axe$/.test(i.name));const hel=a.inventory.items().find(i=>/_helmet$/.test(i.name));
    ok(!!axSlot&&!!hel,`${lbl}: EliteMobs gave the Adventurer's Axe and Helmet`);
    if(axSlot){await hold(a,axSlot.slot>=36?axSlot.slot-36:axSlot.slot);const before=await loreOf(P,'SelectedItem');let e=await insp(P,'hand');L('   EM axe: '+e.raw);L('   EM lore: '+before.slice(0,300));
      ok(e.excluded==='true',`${lbl}: EliteMobs axe is excluded (excluded=true)`);
      await sumw(w,'husk',1,0,',Health:1f');await att(P,'husk');await sleep(1200);const after=await loreOf(P,'SelectedItem');e=await insp(P,'hand');
      ok(e.mob===0&&!/Kills:|Crafted by|Slain:/.test(after),`${lbl}: kill with the EliteMobs axe adds nothing (mob=0, no Kills line)`);
      ok(after===before,`${lbl}: EliteMobs axe lore unchanged by the kill`);
      // anvil rename of the EM item
      await cons(`minecraft:xp add ${P} 30 levels`,300,true);let win=await openBlk(a,w,'anvil','anvil',2,1);
      if(win){const i=C.idx(win,axSlot.name);await C.click(a,i);await C.click(a,0);await sleep(400);a._client.write('name_item',{name:'Renamed'});await sleep(800);
        await take(a,win,2);await close(a);await sleep(500);const ra=a.inventory.items().find(x=>/_axe$/.test(x.name));if(ra)await hold(a,ra.slot>=36?ra.slot-36:ra.slot);
        const al=await loreOf(P,'SelectedItem');L('   EM lore after anvil: '+al.slice(0,300));
        ok(!/Kills:|Crafted by|Slain:|Hits blocked/.test(al),`${lbl}: anvil adds none of our lines to the EliteMobs axe`);
        const cnt=s=>(s.match(/text:/g)||[]).length;ok(cnt(al)<=cnt(before),`${lbl}: EliteMobs lore not doubled after the anvil (${cnt(before)} -> ${cnt(al)} parts)`);}
      else ok(false,`${lbl}: anvil opened`);
      // grindstone of the EM item
      win=await openBlk(a,w,'grind','grindstone[face=floor]',-2,0);
      if(win){const g0=await loreOf(P,'SelectedItem');const i=C.idx(win,a.inventory.items().find(x=>/_axe$/.test(x.name)).name);await C.click(a,i);await C.click(a,0);await sleep(500);
        await take(a,win,2);await close(a);await sleep(500);const ra=a.inventory.items().find(x=>/_axe$/.test(x.name));if(ra)await hold(a,ra.slot>=36?ra.slot-36:ra.slot);
        const gl=await loreOf(P,'SelectedItem');L('   EM lore after grindstone: '+gl.slice(0,300));ok(!/Kills:|Crafted by|Slain:|Hits blocked/.test(gl),`${lbl}: grindstone adds none of our lines to the EliteMobs axe`);
        const cnt=s=>(s.match(/text:/g)||[]).length;ok(cnt(gl)<=cnt(g0),`${lbl}: EliteMobs lore not doubled after the grindstone`);}
      else ok(false,`${lbl}: grindstone opened`);}
    if(hel){await cons(`minecraft:item replace entity ${P} armor.head from entity ${P} ${hel.slot>=36?'hotbar.'+(hel.slot-36):'inventory.'+(hel.slot-9)}`,400,true);
      const hb=await loreOf(P,'armor.head');await sumw(w,'husk',3,3,'');await sleep(600);
      for(let i=0;i<2;i++)await cons(`execute in ${w[1]} positioned ${w[2]} ${w[3]} ${w[4]} run minecraft:damage ${P} 1 minecraft:mob_attack by @e[type=husk,limit=1,sort=nearest]`,700,true);
      await sleep(5500);await openBlk(a,w,'craft','crafting_table',2,0);await close(a);const ha=await loreOf(P,'armor.head');const he=await insp(P,'head');
      ok(he.hits===0&&!/Hits blocked/.test(ha)&&ha===hb,`${lbl}: worn EliteMobs helmet: no hits counted, lore unchanged`);
      await ex(w,`minecraft:kill @e[type=husk,distance=..40,x=${w[2]},y=${w[3]},z=${w[4]}]`,300);}
    // real EliteMobs custom boss, killed with a marked sword: kill counted, no trophy while named trophies are off (default)
    await cons('minecraft:clear '+P,300,true);await cons(`minecraft:item replace entity ${P} hotbar.0 from entity ${Q} weapon.mainhand`,300,true);
    await cons(`minecraft:item modify entity ${P} weapon.mainhand {function:"minecraft:set_enchantments",enchantments:{"minecraft:sharpness":120},add:false}`,400,true);a.setQuickBarSlot(0);
    let s0=await insp(P,'hand');const bossSpawn=await cons(`em spawn bossAt test_boss.yml ${lbl} ${w[2]+1} ${w[3]} ${w[4]} 1`,1500,true);await sleep(1000);
    const bossUp=await alive(w,`@e[type=zombie,x=${w[2]},y=${w[3]},z=${w[4]},distance=..12]`);
    if(!bossUp){L(`   INFO ${lbl}: the EliteMobs test boss did not stay in this world (hub world), EliteMobs boss checks run in em_id_test_dungeon`);}
    else {
    for(let i=0;i<12&&await alive(w,`@e[type=zombie,x=${w[2]},y=${w[3]},z=${w[4]},distance=..12]`);i++){await att(P,'zombie');await sleep(900)}
    await sleep(1500);let s1=await insp(P,'hand');L('   EM boss kill: '+s1.raw);
    ok(!(await alive(w,`@e[type=zombie,x=${w[2]},y=${w[3]},z=${w[4]},distance=..12]`)),`${lbl}: EliteMobs test boss killed`);
    L('   EM boss spawn: '+bossSpawn.replace(/\n/g,' ').slice(0,160));
    ok(s1.mob===s0.mob+1&&!/test/i.test(s1.slain),`${lbl}: EliteMobs boss counts as 1 kill, no trophy while named trophies are off`);
    // named trophy on: match the boss name
    const orig=fs.readFileSync(CFG,'utf8');fs.writeFileSync(CFG,orig.replace(/named:\n    enabled: false\n    list:\n      - \{id: example_boss, match: 'Example Boss Name', label: Example Boss\}/,"named:\n    enabled: true\n    list:\n      - {id: em_test_boss, match: 'Test boss', label: Test Boss}"));
    await cons('mark reload',1200,true);await cons(`em spawn bossAt test_boss.yml ${lbl} ${w[2]+1} ${w[3]} ${w[4]} 1`,1500,true);await sleep(1000);
    for(let i=0;i<12&&await alive(w,`@e[type=zombie,x=${w[2]},y=${w[3]},z=${w[4]},distance=..12]`);i++){await att(P,'zombie');await sleep(900)}
    await sleep(1500);let s2=await insp(P,'hand');L('   EM boss kill (named on): '+s2.raw);fs.writeFileSync(CFG,orig);await cons('mark reload',1200,true);
    ok(/NAME:EM_TEST_BOSS=1/.test(s2.slain)&&/lore=\[[^\]]*Slain: [^\]]*Test Boss/.test(s2.raw),`${lbl}: with named trophies on, the EliteMobs boss is credited "Test Boss"`);
    ok(s2.lore.filter(x=>/^Slain/.test(x)).length===1,`${lbl}: still one Slain line`);
    }
  }
  // F enchanting table (overworld): marked sword keeps its lines exactly
  if(lbl==='overworld'){await cons('minecraft:clear '+P+' iron_sword',300,true);const s2=await craftSword(a,w);await hold(a,s2);
    await sumw(w,'husk',1,0,',Health:1f');await att(P,'husk');await sleep(900);const e0=await insp(P,'hand');const l0=await loreOf(P,'SelectedItem');
    await cons(`minecraft:xp add ${P} 30 levels`,300,true);await cons(`minecraft:give ${P} lapis_lazuli 3`,300,true);
    const win=await openBlk(a,w,'enchant','enchanting_table',-2,2);
    if(win){await put(a,win,'iron_sword',0);await put(a,win,'lapis_lazuli',1);await sleep(1200);a._client.write('enchant_item',{windowId:win.id,enchantment:0});await sleep(1200);
      await take(a,win,0);await close(a);await sleep(500);const sl=items(a,'iron_sword');await hold(a,sl[0]);
      const e1=await insp(P,'hand');const l1=await loreOf(P,'SelectedItem');const en=await cons(`data get entity ${P} SelectedItem.components."minecraft:enchantments"`,500,true);
      L('   enchanted: '+e1.raw);L('   enchantments: '+(en.match(/entity data: (.*)/)||[,en])[1].slice(0,160));
      ok(/sharpness|smite|bane|knockback|fire_aspect|looting|unbreaking|sweeping/.test(en),'overworld: enchanting table enchanted the marked sword');
      ok(l1===l0&&e1.name==='Crafty'&&e1.mob===e0.mob,'overworld: enchanting keeps Crafted by and Kills exactly, no extra lines');}
    else ok(false,'overworld: enchanting table opened');}
}
// for the restart check
const snap={};for(const k in keep)snap[k]={by:keep[k].by,name:keep[k].name,mob:keep[k].mob,slain:keep[k].slain,lore:keep[k].lore};
fs.writeFileSync(process.env.SNAP||'/workspace/makersmark/tests/worlds-snap.json',JSON.stringify(snap,null,1));
const errs=fs.readFileSync('/workspace/mm-rig/boot.log','utf8').split('\n').filter(x=>/(ERROR|WARN|Exception)/.test(x)&&/MakersMark|makersmark/.test(x));
ok(errs.length===0,'no ExcelMakersMark errors or warnings in the log'+(errs.length?': '+errs.slice(0,3).join(' / '):''));
H.summary();a.quit();b.quit();await sleep(1500);process.exit(0)})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
