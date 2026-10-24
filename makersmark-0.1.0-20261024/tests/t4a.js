// Restart persistence, phase A (before restart). TEST RIG ONLY.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,say,Y,P,insp,items,openAt,hold,put,take,close,fresh,has,tp,sum}=C;
const fs=require('fs');const att=(n,t)=>cons(`mmrig attack ${n} ${t}`,300,true);const rep=(n,slot,item)=>cons(`minecraft:item replace entity ${n} ${slot} with ${item}`,400,true);
const alive=async sel=>/Test passed/.test(await cons('execute if entity '+sel,500,true));
(async()=>{L('=== persistence phase A (before restart) '+new Date().toString());
for(const c of ['gamerule spawn_mobs false','gamerule advance_time false','minecraft:kill @e[type=!player]'])await cons(c,300,true);
const a=await join(P);await cons('gamemode survival '+P,300,true);await tp(P,0.5,0.5,-90,0);a.setQuickBarSlot(0);await cons('nick '+P+' ~Keeper',500,true);
await cons('minecraft:effect give '+P+' minecraft:regeneration 600 4 true',300,true);
await fresh(a);await cons('minecraft:give '+P+' diamond 2',300,true);await cons('minecraft:give '+P+' stick 1',300,true);
let w=await openAt(a,2,Y,0);await put(a,w,'diamond',2,true);await put(a,w,'diamond',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
await hold(a,'diamond_sword');await sum(P,'husk',1.5,0.5,',Health:1f');await att(P,'husk');await sleep(900);
await sum(P,'wither',2.5,1.5,',Health:5f,Invul:0');await sleep(600);for(let i=0;i<4&&await alive('@e[type=wither]');i++){await att(P,'wither');await sleep(1000)}
await cons('minecraft:kill @e[type=item]',300,true);await rep(P,'armor.head','iron_helmet');await sum(P,'husk',3.5,3.5,'');await sleep(600);
for(let i=0;i<3;i++)await cons(`minecraft:damage ${P} 1 minecraft:mob_attack by @e[type=husk,limit=1]`,700,true);
await cons('minecraft:kill @e[type=husk]',300,true);await sleep(5500);
const s=await insp(P,'hand'),h=await insp(P,'head');L('   sword: '+s.raw);L('   helmet: '+h.raw);
ok(s.name==='Keeper'&&s.mob===2&&/WITHER=1/.test(s.slain)&&has(s,/^Crafted by Keeper$/)&&has(s,/^Kills: 2$/)&&has(s,/^Slain: Wither$/),'before restart: sword Crafted by Keeper, Kills: 2, Slain: Wither');
ok(h.hits===3&&has(h,/^Hits blocked: 3$/),'before restart: helmet Hits blocked: 3');
let m=await say(a,'/mark ok');m=await say(a,'/mark off');ok(/OFF/.test(m),'/mark off before restart');await sleep(2600);
fs.writeFileSync('/tmp/mm-persist.json',JSON.stringify({sword:s,helmet:h}));
H.summary();a.quit();await sleep(1500);process.exit(0)})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
