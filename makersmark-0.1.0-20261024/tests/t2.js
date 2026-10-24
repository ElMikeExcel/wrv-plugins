// ExcelMakersMark 0.1.0 stats + trophies rig run (mm-rig, TEST RIG ONLY). Bot attacks and item use go through
// the rig-only MMRigAid (server-side Player.attack and the use_item path), since bot inputs don't reach Paper 26.3 via Via.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,say,OX,OZ,Y,P,Q,insp,items,openAt,hold,put,take,close,fresh,has,tp,sum}=C;
const rep=(n,slot,item)=>cons(`minecraft:item replace entity ${n} ${slot} with ${item}`,400,true);
const alive=async sel=>/Test passed/.test(await cons('execute if entity '+sel,500,true));
const att=(n,t)=>cons(`mmrig attack ${n} ${t}`,300,true);
const fs=require('fs');const logSince=()=>{const t=fs.readFileSync('/workspace/mm-rig/boot.log','utf8');const o=t.slice(logSince.m||0);logSince.m=t.length;return o};
(async()=>{
L('=== ExcelMakersMark 0.1.0 stats/trophies rig run '+new Date().toString());
const a=await join(P);const b=await join(Q);
for(const c of ['gamerule spawn_mobs false','gamerule advance_time false','minecraft:kill @e[type=!player]','gamemode survival '+P,'gamemode survival '+Q,'minecraft:clear '+P,'minecraft:clear '+Q,'mark tipreset '+P,
  'minecraft:effect give '+P+' minecraft:regeneration 600 4 true','minecraft:effect give '+Q+' minecraft:regeneration 600 4 true'])await cons(c,300,true);
await tp(P,0.5,0.5,-90,0);await tp(Q,0.5,2.5,-90,0);a.setQuickBarSlot(0);b.setQuickBarSlot(0);await sleep(500);
await cons('nick '+P+' ~Crafty',400,true);
// ---------- S1 melee kills ----------
L('--- S1 melee kills (diamond sword, 2 husks)');
await rep(P,'weapon.mainhand','diamond_sword');
for(let i=0;i<2;i++){await sum(P,'husk',1.5,0.5,',Health:1f');await att(P,'husk');await sleep(900)}
let r=await insp(P,'hand');L('   '+r.raw);ok(r.mob===2&&has(r,/^Kills: 2$/),'sword: 2 melee kills, lore "Kills: 2"');
// ---------- S3 bow ----------
L('--- S3 bow kill (arrow credits the bow that fired it)');
await rep(P,'weapon.mainhand','bow');await rep(P,'hotbar.8','arrow 16');await sum(P,'husk',4.5,0.5,',Health:1f');await tp(P,0.5,0.5,-90,-1);
await cons('mmrig use '+P+' 25',2500,true);await sleep(500);r=await insp(P,'hand');L('   '+r.raw);
await sleep(1500);ok(!(await alive('@e[type=husk,nbt={DeathTime:0s}]'))&&r.mob===1&&has(r,/^Kills: 1$/),'bow: 1 kill, lore "Kills: 1"');
// ---------- S4 crossbow ----------
L('--- S4 crossbow kill');
await rep(P,'weapon.mainhand','crossbow');await sum(P,'husk',4.5,0.5,',Health:1f');
await cons('mmrig use '+P+' 30',2200,true);await cons('mmrig use '+P,1500,true);r=await insp(P,'hand');L('   '+r.raw);ok(r.mob===1,'crossbow: 1 kill');
// ---------- S5 trident ----------
L('--- S5 trident throw kill (counter rides on the thrown trident, still there after pickup)');
await rep(P,'hotbar.8','air');await rep(P,'weapon.mainhand','trident');await sum(P,'husk',4.5,0.5,',Health:1f');
await cons('mmrig use '+P+' 15',1500,true);const td=await cons('minecraft:data get entity @e[type=trident,limit=1] item.components',600,true);L('   flying trident: '+(td.match(/entity data: (.*)/)||[,td])[1].slice(0,240));
ok(/Kills: 1/.test(td),'thrown trident item already shows "Kills: 1"');
await cons('minecraft:kill @e[type=item]',300,true);await cons('minecraft:tp '+P+' @e[type=trident,limit=1]',2500,true);r=await insp(P,'hand');if(r.empty||!/TRIDENT/.test(r.raw)){const s=items(a,'trident');if(s.length)r=await insp(P,s[0])}L('   picked up: '+r.raw);
ok(r.mob===1&&has(r,/^Kills: 1$/),'picked-up trident keeps 1 kill');await tp(P,0.5,0.5,-90,0);
// ---------- S6 armor hits + throttle ----------
L('--- S6 armor hits and redraw throttle (redraw-seconds 5)');
await cons('minecraft:clear '+P,300,true);await rep(P,'armor.head','iron_helmet');await rep(P,'armor.chest','iron_chestplate');await rep(P,'weapon.mainhand','iron_sword');
await sum(P,'husk',3.5,3.5,'');await sleep(5200);
logSince();
// hits 0.6 s apart: vanilla ignores damage during the 0.5 s hurt cooldown, so faster hits are not real hits
for(let i=0;i<4;i++){await cons(`minecraft:damage ${P} 1 minecraft:mob_attack by @e[type=husk,limit=1]`,600,true)}
let h1=await insp(P,'head'),c1=await insp(P,'chest');L('   right after 4 hits: '+h1.raw);L('   '+c1.raw);
ok(h1.hits===4&&c1.hits===4,'both worn pieces count 4 hits in the tag right away');
ok(has(h1,/^Hits blocked: 1$/)&&!has(h1,/Hits blocked: 4/),'lore not redrawn on every hit (shows 1 until the wait is over)');
let s0=await insp(P,'hand');ok(s0.hits===0&&!s0.lore.length,'held sword is not counted as armor');
await sleep(5600);h1=await insp(P,'head');L('   after 5.6 s: '+h1.raw);ok(has(h1,/^Hits blocked: 4$/),'lore catches up to "Hits blocked: 4" after the wait');
for(let i=0;i<2;i++)await cons(`minecraft:damage ${P} 1 minecraft:mob_attack by @e[type=husk,limit=1]`,600,true);
await cons(`minecraft:damage ${P} 1 minecraft:arrow`,600,true);
h1=await insp(P,'head');L('   +2 hits (+1 non-entity arrow cause): '+h1.raw);
await openAt(a,2,Y,0);await sleep(300);let h2=await insp(P,'head');await close(a);L('   after opening a screen: '+h2.raw);
ok(h1.hits===6,'non-entity damage (no attacker) is not counted; 6 hits');ok(has(h2,/^Hits blocked: 6$/),'opening a screen redraws at once ("Hits blocked: 6")');
await cons('minecraft:kill @e[type=husk]',300,true);
// ---------- S7 bosses ----------
L('--- S7 boss trophies: Wither (sword + Helper axe), Elder Guardian (bow + Helper sword)');
await cons('minecraft:clear '+P,300,true);await cons('minecraft:clear '+Q,300,true);
await rep(P,'weapon.mainhand','diamond_sword');await rep(Q,'weapon.mainhand','iron_axe');
await sum(P,'wither',2.5,1.5,',Health:20f,Invul:0');await sleep(800);logSince();
for(let i=0;i<10&&await alive('@e[type=wither]');i++){await att(i%2?P:Q,'wither');await sleep(1150)}
let lg=logSince();L('   '+lg.split('\n').filter(x=>/\[stats\]/.test(x)).map(x=>x.replace(/.*\[stats\] /,'')).join(' | '));
let ws=await insp(P,'hand'),wa=await insp(Q,'hand');L('   '+ws.raw);L('   '+wa.raw);
ok(/WITHER=1/.test(ws.slain)&&has(ws,/^Slain: Wither$/),'MMaker sword credited: "Slain: Wither"');
ok(/WITHER=1/.test(wa.slain)&&has(wa,/^Slain: Wither$/),'Helper axe credited: "Slain: Wither" (both weapons that hurt it)');
// second wither, sword only -> x2
await sum(P,'wither',2.5,1.5,',Health:6f,Invul:0');await sleep(600);for(let i=0;i<5&&await alive('@e[type=wither]');i++){await att(P,'wither');await sleep(1000)}
ws=await insp(P,'hand');wa=await insp(Q,'hand');L('   '+ws.raw);ok(has(ws,/^Slain: Wither x2$/),'second Wither on the sword: "Slain: Wither x2"');ok(/WITHER=1$|WITHER=1;/.test(wa.slain+';'),'axe did not hit the second Wither: still 1');
// elder guardian
await cons('minecraft:kill @e[type=item]',300,true);await rep(P,'hotbar.1','bow');await rep(P,'hotbar.8','arrow 16');a.setQuickBarSlot(1);await sleep(400);
await rep(Q,'weapon.mainhand','stone_sword');
await sum(P,'elder_guardian',4.5,0.5,',Health:30f');await sleep(800);await tp(P,0.5,0.5,-90,0);logSince();
for(let i=0;i<3;i++){await cons('mmrig use '+P+' 25',2200,true);if(/hurt boss ELDER_GUARDIAN/.test(fs.readFileSync('/workspace/mm-rig/boot.log','utf8').slice(-4000)))break}
for(let i=0;i<15&&await alive('@e[type=elder_guardian]');i++){await att(Q,'elder_guardian');await sleep(800)}
lg=logSince();L('   '+lg.split('\n').filter(x=>/\[stats\]/.test(x)).map(x=>x.replace(/.*\[stats\] /,'')).join(' | '));
let eb=await insp(P,'hand'),es=await insp(Q,'hand');L('   '+eb.raw);L('   '+es.raw);
ok(/ELDER_GUARDIAN=1/.test(eb.slain)&&has(eb,/^Slain: Elder Guardian$/),'MMaker bow credited: "Slain: Elder Guardian"');
ok(/ELDER_GUARDIAN=1/.test(es.slain)&&has(es,/^Slain: Elder Guardian$/),'Helper stone sword credited: "Slain: Elder Guardian"');
// ---------- S11 despawn clears the fight record ----------
L('--- S11 boss despawn clears its damager list');
await rep(Q,'weapon.mainhand','iron_axe');await sum(P,'wither',2.5,1.5,',Health:200f,Invul:0');await sleep(600);logSince();await att(Q,'wither');await sleep(500);
await cons('difficulty peaceful',1200,true);await cons('difficulty normal',300,true);lg=logSince();
L('   '+lg.split('\n').filter(x=>/\[stats\]/.test(x)).map(x=>x.replace(/.*\[stats\] /,'')).join(' | '));
ok(/boss WITHER removed \(DESPAWN\), cleared 1/.test(lg)&&!(await alive('@e[type=wither]')),'Wither despawned (peaceful): its damager list was cleared, no trophy given');
wa=await insp(Q,'hand');L('   '+wa.raw);ok(wa.slain===''&&!has(wa,/Slain/),'the axe that hit the despawned Wither got no trophy');
// ---------- S8 counters survive anvil, smithing, grindstone ----------
L('--- S8 kills + trophies survive anvil, netherite smithing, grindstone');
await cons('minecraft:clear '+P,300,true);await cons('minecraft:kill @e[type=item]',300,true);
// a crafted diamond sword with kills and a trophy (craft it, then give it stats via a kill and a wither)
await cons('minecraft:give '+P+' diamond 2',300,true);await cons('minecraft:give '+P+' stick 1',300,true);
let w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
await hold(a,'diamond_sword');await sum(P,'husk',1.5,0.5,',Health:1f');await att(P,'husk');await sleep(900);
await sum(P,'wither',2.5,1.5,',Health:5f,Invul:0');await sleep(600);for(let i=0;i<4&&await alive('@e[type=wither]');i++){await att(P,'wither');await sleep(1000)}
await cons('minecraft:kill @e[type=item]',300,true);
r=await insp(P,'hand');L('   start: '+r.raw);const want=x=>x.name==='Crafty'&&x.mob===2&&/WITHER=1/.test(x.slain)&&has(x,/^Crafted by Crafty$/)&&has(x,/^Kills: 2$/)&&has(x,/^Slain: Wither$/);
ok(want(r),'sword has Crafted by, Kills: 2 (husk + the Wither), Slain: Wither');
await cons('minecraft:xp add '+P+' 50 levels',300,true);
w=await openAt(a,2,Y,1);await put(a,w,'diamond_sword',0);a._client.write('name_item',{name:'Champ'});await sleep(600);await take(a,w,2);await close(a);
r=await insp(P,items(a,'diamond_sword')[0]);L('   after anvil rename: '+r.raw);ok(want(r),'anvil rename keeps mark, kills and trophy');
await cons('minecraft:give '+P+' netherite_upgrade_smithing_template 1',300,true);await cons('minecraft:give '+P+' netherite_ingot 1',300,true);
w=await openAt(a,2,Y,-1);await put(a,w,'netherite_upgrade_smithing_template',0);await put(a,w,'diamond_sword',1);await put(a,w,'netherite_ingot',2);await sleep(500);await take(a,w,3);await close(a);
r=await insp(P,items(a,'netherite_sword')[0]);L('   after netherite: '+r.raw);ok(want(r),'netherite upgrade keeps mark, kills and trophy');
await hold(a,'netherite_sword');await cons(`minecraft:enchant ${P} sharpness 2`,300,true);
w=await openAt(a,-2,Y,0);await put(a,w,'netherite_sword',0);await sleep(500);await take(a,w,2);await close(a);
r=await insp(P,items(a,'netherite_sword')[0]);L('   after grindstone: '+r.raw);ok(want(r),'grindstone keeps mark, kills and trophy');
// ---------- S2 PvP ----------
L('--- S2 player kill (shown as "(Players: 1)")');
await hold(a,'netherite_sword');await cons('minecraft:clear '+Q,300,true);await cons(`minecraft:attribute ${Q} minecraft:max_health base set 1`,500,true);await sleep(500);
await att(P,Q);await sleep(1500);r=await insp(P,'hand');L('   '+r.raw);
ok(r.pvp===1&&r.mob===2&&has(r,/^Kills: 3 \(Players: 1\)$/),'player kill kept separate (mob 2, pvp 1): "Kills: 3 (Players: 1)"');
await cons(`minecraft:attribute ${Q} minecraft:max_health base set 20`,300,true);
// ---------- S9 /mark stats off ----------
L('--- S9 /mark stats off: lines hidden, nothing counted; back on restores');
let m=await say(a,'/mark stats off');ok(/OFF/.test(m),'/mark stats off reply: '+m);
await openAt(a,2,Y,0);await close(a);r=await insp(P,'hand');L('   '+r.raw);
ok(has(r,/^Crafted by Crafty$/)&&!has(r,/Kills|Slain/)&&r.mob===2&&r.pvp===1,'stats lines hidden, data kept, Crafted by stays');
await sum(P,'husk',1.5,0.5,',Health:1f');await att(P,'husk');await sleep(900);r=await insp(P,'hand');ok(r.mob===2,'kill while stats off not counted (mob still 2)');
m=await say(a,'/mark stats on');ok(/ON/.test(m),'/mark stats on reply: '+m);await openAt(a,2,Y,0);await close(a);r=await insp(P,'hand');L('   '+r.raw);
ok(has(r,/^Kills: 3 \(Players: 1\)$/)&&has(r,/^Slain: Wither$/),'lines come back after /mark stats on');
// ---------- S10 PAPI ----------
L('--- S10 PlaceholderAPI');
let pp=await cons(`papi parse ${P} M=%makersmark_mark% S=%makersmark_stats%`,800,true);L('   '+pp.split('\n').filter(x=>/M=/.test(x)).join(' '));ok(/M=ON S=ON/.test(pp),'%makersmark_mark% and %makersmark_stats% report ON');
await say(a,'/mark off',600);pp=await cons(`papi parse ${P} M=%makersmark_mark%`,800,true);ok(/M=OFF/.test(pp),'%makersmark_mark% reports OFF after /mark off');await say(a,'/mark on',600);
H.summary();a.quit();b.quit();await sleep(1000);process.exit(0);
})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
