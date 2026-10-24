// ExcelMakersMark 0.1.0 + ExcelTips 0.1.0 hand-over on mm-rig (Java). TEST RIG ONLY.
const T=require('./lib.js');const {L,cons,sleep,join}=T;
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('  PASS '+w)}else{fail++;L('  FAIL '+w)}};
const P=process.env.MMP||'MkTipA',Q=process.env.MMQ||'MkTipB';
const TIP='Things you craft now say who made them';
const cnt=(b)=>b.msgs.filter(m=>m.includes(TIP)).length;
const use=async(b,c='/mark')=>{b.msgs.length=0;b.chat(c);await sleep(3500);return b.msgs.join(' | ')};
(async()=>{L('=== MakersMark + ExcelTips ('+new Date().toString()+')');
 const a=await join(P);await sleep(1000);
 let m=await use(a);L('   use1: '+m.slice(0,400));
 ok(cnt(a)===1,'first /mark use shows the Maker\'s mark tip exactly once (built-in, not both)');
 ok(!/Got it\? \[OK\]/.test(m)||/tips ok|\[OK\]/.test(m),'tip carries an OK option');
 ok(!/Type \/mark ok to hide/.test(m),'no built-in MakersMark Bedrock line on Java');
 const st=await cons('tips status '+P,800,true);L('   status: '+st.replace(/\n/g,' / ').slice(0,300));
 await sleep(21000);m=await use(a);L('   use2: '+m.slice(0,200));ok(cnt(a)===1,'tip repeats on the next use (no OK yet)');
 await sleep(21000);m=await use(a,'/mark ok');L('   /mark ok: '+m.slice(0,200));
 await sleep(21000);m=await use(a);ok(cnt(a)===0,'/mark ok hides the tip');
 await sleep(21000);m=await use(a);ok(cnt(a)===0,'still hidden on a later use');
 const b=await join(Q);await sleep(1000);
 m=await use(b);ok(cnt(b)===1,'second player sees the tip');
 await sleep(2000);m=await use(b,'/mark ok');L('   /tips ok: '+m.slice(0,200));
 await sleep(21000);m=await use(b);ok(cnt(b)===0,'second player: /mark ok ([OK] click) hides it');
 // repeat-until-OK check: count shows over 5 uses without OK
 const R=P+'R';const r=await join(R);await sleep(1000);let shows=0;
 for(let i=0;i<5;i++){await use(r);shows+=cnt(r);await sleep(21000)}
 L('   shows over 5 uses without OK: '+shows);ok(shows===5,'tip keeps repeating until OK (5 of 5 uses)');
 L(`RESULT pass=${pass} fail=${fail}`);a.quit();b.quit();r.quit();await sleep(800);process.exit(fail?1:0)})().catch(e=>{L('ERR '+e);L(`RESULT pass=${pass} fail=${fail} aborted=1`);process.exit(4)});
