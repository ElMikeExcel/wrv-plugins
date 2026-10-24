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
const BLK={craft:'crafting_table',anvil:'anvil',smith:'smithing_table',grind:'grindstone[face=floor]'};
// Re-place the block first: anvils get chipped by use (and openAnvil refuses a non-ANVIL block), and fights nearby can break blocks.
const openAt=async(b,x,y,z)=>{const k=KIND[x+','+z];if(k==='anvil')await cons(`setblock ${x+OX} ${y} ${z+OZ} ${BLK[k]}`,250,true);const p=winOpen(b);await cons(`mmrig open ${b.username} ${k} ${x+OX} ${y} ${z+OZ}`,300,true);const w=await p;await sleep(500);
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
module.exports={H,L,cons,sleep,join,ok,say,click,OX,OZ,Y,P,Q,give,insp,b2,items,winOpen,openAt,IS,hold,idx,put,take,close,fresh,mlog,has,tp,sum};
