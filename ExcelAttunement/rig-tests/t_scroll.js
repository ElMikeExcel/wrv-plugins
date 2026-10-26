// ExcelAttunement 0.1.7 rig test: stance lock must never wall off a hotbar slot.
// Layout (the reported case): wand in slot 5, heal potions in slots 1 and 9.
const T=require('./lib');const {L,cons,sleep,join,since,num}=T;
const R=[];const P=(ok,name,detail)=>{const s=(ok?'PASS ':'FAIL ')+name+(detail?' | '+detail:'');R.push(s);L(s)};
(async()=>{const r=Date.now()%10000;const N='Sc'+r;L('=== stance scroll rig run '+r+' jar='+(process.env.JAR||'?')+' '+new Date().toString());
const A=await join(N);
const sel=()=>num('data get entity '+N+' SelectedItemSlot');
const names=()=>A.inventory.slots.slice(36,45).map(x=>x?x.name:'-');
const logLines=re=>since().split('\n').map(T.clean).filter(x=>re.test(x));
await cons('difficulty peaceful',300,true);await cons('gamemode survival '+N,300,true);await cons('clear '+N,400,true);await cons('attunement clear '+N,300,true);
await cons('tp '+N+' 0.5 -60 0.5 0 0',300,true);
const put=(i,it)=>cons('item replace entity '+N+' hotbar.'+i+' with '+it,400,true);
await put(0,'potion[potion_contents={potion:"minecraft:healing"}]');await put(1,'bread 8');await put(2,'iron_sword');await put(3,'cobblestone 16');
A.setQuickBarSlot(3);await sleep(500);
await cons('wandp '+N+' student',1500,true);await sleep(500);
await put(5,'torch 8');await put(6,'apple 4');await put(7,'shield');await put(8,'splash_potion[potion_contents={potion:"minecraft:strong_healing"}]');
await sleep(700);const before=names();L('  hotbar 1-9 (client view, potions may show -): '+before.join(' '));
const sid=async i=>{const o=await cons('data get entity '+N+' Inventory[{Slot:'+i+'b}].id',600,true);return (o.match(/"minecraft:([a-z_]+)"/)||[])[1]||'-'};
const srv=[];for(let i=0;i<9;i++)srv.push(await sid(i));L('  hotbar 1-9 (server): '+srv.join(' '));
P(srv[0]==='potion'&&srv[8]==='splash_potion'&&srv[4]!=='-'&&srv[4]!=='torch','layout: potions in 1 and 9, wand in 5',srv.join(' '));
const count=()=>A.inventory.slots.filter(Boolean).reduce((a,b)=>a+b.count,0);const c0=count();
// scroll = one-slot steps from the server's current slot
const scrollRun=async(start,dir,steps,label)=>{A.setQuickBarSlot(start);await sleep(500);const seen=[];let cur=await sel();seen.push(cur);
 for(let i=0;i<steps;i++){const nx=((cur+dir)%9+9)%9;A.setQuickBarSlot(nx);await sleep(450);cur=await sel();seen.push(cur);}
 L('  '+label+': slots '+seen.map(x=>x+1).join('>'));return seen};
const allReach=(seen,lockedIdx)=>{const u=new Set(seen);for(let i=0;i<9;i++){if(i===lockedIdx)continue;if(!u.has(i))return false}return true};
const fighter=async()=>{await cons('execute unless entity @e[tag=z] run summon zombie 0.5 -60 2.5 {NoAI:1b,PersistenceRequired:1b,Tags:["z"]}',600,true);await cons('data merge entity @e[tag=z,limit=1] {Health:20f,Fire:0s}',300,true);A.setQuickBarSlot(2);await sleep(500);const z=Object.values(A.entities).filter(e=>e.name==='zombie').sort((a,b)=>a.position.distanceTo(A.entity.position)-b.position.distanceTo(A.entity.position))[0];if(z)A.attack(z);await sleep(800)};
const stance=async()=>{const o=await cons('attunement status',700,true);return (o.match(new RegExp(N+': (\\w+)'))||[])[1]||'NONE'};

// 1) no stance: the wand slot is reachable like any other
let s=await scrollRun(2,1,3,'no stance, scroll right from 3');P(s.includes(4),'no stance: scroll lands on the wand slot normally');
// 2) Fighter stance (sword hit)
await cons('difficulty easy',300,true);await cons('time set midnight',300,true);await cons('effect give @e[type=zombie] fire_resistance infinite 0 true',300,true);await cons('summon zombie 0.5 -60 2.5 {NoAI:1b,PersistenceRequired:1b,Tags:["z"],Health:200f,Attributes:[{id:"minecraft:max_health",base:200}]}',600,true);
A.setQuickBarSlot(2);await sleep(600);since();let z=Object.values(A.entities).filter(e=>e.name==='zombie').sort((a,b)=>a.position.distanceTo(A.entity.position)-b.position.distanceTo(A.entity.position))[0];
if(z)A.attack(z);await sleep(900);let st=await stance();P(st==='MARTIAL','sword hit sets Fighter stance',st);
const hopLog=[];
await fighter();s=await scrollRun(3,1,12,'MARTIAL scroll right from 4');P(!s.slice(1).includes(4),'MARTIAL: wand never comes into hand while scrolling right');P(allReach(s,4),'MARTIAL: scrolling right reaches all 8 other slots incl. potions 1 and 9');
await fighter();s=await scrollRun(5,-1,12,'MARTIAL scroll left from 6');P(!s.slice(1).includes(4),'MARTIAL: wand never comes into hand while scrolling left');P(allReach(s,4),'MARTIAL: scrolling left reaches all 8 other slots incl. potions 1 and 9');
// The reported case: from the sword (3) to the potion in 9 and back to the potion in 1 by scroll only
await fighter();s=await scrollRun(2,1,5,'MARTIAL sword(3) -> potion(9) by scroll');P(s[s.length-1]===8,'MARTIAL: sword slot 3 to potion slot 9 in 5 scroll notches (hops the wand)',s.map(x=>x+1).join('>'));
await fighter();s=await scrollRun(8,-1,7,'MARTIAL potion(9) -> potion(1) by scroll');P(s[s.length-1]===0,'MARTIAL: potion 9 to potion 1 scrolling left (hops the wand)',s.map(x=>x+1).join('>'));
// number keys / taps
await fighter();A.setQuickBarSlot(0);await sleep(500);A.setQuickBarSlot(4);await sleep(600);let v=await sel();P(v===0,'MARTIAL: number key 5 (wand) still refused, stays on 1',''+(v+1));
st=await stance();P(st==='MARTIAL','Fighter stance live during number-key test',st);
for(const k of [8,0,2,6]){A.setQuickBarSlot(k);await sleep(500);v=await sel();P(v===k,'MARTIAL: number key '+(k+1)+' works',''+(v+1));}
since();await fighter();since();await cons('castp '+N+' missile',1200,true);L('  cast while Fighter: '+T.fs.readFileSync(T.LOG,'utf8').split('\n').filter(x=>/missile|LOCKED cast|blocked/i.test(x)).slice(-2).map(T.clean).join(' || '));
// 3) Caster stance: sword in slot 5 instead
await cons('attunement clear '+N,300,true);
await cons('item replace entity '+N+' hotbar.2 from entity '+N+' hotbar.4',300,true);await put(4,'iron_sword');await sleep(600);L('  caster layout: '+names().join(' '));
A.setQuickBarSlot(2);await sleep(500);since();await cons('castp '+N+' stormshackle',1500,true);st=await stance();
if(st!=='CASTER'){A.activateItem();await sleep(900);A.setQuickBarSlot(8);await sleep(1500);A.activateItem();await sleep(800);st=await stance();}
P(st==='CASTER','combat cast sets Caster stance',st);
const caster=async()=>{A.setQuickBarSlot(2);await sleep(400);await cons('castp '+N+' stormshackle',1200,true)};
if(st==='CASTER'){s=await scrollRun(3,1,12,'CASTER scroll right from 4');P(!s.slice(1).includes(4)&&allReach(s,4),'CASTER: sword in slot 5 hopped, all other slots reachable right');
 await caster();s=await scrollRun(5,-1,12,'CASTER scroll left from 6');P(!s.slice(1).includes(4)&&allReach(s,4),'CASTER: sword in slot 5 hopped, all other slots reachable left');}
// 4) items safe
await sleep(500);const c1=count();P(c1===c0,'no item lost or duplicated (count '+c0+' -> '+c1+')');
// 5) stance cleared: slot 5 reachable again
await cons('attunement clear '+N,300,true);s=await scrollRun(3,1,1,'cleared, scroll onto 5');P(s[1]===4,'stance clear: slot 5 reachable again');
const o=await cons('attunement status',700,true);L('  status: '+o.replace(/\n/g,' | ').slice(0,300));
L('  plugin log: '+T.fs.readFileSync(T.LOG,'utf8').split('\n').filter(x=>/HOP hotbar|LOCKED hotbar/.test(x)).slice(-6).map(T.clean).join(' || '));
await cons('kill @e[tag=z]',300,true);
L('=== RESULT '+R.filter(x=>x.startsWith('PASS')).length+' pass, '+R.filter(x=>x.startsWith('FAIL')).length+' fail');A.quit();setTimeout(()=>process.exit(0),800)})();
