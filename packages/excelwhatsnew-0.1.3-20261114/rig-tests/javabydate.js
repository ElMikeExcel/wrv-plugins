// ExcelWhatsNew By date: Java (mineflayer) on the box rig, live config (show-count 10).
// WRITTEN FOR 0.1.2 against the Nov 13, 2026 snapshot (96 entries, Nov 13 = 5 entries,
// newest = section-colours). FOR THE 0.1.3 RIG (later leg, see BUILD-RECORD.md):
// re-pin every count/id below to the rig-day changelog snapshot, AND flip the day
// walk: a day tap opens the day's OLDEST entry with a "Fri, Nov 13 (1/M)" position
// line in the text, no [Older news] at walk start, [Newer news] walks M-1 steps to
// the day's newest entry (position (M/M), Older only there). Sections 5 and 6
// change direction (Older -> Newer); sections 1-4 and 7-11 stand as written.
process.env.OUT=process.env.OUT||'/workspace/batch-next/whatsnew-bydate-20261113/tests/rig/java-bydate.out';
const {fs,sleep,L,cons,join}=require('/workspace/batch-next/whatsnew/tests/rig/lib.js');
const N='WNByDateJ';let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const clicks=b=>{const out=[];for(const r of b.raw){const walk=o=>{if(!o||typeof o!=='object')return;const ce=o.clickEvent||o.click_event;if(ce)out.push({text:(o.text||'').trim(),cmd:ce.value||ce.command});for(const k of ['extra','with'])if(Array.isArray(o[k]))o[k].forEach(walk)};try{walk(JSON.parse(r))}catch(e){}}return out};
async function view(b,cmd){b.msgs.length=0;b.raw.length=0;b.chat(cmd);await sleep(1500);return {txt:b.msgs.join('\n'),cl:clicks(b)}}
(async()=>{
 L('=== javabydate '+new Date().toString());
 await cons('whatsnew reset '+N,800);
 let b=await join(N);b.on('windowOpen',w=>setTimeout(()=>{try{if(b.currentWindow)b.closeWindow(b.currentWindow)}catch(e){}},1000));
 await sleep(14000);
 // 1. Flat list: By date first, 10 Read rows, range line unchanged.
 let v=await view(b,'/whatsnew list');L('list buttons '+JSON.stringify(v.cl));
 ok(v.cl.length>0&&v.cl[0].text==='[By date]'&&v.cl[0].cmd==='/whatsnew bydate','list: [By date] is the first line, runs /whatsnew bydate');
 ok(v.cl.filter(c=>c.text==='[Read]').length===10,'list: 10 [Read] rows (show-count 10)');
 // 2. Months: exactly November 72 + October 24 + Back.
 v=await view(b,'/whatsnew bydate');L('months '+JSON.stringify(v.cl.map(c=>c.text)));
 ok(v.cl.some(c=>c.text==='[November 2026 — 72 changes]'&&c.cmd==='/whatsnew bydate 2026-11'),'months: November 2026 — 72 changes');
 ok(v.cl.some(c=>c.text==='[October 2026 — 24 changes]'&&c.cmd==='/whatsnew bydate 2026-10'),'months: October 2026 — 24 changes');
 ok(v.cl[v.cl.length-1].text==='[Back]'&&v.cl[v.cl.length-1].cmd==='/whatsnew list','months: Back last, returns to the flat list');
 // 3. November days: newest first, counts, no Nov 14; Back -> months.
 v=await view(b,'/whatsnew bydate 2026-11');L('nov days '+JSON.stringify(v.cl.map(c=>c.text)));
 ok(v.cl[0].text==='[Fri, Nov 13 — 5 changes]'&&v.cl[0].cmd==='/whatsnew bydate 2026-11-13','days: Fri, Nov 13 — 5 changes first');
 ok(v.cl.some(c=>c.text==='[Sat, Nov 1 — 4 changes]'),'days: Nov 1 present (whole month listed)');
 ok(!v.txt.includes('Nov 14'),'days: no Nov 14 row');
 ok(v.cl[v.cl.length-1].text==='[Back]'&&v.cl[v.cl.length-1].cmd==='/whatsnew bydate','days: Back last, returns to months');
 // 4. October days: no Oct 27.
 v=await view(b,'/whatsnew bydate 2026-10');L('oct days '+JSON.stringify(v.cl.map(c=>c.text)));
 ok(v.cl.some(c=>c.text==='[Mon, Oct 26 — 10 changes]'),'days: Mon, Oct 26 — 10 changes');
 ok(!v.txt.includes('Oct 27'),'days: no Oct 27 row');
 // 5. Nov 13 entry: newest first, day-bounded Older walks 5 then stops; Back -> days.
 v=await view(b,'/whatsnew bydate 2026-11-13');L('nov13 buttons '+JSON.stringify(v.cl));
 ok(/section of the compass menu has its own colour/i.test(v.txt),'Nov 13 opens the section-colours entry');
 ok(v.cl.some(c=>c.text==='[Back]'&&c.cmd==='/whatsnew bydate 2026-11'),'Nov 13: [Back] runs /whatsnew bydate 2026-11');
 ok(!v.cl.some(c=>/Newer/.test(c.text)),'Nov 13 newest: no Newer news');
 let steps=0;while(true){const ol=v.cl.find(c=>c.text==='[Older news]');if(!ol)break;v=await view(b,ol.cmd);steps++;if(steps>10)break}
 ok(steps===4,'Older walks all 5 Nov 13 entries and stops ('+steps+' steps)');
 ok(!v.cl.some(c=>/Older/.test(c.text))&&v.cl.some(c=>c.text==='[Newer news]'),'Nov 13 oldest: Newer only, no Older');
 v=await view(b,v.cl.find(c=>c.text==='[Back]').cmd);
 ok(v.cl.some(c=>c.text==='[Fri, Nov 13 — 5 changes]'),'Back from the day entry returns to the November days');
 // 6. Oct 26: 10 entries, paging never leaves the day.
 v=await view(b,'/whatsnew bydate 2026-10-26');steps=0;let sawMend=false;
 while(true){if(/Mend/i.test(v.txt))sawMend=true;const ol=v.cl.find(c=>c.text==='[Older news]');if(!ol)break;v=await view(b,ol.cmd);steps++;if(steps>12)break}
 ok(steps===9,'Oct 26: Older walks all 10 entries ('+steps+' steps)');
 ok(sawMend,'Oct 26 walk passes the Mend news (paging stayed in Oct 26)');
 // 7. Single-entry day: no Newer/Older, Back works.
 v=await view(b,'/whatsnew bydate 2026-10-24');L('oct24 buttons '+JSON.stringify(v.cl));
 ok(!v.cl.some(c=>/Newer|Older/.test(c.text))&&v.cl.some(c=>c.text==='[Back]'),'Oct 24 single entry: no Newer/Older, Back present');
 // 8. Garbage date: months list + did-not-match line, no error.
 v=await view(b,'/whatsnew bydate 2026-13');
 ok(/did not match any news/.test(v.txt)&&v.cl.some(c=>c.text==='[November 2026 — 72 changes]'),'garbage date returns the months list + did-not-match line');
 // 9. Entry view also carries By date (compass opens the newest entry directly).
 v=await view(b,'/whatsnew');
 ok(v.cl.some(c=>c.text==='[By date]'&&c.cmd==='/whatsnew bydate'),'entry view: [By date] present');
 // 10. Regressions: /whatsnew 11 clamps to entry 10; /whatsnew 1 is the newest.
 v=await view(b,'/whatsnew 99');ok(v.cl.some(c=>c.text==='[Newer news]'&&c.cmd==='/whatsnew 9'),'/whatsnew 99 clamps to entry 10 (Newer runs /whatsnew 9)');
 v=await view(b,'/whatsnew 1');ok(/section of the compass menu has its own colour/i.test(v.txt),'/whatsnew 1 is the newest entry');
 // 11. Reload keeps the views working (no restart); a new entry landing in the
 // flat list AND under its month/day is covered by the desk reload check.
 await cons('whatsnew reload',800);
 v=await view(b,'/whatsnew bydate 2026-11');ok(v.txt.includes('Nov 13'),'after reload: November days still render');
 b.chat('/whatsnew ok');await sleep(1200);
 v=await view(b,'/whatsnew 2');ok(!v.cl.some(c=>c.text==='[OK]'),'after OK: entry 2 has no OK button');
 b.quit();await sleep(1500);
 L('RESULT javabydate '+pass+' passed '+fail+' failed');process.exit(0)
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
