const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,openCmd,click,chest,wcount,lastlog,peek,say}=H;
const Y=-60;const give=(n,i,c)=>cons(`minecraft:give ${n} ${i} ${c}`,500,true);
const tpTo=async(n,x,z=100)=>{await cons(`minecraft:tp ${n} ${x+0.5} ${Y} ${z-2.5} 0 20.5`,800,true)}; // 3 blocks north of (x,Y,z), looking at it
const put=async(x,items,z=100,kind='chest[facing=north]')=>{const s=items.map(([id,c],k)=>`{Slot:${k}b,id:"minecraft:${id}",count:${c}}`).join(',');await cons(`setblock ${x} ${Y} ${z} air`,300,true);await cons(`setblock ${x} ${Y} ${z} ${kind}{Items:[${s}]}`,500,true)};
const inv=(w,name)=>{const i=w.slots.findIndex((s,i)=>i>=w.inventoryStart&&s&&s.name===name);if(i<0)L('   (no '+name+' in window: '+w.slots.map((s,k)=>s?k+':'+s.name:null).filter(Boolean).join(' ')+')');return i};
const fresh=async b=>{if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(400)}await cons('clear '+b.username,400,true);await cons('kill @e[type=item]',300,true)};
(async()=>{
const R=process.env.RUN||'1';L(`=== ExcelTrash 0.2.0 rig run ${R} ${new Date().toString()}`);
await cons('difficulty peaceful',300,true);await cons('time set day',300,true);await cons('gamerule doDaylightCycle false',300,true);await cons('forceload add 80 80 220 140',800,true);await cons('forceload add 2990 2990 3010 3010',800,true);
const a=await join('TrA');
await cons('gamemode survival TrA',300,true);
// 1. 0.1.0 batch migrated, /trash undo still works, second undo does nothing
L('--- T1 upgrade: 0.1.0 pending batch -> /trash undo');
if(R==='1'){await fresh(a);let m=await say(a,'/trash undo');ok(cnt(a,'dirt')===64,'0.1.0 batch (made before the upgrade) returned by /trash undo: dirt='+cnt(a,'dirt')+' msg='+m);
 m=await say(a,'/trash undo');ok(/Nothing to recover/.test(m)&&cnt(a,'dirt')===64,'second /trash undo does nothing: '+m);}
// 2. Mode A: OK delete, Recover button, second undo nothing
L('--- T2 inventory mode: OK delete then Recover button');
await fresh(a);await give('TrA','dirt',64);await give('TrA','stone',10);await sleep(300);
let w=await openCmd(a,'/trash');ok(w&&w.slots.length===72&&/Trash: put items here/.test(JSON.stringify(w.title)),'/trash opens 4-row inventory trash: '+(w&&JSON.stringify(w.title)));
L('   buttons: '+[27,31,33,35].map(i=>i+'='+(w.slots[i]&&w.slots[i].name)).join(' '));
await click(a,inv(w,'dirt'));await click(a,0);await click(a,inv(w,'stone'));await click(a,1);
await sleep(300);ok(wcount(w,0,27)===74&&cnt(a,'dirt')===0,'74 items on the red side, none left in inventory (red='+wcount(w,0,27)+' dirt='+cnt(a,'dirt')+')');
await click(a,33,0,0,700);ok(wcount(w,0,27)===0&&!!a.currentWindow,'OK deletes the red side, screen stays open');
let pk=await peek('TrA');ok(/items=74/.test(pk),'stored batch: '+pk);L('   log: '+lastlog().replace(/^\S+ /,''));
await click(a,31,0,0,800);ok(cnt(a,'dirt')===64&&cnt(a,'stone')===10,'Recover button returns the OK-deleted batch: dirt='+cnt(a,'dirt')+' stone='+cnt(a,'stone'));
let m=await say(a,'/trash undo');ok(/Nothing to recover/.test(m)&&cnt(a,'dirt')===64,'then /trash undo does nothing: '+m);
a.closeWindow(w);await sleep(500);
// 3. OK, close, /trash undo and /bin undo
L('--- T3 OK, close, /trash undo, second undo');
w=await openCmd(a,'/bin');ok(!!w,'/bin alias opens');await click(a,inv(w,'dirt'));await click(a,0);await click(a,33,0,0,700);a.closeWindow(w);await sleep(500);
ok(cnt(a,'dirt')===0,'dirt deleted');m=await say(a,'/trash undo');ok(cnt(a,'dirt')===64&&/Recovered 64/.test(m),'/trash undo returns batch: '+m);
m=await say(a,'/bin undo');ok(/Nothing to recover/.test(m)&&cnt(a,'dirt')===64,'second undo (/bin undo) does nothing');
// 3b. two OKs in one screen = one batch
w=await openCmd(a,'/trash');await click(a,inv(w,'dirt'));await click(a,0);await click(a,33,0,0,700);await click(a,inv(w,'stone'));await click(a,0);await sleep(200);await click(a,33,0,0,700);a.closeWindow(w);await sleep(400);
m=await say(a,'/trash undo');ok(cnt(a,'dirt')===64&&cnt(a,'stone')===10,'two OK presses in one screen restore together: '+m);
// 3c. new screen replaces the last batch
w=await openCmd(a,'/trash');await click(a,inv(w,'dirt'));await click(a,0);await click(a,33,0,0,700);a.closeWindow(w);await sleep(300);
w=await openCmd(a,'/trash');await click(a,inv(w,'stone'));await click(a,0);await click(a,33,0,0,700);a.closeWindow(w);await sleep(300);
m=await say(a,'/trash undo');ok(cnt(a,'stone')===10&&cnt(a,'dirt')===0,'only the last batch comes back (stone yes, dirt no): '+m);
// 4. close without OK gives back
L('--- T4 close without OK');
await fresh(a);await give('TrA','dirt',64);w=await openCmd(a,'/trash');await click(a,inv(w,'dirt'));await click(a,0);a.closeWindow(w);await sleep(600);
ok(cnt(a,'dirt')===64,'closing without OK gives items back: '+a.msgs.join(' | '));pk=await peek('TrA');ok(!/items=/.test(pk)||/none|nothing/i.test(pk),'no batch stored: '+pk);
// 4b. Cancel button
w=await openCmd(a,'/trash');await click(a,inv(w,'dirt'));await click(a,0);await click(a,35,0,0,800);ok(!a.currentWindow&&cnt(a,'dirt')===64,'Cancel button closes and gives back');
// 5. overflow to feet
L('--- T5 undo overflow drops at feet');
await fresh(a);await tpTo('TrA',200);await give('TrA','dirt',128);w=await openCmd(a,'/trash');
for(let k=0;k<2;k++){await click(a,inv(w,'dirt'));await click(a,k)}await click(a,33,0,0,700);a.closeWindow(w);await sleep(400);
await give('TrA','cobblestone',36*64);await sleep(500);ok(a.inventory.emptySlotCount()===0,'inventory full ('+a.inventory.emptySlotCount()+' free)');
m=await say(a,'/trash undo',1500);const ground=H.groundItems(a,'dirt');ok(/128 dropped at your feet/.test(m)&&/dropped=128/.test(lastlog()),'overflow drops at feet: '+m+' | ground dirt seen='+ground);
await cons('kill @e[type=item]',300,true);m=await say(a,'/trash undo');ok(/Nothing to recover/.test(m),'second undo does nothing');
// 6. chest mode basic
L('--- T6 chest mode: tap moves real items, OK, /trash undo back to the chest');
await fresh(a);await tpTo('TrA',100);await put(100,[['diamond',5],['iron_ingot',20],['gold_ingot',7]]);
w=await openCmd(a,'/trash chest');ok(w&&w.slots.length===90,'/trash chest opens 6-row screen: '+(w&&JSON.stringify(w.title))+' '+a.msgs.join(' | '));
if(!w){L('chest mode failed, abort');H.summary();process.exit(1)}
ok(w.slots[0]&&w.slots[0].name==='diamond'&&w.slots[1].name==='iron_ingot','top rows mirror the chest');
await click(a,0,0,0,600);let c=await chest(100,Y,100);ok(!c.diamond&&w.slots[36]&&w.slots[36].name==='diamond'&&w.slots[36].count===5,'tap: diamond left the real chest and is on the red side');
await click(a,33,0,0,700);pk=await peek('TrA');ok(/chest=5/.test(pk)||/fromchest|chest=/.test(pk),'batch marks chest items: '+pk);L('   log: '+lastlog().replace(/^\S+ /,''));
a.closeWindow(w);await sleep(400);m=await say(a,'/trash undo');c=await chest(100,Y,100);ok(c.diamond===5&&cnt(a,'diamond')===0,'/trash undo puts diamonds back in the chest: '+m);
m=await say(a,'/trash undo');ok(/Nothing to recover/.test(m),'second undo does nothing');
// 7. Recover button inside chest screen
w=await openCmd(a,'/trash chest');await click(a,1,0,0,500);await click(a,33,0,0,700);c=await chest(100,Y,100);ok(!c.iron_ingot,'iron deleted from chest');
await click(a,31,0,0,800);c=await chest(100,Y,100);await sleep(700);ok(c.iron_ingot===20&&w.slots.slice(0,27).some(s=>s&&s.name==='iron_ingot'),'Recover button: iron back in chest and on the picture');
// 8. put back from red side and from own inventory
await give('TrA','emerald',3);await sleep(300);await click(a,inv(w,'emerald'));await click(a,10,0,0,500);c=await chest(100,Y,100);ok(c.emerald===3&&cnt(a,'emerald')===0,'cursor item tapped on an empty picture slot goes into the real chest');
await click(a,10,0,0,500);ok(w.slots[36]&&w.slots[36].name==='emerald','tap again moves it to the red side');
await click(a,36,0,0,300);await click(a,11,0,0,500);c=await chest(100,Y,100);ok(c.emerald===3,'pick up from red side, tap picture: back in chest');
// 9. (double-click collect: mineflayer can't send mode 6; covered by code review: COLLECT_TO_CURSOR is cancelled)
a.closeWindow(w);await sleep(600);
// 10. fast clicks
L('--- T10 fast-click spam, conservation');
await fresh(a);await put(100,[['diamond',5],['iron_ingot',20],['gold_ingot',7]]);w=await openCmd(a,'/trash chest');
const ps=[];for(let k=0;k<60;k++){const sl=[0,1,2,36,37,38,5,40][k%8];ps.push(a.clickWindow(sl,k%2,0).catch(()=>{}))}await Promise.all(ps);await sleep(800);
const inWin=n=>wcount(w,36,54,n);L(`   after spam: chest=${JSON.stringify(await chest(100,Y,100))} redside d/i/g=${inWin('diamond')}/${inWin('iron_ingot')}/${inWin('gold_ingot')}`);
a.closeWindow(w);await sleep(800);c=await chest(100,Y,100);
const tot=n=>(c[n]||0)+cnt(a,n);ok(tot('diamond')===5&&tot('iron_ingot')===20&&tot('gold_ingot')===7,`spam then close: totals d=${tot('diamond')} i=${tot('iron_ingot')} g=${tot('gold_ingot')} (want 5/20/7), chest=${c._total} mine=${cnt(a,'diamond')+cnt(a,'iron_ingot')+cnt(a,'gold_ingot')}`);
// 11. close mid-move
await fresh(a);await put(100,[['diamond',5],['iron_ingot',20],['gold_ingot',7]]);w=await openCmd(a,'/trash chest');a.clickWindow(0,0,0).catch(()=>{});a.clickWindow(1,0,0).catch(()=>{});a.closeWindow(w);await sleep(900);
c=await chest(100,Y,100);ok(c.diamond===5&&c.iron_ingot===20&&c.gold_ingot===7&&cnt(a,'diamond')===0,'close mid-move: all back in the chest, nothing duped: '+JSON.stringify({d:c.diamond,i:c.iron_ingot,g:c.gold_ingot}));
// 12. locks: second player, hopper, explosion
L('--- T12 lock while in use');
const b=await join('TrB');if(b.currentWindow)b.closeWindow(b.currentWindow);await cons('clear TrB',300,true);await cons('minecraft:tp TrB 100.5 -60 97.5 0 20.5',800,true);
w=await openCmd(a,'/trash chest');await click(a,0,0,0,400);
let po=await cons('probe open TrB 100 -60 100',800,true);ok(/DENIED/.test(po)&&!(b.currentWindow&&b.currentWindow.slots.length>=63),'other player can\'t open the chest while it is in Trash: '+(po.match(/probe open \w+/)||[''])[0]+' | '+b.msgs.slice(-1));
po=await cons('probe break TrB 100 -60 100',800,true);ok(/DENIED now=CHEST/.test(po),'other player can\'t break the chest while it is in Trash: '+(po.match(/probe break .*/)||[''])[0]);
m=await say(b,'/trash chest');ok(/Someone else is using Trash/.test(m)&&!(b.currentWindow&&b.currentWindow.slots.length===90),'other player /trash chest refused: '+m);
await cons(`setblock 100 ${Y-1} 100 hopper`,3000,true);c=await chest(100,Y,100);let hp=await chest(100,Y-1,100);ok(c.iron_ingot===20&&!hp.iron_ingot,'hopper under the chest frozen while in use (hopper '+hp._total+')');
await cons(`summon tnt 101 ${Y} 101 {fuse:0}`,1500,true);let o=await cons(`execute if block 100 ${Y} 100 chest`,500,true);ok(/passed/i.test(o),'explosion next to it: chest survives while in use');
await cons('kill @e[type=item]',300,true);
await cons(`setblock 99 ${Y-1} 100 redstone_block`,300,true);a.closeWindow(w);await sleep(1200);c=await chest(100,Y,100);hp=await chest(100,Y-1,100);ok((c.diamond||0)+(hp.diamond||0)===5&&cnt(a,'diamond')===0,'close: diamond back in chest (chest '+(c.diamond||0)+' + hopper '+(hp.diamond||0)+')');await cons(`setblock 99 ${Y-1} 100 grass_block`,300,true);await sleep(2500);hp=await chest(100,Y-1,100);ok(hp._total>0,'after close, hopper works again (hopper '+hp._total+')');po=await cons('probe open TrB 100 -60 100',800,true);ok(/OPENED/.test(po),'after close, other player can open it again');if(b.currentWindow)b.closeWindow(b.currentWindow);await sleep(300);
await cons(`setblock 100 ${Y-1} 100 grass_block`,300,true);await cons(`minecraft:tp TrB 300 -60 300`,500,true);
// 13. someone else viewing
await put(120,[['diamond',2]]);await cons('minecraft:tp TrB 120.5 -60 97.5 0 20.5',900,true);po=await cons('probe open TrB 120 -60 100',900,true);
await tpTo('TrA',120);m=await say(a,'/trash chest',1200);ok(/OPENED/.test(po)&&/Someone else has that chest open/.test(m)&&!(a.currentWindow&&a.currentWindow.slots.length===90),'chest open by someone else: Trash refused: '+m);if(b.currentWindow)b.closeWindow(b.currentWindow);await sleep(300);
// 14. chest replaced under you
L('--- T14 chest removed while open');
await fresh(a);await put(120,[['diamond',4],['coal',9]]);w=await openCmd(a,'/trash chest');await click(a,0,0,0,400);await cons(`setblock 120 ${Y} 100 stone`,1200,true);
ok(!a.currentWindow||a.currentWindow.slots.length!==90,'screen closes when the chest is gone');ok(cnt(a,'diamond')===4&&cnt(a,'coal')===0,'red side diamonds go to the player, picture items not copied (diamond='+cnt(a,'diamond')+' coal='+cnt(a,'coal')+')');
// 15. double chest pages
L('--- T15 double chest');
await fresh(a);await cons(`setblock 140 ${Y} 100 air`,200,true);await cons(`setblock 141 ${Y} 100 air`,200,true);
await cons(`setblock 140 ${Y} 100 chest[facing=north,type=left]{Items:[{Slot:0b,id:"minecraft:diamond",count:1}]}`,300,true);await cons(`setblock 141 ${Y} 100 chest[facing=north,type=right]{Items:[{Slot:5b,id:"minecraft:gold_ingot",count:9}]}`,500,true);
await tpTo('TrA',140);w=await openCmd(a,'/trash chest');const nm=s=>s?JSON.stringify(s.customName||s.displayName||'')+JSON.stringify((s.components||[]).filter(x=>/lore/.test(x.type)).map(x=>x.data)):'';
L('   info: '+nm(w.slots[28]).slice(0,200));let pg=w.slots.slice(0,27).map(s=>s&&s.name).filter(Boolean);L('   page1: '+pg.join(','));
await click(a,35,0,0,700);let pg2=w.slots.slice(0,27).map(s=>s&&s.name).filter(Boolean);L('   page2: '+pg2.join(','));
ok((pg.includes('diamond')&&pg2.includes('gold_ingot'))||(pg.includes('gold_ingot')&&pg2.includes('diamond')),'double chest shows as 2 pages');
const gi=w.slots.slice(0,27).findIndex(s=>s&&s.name==='gold_ingot');const di=w.slots.slice(0,27).findIndex(s=>s&&s.name==='diamond');await click(a,gi>=0?gi:di,0,0,500);
await click(a,33,0,0,700);a.closeWindow(w);await sleep(400);let c1=await chest(140,Y,100),c2=await chest(141,Y,100);ok((c1._total+c2._total)===(gi>=0?1:9),'item deleted from page 2 half');
m=await say(a,'/trash undo');c1=await chest(140,Y,100);c2=await chest(141,Y,100);ok((c1._total+c2._total)===10,'undo returns it into the double chest: '+m);
// 16. shulker items keep contents; no shulker into shulker block
L('--- T16 shulkers');
await fresh(a);await cons(`setblock 160 ${Y} 100 air`,200,true);await cons(`setblock 160 ${Y} 100 chest[facing=north]{Items:[{Slot:0b,id:"minecraft:shulker_box",count:1,components:{"minecraft:container":[{slot:0,item:{id:"minecraft:diamond",count:10}}]}}]}`,500,true);
await tpTo('TrA',160);w=await openCmd(a,'/trash chest');await click(a,0,0,0,400);await click(a,33,0,0,700);a.closeWindow(w);await sleep(300);
o=await cons(`data get block 160 ${Y} 100 Items`,600,true);ok(!/shulker_box/.test(o),'shulker item deleted');m=await say(a,'/trash undo');o=await cons(`data get block 160 ${Y} 100 Items`,600,true);ok(/shulker_box/.test(o)&&/diamond/.test(o)&&/count: 10/.test(o),'undo: shulker back with its 10 diamonds inside');
await cons(`setblock 170 ${Y} 100 air`,200,true);await cons(`setblock 170 ${Y} 100 shulker_box`,400,true);await give('TrA','red_shulker_box',1);await tpTo('TrA',170);w=await openCmd(a,'/trash chest');ok(!!w,'shulker box block works as target');
if(w){await click(a,inv(w,'red_shulker_box'));await click(a,0,0,0,500);o=await cons(`data get block 170 ${Y} 100`,500,true);ok(!/red_shulker_box/.test(o),'shulker can\'t go into a shulker box: '+a.msgs.slice(-1));a.closeWindow(w);await sleep(400);ok(cnt(a,'red_shulker_box')===1,'shulker stays with the player');}
// 17. GriefPrevention claim
L('--- T17 GriefPrevention');
await fresh(a);await cons(`setblock 3000 ${Y} 3000 chest[facing=north]{Items:[{Slot:0b,id:"minecraft:diamond",count:3}]}`,500,true);await tpTo('TrA',3000,3000);
const aw=H.winOpen(a);m=await say(a,'/trash chest',1500);const aw2=await aw;ok(/can't use that chest/.test(m)&&!(aw2&&aw2.slots.length===90),'chest in someone else\'s claim: refused: '+m);if(a.currentWindow)a.closeWindow(a.currentWindow);
c=await chest(3000,Y,3000);ok(c.diamond===3,'claim chest untouched');
// 18. compass menu entry
L('--- T18 compass Trash Bin entry');
await fresh(a);await tpTo('TrA',200);const mw=H.winOpen(a);await cons('excelmenu open server_menu TrA',900,true);const menu=await mw;ok(menu&&menu.slots[26]&&menu.slots[26].name==='lime_dye','server menu slot 26 = Trash Bin');
if(menu){const tw=H.winOpen(a);await a.clickWindow(26,0,0).catch(()=>{});const t=await tw;ok(t&&/Trash: put items here/.test(JSON.stringify(t.title)),'compass Trash Bin opens the new trash: '+(t&&JSON.stringify(t.title)));if(a.currentWindow)a.closeWindow(a.currentWindow);}
// 19. chest button from inventory mode
await put(200,[['diamond',2]]);w=await openCmd(a,'/trash');const sw=H.winOpen(a);await click(a,27,0,0,200);const sww=await sw;ok(sww&&sww.slots.length===90,'chest button in inventory trash switches to chest mode');if(a.currentWindow)a.closeWindow(a.currentWindow);await sleep(300);
// 20. death with items on red side
L('--- T20 death');
await fresh(a);await cons('gamerule keepInventory false',200,true);await give('TrA','dirt',64);w=await openCmd(a,'/trash');await click(a,inv(w,'dirt'));await click(a,0);await cons('kill TrA',1500,true);
const gd=H.groundItems(a,'dirt');L('   ground dirt '+gd);pk=await peek('TrA');ok(gd===64||/dirt/i.test(await cons('execute if entity @e[type=item,nbt={Item:{id:"minecraft:dirt"}}]',500,true)),'die with items on red side: they drop like normal ('+gd+'), not deleted: '+pk);
await cons('kill @e[type=item]',300,true);a.respawn&&a.respawn();await sleep(800);
// audit
const au=await cons('audit TrA 1 trash',1200,true);L('   audit: '+au.split('\n').slice(0,4).join(' / ').slice(0,400));ok(/\[trash\] trash/.test(au)&&/\[trash\] undo/.test(au),'AuditLog reads TRASH and UNDO lines');
b.quit();a.quit();await sleep(500);const r=H.summary();H.fs.writeFileSync(`/workspace/trash020/tests/result-run${R}.json`,JSON.stringify(r,null,1));process.exit(0)})().catch(e=>{L('CRASH '+e.stack);H.summary();process.exit(1)});
