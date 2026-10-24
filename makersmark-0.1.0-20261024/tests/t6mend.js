// Mending and durability repair keep the mark and counters. TEST RIG ONLY.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,Y,P,insp,items,openAt,hold,put,take,close,fresh,has,tp,sum}=C;
const att=(n,t)=>cons(`mmrig attack ${n} ${t}`,300,true);
(async()=>{L('=== mending check '+new Date().toString());
for(const c of ['gamerule spawn_mobs false','minecraft:kill @e[type=!player]'])await cons(c,300,true);
const a=await join(P);await cons('gamemode survival '+P,300,true);await tp(P,0.5,0.5,-90,0);a.setQuickBarSlot(0);await cons('nick '+P+' ~Mender',400,true);
await fresh(a);await cons('minecraft:give '+P+' diamond 2',300,true);await cons('minecraft:give '+P+' stick 1',300,true);
let w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
await hold(a,'diamond_sword');await sum(P,'husk',1.5,0.5,',Health:1f');await att(P,'husk');await sleep(900);await cons('minecraft:kill @e[type=item]',300,true);
await cons(`minecraft:enchant ${P} mending`,400,true);
await cons(`minecraft:item modify entity ${P} weapon.mainhand {function:"minecraft:set_damage",damage:0.5}`,500,true);
let r0=await insp(P,'hand');L('   damaged: '+r0.raw);
for(let i=0;i<6;i++)await cons(`minecraft:summon experience_orb ${1000.5} ${Y} ${1000.5} {Value:50s}`,300,true);await sleep(3000);
let r=await insp(P,'hand');L('   after mending: '+r.raw);
ok(r.dmg<r0.dmg,'mending repaired the sword (damage '+r0.dmg+' -> '+r.dmg+')');
ok(r.name==='Mender'&&r.mob===1&&has(r,/^Crafted by Mender$/)&&has(r,/^Kills: 1$/)&&r.lore.length===2,'mark and Kills: 1 unchanged after mending, no duplicate lines');
H.summary();a.quit();await sleep(1200);process.exit(0)})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
