const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,openCmd,click,chest}=H;
(async()=>{L('=== T21 server stop while items sit on the red side (part A)');
const a=await join('TrA');await cons('clear TrA',300,true);await cons('setblock 180 -60 100 chest[facing=north]{Items:[{Slot:0b,id:"minecraft:diamond",count:3},{Slot:1b,id:"minecraft:gold_ingot",count:4}]}',500,true);
await cons('minecraft:give TrA emerald 2',500,true);await cons('minecraft:tp TrA 180.5 -60 97.5 0 20.5',900,true);
const w=await openCmd(a,'/trash chest');await click(a,0,0,0,400);const ei=w.slots.findIndex((s,i)=>i>=w.inventoryStart&&s&&s.name==='emerald');await click(a,ei);await click(a,40,0,0,400);
L('  red side: '+w.slots.slice(36,54).map(s=>s&&s.name+'x'+s.count).filter(Boolean).join(','));const c=await chest(180,-60,100);L('  chest now: '+JSON.stringify({d:c.diamond||0,g:c.gold_ingot||0}));
H.fs.appendFileSync('/workspace/trash-rig/rigconsole','stop\n');await sleep(8000);H.summary();process.exit(0)})();
