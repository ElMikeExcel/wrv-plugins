// Staged compass menu entries on the rig's menu COPY: reload, render, click toggles.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,say,P}=C;
(async()=>{L('=== menu entry rig check '+new Date().toString());
const o=await cons('excelmenu reload',2500,true);L('   '+o.split('\n').filter(x=>/server_menu|FAILED|loaded|ExcelMenu/i.test(x)&&!/spawnshop/.test(x)).slice(0,4).join(' | '));
ok(!/FAILED \[server_menu\]/.test(o),'server_menu with the 4 new entries loads without a FAILED line');
const a=await join(P);await say(a,'/mark on',500);await say(a,'/mark stats on',500);await sleep(2500);
const open=async()=>{const p=new Promise(r=>{const t=setTimeout(()=>r(null),4000);a.once('windowOpen',w=>{clearTimeout(t);r(w)})});await cons('excelmenu open server_menu '+P,300,true);const w=await p;await sleep(800);return w};
const nm=s=>s?(s.customName||s.displayName||s.name).toString():'empty';
const txt=s=>{if(!s)return 'empty';try{const c=s.customName;return (typeof c==='string'?c:JSON.stringify(c)).replace(/"[a-z_]+":/g,'')}catch(e){return s.name}};
let w=await open();ok(!!w,'menu opens');L('   23: '+(w&&w.slots[23]&&w.slots[23].name)+' '+txt(w&&w.slots[23]).slice(0,120));L('   24: '+(w&&w.slots[24]&&w.slots[24].name)+' '+txt(w&&w.slots[24]).slice(0,120));
ok(w&&w.slots[23]&&w.slots[23].name==='name_tag'&&w.slots[24]&&w.slots[24].name==='iron_sword','ON buttons shown (name tag at 23, iron sword at 24)');
await a.clickWindow(23,0,0).catch(()=>{});await sleep(1500);await a.clickWindow(24,0,0).catch(()=>{});await sleep(1500);
w=a.currentWindow||await open();L('   after clicks 23: '+(w&&w.slots[23]&&w.slots[23].name)+' 24: '+(w&&w.slots[24]&&w.slots[24].name));
ok(w&&w.slots[23]&&w.slots[23].name==='paper'&&w.slots[24]&&w.slots[24].name==='wooden_sword','clicks turned both OFF and the buttons refreshed');
const pp=await cons(`papi parse ${P} M=%makersmark_mark% S=%makersmark_stats%`,800,true);ok(/M=OFF S=OFF/.test(pp),'placeholders agree: '+(pp.match(/M=\S+ S=\S+/)||[''])[0]);
await a.clickWindow(23,0,0).catch(()=>{});await sleep(1500);await a.clickWindow(24,0,0).catch(()=>{});await sleep(1500);
w=a.currentWindow||await open();ok(w&&w.slots[23]&&w.slots[23].name==='name_tag'&&w.slots[24]&&w.slots[24].name==='iron_sword','clicked again: both back ON');
if(a.currentWindow)a.closeWindow(a.currentWindow);H.summary();a.quit();await sleep(800);process.exit(0)})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
