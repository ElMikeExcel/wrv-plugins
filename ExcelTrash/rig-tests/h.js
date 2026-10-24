const T=require('./lib.js');const {L,cons,sleep,fs}=T;
let pass=0,fail=0;const res=[];
const ok=(c,m)=>{if(c){pass++;L('  PASS '+m)}else{fail++;L('  FAIL '+m)}res.push((c?'PASS ':'FAIL ')+m)};
const cnt=(b,name)=>{const w=b.currentWindow;if(w&&w.inventoryStart)return wcount(w,w.inventoryStart,w.slots.length,name);return b.inventory.items().filter(i=>i.name===name).reduce((a,i)=>a+i.count,0)};
const winOpen=b=>new Promise(r=>{const t=setTimeout(()=>r(null),3000);b.once('windowOpen',w=>{clearTimeout(t);r(w)})});
const say=async(b,c,ms=900)=>{b.msgs=[];b.chat(c);await sleep(ms);return b.msgs.join(' | ')};
const openCmd=async(b,c)=>{b.msgs=[];const p=winOpen(b);b.chat(c);const w=await p;await sleep(400);return w};
const click=async(b,slot,btn=0,mode=0,ms=350)=>{await b.clickWindow(slot,btn,mode).catch(e=>L('click err '+e.message));await sleep(ms)};
// chest contents: parse "data get block" — top-level only (no shulkers) -> {id:count}
const chest=async(x,y,z)=>{const o=await cons(`data get block ${x} ${y} ${z} Items`,700,true);const m={};let n=0;
  const re=/Slot: (\d+)b, id: "minecraft:([a-z_]+)", count: (\d+)/g;let r;while((r=re.exec(o))){m[r[2]]=(m[r[2]]||0)+(+r[3]);n+=+r[3]}
  if(/has the following block data: \[\]/.test(o))return {_total:0};m._total=n;m._raw=o.slice(-300);return m};
const wcount=(w,from,to,name)=>{let n=0;for(let i=from;i<to;i++){const s=w.slots[i];if(s&&(!name||s.name===name))n+=s.count}return n};
const trashlog=()=>{try{return fs.readFileSync('/workspace/trash-rig/plugins/ExcelTrash/trash.log','utf8')}catch(e){return ''}};
const lastlog=()=>{const t=trashlog().trim().split('\n');return t[t.length-1]||''};
const peek=async n=>{const o=await cons('trash peek '+n,700,true);const m=o.match(/peek \S+: (.*)/);return m?m[1]:o.slice(-200)};
const groundItems=(b,name)=>Object.values(b.entities).filter(e=>e.name==='item'||e.displayName==='Item').map(e=>{const it=e.getDroppedItem&&e.getDroppedItem();return it}).filter(i=>i&&(!name||i.name===name)).reduce((a,i)=>a+i.count,0);
const summary=()=>{L(`RESULT pass=${pass} fail=${fail}`);return {pass,fail,res}};
const join1=n=>new Promise(r=>{const b=T.mk(n);b.once('spawn',()=>r({b}));b.once('kicked',k=>r({k:String(typeof k==='string'?k:JSON.stringify(k))}))});
const join=async n=>{let b;for(let t=0;t<4;t++){const x=await join1(n);if(x.b){b=x.b;break}const m=x.k.match(/wait (\d+) seconds/);L('   (login cooldown, waiting '+(m?m[1]:5)+'s)');await sleep(((m?+m[1]:5)+2)*1000)}await sleep(1500);await sleep(2500);if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(400)}return b};
module.exports={...T,join,ok,cnt,winOpen,say,openCmd,click,chest,wcount,trashlog,lastlog,peek,groundItems,summary};
