const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,openCmd,click}=H;
(async()=>{L('=== T22 rollback to 0.1.0 with a 0.2.0 batch pending (part A)');const a=await join('TrA');await cons('clear TrA',300,true);await cons('minecraft:give TrA dirt 64',500,true);
const w=await openCmd(a,'/trash');const i=w.slots.findIndex((s,k)=>k>=w.inventoryStart&&s&&s.name==='dirt');await click(a,i);await click(a,0);await click(a,33,0,0,700);a.closeWindow(w);await sleep(500);
const pk=await H.peek('TrA');ok(/items=64/.test(pk)&&cnt(a,'dirt')===0,'0.2.0 batch pending before rollback: '+pk);a.quit();await sleep(500);H.summary();process.exit(0)})();
