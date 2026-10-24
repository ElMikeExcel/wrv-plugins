// ExcelMakersMark 0.1.0 rig run on /workspace/mm-rig (port 25975). TEST RIG ONLY.
const H=require('./h.js');const {L,cons,sleep,join,ok,say,click}=H;
const OX=1000,OZ=1000,Y=-60, P=process.env.MMP||'MMaker', Q=process.env.MMQ||'MHelper';
const give=(n,i,c=1)=>cons(`minecraft:give ${n} ${i} ${c}`,500,true);
const insp=async(n,slot)=>{const o=await cons(`mark inspect ${n} ${slot}`,600,true);const m=o.match(/\[MM-INSPECT\][^\n]*/);const s=m?m[0]:o;
  const g=k=>{const r=s.match(new RegExp(' '+k+'=(\\S*)'));return r?r[1]:null};const lr=s.match(/lore=\[(.*)\] dmg=/);
  return {raw:s,by:g('by'),name:g('name'),date:g('date'),mob:+g('mob'),pvp:+g('pvp'),hits:+g('hits'),slain:g('slain'),excluded:g('excluded'),dmg:+g('dmg'),
    lore:lr?(lr[1]?lr[1].split(', '):[]):[],empty:/ empty$/.test(s)}};
const b2=s=>s>=36&&s<=44?s-36:s;
const items=(b,name)=>b.inventory.items().filter(i=>i.name===name&&i.slot>=9&&i.slot<=44).map(i=>b2(i.slot));
const winOpen=b=>new Promise(r=>{const t=setTimeout(()=>r(null),4000);b.once('windowOpen',w=>{clearTimeout(t);r(w)})});
// Bot block clicks do not reach Paper 26.3 through ViaVersion on the rigs, so the rig-only MMRigAid opens the real block menu server-side.
const KIND={'2,0':'craft','2,1':'anvil','2,-1':'smith','-2,0':'grind'};
const openAt=async(b,x,y,z)=>{const k=KIND[x+','+z];if(k==='anvil')await cons(`setblock ${x+OX} ${y} ${z+OZ} anvil`,250,true);/* rig-only: vanilla chips the anvil block with use; a CHIPPED_ANVIL opens a CRAFTING view (R3/R4 5:11, 5:29 PM) */const p=winOpen(b);await cons(`mmrig open ${b.username} ${k} ${x+OX} ${y} ${z+OZ}`,300,true);const w=await p;await sleep(500);
  // prismarine-windows still has the old 3-slot smithing table; 1.20+ has template 0, base 1, addition 2, result 3.
  if(w&&w.type==='minecraft:smithing'){w.craftingResultSlot=3;w.inventoryStart=4;w.inventoryEnd=40;w.hotbarStart=31}return w};
const IS=w=>w.slots.length-36;
const hold=async(b,name)=>{const it=b.inventory.items().find(i=>i.name===name&&i.slot>=9&&i.slot<=44);if(!it){L('   (hold: no '+name+')');return}
  if(it.slot>=36){b.setQuickBarSlot(it.slot-36);await sleep(300);return}await b.clickWindow(it.slot,0,2).catch(e=>L('swap err '+e.message));await sleep(400);b.setQuickBarSlot(0);await sleep(300)};
const idx=(w,name,skip=[])=>w.slots.findIndex((s,i)=>i>=IS(w)&&s&&s.name===name&&!skip.includes(i));
const put=async(b,w,name,dest,one=false,skip=[])=>{const i=idx(w,name,skip);if(i<0){L('   (no '+name+' in window)');return false}
  await click(b,i);if(one){await click(b,dest,1);await click(b,i)}else await click(b,dest);return true};
const take=async(b,w,slot,shift=false)=>{if(shift){await click(b,slot,0,1,800);return}await click(b,slot,0,0,500);let k=null;for(let i=w.slots.length-1;i>=IS(w);i--)if(!w.slots[i]){k=i;break}if(k==null){L('   (no empty slot)');return}await click(b,k,0,0,500)};
const close=async b=>{if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(500)}};
const fresh=async b=>{await close(b);await cons('minecraft:clear '+b.username,400,true);await cons('minecraft:kill @e[type=item]',300,true);await cons('minecraft:kill @e[type=arrow]',200,true);await cons('minecraft:kill @e[type=trident]',200,true)};
const mlog=()=>require('fs').readFileSync('/workspace/mm-rig/boot.log','utf8');
const has=(x,re)=>x.lore.some(l=>re.test(l));
const tp=(n,x,z,yaw=0,pitch=0)=>cons(`minecraft:tp ${n} ${x+OX} ${Y} ${z+OZ} ${yaw} ${pitch}`,700,true);
const sum=async(n,type,x,z,extra='')=>cons(`minecraft:summon ${type} ${x+OX} ${Y} ${z+OZ} {NoAI:1b,PersistenceRequired:1b${extra}}`,600,true);
(async()=>{
L('=== ExcelMakersMark 0.1.0 rig run '+new Date().toString());
for(const c of ['gamerule advance_time false','gamerule spawn_mobs false','time set day','weather clear','difficulty normal','minecraft:kill @e[type=!player]',
  ])await cons(c,300,true);
const a=await join(P);await cons('minecraft:tp '+P+' 1000.5 -60 1000.5',1500,true);a.setQuickBarSlot(0);await sleep(300);
for(const c of ['setblock 1002 -60 1000 crafting_table','setblock 1002 -60 1001 anvil','setblock 1002 -60 999 smithing_table','setblock 998 -60 1000 grindstone[face=floor]'])await cons(c,400);
await cons('gamemode survival '+P,300,true);await tp(P,0.5,0.5);await cons('nick '+P+' ~Crafty',600);await cons('minecraft:xp add '+P+' 100 levels',300,true);
await cons('mark tipreset '+P,500);a.msgs=[];
// ---------- T1 normal craft ----------
L('--- T1 normal craft (diamond sword at a crafting table)');
await fresh(a);await give(P,'diamond',2);await give(P,'stick',1);
let w=await openAt(a,2,Y,0);ok(!!w,'crafting table opens');
await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(400);
L('   result preview: '+(w.slots[0]&&w.slots[0].name));
await take(a,w,0);await close(a);
let s=items(a,'diamond_sword');let r=s.length?await insp(P,s[0]):{lore:[]};L('   '+r.raw);
ok(r.by&&r.name==='Crafty'&&has(r,/^Crafted by Crafty$/),'sword signed "Crafted by Crafty" (Essentials nick ~Crafty, ~ stripped), uuid stored');
ok(/^\d{4}-\d\d-\d\d$/.test(r.date||''),'craft date stored: '+r.date);
const tipSeen=a.msgs.some(m=>/\[Tip\].*Crafted by/.test(m))&&a.msgs.some(m=>/\[OK\]/.test(m));ok(tipSeen,'first-use tip shown with [OK]: '+a.msgs.filter(m=>/Tip|OK/.test(m)).slice(0,4).join(' | '));
// ---------- T2 shift craft ----------
L('--- T2 shift-click craft (2 iron swords in one shift-click)');
await fresh(a);await give(P,'iron_ingot',4);await give(P,'stick',2);
w=await openAt(a,2,Y,0);await put(a,w,'iron_ingot',2,true);await put(a,w,'iron_ingot',5,true);
await put(a,w,'iron_ingot',2,true);await put(a,w,'iron_ingot',5,true);await put(a,w,'stick',8);await sleep(300);
L('   grid: '+[2,5,8].map(i=>i+'='+(w.slots[i]&&w.slots[i].name+'x'+w.slots[i].count)).join(' '));
await take(a,w,0,true);await close(a);
s=items(a,'iron_sword');const rs=[];for(const x of s)rs.push(await insp(P,x));rs.forEach(x=>L('   '+x.raw));
ok(rs.length===2&&rs.every(x=>x.name==='Crafty'&&has(x,/^Crafted by Crafty$/)),'shift-click made '+rs.length+' swords, all signed');
// ---------- T3 stackables ----------
L('--- T3 stackable items stay unsigned (torches, bucket)');
await fresh(a);await give(P,'coal',1);await give(P,'stick',1);await give(P,'iron_ingot',3);
w=await openAt(a,2,Y,0);await put(a,w,'coal',2);await put(a,w,'stick',5);await sleep(300);await take(a,w,0,true);
await put(a,w,'iron_ingot',4,true);await put(a,w,'iron_ingot',6,true);await put(a,w,'iron_ingot',8,true);await sleep(300);await take(a,w,0);await close(a);
let t=await insp(P,items(a,'torch')[0]);let bk=await insp(P,items(a,'bucket')[0]);L('   '+t.raw);L('   '+bk.raw);
ok(/TORCH x4/.test(t.raw)&&t.by==='null'&&t.lore.length===0,'torches unsigned, no lore');ok(/BUCKET/.test(bk.raw)&&bk.by==='null'&&bk.lore.length===0,'bucket unsigned, no lore');
// ---------- T4 anvil rename + lore attempts ----------
L('--- T4 anvil: rename keeps the mark; faked lines are removed');
await fresh(a);await give(P,'iron_ingot',2);await give(P,'stick',1);
w=await openAt(a,2,Y,0);await put(a,w,'iron_ingot',2,true);await put(a,w,'iron_ingot',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
await hold(a,'iron_sword');
// (a) add fake lines to a real marked sword with /item modify, then rename in the anvil
await cons(`minecraft:item modify entity ${P} weapon.mainhand {type:"minecraft:set_lore",lore:["Soulbound","Kills: 999","Crafted by Faker"],mode:"append"}`,600);
r=await insp(P,'hand');L('   before anvil: '+r.raw);
w=await openAt(a,2,Y,1);ok(!!w,'anvil opens');await put(a,w,'iron_sword',0);a._client.write('name_item',{name:'Shiny'});await sleep(600);
L('   anvil result: '+(w.slots[2]&&w.slots[2].name));await take(a,w,2);await close(a);
r=await insp(P,items(a,'iron_sword')[0]);L('   after anvil: '+r.raw);
ok(has(r,/^Crafted by Crafty$/)&&!has(r,/Faker|999/)&&r.lore.length===2,'renamed sword: real mark kept, fake "Kills: 999" and "Crafted by Faker" removed');
ok(r.lore[0]==='Soulbound'&&r.lore[1]==='Crafted by Crafty','another plugin\'s lore line ("Soulbound") is kept, in place, above ours');
// (b) an unmarked sword with a hand-made "Crafted by" line
await fresh(a);await give(P,'iron_sword[lore=[{text:"Crafted by Mike",color:"gray",italic:false}]]');
w=await openAt(a,2,Y,1);await put(a,w,'iron_sword',0);a._client.write('name_item',{name:'Fake'});await sleep(600);await take(a,w,2);await close(a);
r=await insp(P,items(a,'iron_sword')[0]);L('   '+r.raw);ok(r.by==='null'&&r.lore.length===0,'fake "Crafted by Mike" line on an unsigned sword is stripped by the anvil');
// (c) lore wiped by /item modify, then anvil brings it back from the tag
await fresh(a);await give(P,'diamond',2);await give(P,'stick',1);
w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
await hold(a,'diamond_sword');
await cons(`minecraft:item modify entity ${P} weapon.mainhand {type:"minecraft:set_lore",lore:[],mode:"replace_all"}`,600);
r=await insp(P,'hand');L('   wiped: '+r.raw);
w=await openAt(a,2,Y,1);await put(a,w,'diamond_sword',0);a._client.write('name_item',{name:'Back'});await sleep(600);await take(a,w,2);await close(a);
r=await insp(P,items(a,'diamond_sword')[0]);L('   '+r.raw);ok(has(r,/^Crafted by Crafty$/),'stripped lore is rebuilt from the tag at the anvil');
// ---------- T5 netherite upgrade ----------
L('--- T5 smithing: netherite upgrade keeps the original crafter; unsigned base gets signed');
await give(P,'netherite_upgrade_smithing_template',2);await give(P,'netherite_ingot',2);await give(P,'diamond_pickaxe');
await cons('nick '+P+' ~Upgrader',500,true);
w=await openAt(a,2,Y,-1);ok(!!w,'smithing table opens');
await put(a,w,'netherite_upgrade_smithing_template',0,true);await put(a,w,'diamond_sword',1);await put(a,w,'netherite_ingot',2,true);await sleep(500);
L('   result: '+(w.slots[3]&&w.slots[3].name));await take(a,w,3);
await put(a,w,'netherite_upgrade_smithing_template',0,true);await put(a,w,'diamond_pickaxe',1);await put(a,w,'netherite_ingot',2,true);await sleep(500);await take(a,w,3);await close(a);
r=await insp(P,items(a,'netherite_sword')[0]);L('   '+r.raw);ok(r.name==='Crafty'&&has(r,/^Crafted by Crafty$/)&&r.lore.length===1,'netherite sword keeps "Crafted by Crafty" (crafter at craft time, not the upgrader)');
let pk=await insp(P,items(a,'netherite_pickaxe')[0]);L('   '+pk.raw);ok(pk.name==='Upgrader'&&has(pk,/^Crafted by Upgrader$/),'unsigned diamond pickaxe upgraded: signed by the player who upgraded it');
await cons('nick '+P+' ~Crafty',500,true);
// ---------- T6 grindstone ----------
L('--- T6 grindstone: enchant removed, mark rebuilt (also after a lore wipe)');
await hold(a,'netherite_sword');
await cons(`minecraft:enchant ${P} sharpness 3`,400);await cons(`minecraft:item modify entity ${P} weapon.mainhand {type:"minecraft:set_lore",lore:["Crafted by Faker"],mode:"replace_all"}`,500);
r=await insp(P,'hand');L('   before: '+r.raw);
w=await openAt(a,-2,Y,0);ok(!!w,'grindstone opens');await put(a,w,'netherite_sword',0);await sleep(500);L('   result: '+(w.slots[2]&&w.slots[2].name));await take(a,w,2);await close(a);
r=await insp(P,items(a,'netherite_sword')[0]);L('   after: '+r.raw);ok(has(r,/^Crafted by Crafty$/)&&!has(r,/Faker/),'grindstone result: real mark back, fake line gone');
// ---------- T7 grid repair ----------
L('--- T7 grid repair (2x2): first marked input wins, counters keep the higher value');
await fresh(a);await give(P,'stone_sword[damage=100,custom_data={PublicBukkitValues:{"excelmakersmark:kills_mob":7}}]');
await give(P,'stick',1);await give(P,'cobblestone',2);
w=await openAt(a,2,Y,0);await put(a,w,'cobblestone',2,true);await put(a,w,'cobblestone',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
let ss=items(a,'stone_sword');let marked=null,unm=null;for(const x of ss){const q=await insp(P,x);if(q.by!=='null')marked=x;else unm=x}
a.setQuickBarSlot(0);await cons(`minecraft:item modify entity ${P} container.${marked} {type:"minecraft:set_damage",damage:0.5,add:false}`,400,true);
await cons(`minecraft:item modify entity ${P} container.${marked} {type:"minecraft:set_custom_data",tag:{PublicBukkitValues:{"excelmakersmark:kills_mob":3}}}`,400,true);
L('   unmarked: '+(await insp(P,unm)).raw);L('   marked:   '+(await insp(P,marked)).raw);
const iw=a.inventory;await click(a,unm>=9?unm:unm+36);await click(a,1);await click(a,marked>=9?marked:marked+36);await click(a,2);await sleep(500);
L('   repair preview: '+(iw.slots[0]&&iw.slots[0].name));await click(a,0,0,1,800);
ss=items(a,'stone_sword');r=ss.length===1?await insp(P,ss[0]):{raw:'count='+ss.length,lore:[]};L('   '+r.raw);
ok(ss.length===1&&r.name==='Crafty'&&r.mob===7&&has(r,/^Crafted by Crafty$/)&&has(r,/^Kills: 7$/),'grid repair: mark copied from the marked input, kills = max(7,3) = 7');
// ---------- T8 Magic wand craft ----------
L('--- T8 Magic (elmakers) magicsword recipe stays unsigned');
await fresh(a);await give(P,'amethyst_shard',2);await give(P,'stick',1);
w=await openAt(a,2,Y,0);await put(a,w,'amethyst_shard',2,true);await put(a,w,'amethyst_shard',5,true);await put(a,w,'stick',8);await sleep(500);
const mres=w.slots[0];L('   result: '+(mres&&mres.name));
if(mres){await take(a,w,0);await close(a);const inv=a.inventory.items().filter(i=>i.slot>=9);r=await insp(P,b2(inv[0].slot));L('   '+r.raw);
  ok(r.excluded==='true'&&r.by==='null'&&!has(r,/Crafted by/),'Magic wand craft is not signed (excluded=true)');}
else{await close(a);await cons('mgive '+P+' wand:mmtestsword',800);const inv=a.inventory.items().filter(i=>i.slot>=9);r=inv.length?await insp(P,b2(inv[0].slot)):{raw:'none'};L('   (no recipe output; mgive) '+r.raw);ok(false,'Magic recipe produced nothing; mgive fallback excluded='+r.excluded);}
// Magic wand item kills nothing
await fresh(a);await cons('mgive '+P+' wand:beginner',800);let wi=a.inventory.items().filter(i=>i.slot>=9);let wr=wi.length?await insp(P,b2(wi[0].slot)):{raw:'none'};L('   wand: '+wr.raw);ok(wr.excluded==='true','Magic wand item is excluded');
// ---------- T9 EliteMobs-tagged item ----------
L('--- T9 EliteMobs (MagmaGuy) tagged item: no mark, no counters');
await fresh(a);await give(P,'diamond_sword[custom_data={PublicBukkitValues:{"elitemobs:custom_item_id":"rig_test"}}]');a.setQuickBarSlot(0);
await sum(P,'husk',1.5,0.5,',Health:1f');await sleep(500);L('   '+await cons('mmrig attack '+P+' husk',800,true));
w=await openAt(a,2,Y,1);await put(a,w,'diamond_sword',0);a._client.write('name_item',{name:'Elite'});await sleep(600);await take(a,w,2);await close(a);
r=await insp(P,items(a,'diamond_sword')[0]);L('   '+r.raw);ok(r.excluded==='true'&&r.mob===0&&r.by==='null'&&r.lore.length===0,'EliteMobs item: no kill counted, nothing added at the anvil');
// ---------- T10 /mark off ----------
L('--- T10 /mark off: crafts not signed; /mark on again');
await fresh(a);let m=await say(a,'/mark off');ok(/OFF/.test(m),'/mark off reply: '+m);await sleep(2600);
ok(/mark: false/.test(require('fs').readFileSync('/workspace/mm-rig/plugins/ExcelMakersMark/players.yml','utf8')),'/mark off saved in players.yml (mark: false)');
await give(P,'diamond',2);await give(P,'stick',1);w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
r=await insp(P,items(a,'diamond_sword')[0]);L('   '+r.raw);ok(r.by==='null'&&r.lore.length===0,'with /mark off the sword is not signed');
m=await say(a,'/mark on');ok(/ON/.test(m),'/mark on reply: '+m);await sleep(2600);
const yml=require('fs').readFileSync('/workspace/mm-rig/plugins/ExcelMakersMark/players.yml','utf8');ok(/mark: true/.test(yml),'choice saved in players.yml');
// ---------- tip OK ----------
m=await say(a,'/mark ok');ok(/will not show again/.test(m),'/mark ok hides the tip: '+m);
await fresh(a);await give(P,'diamond',2);await give(P,'stick',1);a.msgs=[];w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);await sleep(1500);
ok(!a.msgs.some(m=>/\[Tip\]/.test(m)),'after OK, the next craft shows no tip');
H.summary();await close(a);a.quit();await sleep(1000);process.exit(0);
})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
