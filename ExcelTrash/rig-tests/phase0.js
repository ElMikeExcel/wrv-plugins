const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,openCmd,click}=H;
(async()=>{L('=== phase0 (ExcelTrash 0.1.0 installed): make a pending batch to migrate');
await cons('gamerule sendCommandFeedback true',400,true);
const b=await join('TrA');await cons('clear TrA',500,true);await cons('give TrA dirt 64',800);
const w=await openCmd(b,'/trash');ok(w&&w.slots.length>=36,'0.1.0 /trash opens bin (slots '+(w&&w.slots.length)+') title '+(w&&JSON.stringify(w.title)));
const inv=w.slots.findIndex((s,i)=>i>=36&&s&&s.name==='dirt');await click(b,inv);await click(b,0);b.closeWindow(w);await sleep(800);
ok(cnt(b,'dirt')===0,'dirt trashed by 0.1.0');L('  batches.dat: '+H.fs.readFileSync('/workspace/trash-rig/plugins/ExcelTrash/batches.dat','utf8').slice(0,80).replace(/\t/g,' '));
L('  log: '+H.lastlog());b.quit();await sleep(500);H.summary();process.exit(0)})();
