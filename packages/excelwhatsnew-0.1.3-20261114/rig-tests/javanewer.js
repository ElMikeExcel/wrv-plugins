// ExcelWhatsNew 0.1.1 Newer news: Java (mineflayer) on the box rig, live config 62b95a5e + live changelog 0091bc72 (8 entries).
process.env.OUT=process.env.OUT||'/workspace/batch-next/whatsnew-newer-20261026/tests/rig/java-newer.out';
const {fs,sleep,L,cons,join}=require('/workspace/batch-next/whatsnew/tests/rig/lib.js');
const N='WNNewerJ';let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const clicks=b=>{const out=[];for(const r of b.raw){const walk=o=>{if(!o||typeof o!=='object')return;const ce=o.clickEvent||o.click_event;if(ce)out.push({text:(o.text||'').trim(),cmd:ce.value||ce.command});for(const k of ['extra','with'])if(Array.isArray(o[k]))o[k].forEach(walk)};try{walk(JSON.parse(r))}catch(e){}}return out};
async function view(b,cmd){b.msgs.length=0;b.raw.length=0;b.chat(cmd);await sleep(1500);return {txt:b.msgs.join('\n'),cl:clicks(b)}}
(async()=>{
 L('=== javanewer '+new Date().toString());
 await cons('whatsnew reset '+N,800);
 let b=await join(N);b.on('windowOpen',w=>setTimeout(()=>{try{if(b.currentWindow)b.closeWindow(b.currentWindow)}catch(e){}},1000));
 await sleep(14000);
 let v=await view(b,'/whatsnew');L('newest buttons '+JSON.stringify(v.cl));
 ok(v.cl.some(c=>c.text==='[OK]'&&/^\/whatsnew ok /.test(c.cmd)),'newest, not OK yet: [OK] still there');
 ok(v.cl.some(c=>c.text==='[Older news]'&&c.cmd==='/whatsnew 2'),'newest: [Older news] runs /whatsnew 2');
 ok(!v.cl.some(c=>/Newer/.test(c.text)),'newest: no [Newer news]');
 v=await view(b,'/whatsnew 3');L('entry 3 buttons '+JSON.stringify(v.cl));
 ok(v.cl.some(c=>c.text==='[Newer news]'&&c.cmd==='/whatsnew 2'),'entry 3: [Newer news] runs /whatsnew 2 (default label from code, key not in live config)');
 ok(v.cl.some(c=>c.text==='[Older news]'&&c.cmd==='/whatsnew 4'),'entry 3: [Older news] runs /whatsnew 4');
 const order=v.cl.filter(c=>/news\]/.test(c.text)).map(c=>c.text);ok(order[0]==='[Newer news]'&&order[1]==='[Older news]','entry 3 order: Newer then Older');
 // Follow Newer all the way back to the newest
 let cur=3,steps=0;while(true){const nw=v.cl.find(c=>c.text==='[Newer news]');if(!nw)break;v=await view(b,nw.cmd);cur--;steps++;if(steps>10)break}
 ok(cur===1&&steps===2,'clicking Newer twice from entry 3 reaches the newest ('+steps+' steps)');
 ok(/Mend/i.test(v.txt),'newest entry text is back on screen (Mend news)');
 v=await view(b,'/whatsnew 8');L('entry 8 buttons '+JSON.stringify(v.cl));
 ok(v.cl.some(c=>c.text==='[Newer news]'&&c.cmd==='/whatsnew 7')&&!v.cl.some(c=>/Older/.test(c.text)),'last entry (8): Newer only, no Older');
 v=await view(b,'/whatsnew 99');ok(v.cl.some(c=>c.text==='[Newer news]'&&c.cmd==='/whatsnew 7'),'/whatsnew 99 clamps to entry 8 with Newer news');
 v=await view(b,'/whatsnew list');ok(v.cl.filter(c=>c.text==='[Read]').length===8&&!/or 3/.test(v.txt),'Java list: 8 Read buttons, no Bedrock hint');
 b.chat('/whatsnew ok');await sleep(1200);
 v=await view(b,'/whatsnew 2');ok(!v.cl.some(c=>c.text==='[OK]')&&v.cl.some(c=>c.text==='[Newer news]'),'after OK: entry 2 has Newer, no OK');
 b.quit();await sleep(1500);
 L('RESULT javanewer '+pass+' passed '+fail+' failed');process.exit(0)
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
