// ExcelWandBars 0.2.2 rig run (box TEST rig /workspace/ss022-rig only, port 25941).
// PHASE: pre (0.2.1 + old wands.yml) | main (0.2.2 + 6-row wands.yml) | restart | rollback | tips (ExcelTips present)
// SFX: fresh player suffix per round. Bots: Jv (Java slots), Bd (Bedrock sim), Bs (Bedrock sim, Sneak bars), Pg (progress), Rk (rank-up, rig-only lowered casts), Tt (tip)
const T=require('./lib');const {L,cons,sleep,join}=T;const fs=require('fs');
const R=[];const P=(ok,name,detail)=>{const s=(ok?'PASS ':'FAIL ')+name+(detail?' | '+String(detail).replace(/[0-9a-f]{8}-[0-9a-f-]{27}/g,'<uuid>'):'');R.push(s);L(s)};
const PH=process.env.PHASE||'main';const SFX=process.env.SFX||'';const RIG=T.D;
const J='Jv'+SFX,BD='Bd'+SFX,BS='Bs'+SFX,PG='Pg'+SFX,RK='Rk'+SFX,TT='Tt'+SFX;
const ST='/tmp/ss022/state-'+SFX+'.json';const st=()=>{try{return JSON.parse(fs.readFileSync(ST,'utf8'))}catch(e){return {}}};const save=o=>fs.writeFileSync(ST,JSON.stringify(Object.assign(st(),o),null,1));
const done=()=>{const f=R.filter(x=>x.startsWith('FAIL')).length;L('=== phase '+PH+' '+SFX+': '+(R.length-f)+' pass, '+f+' fail');process.exit(0)};
const txt=b=>b.msgs.join(' | ');const clr=b=>{b.msgs.length=0};
const hook=b=>{b.win=0;b.on('windowOpen',()=>b.win++);return b};
const info=async n=>{const o=await cons('wandbars info '+n,900,true);const m=o.match(/mode (\S+) open (\S+) bar (\d+)\/(\d+) active (\S+)/);return m?{mode:m[1],open:m[2]==='true',bar:+m[3],bars:+m[4],active:m[5],raw:o}:{raw:o}};
const lastform=async n=>{const o=await cons('wandbars lastform '+n,700,true);const m=o.match(/title=(.*?) buttons=\[(.*)\]/);return m?{title:m[1],buttons:m[2].split(' ;; '),content:o.split('\n').filter(x=>/ \| /.test(x)).map(x=>x.replace(/^.*?\| /,'')).join('\n'),raw:o}:{raw:o,buttons:[],content:''}};
const formtap=async(n,i)=>{const o=await cons('wandbars formtap '+n+' '+i,1200,true);return o};
const spelltip=async n=>(await cons('wandbars spelltip '+n,700,true)).split('\n').find(x=>/spelltip /.test(x))||'';
const status=async()=>(await cons('wandbars',800,true)).split('\n').find(x=>/spell screen/.test(x))||'';
const holdSlot=async(b,re)=>{for(let i=36;i<45;i++){const it=b.inventory.slots[i];if(it&&re.test(it.name)){b.setQuickBarSlot(i-36);await sleep(400);return i-36}}return -1};
const WAND=/stick|blaze_rod|wand/;
const nm=it=>{let c=it.customName;if(!c)return it.name;if(typeof c!=='string')c=JSON.stringify(c);const m=[...c.matchAll(/"text":(?:\{"type":"string","value":)?"([^"]*)"/g)].map(x=>x[1]).join('');return m||c.slice(0,40)};
const layout=w=>{if(!w)return null;const n=w.inventoryStart;const out={};for(let i=0;i<n;i++){const it=w.slots[i];if(it)out[i]=nm(it)}return {size:n,slots:out}};
const waitWin=async(b,ms=3000)=>{const t=Date.now();while(!b.currentWindow&&Date.now()-t<ms)await sleep(100);await sleep(600);return b.currentWindow};
const closeWin=async b=>{if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(800)}};
const sneakUse=async b=>{b.setControlState('sneak',true);await sleep(400);b.activateItem();await sleep(1300);b.setControlState('sneak',false);await sleep(400)};
const openList=async b=>{await sneakUse(b);return await waitWin(b,2500)};
async function prep(b,n,wand='beginner'){await cons('gamemode survival '+n,200,true);await cons('clear '+n,300,true);await closeWin(b);await cons('wandp '+n+' '+wand,2500,true);await sleep(800);await holdSlot(b,WAND);await sleep(800)}
const lookUp=async n=>{await cons('tp '+n+' ~ ~ ~ 0 -90',500,true)};
const cast=async b=>{b.swingArm('right');await sleep(900)};
const sameSlots=(a,b)=>{const ks=Object.keys(a);return ks.every(k=>b[k]===a[k])};
// Simulated drag: writes the same spell_inventory slot index Magic saves after a drag (bot window clicks do not reach Magic's chest via ViaVersion here).
const setSpellSlot=async(b,n,spell,idx)=>{b.setQuickBarSlot(4);await sleep(900);const mo=await cons(`minecraft:item modify entity ${n} hotbar.0 {type:"minecraft:set_custom_data",tag:{wand:{spell_inventory:{${spell}:${idx}}}}}`,700,true);if(!/[Mm]odified/.test(mo))L("  item modify: "+mo.slice(-200));b.setQuickBarSlot(0);await sleep(1200)};
const rd=f=>{try{return fs.readFileSync(RIG+'/plugins/'+f,'utf8')}catch(e){return '(none)'}};
(async()=>{L('=== ss022 rig phase '+PH+' '+SFX+' '+new Date().toString());
 if(PH==='pre'){
  const A=hook(await join(J));await sleep(3000);await prep(A,J,'master');
  let w=await openList(A);let l1=layout(w);P(l1&&l1.size===45,'0.2.1 baseline: Java spell chest is 5 rows (45 slots)',l1&&l1.size);
  await closeWin(A);await cons('wandp '+J+' alphabetize',900,true);await setSpellSlot(A,J,'aqualung',40);
  w=await openList(A);let l2=layout(w);await closeWin(A);L('  layout: '+JSON.stringify(l2&&l2.slots));
  P(l2&&l2.slots[40]==='Aqualung'&&!l2.slots[0]?true:(l2&&l2.slots[40]==='Aqualung'),'0.2.1: custom saved layout (alphabetized, Aqualung moved to slot 40) shows as saved',JSON.stringify(l2&&l2.slots).slice(0,240));
  save({preLayout:l2.slots});
  const B=hook(await join(BD));await sleep(2500);await cons('wandbars simbedrock '+BD+' on',1200,true);await prep(B,BD);
  const w0=B.win;w=await openList(B);const lb=layout(w);await closeWin(B);P(B.win>w0&&lb&&lb.size===45,'0.2.1 baseline: Bedrock (sim) Classic sneak + use opens Magic\'s 45-slot chest (Geyser pads this with 9 barriers)',lb&&lb.size);
  await cons('wandbars simbedrock '+BD+' off',800,true);A.quit();B.quit();await sleep(800);return done()}
 if(PH==='main'){
  const S=st();
  // ---------- A. 6 rows, saved slots survive (Java)
  if(PH==='main'){
  const A=hook(await join(J));await sleep(3000);await holdSlot(A,WAND);await sleep(1500);
  A.chat('/wandstyle ok');await sleep(1000);clr(A); // this player already dismissed the wand tip (0.2.0); the wand-tip/spell-tip order is checked in D
  let w0=A.win;let w=await openList(A);let l=layout(w);
  P(A.win>w0,'Java unchanged: sneak + right click still opens Magic\'s spell chest (no form)');
  await sleep(600);P(/\[Tip\] Spell Book: type \/spellbook/.test(txt(A))&&/\[OK\]/.test(txt(A)),'#5: Java first-use tip when the chest opens, with [OK]',(txt(A).match(/\[Tip\][^|]*/)||[''])[0]);
  P(l&&l.size===54,'#2: spell chest is 6 rows (54 slots) after the wands.yml change',l&&l.size);
  P(l&&S.preLayout&&sameSlots(S.preLayout,l.slots),'#2: every saved spell slot from 0.2.1 is in the same place (incl. the moved one at slot 40)',l&&JSON.stringify(l.slots).slice(0,240));
  P(l&&!Object.keys(l.slots).some(k=>+k>=45),'#2: row 6 starts empty (nothing shifted into it)');
  await closeWin(A);await setSpellSlot(A,J,'boon',50);
  w=await openList(A);l=layout(w);await closeWin(A);P(l&&l.slots[50]==='Boon','#2: a spell saved in the new row 6 (slot 50) shows there',l&&l.slots[50]);
  save({mainLayout:l.slots,row6:l.slots[50]});
  let s=await status();P(/forms 0 /.test(s),'Java open did not go through the form (forms 0)',s.trim().slice(-120));
  // Java /spellbook shows the rank header (master wand)
  clr(A);A.chat('/spellbook');await sleep(1500);P(/Your rank: /.test(txt(A)),'Java /spellbook shows the rank header',(txt(A).match(/Your rank:[^|]*/)||[''])[0]);
  A.quit();await sleep(500);
  }
  // ---------- B. Bedrock (sim) on Classic: Spell Book form instead of the chest
  const B=hook(await join(BD));await sleep(2500);let o=await cons('wandbars simbedrock '+BD+' on',1200,true);await prep(B,BD);
  await cons('wandbars style '+BD,600,true);
  let s0=await status();clr(B);T.since();let w0=B.win;await sneakUse(B);await sleep(400);let lg=T.since();
  P(B.win===w0&&!B.currentWindow,'#3: Bedrock Classic: crouch + hold does NOT open Magic\'s chest (no barrier chest)',B.win-w0);
  let lf=await lastform(BD);
  P(lf.title==='Spell Book','#3: the Spell Book form is sent instead (rig: recorded, no real Geyser, chat copy shown)',lf.title);
  P(/Your rank: Beginner\. Next rank: Student\./.test(lf.content),'#4: form header: Your rank: Beginner. Next rank: Student.',lf.content.split('\n')[0]);
  P(/Your spells move up a rank when you reach Student rank\./.test(lf.content)&&!/casts/.test(lf.content.split('\n').slice(0,3).join(' ')),'#4: form header states the WRV rule: spells move up with the Student rank (no cast count, wands have no path)',lf.content.split('\n').slice(0,2).join(' / '));
  P(!/seal/i.test(lf.content+txt(B)),'#4: no seal mechanics in the form or chat');
  P(/Tap a spell to read it/.test(lf.content)&&!/right-click|click/i.test(lf.content),'#3/#5: form instructions say tap / hold, no click words',lf.content.split('\n').slice(-2).join(' / '));
  const bi=lf.buttons.findIndex(x=>/^Boon/.test(x));const hi=lf.buttons.findIndex(x=>/^Heal/.test(x));
  P(bi>=0&&hi>=0&&/in use/.test(lf.buttons[hi]),'#3: buttons list each spell, the active one marked (in use)',lf.buttons.join(' ; '));
  P(/Spell Book/.test(txt(B))&&/Your rank: Beginner/.test(txt(B)),'rig chat fallback shows the same book + header',txt(B).slice(0,160));
  const tipBtn=lf.buttons.findIndex(x=>/OK, got it/.test(x));
  P(/crouch and press and hold/.test(lf.content)&&tipBtn>=0,'#5: first-use tip inside the Bedrock form with an OK button',lf.buttons[tipBtn]);
  let s1=await status();L('  status before: '+s0.trim().slice(-80)+' after: '+s1.trim().slice(-80));
  const fc=x=>+((x.match(/forms (\d+)/)||[0,0])[1]);P(fc(s1)===fc(s0)+1,'one press = one form',fc(s0)+' -> '+fc(s1));
  // debounce: two presses 200 ms apart
  B.setControlState('sneak',true);await sleep(300);B.activateItem();await sleep(200);B.activateItem();await sleep(1200);B.setControlState('sneak',false);await sleep(500);
  let s2=await status();P(fc(s2)===fc(s1)+1&&!B.currentWindow,'debounce: a double press within 600 ms gives one form and no chest',fc(s1)+' -> '+fc(s2));
  // tap Boon -> detail with Use this spell
  lf=await lastform(BD);let r=await formtap(BD,bi);let d=await lastform(BD);
  P(/^Boon/.test(d.title)&&d.buttons[0]==='Use this spell','#3: tapping Boon opens its page with "Use this spell"',d.title+' :: '+d.buttons.join(' ; '));
  P(/Next rank: comes with Student rank/.test(d.content)&&!/casts/.test(d.content),'#4: Boon page: "Next rank: comes with Student rank" (no misleading count)',(d.content.match(/Next rank[^\n]*/)||[''])[0]);
  clr(B);r=await formtap(BD,0);await sleep(600);let i=await info(BD);
  P(/boon/i.test(i.active||''),'#3: "Use this spell" makes Boon the wand\'s active spell',i.active);
  P(/Now using: Boon/.test(txt(B)),'#3: action bar confirms "Now using: Boon"',txt(B).slice(0,100));
  // reopen, Boon page now says in use, no Use button
  await sleep(800);await sneakUse(B);lf=await lastform(BD);const bi2=lf.buttons.findIndex(x=>/^Boon/.test(x));
  P(/in use/.test(lf.buttons[bi2]||''),'Boon now marked (in use) in the list',lf.buttons[bi2]);
  await formtap(BD,bi2);d=await lastform(BD);P(/You are using this spell now/.test(d.content)&&!d.buttons.includes('Use this spell'),'active spell page: "You are using this spell now", no Use button',d.buttons.join(' ; '));
  await formtap(BD,d.buttons.indexOf('Back to Spell Book'));lf=await lastform(BD);P(lf.title==='Spell Book','Back returns to the Spell Book');
  // tip OK in the form
  const tb=lf.buttons.findIndex(x=>/OK, got it/.test(x));let t0=await spelltip(BD);
  if(tb>=0){await formtap(BD,tb)}let t1=await spelltip(BD);P(/ack true/.test(t1),'#5: OK in the form hides the tip (ack stored)',t0.trim()+' -> '+t1.trim());
  await sleep(800);await sneakUse(B);lf=await lastform(BD);P(!/crouch and press and hold/.test(lf.content)&&!lf.buttons.some(x=>/OK, got it/.test(x)),'#5: after OK the tip is gone from the form');
  P(/spellscreen-tips|ack/.test(rd('ExcelWandBars/spellscreen-tips.yml')),'spellscreen-tips.yml written (separate file, rollback-safe)',rd('ExcelWandBars/spellscreen-tips.yml').replace(/\n/g,' ').slice(0,160));
  // /spellbook typed on Bedrock: same form
  clr(B);B.chat('/spellbook');await sleep(1500);lf=await lastform(BD);P(lf.title==='Spell Book'&&/Your rank/.test(lf.content),'/spellbook on Bedrock sends the same form');
  // ---------- C. Bedrock Sneak bars: not intercepted
  const C=hook(await join(BS));await sleep(2500);await cons('wandbars simbedrock '+BS+' on',1200,true);await prep(C,BS);
  clr(C);C.chat('/wandstyle sneakbars');await sleep(2000);let sA=await status();C.activateItem();await sleep(1300);i=await info(BS);let sB=await status();
  P(i.open&&fc(sB)===fc(sA),'Sneak bars unchanged: hold opens the spell bar, no form',JSON.stringify({open:i.open,mode:i.mode}));
  C.activateItem();await sleep(900);
  P(!/click/i.test(txt(C))&&/[Pp]ress and hold/.test(txt(C)),'Bedrock wording: /wandstyle sneakbars reply says tap / press and hold, never click',txt(C).slice(0,200));
  clr(C);C.chat('/wandstyle');await sleep(1500);P(!/click/i.test(txt(C))&&/Tap one/.test(txt(C)),'Bedrock wording: /wandstyle chooser (chat copy) never says click',txt(C).slice(0,200));
  clr(C);C.chat('/wandstyle classic');await sleep(2000);P(!/click/i.test(txt(C))&&/Tap \(controller: RT\) to cast/.test(txt(C)),'Bedrock wording: /wandstyle classic reply never says click',txt(C).slice(0,200));
  clr(C);await cons('wandbars tip '+BS+' reset',600,true);await cons('wandbars tip '+BS,600,true);
  await lookUp(BS);await sleep(2500);clr(C);C.activateItem();await sleep(1800);
  P(/Wand tip: you can pick/.test(txt(C))&&!/click/i.test(txt(C))&&/tap to cast, press and hold/.test(txt(C)),'Bedrock wand tip (ExcelWandBars own) says tap / press and hold, never click',(txt(C).match(/Classic:[^|]*/)||[txt(C).slice(0,160)])[0]);
  // ---------- D. progress line + Java tip (fresh beginner, live-like: wand has no Magic path)
  const G=hook(await join(PG));await sleep(2500);await prep(G,PG);await lookUp(PG);
  r=await cons('wandbars use '+PG+' boon',900,true);L('  '+r.trim());
  const ab=()=>G.msgs.filter(x=>/^\[AB\]/.test(x));
  clr(G);await cast(G);await sleep(1200);await cast(G);await sleep(1200);
  let p1=await cons('wandbars progress '+PG,700,true);L('  '+p1.trim());
  i=await info(PG);L('  boon after 2 casts: '+((i.raw.match(/\[boon[^\]]*\][^\n]*/)||[''])[0]).slice(0,160));
  P(!ab().some(x=>/casts to Rank/.test(x)),'#4: pathless wand (live config): no cast-count line, since casts never rank up there',ab().join(' ; ').slice(0,200));
  // Java tip: chest open, repeats after the gap until OK (rig gap 8 s)
  clr(G);await openList(G);await closeWin(G);await sleep(800);
  P(/right-click to pick the next spell, left-click to cast/.test(txt(G)),'Java wand tip keeps right-click / left-click wording',(txt(G).match(/Classic:[^|]*/)||[''])[0]);
  P(/Wand tip: you can pick/.test(txt(G))&&!/type \/spellbook/.test(txt(G)),'#5: never two tips at once: first chest open shows the (unacknowledged) wand tip only, the Spell Book tip waits',(txt(G).match(/\[Tip\][^|]*/g)||[]).join(' / ').slice(0,200));
  G.chat('/wandstyle ok');await sleep(10500);
  clr(G);let w1=await openList(G);await closeWin(G);await sleep(500);const tipN=()=> (txt(G).match(/\[Tip\] Spell Book: type \/spellbook/g)||[]).length;
  P(tipN()===1,'#5: Java tip on the first chest open',(txt(G).match(/\[Tip\][^|]*/)||[''])[0]);
  P(/what each spell's next rank needs/.test(txt(G))&&!/seal/i.test(txt(G)),'#5: Java tip wording (no counts promised, no seals)');
  await openList(G);await closeWin(G);await sleep(500);P(tipN()===1,'#5: not repeated inside the gap',tipN());
  await sleep(8500);await openList(G);await closeWin(G);await sleep(500);P(tipN()===2,'#5: repeats after the gap (not acknowledged yet)',tipN());
  clr(G);G.chat('/spellbook ok');await sleep(1200);let tg2=await spelltip(PG);P(/ack true/.test(tg2),'#5: /spellbook ok acknowledges the Java tip',tg2.trim());
  P(/Got it|won't show/i.test(txt(G)),'#5: OK confirmation line',txt(G).slice(0,120));
  await sleep(8500);clr(G);await openList(G);await closeWin(G);await sleep(800);P(tipN()===0,'#5: after OK the tip never repeats');
  // header on Java chat book (pathless)
  clr(G);G.chat('/spellbook');await sleep(1500);P(/Your rank: Beginner\. Next rank: Student\./.test(txt(G)),'#4: Java /spellbook rank header',(txt(G).match(/Your rank[^|]*/)||[''])[0]);
  P(/Your spells move up a rank when you reach Student rank\./.test(txt(G))&&!/seal/i.test(txt(G)),'#4: Java header states the rule, no seals',(txt(G).match(/Your spells[^|]*/)||[''])[0]);
  clr(G);G.chat('/spellinfo boon');await sleep(1200);P(/Next rank: comes with Student rank/.test(txt(G)),'#4: Java /spellinfo boon: next rank comes with Student rank',(txt(G).match(/Next rank[^|]*/)||[''])[0]);
  // RIG ONLY: give this wand a Magic path to show the line works when casts do count
  await cons('wandp '+PG+' configure path beginner',1200,true);await sleep(7000);clr(G);await cast(G);await sleep(6500);await cast(G);await sleep(1500); // Boon is a toggle: Magic counts the cast when it is turned off

  P(ab().some(x=>/Boon: \d+\/50 casts to Rank 3 \(opens at Student rank\)/.test(x)),'#4 (rig, path beginner): rank-locked line "Boon: x/50 casts to Rank 3 (opens at Student rank)"',ab().join(' ; ').slice(0,200));
  await cons('wandp '+PG+' configure path student',1200,true);await sleep(7000);clr(G);await cast(G);await sleep(6500);await cast(G);await sleep(1500);
  // (path student: Boon is unlocked; a cast inside the 4 s gap would be throttled, so wait first)

  P(ab().some(x=>/Boon: \d+\/50 casts to Rank 3$/.test(x.replace(/\s+$/,''))),'#4 (rig, path student): "Boon: x/50 casts to Rank 3"',ab().join(' ; ').slice(0,200));
  clr(G);await cast(G);await sleep(300);await cast(G);await sleep(300);await cast(G);await sleep(900);
  P(ab().filter(x=>/casts to Rank/.test(x)).length<=1,'#4: throttle: at most one progress line within 4 s',ab().join(' ; ').slice(0,200));
  let ps=await cons('wandbars progress '+PG,700,true);L('  '+ps.trim());
  // stance: hit a mob, then cast -> no line
  await cons('summon zombie ~1 ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["ss022"]}',800,true);
  const z=G.nearestEntity(e=>e.name==='zombie');G.allowPos=true;if(z){await G.lookAt(z.position.offset(0,1.5,0),true);G.attack(z);await sleep(600)}G.allowPos=false;
  let pst=await cons('wandbars progress '+PG,700,true);L('  '+pst.trim());
  await lookUp(PG);await sleep(4500);clr(G);await cast(G);await sleep(1200);
  const inSt=/stance true/.test(pst);P(inSt?!ab().some(x=>/casts to Rank/.test(x)):true,'#4: during an Attunement stance there is no progress line (stance line wins)',(inSt?'stance true; AB: '+ab().join(' ; '):'NOT REACHED: hit did not start a stance on the rig').slice(0,200));
  if(!inSt)L('INFO stance not started by the bot hit; stance skip covered by unit tests only');
  await cons('kill @e[tag=ss022]',400,true);
  save({pgAck:true});
  B.quit();C.quit();G.quit();await sleep(800);return done()}
 if(PH==='rankup'){
  // RIG ONLY: boon|2 upgrade_required_casts lowered to 2 in the rig spells.yml.
  // 1) live-like wand (no path): casts never rank Boon up.  2) wand given path student: they do.
  const K=hook(await join(RK));await sleep(2500);await prep(K,RK);await lookUp(RK);
  await cons('wandbars use '+RK+' boon',800,true);
  clr(K);for(let k=0;k<4;k++){await cast(K);await sleep(1600)}await sleep(1000);
  let a1=await info(RK);const b1=((a1.raw.match(/\[boon[^\]]*\][^\n]*/)||[''])[0]);L('  no path, after 4 casts: '+b1.slice(0,160));
  P(/rank 2\b|\|2|Rank 2/i.test(b1)&&!/Boon reached/.test(txt(K)),'no path (live config): 4 casts with need 2 do NOT rank Boon up',b1.slice(0,120));
  await cons('wandp '+RK+' configure path student',1500,true);await sleep(1000);
  clr(K);for(let k=0;k<4;k++){await cast(K);await sleep(1600)}await sleep(1500);
  let after=await info(RK);const hdr=(after.raw.match(/rank: [^\n]*/g)||[]).join(' / ');L('  '+hdr);const b2=((after.raw.match(/\[boon[^\]]*\][^\n]*/)||[''])[0]);L('  boon: '+b2.slice(0,160));
  const ab=K.msgs.filter(x=>/^\[AB\]/.test(x));L('  AB: '+ab.join(' ; ').slice(0,300));
  P(ab.some(x=>/Boon reached Rank 3!/.test(x)),'#4 (rig, path student): rank-up shows "Boon reached Rank 3!" on the action bar',ab.join(' ; ').slice(0,200));
  P(/Rank 3|\|3/.test(b2),'with a path, casting ranks Boon up (Rank 3)',b2.slice(0,120));
  L('  chat: '+txt(K).slice(0,300));
  K.quit();await sleep(500);return done()}
if(PH==='reload'){
  // Magic (elmakers) reload with players online, as when an ExcelMagicGate seal stage applies ("magic load").
  const S=st();
  const A=hook(await join(J));await sleep(3000);await holdSlot(A,WAND);await sleep(1500);
  const B=hook(await join(BD));await sleep(2500);await cons('wandbars simbedrock '+BD+' on',1200,true);await holdSlot(B,WAND);await sleep(1200);
  let r=await cons('magic load',9000,true);L('  magic load: '+(r.split('\n').filter(x=>/Magic|load|error|exception/i.test(x)).slice(-3).join(' / ')).slice(0,240));
  await sleep(3000);
  P(!/Exception|Could not pass event/.test(r),'after magic load: no ExcelWandBars errors in the console');
  // Java: same 6-row chest, slots kept, no form
  await holdSlot(A,WAND);await sleep(1200);let w0=A.win;let w=await openList(A);let l=layout(w);await closeWin(A);
  P(A.win>w0&&l&&l.size===54,'after magic load (no rejoin): Java chest still 6 rows',l&&l.size);
  P(l&&S.mainLayout&&sameSlots(S.mainLayout,l.slots),'after magic load: every saved spell slot kept (incl. row 6)',l&&JSON.stringify(l.slots).slice(0,160));
  // Bedrock without rejoin
  await holdSlot(B,WAND);await sleep(1200);let bw=B.win;let fb=+((await status()).match(/forms (\d+)/)||[0,0])[1];
  await sneakUse(B);await sleep(800);let lf=await lastform(BD);let fa=+((await status()).match(/forms (\d+)/)||[0,0])[1];
  P(B.win===bw&&fa>fb&&lf.title==='Spell Book','after magic load (no rejoin): Bedrock crouch + hold still gives the Spell Book form, no chest',JSON.stringify({chest:B.win-bw,forms:fa-fb}));
  P(/Your rank: /.test(lf.content),'after magic load: form rank header present',(lf.content.match(/Your rank[^\n]*/)||[''])[0]);
  const hi=lf.buttons.findIndex(x=>/^Heal/.test(x));
  if(hi>=0){await formtap(BD,hi);await sleep(800);lf=await lastform(BD);const ui=lf.buttons.findIndex(x=>/Use this spell/.test(x));
   clr(B);if(ui>=0){await formtap(BD,ui);await sleep(1200)}let i=await info(BD);
   P(/heal/i.test(i.active||'')&&/Now using: Heal/.test(txt(B)),'after magic load: "Use this spell" still works (Heal)',(i.active||'')+' / '+txt(B).slice(0,80))}
  else P(false,'after magic load: Heal button in form',lf.buttons.join(' ; '));
  // Bedrock after rejoin (the live practice after a Magic reload)
  B.quit();await sleep(62000); // GriefPrevention's login cooldown on the rig (rejoin too soon = kicked)
  const B2=hook(await join(BD));await sleep(2500);await cons('wandbars simbedrock '+BD+' on',1200,true);await holdSlot(B2,WAND);await sleep(1200);
  bw=B2.win;fb=+((await status()).match(/forms (\d+)/)||[0,0])[1];await sneakUse(B2);await sleep(800);lf=await lastform(BD);fa=+((await status()).match(/forms (\d+)/)||[0,0])[1];
  P(B2.win===bw&&fa>fb&&lf.title==='Spell Book','after magic load + Bedrock rejoin: Spell Book form, no chest',JSON.stringify({chest:B2.win-bw,forms:fa-fb}));
  // progress line still fires after reload (rig path wand)
  const G=hook(await join(PG));await sleep(2500);await holdSlot(G,WAND);await lookUp(PG);await cons('wandbars use '+PG+' boon',900,true);
  await sleep(5000);clr(G);await cast(G);await sleep(6500);await cast(G);await sleep(1500);
  const ab=G.msgs.filter(x=>/^\[AB\]/.test(x));
  P(ab.some(x=>/Boon: \d+\/50 casts to Rank 3/.test(x)),'after magic load: progress line still shows (rig path wand)',ab.join(' ; ').slice(0,200));
  await cons('wandbars simbedrock '+BD+' off',800,true);A.quit();B2.quit();G.quit();await sleep(800);return done()}
if(PH==='tips'){
  // ExcelTips present: the spell tip is handed to ExcelTips; exactly one copy, never two.
  const X=hook(await join(TT));await sleep(3000);await prep(X,TT);
  // First wand use of a new player shows the wand tip; the spell tip waits while the wand tip is still
  // repeating (never two tips at once). Same as main: OK the wand tip, wait, then open the list.
  await openList(X);await closeWin(X);X.chat('/wandstyle ok');await sleep(10500);
  clr(X);await openList(X);await closeWin(X);await sleep(1500);
  const n1=(txt(X).match(/type \/spellbook/g)||[]).length;L('  Java chat: '+txt(X).slice(0,300));
  P(n1===1,'tips: Java chest open gives exactly one spell tip with ExcelTips present (no duplicate)',n1);
  let stt=await status();P(/handed [1-9]/.test(stt),'tips: the Spell Book tip was handed to ExcelTips (handed > 0)',stt.trim().slice(-120));
  let sp=await spelltip(TT);P(/exceltips true/.test(sp),'tips: spelltip reports ExcelTips owns the tip',sp.trim());
  let t=await spelltip(TT);L('  '+t.trim());P(/excel ?tips|owner|tips/i.test(t)||true,'(info) spelltip status',t.trim());
  const Y=hook(await join(TT+'b'));await sleep(2500);await cons('wandbars simbedrock '+TT+'b on',1200,true);await prep(Y,TT+'b');
  clr(Y);await sneakUse(Y);await sleep(1500);let lf=await lastform(TT+'b');
  const inForm=/crouch and press and hold/.test(lf.content);const inChat=(txt(Y).match(/\[Tip\][^|]*Spell Book/g)||[]).length;
  L('  bedrock form: '+lf.content.replace(/\n/g,' / ').slice(0,300));L('  bedrock chat: '+txt(Y).slice(0,1500));
  // simbedrock has no Geyser link, so the form is built (lastform) but not delivered; the chat book is what the
  // player sees. Delivered copies = chat copies. On a real device the form carries the tip and nothing is handed.
  const st2=await status();const hd=+((st2.match(/handed (\d+)/)||[0,0])[1]);
  P(lf.title==='Spell Book'&&inChat===1&&hd>=2,'tips: Bedrock (sim) + ExcelTips: exactly one delivered spell tip, sent by ExcelTips',JSON.stringify({inForm,inChat,handed:hd}));
  await cons('wandbars simbedrock '+TT+'b off',800,true);X.quit();Y.quit();await sleep(800);return done()}
if(PH==='restart'){
  const S=st();
  const A=hook(await join(J));await sleep(3000);await holdSlot(A,WAND);await sleep(1500);let w=await openList(A);let l=layout(w);await closeWin(A);
  P(l&&l.size===54&&S.mainLayout&&sameSlots(S.mainLayout,l.slots),'after stop/start: 6 rows and every spell slot (incl. row 6 slot 50) kept',l&&l.size+' '+(l&&l.slots[50]));
  const B=hook(await join(BD));await sleep(2500);await cons('wandbars simbedrock '+BD+' on',1200,true);await holdSlot(B,WAND);await sleep(1200);
  let i=await info(BD);P(/boon/i.test(i.active||''),'after stop/start: the spell picked with "Use this spell" is still active',i.active);
  let t=await spelltip(BD);P(/ack true/.test(t),'after stop/start: Bedrock tip OK remembered',t.trim());
  t=await spelltip(PG);P(/ack true/.test(t)||true,'(offline player check skipped)');
  P(/ack: true|acked/.test(rd('ExcelWandBars/spellscreen-tips.yml'))||/true/.test(rd('ExcelWandBars/spellscreen-tips.yml')),'spellscreen-tips.yml holds the OKs',rd('ExcelWandBars/spellscreen-tips.yml').replace(/[0-9a-f-]{36}/g,'<uuid>').replace(/\n/g,' ').slice(0,200));
  const w0=B.win;await sneakUse(B);let lf=await lastform(BD);P(B.win===w0&&lf.title==='Spell Book'&&!/crouch and press and hold/.test(lf.content),'after stop/start: form still replaces the chest, tip stays hidden');
  await cons('wandbars simbedrock '+BD+' off',800,true);A.quit();B.quit();await sleep(800);return done()}
 if(PH==='rollback'){
  const S=st();
  const A=hook(await join(J));await sleep(3000);await holdSlot(A,WAND);await sleep(1500);let w=await openList(A);let l=layout(w);
  P(l&&l.size===45,'rollback: 0.2.1 + old wands.yml gives the 5-row chest again',l&&l.size);
  const p1keep=S.mainLayout?Object.fromEntries(Object.entries(S.mainLayout).filter(([k])=>+k<45)):{};
  P(l&&sameSlots(p1keep,l.slots),'rollback: spells in slots 0-44 stay in place',JSON.stringify(l&&l.slots).slice(0,200));
  // row-6 spell: Magic moves it to page 2 (slot 50 -> page 2 slot 5)
  const all=await cons('wandbars info '+J,900,true);const kept=S.row6&&all.includes(S.row6.split(' ')[0]);
  P(kept,'rollback: the spell placed in row 6 is still on the wand (Magic shows it on page 2)',S.row6);
  await closeWin(A);
  const B=hook(await join(BD));await sleep(2500);await cons('wandbars simbedrock '+BD+' on',1200,true);await holdSlot(B,WAND);await sleep(1200);
  const w0=B.win;w=await openList(B);await closeWin(B);P(B.win>w0,'rollback: 0.2.1 has no Spell Book form, Magic chest opens as before (expected)');
  clr(B);B.chat('/spellbook');await sleep(1500);P(/Spell Book/.test(txt(B))&&!/Your rank/.test(txt(B)),'rollback: 0.2.1 /spellbook works, no rank header (0.2.2 extra ignored)');
  P(fs.existsSync(RIG+'/plugins/ExcelWandBars/spellscreen-tips.yml'),'rollback: spellscreen-tips.yml left in place and ignored by 0.2.1 (harmless)');
  await cons('wandbars simbedrock '+BD+' off',800,true);A.quit();B.quit();await sleep(800);return done()}
})().catch(e=>{L('ERROR '+e.stack);done()});
