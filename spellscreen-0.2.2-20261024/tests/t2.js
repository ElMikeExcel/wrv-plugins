// v2 rig run: Spellbook button, Classic vs Sneak bars (Bedrock only), wand tip, AutoSort.
// PHASE: main | restart | rollback. Bots: Jv (Java), Bd (Bedrock simulated with /wandbars simbedrock), Tp (tip cap), Ob (observer).
const T=require('./lib');const {L,cons,sleep,join}=T;const fs=require('fs');const V=require('vec3');
const R=[];const P=(ok,name,detail)=>{const s=(ok?'PASS ':'FAIL ')+name+(detail?' | '+detail:'');R.push(s);L(s)};
const PH=process.env.PHASE||'main';const SFX=process.env.SFX||'';const RIG='/workspace/sbtn-rig';
const J='Jv'+SFX,BD='Bd'+SFX,TP='Tpc'+SFX,OB='Ob'+SFX;
const done=()=>{const f=R.filter(x=>x.startsWith('FAIL')).length;L('=== phase '+PH+': '+(R.length-f)+' pass, '+f+' fail');process.exit(0)};
const txt=b=>b.msgs.join(' | ');const clr=b=>{b.msgs.length=0};
const hook=b=>{b.win=0;b.on('windowOpen',()=>b.win++);return b};
const styleOf=async n=>{const o=await cons('wandbars style '+n,900,true);return (o.split('\n').find(x=>/style /.test(x))||o).trim()};
const info=async n=>{const o=await cons('wandbars info '+n,800,true);const m=o.match(/mode (\S+) open (\S+) bar (\d+)\/(\d+) active (\S+)/);return m?{mode:m[1],open:m[2]==='true',bar:+m[3],bars:+m[4],active:m[5]}:{raw:o.slice(0,200)}};
const tip=async n=>{const o=await cons('wandbars tip '+n,700,true);return (o.split('\n').find(x=>/tip /.test(x))||o).trim()};
const holdSlot=async(b,re)=>{for(let i=36;i<45;i++){const it=b.inventory.slots[i];if(it&&re.test(it.name)){b.setQuickBarSlot(i-36);await sleep(400);return i-36}}return -1};
const WAND=/stick|blaze_rod|wand/;
const layout=w=>{const out=[];if(!w)return 'nowin';const n=w.inventoryStart;for(let i=0;i<n;i++){const it=w.slots[i];if(it)out.push(i+':'+it.name+'x'+it.count)}return out.join(',')};
const waitWin=async(b,ms=3000)=>{const t=Date.now();while(!b.currentWindow&&Date.now()-t<ms)await sleep(100);await sleep(600);return b.currentWindow};
const closeWin=async b=>{if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(700)}};
const rd=f=>{try{return fs.readFileSync(RIG+'/plugins/'+f,'utf8')}catch(e){return '(none)'}};
const sneakPress=async(b,hold=250)=>{b.setControlState('sneak',true);await sleep(hold);b.setControlState('sneak',false);await sleep(450)};
const pos=async n=>{const o=await cons('data get entity '+n+' Pos',600,true);const m=/\[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]/.exec(o);return m?[Math.floor(+m[1]),Math.floor(+m[2]),Math.floor(+m[3])]:null};
const ITEMS='[{Slot:0b,id:"minecraft:diamond",count:1},{Slot:5b,id:"minecraft:dirt",count:10},{Slot:9b,id:"minecraft:stone",count:5},{Slot:20b,id:"minecraft:dirt",count:10}]';
const SCAT='0:diamondx1,5:dirtx10,9:stonex5,20:dirtx10',SORTED='0:diamondx1,1:dirtx20,2:stonex5';
let C=null;const spots=()=>[[C[0]+2,C[1],C[2],'chest'],[C[0]-2,C[1],C[2],'barrel'],[C[0],C[1],C[2]+2,'shulker_box'],[C[0],C[1],C[2]-2,'ender_chest']];
async function place(n){for(const [x,y,z,k] of spots()){await cons(`setblock ${x} ${y} ${z} air`,120,true);await cons(`setblock ${x} ${y} ${z} minecraft:${k}`+(k==='ender_chest'?'':`{Items:${ITEMS}}`),250,true)}
 for(let i=0;i<27;i++)await cons('minecraft:item replace entity '+n+' enderchest.'+i+' with minecraft:air',25,true);
 for(const [s,it] of [[0,'diamond 1'],[5,'dirt 10'],[9,'stone 5'],[20,'dirt 10']])await cons('minecraft:item replace entity '+n+' enderchest.'+s+' with minecraft:'+it,120,true);await sleep(500)}
// Opens go through the rig-only RigOpen plugin (Player.openInventory on the block's own inventory, so
// InventoryOpen/CloseEvent fire exactly as for a click): bot block clicks don't reach Paper 26.3 via ViaVersion here.
const rigOpen=async(b,x,y,z)=>{const o=await cons(`rigopen ${b.username} ${x} ${y} ${z}`,500,true);if(!/opened/.test(o))L('  rigopen: '+o.trim().slice(-120))};
async function openAt(b,x,y,z){await emptyHand(b);await rigOpen(b,x,y,z);const w=await waitWin(b);const l=layout(w);await closeWin(b);await sleep(400);return l}
async function round(b,label,expect){for(const [x,y,z,k] of spots()){const l=await openAt(b,x,y,z);P(l===expect,label+': '+k.replace('_',' ')+(expect===SORTED?' sorted':' left as it was'),l)}}
async function blockItems(x,y,z){const o=await cons(`data get block ${x} ${y} ${z} Items`,600,true);return o.replace(/\s+/g,' ').slice(-260)}
async function guiSnap(b,open){await open();const w=await waitWin(b,4000);const l=layout(w);await closeWin(b);return l}
async function prep(b,n,wand='beginner'){await cons('gamemode survival '+n,200,true);await cons('clear '+n,300,true);await closeWin(b);await cons('wandp '+n+' '+wand,2500,true);await sleep(800)}
const emptyHand=async b=>{for(let i=36;i<45;i++){if(!b.inventory.slots[i]){b.setQuickBarSlot(i-36);await sleep(300);return i-36}}return -1};
(async()=>{L('=== sbtn v2 rig phase '+PH+' '+new Date().toString());
 if(PH==='restart'){
  const A=hook(await join(J));const B=hook(await join(BD));await sleep(6000);
  P(/[0-9a-f-]{36}: bars/.test(rd('ExcelWandBars/styles.yml')),'styles.yml still holds the Sneak bars choice after a full restart',rd('ExcelWandBars/styles.yml').split('\n').filter(x=>/: /.test(x)).join(' / ').replace(/[0-9a-f-]{36}/g,'<uuid>'));
  await holdSlot(B,WAND);await sleep(1500);let s=await styleOf(BD);
  P(/mode=chest/.test(s)&&/: classic/.test(s),'after restart, before simbedrock: the saved-bars player is on Java here, so Classic is on the wand',s.slice(-170));
  let o=await cons('wandbars simbedrock '+BD+' on',2500,true);s=await styleOf(BD);P(/mode=inventory/.test(s)&&/matches=true/.test(s)&&/: bars/.test(s),'as Bedrock again: Sneak bars comes back from the saved choice (no re-pick)',s.slice(-170));
  B.activateItem();await sleep(1200);let i0=await info(BD);await sneakPress(B);let i1=await info(BD);P(i0.open&&i1.bar===(i0.bar%i0.bars)+1,'after restart: sneak flips the bar',JSON.stringify([i0.bar,i1.bar,i0.bars]));B.activateItem();await sleep(1000);
  const t=await tip(J);P(/ack true/.test(t),'tip acknowledgement survives the restart',t);
  const t2=await tip(TP);L('  '+t2);
  const uc=JSON.parse(fs.readFileSync(RIG+'/usercache.json','utf8'));const ju=(uc.find(e=>e.name===J)||{}).uuid;P(!!ju&&rd('autosort/active.txt').split('\n').map(x=>x.trim()).includes(ju),'AutoSort active.txt still holds the Java player after restart (toggle persists)');
  C=await pos(J);await place(J);await round(A,'after restart, AutoSort kept ON for the Java player',SORTED);
  await cons('wandbars simbedrock '+BD+' off',1500,true);
  A.quit();B.quit();await sleep(800);return done()}
 if(PH==='rollback'){
  const B=hook(await join(BD));await sleep(6000);await holdSlot(B,WAND);await sleep(1500);
  let s=await styleOf(BD);L('  0.2.0 style: '+s);
  P(/bars/.test(s),'WandBars 0.2.0 back: a saved Sneak bars choice is plain "bars" to 0.2.0 (applies to that player on any platform, as in live 0.2.0)',s.slice(-150));
  clr(B);B.chat('/wandstyle classic');await sleep(2200);s=await styleOf(BD);P(/mode=chest/.test(s)&&/matches=true/.test(s),'0.2.0: /wandstyle classic restores classic',s.slice(-150));
  clr(B);B.chat('/wandstyle ok');await sleep(1200);P(/Use \/wandstyle classic or \/wandstyle bars/.test(txt(B)),'0.2.0 ignores the tip (no tip, /wandstyle ok unknown arg): expected after rollback',txt(B).slice(0,120));
  B.quit();await sleep(800);return done()}
 // ================= main
 const A=hook(await join(J));const B=hook(await join(BD));const O=hook(await join(OB));await sleep(4000);
 await prep(A,J);await prep(B,BD,'master');await prep(O,OB,'master');
 C=await pos(J);L('  spot '+C);
 // ---------- 1. Spellbook (Java) with the compass in hand
 clr(A);A.chat('/compass');await sleep(1500);let cs=await holdSlot(A,/^compass$/);L('  compass slot '+cs+' '+txt(A).slice(0,100));
 clr(A);A.chat('/spellinfo');await sleep(1500);P(/Spell Book/.test(txt(A))&&!/No Magic wand found/.test(txt(A)),'/spellinfo with the compass in hand (wand in inventory) shows the Spell Book',txt(A).slice(0,140));
 clr(A);A.chat('/spellbook');await sleep(1500);P(/Spell Book/.test(txt(A)),'/spellbook alias');
 clr(A);A.activateItem();let w=await waitWin(A,4000);const ml=layout(w);P(/20:writable_book/.test(ml)&&/21:hopper/.test(ml),'compass opens the Java server menu with Spellbook (20) and Auto-sort (21)',ml.slice(-140));
 P(!/Wand controls/i.test(w?JSON.stringify(w.slots.filter(x=>x).map(x=>x.customName||x.name)):''),'Java menu has no Wand controls button (Sneak bars is Bedrock only)');
 if(w){clr(A);await A.clickWindow(20,0,0);await sleep(1800)}P(!A.currentWindow&&/Spell Book/.test(txt(A)),'menu Spellbook click closes the menu and shows the Spell Book',txt(A).slice(0,120));await closeWin(A);
 P((A.heldItem&&A.heldItem.name)==='compass','compass still in hand');
 await cons('minecraft:clear '+OB,500,true);await sleep(400);clr(O);O.chat('/spellinfo');await sleep(1500);P(/No Magic wand found/.test(txt(O)),'no wand anywhere: hint mentions inventory and Utility Pouch',txt(O).slice(0,140));
 await cons('wandp '+OB+' beginner',2500,true);
 // ---------- 2. wand tip (Java), before any style pick
 await holdSlot(A,WAND);await sleep(1200);await cons('wandbars tip '+J+' reset',500,true);
 clr(A);A.activateItem();await sleep(1500);let t=txt(A);
 P(/Wand tip: you can pick how your wand works!/.test(t)&&/Classic: right-click to pick the next spell, left-click to cast\./.test(t)&&/Click \[OK\] to hide this tip\./.test(t),'Java: first wand use shows the tip with [OK]',t.slice(0,300));
 P(!/Sneak bars/.test(t),'Java tip has no Sneak bars line');P(/\/wandstyle/.test(t)&&/compass menu/.test(t),'Java tip mentions /wandstyle and the compass menu');
 clr(A);A.activateItem();await sleep(700);P(!/Wand tip/.test(txt(A)),'a second click within 2 s is the same use (no repeat)');
 await sleep(1600);clr(A);A.activateItem();await sleep(1200);P(/Wand tip/.test(txt(A)),'next use after 2 s shows it again (not acknowledged yet)');
 t=await tip(J);P(/uses 2\/15 ack false/.test(t),'count 2/15 stored, not acknowledged',t);
 P(/uses: 2/.test(rd('ExcelWandBars/tips.yml')),'tips.yml written (persistent)');
 clr(A);A.chat('/wandstyle ok');await sleep(1200);P(/Got it! The wand tip won't show again/.test(txt(A)),'[OK] (runs /wandstyle ok) acknowledges',txt(A).slice(0,120));
 await sleep(2200);clr(A);A.activateItem();await sleep(1200);P(!/Wand tip/.test(txt(A)),'after OK: no more tips');t=await tip(J);P(/ack true/.test(t)&&/uses 2\//.test(t),'ack stored, count frozen',t);
 // ---------- 3. Java stays Classic
 let s=await styleOf(J);P(/: classic/.test(s)&&/mode=chest/.test(s)&&/right_click=cycle/.test(s)&&/right_click_sneak=toggle/.test(s)&&/matches=true/.test(s),'Java default: Classic (chest, right click cycle, sneak+right click list)',s.slice(-200));
 let a0=await info(J);A.activateItem();await sleep(1100);let a1=await info(J);P(a0.active&&a1.active&&a0.active!==a1.active,'Java Classic: right click picks the next spell',a0.active+' -> '+a1.active);
 a0=await info(J);let w0=A.win;await sneakPress(A,600);a1=await info(J);P(A.win===w0&&a0.active===a1.active&&!a1.open,'Java Classic: plain sneak does nothing to the wand',JSON.stringify(a1));
 w0=A.win;A.setControlState('sneak',true);await sleep(400);A.activateItem();await sleep(1200);A.setControlState('sneak',false);await sleep(400);P(A.win>w0,'Java Classic: sneak + right click opens the spell list');await closeWin(A);
 clr(A);A.chat('/wandstyle sneakbars');await sleep(1800);P(/Sneak bars are for Bedrock players/.test(txt(A)),'Java: /wandstyle sneakbars is refused with a friendly line',txt(A).slice(0,160));
 s=await styleOf(J);P(/: classic/.test(s)&&/mode=chest/.test(s)&&/\(default\)/.test(s),'Java: refusal saves nothing, wand unchanged',s.slice(0,120));
 clr(A);A.chat('/wandstyle');await sleep(1500);P(/Wand controls: Classic \(Java\)/.test(txt(A))&&!/\[Sneak bars\]/.test(txt(A)),'Java /wandstyle shows Classic only (no Sneak bars button)',txt(A).slice(0,200));
 clr(A);A.chat('/wandstyle switch');await sleep(1500);P(/Sneak bars are for Bedrock/.test(txt(A)),'Java /wandstyle switch also refused');
 // ---------- 4. Bedrock (simulated): tip, pick Sneak bars, flips, switch back
 let o=await cons('wandbars simbedrock '+BD+' on',1500,true);P(/bedrock=true, real=false/.test(o),'simbedrock marks the test player as Bedrock (real Geyser flag false)',o.split('\n').find(x=>/simbedrock/.test(x)));
 const refClassic=(await styleOf(OB));
 await holdSlot(B,WAND);await sleep(1200);
 await cons('wandbars tip '+BD+' reset',400,true);T.since();clr(B);B.activateItem();await sleep(2600);t=txt(B);const lg=T.since();
 P(/Sneak bars \(Bedrock\): right-click to open your spell bar, tap a spell to cast, sneak to flip to the next bar\./.test(t)&&/Pick one with \/wandstyle or the Wand controls button in the compass menu\./.test(t)&&/Tap OK on the pop-up, or type \/wandstyle ok, to hide this tip\./.test(t),'Bedrock: tip has the Sneak bars line, compass button and /wandstyle ok',t.slice(0,400));
 P(!/\[OK\]/.test(t),'Bedrock tip has no clickable [OK] (does not work over Geyser)');
 P(/Wand tip pop-up for /.test(lg),'Bedrock: OK pop-up attempted (rig: no real Geyser connection, so "not sent")',(lg.split('\n').find(x=>/pop-up/.test(x))||'').replace(/\x1b\[[0-9;]*m/g,'').trim().slice(-90));
 await sleep(2200);T.since();clr(B);B.activateItem();await sleep(2600);const lg2=T.since();P(/Wand tip/.test(txt(B))&&!/Wand tip pop-up/.test(lg2),'Bedrock: chat tip repeats, pop-up not repeated in the same session');
 s=await styleOf(BD);P(/: classic/.test(s)&&/mode=chest/.test(s),'Bedrock default is Classic too',s.slice(-120));
 // classic on Bedrock: sneak does nothing
 a0=await info(BD);await sneakPress(B,500);a1=await info(BD);P(a0.active===a1.active&&!a1.open,'Bedrock Classic: plain sneak does nothing (controller players keep classic)',JSON.stringify(a1));
 clr(B);B.chat('/wandstyle');await sleep(1500);P(/\[Sneak bars\]/.test(txt(B))&&/\[Classic\]/.test(txt(B)),'Bedrock chooser offers Classic and Sneak bars (rig shows the chat fallback; real Bedrock gets the form)',txt(B).slice(0,160));
 clr(B);B.chat('/wandstyle sneakbars');await sleep(2500);t=txt(B);P(/Wand controls: Sneak bars\./.test(t)&&/Sneak once to flip to the next bar/.test(t),'Bedrock: pick Sneak bars',t.slice(0,260));
 let tb=await tip(BD);P(/ack true/.test(tb),'picking a style acknowledges the tip',tb);
 s=await styleOf(BD);P(/: bars/.test(s)&&/mode=inventory/.test(s)&&/right_click=toggle/.test(s)&&/quick_cast=true/.test(s)&&/matches=true/.test(s),'Sneak bars on the wand (inventory mode, quick cast)',s.slice(-200));
 P(/[0-9a-f-]{36}: bars/.test(rd('ExcelWandBars/styles.yml')),'saved in styles.yml');
 let i0=await info(BD);P(i0.bars>=2,'test wand has '+i0.bars+' bars',JSON.stringify(i0));
 // bar closed: sneak does nothing
 await sneakPress(B);let i1=await info(BD);P(!i1.open&&i1.bar===i0.bar,'Sneak bars, bar closed: sneak (moving) does not open or flip anything',JSON.stringify(i1));
 B.activateItem();await sleep(1200);i0=await info(BD);P(i0.open,'right click opens the spell bar',JSON.stringify(i0));
 const seq=[i0.bar];for(let k=0;k<i0.bars;k++){await sneakPress(B);seq.push((await info(BD)).bar)}
 const want=[i0.bar];for(let k=0;k<i0.bars;k++)want.push((want[want.length-1]%i0.bars)+1);
 P(JSON.stringify(seq)===JSON.stringify(want),'each sneak press goes to the next bar and wraps around',seq.join('->'));
 i0=await info(BD);B.setControlState('sneak',true);await sleep(2500);i1=await info(BD);B.setControlState('sneak',false);await sleep(500);let i2=await info(BD);
 P(i1.bar===(i0.bar%i0.bars)+1&&i2.bar===i1.bar,'holding sneak (sneak-walking) flips once, release does not flip',[i0.bar,i1.bar,i2.bar].join('->'));
 i0=await info(BD);B.setControlState('sneak',true);await sleep(60);B.setControlState('sneak',false);await sleep(60);B.setControlState('sneak',true);await sleep(60);B.setControlState('sneak',false);await sleep(600);i1=await info(BD);
 P(i1.bar===(i0.bar%i0.bars)+1,'double tap within 250 ms = one flip (debounce)',i0.bar+'->'+i1.bar);
 B.activateItem();await sleep(1000);
 // switch back to classic
 clr(B);B.chat('/wandstyle classic');await sleep(2500);s=await styleOf(BD);
 const norm=x=>x.replace(/^.*?wand /,'').replace(/^\S+ /,'').replace(/ saved=\S+ bedrock=\S+/,'').replace('quick_cast=null','quick_cast=false');
 P(/: classic/.test(s)&&norm(s)===norm(refClassic),'back to Classic: wand controls identical to a never-switched player',norm(s)+' VS '+norm(refClassic));
 a0=await info(BD);B.activateItem();await sleep(1100);a1=await info(BD);P(a1.mode==='CHEST'&&a0.active!==a1.active&&!a1.open,'Classic again: right click picks the next spell',a0.active+' -> '+a1.active);
 a0=await info(BD);await sneakPress(B,500);a1=await info(BD);P(a0.active===a1.active&&!a1.open,'Classic again: plain sneak does nothing');
 w0=B.win;B.setControlState('sneak',true);await sleep(400);B.activateItem();await sleep(1200);B.setControlState('sneak',false);await sleep(400);P(B.win>w0,'Classic again: sneak + right click opens the spell list');await closeWin(B);
 // pick bars again, then simulate leaving Bedrock: Java gets classic, choice kept
 B.chat('/wandstyle sneakbars');await sleep(2500);o=await cons('wandbars simbedrock '+BD+' off',2500,true);s=await styleOf(BD);
 P(/: classic/.test(s)&&/mode=chest/.test(s)&&/saved=bars/.test(s),'same account on Java: Classic is put on the wand, saved choice kept',s.slice(-200));
 o=await cons('wandbars simbedrock '+BD+' on',2500,true);s=await styleOf(BD);P(/mode=inventory/.test(s)&&/matches=true/.test(s),'back on Bedrock: Sneak bars again');
 await cons('magic load',8000,true);await sleep(3000);s=await styleOf(BD);P(/mode=inventory/.test(s)&&/matches=true/.test(s),'after magic load (seal applies): still Sneak bars',s.slice(-120));
 s=await styleOf(J);P(/mode=chest/.test(s)&&/matches=true/.test(s),'after magic load: Java player still Classic');
 // ---------- 5. tip cap at 15 (Java)
 B.quit();await sleep(1500);const Tb=hook(await join(TP));await sleep(3000);await prep(Tb,TP);await holdSlot(Tb,WAND);await sleep(1000);
 let shown=0;for(let k=0;k<17;k++){clr(Tb);Tb.activateItem();await sleep(2150);if(/Wand tip/.test(txt(Tb)))shown++}
 t=await tip(TP);P(shown===15&&/uses 15\/15 ack false/.test(t),'never acknowledged: shown on exactly 15 uses, then stops for good',shown+' shown; '+t);
 // ---------- 6. AutoSort
 const asN=()=>rd('autosort/active.txt').split('\n').filter(x=>/^[0-9a-f-]{36}$/.test(x.trim())).length;const as0=asN();L('  active.txt lines before (earlier rig runs): '+as0);
 clr(A);A.chat('/sort');await sleep(1500);P(/Sorting enabled for: Barrel, Chest, Ender Chest, Shulker Box/.test(txt(A)),'/sort reaches AutoSort (no clash)',txt(A).slice(0,120));
 clr(A);A.chat('/autosort');await sleep(1500);P(/Sorting disabled/.test(txt(A)),'/autosort toggles it off');
 P(asN()===as0,'active.txt back to its start after on+off (this player not added; default off, nothing pre-filled)',as0+' -> '+asN());
 C=await pos(J);L('  spot now '+C);await place(J);await round(A,'AutoSort OFF',SCAT);
 const guis=[['ExcelMenu server menu',()=>{A.activateItem()}],['Utility Pouch (rig build)',()=>{A.chat('/pouch')}],['Trash (ExcelTrash)',()=>{A.chat('/trash')}],['Trash undo / recover',()=>{A.chat('/trash undo')}],['Leaderboard',()=>{A.chat('/leaderboard')}],['ExcelShops browse',()=>{A.chat('/excelshop')}],['TP menu',()=>{A.chat('/tpmenu')}],['Commands menu',()=>{A.chat('/dm open commands_menu')}]];
 await holdSlot(A,/^compass$/);const off={};for(const [g,f] of guis){off[g]=await guiSnap(A,f);L('  OFF '+g+': '+off[g].slice(0,140))}
 await holdSlot(A,WAND);await sleep(600);const spellOpen=async()=>{A.setControlState('sneak',true);await sleep(400);A.activateItem();await sleep(1200);A.setControlState('sneak',false)};
 const spOff=await guiSnap(A,spellOpen);L('  OFF spell list: '+spOff.slice(0,140));
 await holdSlot(A,/^compass$/);clr(A);A.activateItem();w=await waitWin(A,4000);if(w){await A.clickWindow(21,0,0);await sleep(1800)}
 P(/Sorting enabled/.test(txt(A))&&!A.currentWindow,'Java menu Auto-sort button turns AutoSort ON',txt(A).slice(0,120));
 P(asN()===as0+1,'active.txt gains exactly this one player',as0+' -> '+asN());
 await holdSlot(A,/^compass$/);for(const [g,f] of guis){const on=await guiSnap(A,f);P(on===off[g],g+': not touched by AutoSort'+(on==='nowin'?' (no Java window: chat/form based)':''),on.slice(0,100))}
 await holdSlot(A,WAND);await sleep(600);const spOn=await guiSnap(A,spellOpen);P(spOn===spOff&&spOn!=='nowin','Magic (elmakers) spell list: not touched',spOn.slice(0,80));await holdSlot(A,/^compass$/);
 await place(J);await round(A,'AutoSort ON (sort on open)',SORTED);
 // sort on close: open chest, then put items scattered while open via console, close -> sorted
 {const [x,y,z]=spots()[0];await cons(`setblock ${x} ${y} ${z} air`,120,true);await cons(`setblock ${x} ${y} ${z} minecraft:chest`,250,true);
  await emptyHand(A);await rigOpen(A,x,y,z);await waitWin(A);
  for(const [sl,it] of [[3,'dirt 4'],[8,'stone 2'],[15,'dirt 4']])await cons(`minecraft:item replace block ${x} ${y} ${z} container.${sl} with minecraft:${it}`,150,true);await sleep(400);
  const before=await blockItems(x,y,z);await closeWin(A);await sleep(600);const after=await blockItems(x,y,z);
  P(/Slot: 3b/.test(before)&&/Slot: 0b/.test(after)&&!/Slot: 15b/.test(after),'sort on close: items added while open are sorted when the chest closes',after.slice(-160))}
 // other player (never toggled) untouched, second viewer rule
 await place(OB);await round(O,'Observer never toggled',SCAT);
 {await place(J);const [x,y,z]=spots()[0];await emptyHand(O);await rigOpen(O,x,y,z);await waitWin(O);const l=await openAt(A,x,y,z);P(l===SCAT,'chest already open by another player: AutoSort skips it',l);await closeWin(O)}
 // Bedrock menu path = performCommand("autosort"/"spellinfo"/"wandstyle"): same as the player typing it (checked above)
 await cons('wandbars simbedrock '+BD+' off',800,true);
 A.quit();O.quit();Tb.quit();await sleep(800);done()})();
