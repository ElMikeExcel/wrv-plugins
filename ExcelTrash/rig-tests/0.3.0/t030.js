// ExcelTrash 0.3.0 rig test (TEST rig /workspace/trash-rig only, port 25951)
process.env.OUT=process.env.OUT||'/workspace/trash030/tests/run.log';
const H=require('/workspace/trash020/tests/h.js');const {L,cons,sleep,join,ok,cnt,openCmd,click,chest,wcount,lastlog,peek,say,winOpen,groundItems}=H;
const Y=-60;const give=(n,i,c)=>cons(`minecraft:give ${n} ${i} ${c}`,500,true);
const nm=s=>s?String(s.customName||'')+' '+JSON.stringify(s.components||s.nbt||''):'';
const has=(s,re)=>re.test(nm(s));
const put=async(x,items,z=100,kind='chest[facing=north]')=>{const s=items.map(([id,c],k)=>`{Slot:${k}b,id:"minecraft:${id}",count:${c}}`).join(',');await cons(`setblock ${x} ${Y} ${z} air`,300,true);await cons(`setblock ${x} ${Y} ${z} ${kind}{Items:[${s}]}`,500,true)};
const tap=async(b,x,y,z)=>{const o=await cons(`probe tap ${b.username} ${x} ${y} ${z}`,500,true);L('   '+(o.match(/probe tap \S+/)||[o.slice(-80)])[0]);await sleep(300)};
const inv=(w,name)=>w.slots.findIndex((s,i)=>i>=w.inventoryStart&&s&&s.name===name);
const fresh=async b=>{if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(400)}await cons('clear '+b.username,400,true);await cons('minecraft:kill @e[type=item]',300,true)};
(async()=>{
L('=== ExcelTrash 0.3.0 rig run '+new Date().toString());
await cons('difficulty peaceful',300,true);await cons('time set day',300,true);await cons('forceload add 80 80 220 140',800,true);
for(let x=95;x<=125;x++)await cons(`setblock ${x} ${Y} 100 air`,60,true);
await cons('fill 95 -60 95 125 -58 105 air',500,true);
const a=await join('TrC');await cons('gamemode survival TrC',300,true);
// ---- hint (first open only)
L('--- T1 first-open hint, help button, door cancel');
await fresh(a);await cons('minecraft:tp TrC 110.5 -60 90.5 180 0',800,true); // looking north at open air
let w=await openCmd(a,'/trash');const m1=a.msgs.join(' | ');
ok(/Trash tip/.test(m1)&&/trash from a chest/i.test(m1),'first /trash shows the one-time hint: '+m1);
ok(has(w.slots[29],/Help: put items in the top slots, tap OK twice/),'slot 29 Help button name readable');
ok(w.slots[35]&&w.slots[35].name==='oak_door'&&has(w.slots[35],/Cancel/),'Cancel is an oak door (not a barrier)');
ok(has(w.slots[27],/tap, then tap the chest/),'not looking at a chest: chest button says tap, then tap the chest');
ok(has(w.slots[33],/OK: delete the trash slots/),'OK normal label');
a.closeWindow(w);await sleep(400);w=await openCmd(a,'/trash');ok(!/Trash tip/.test(a.msgs.join(' ')),'second open: no hint');
// ---- two-tap OK
L('--- T2 two-tap OK, timeout, empty');
await give('TrC','dirt',10);await sleep(400);a.closeWindow(w);await sleep(400);w=await openCmd(a,'/trash');
await click(a,inv(w,'dirt'));await click(a,0);await sleep(200);
await click(a,33,0,0,700);ok(wcount(w,0,27)===10&&has(w.slots[33],/Tap again to delete 10 items/)&&w.slots[33].name==='tnt','first OK does NOT delete; button = TNT "Tap again to delete 10 items"');
await sleep(5600);ok(has(w.slots[33],/OK: delete the trash slots/)&&wcount(w,0,27)===10,'after 5 s it goes back to normal, items still there');
await click(a,33,0,0,700);a.msgs=[];await click(a,33,0,0,800);
ok(wcount(w,0,27)===0&&w.slots[33].name==='lime_concrete'&&has(w.slots[33],/Deleted 10 items/),'second tap deletes; button shows "Deleted 10 items"');
const dm=a.msgs.join(' | ');ok(/Deleted 10 items\. Changed your mind\? Tap Recover or type \/trash undo within 5 minutes/.test(dm),'chat: '+dm);
ok(/TRASH .*dirtx10|DIRTx10/i.test(lastlog()),'trash.log row: '+lastlog().replace(/^\S+ /,''));
await sleep(3300);ok(has(w.slots[33],/OK: delete the trash slots/),'Deleted label goes back to normal after 3 s');
a.msgs=[];await click(a,33,0,0,700);ok(has(w.slots[33],/Nothing to delete/)&&/Nothing was deleted/.test(a.msgs.join(' ')),'OK on empty: "Nothing to delete" + chat: '+a.msgs.join(' | '));
await sleep(3300);
// count changes between taps -> re-arm
await click(a,31,0,0,800);ok(cnt(a,'dirt')===10,'Recover button gives the 10 dirt back');
await click(a,inv(w,'dirt'));await click(a,0,1);await sleep(200); // right click = place 1
await click(a,33,0,0,600);ok(has(w.slots[33],/Tap again to delete 1 item"|Tap again to delete 1 item[^s]/),'armed for 1 item');
await click(a,0,1);await sleep(200); // place 1 more from cursor
await click(a,33,0,0,600);ok(wcount(w,0,27)===2&&has(w.slots[33],/Tap again to delete 2 items/),'count changed: second tap re-arms with new count, nothing deleted');
await click(a,33,0,0,600);ok(wcount(w,0,27)===0,'third tap deletes the 2');
if(a.currentWindow){a.closeWindow(a.currentWindow);await sleep(400)}
// ---- chest you're looking at
L('--- T3 looking at a chest: button names it, opens chest view, two-tap OK there');
await fresh(a);await put(110,[['diamond',5],['iron_ingot',20]],100);
await cons('minecraft:tp TrC 110.5 -60 97.5 0 20.5',800,true); // 3 blocks north of the chest, looking at it
w=await openCmd(a,'/trash');ok(has(w.slots[27],/Trash from the Chest you're looking at/)&&w.slots[27].name==='chest','button: "Trash from the Chest you\'re looking at"');
let p=winOpen(a);await click(a,27,0,0,200);w=await p;await sleep(400);
ok(w&&w.slots.length===90&&/Trash from Chest/.test(JSON.stringify(w.title)),'tap opens the chest view: '+(w&&JSON.stringify(w.title)));
ok(has(w.slots[28],/Help: tap chest items above to move them down, tap OK twice/),'chest view help label');
await click(a,0,0,0,600);ok(w.slots[36]&&w.slots[36].name==='diamond','tap moves the diamond to the trash slots');
await click(a,33,0,0,600);let c=await chest(110,Y,100);ok(has(w.slots[33],/Tap again to delete 5 items/)&&!c.diamond,'chest view first OK arms (5 items)');
a.msgs=[];await click(a,33,0,0,800);ok(!w.slots[36]&&has(w.slots[33],/Deleted 5 items/)&&/Deleted 5 items/.test(a.msgs.join(' ')),'chest view second OK deletes + chat: '+a.msgs.join(' | '));
a.closeWindow(w);await sleep(500);let m=await say(a,'/trash undo',1200);c=await chest(110,Y,100);ok(c.diamond===5,'/trash undo puts the 5 diamonds back in the chest: '+m);
// ---- pick mode from the button
L('--- T4 not looking at a chest: tap chest button, screen closes, tap the chest');
await fresh(a);await cons('minecraft:tp TrC 110.5 -60 97.5 180 0',800,true); // facing away
w=await openCmd(a,'/trash');ok(has(w.slots[27],/tap, then tap the chest/),'facing away: pick wording');
a.msgs=[];await click(a,27,0,0,1200);ok(!a.currentWindow,'screen closed');ok(/Now tap \(right-click\) the chest/.test(a.msgs.join(' ')),'chat: '+a.msgs.join(' | '));
p=winOpen(a);await tap(a,110,Y,100);w=await p;await sleep(400);
ok(w&&w.slots.length===90&&/Trash from Chest/.test(JSON.stringify(w.title)),'tapping the chest opens the chest TRASH view (not the plain chest): '+(w&&JSON.stringify(w.title)));
if(w){a.closeWindow(w);await sleep(400)}
p=winOpen(a);await tap(a,110,Y,100);w=await p;await sleep(300);
ok(w&&w.slots.length!==90,'next tap after that opens the chest normally (pick used up): '+(w&&JSON.stringify(w.title)));
if(w){a.closeWindow(w);await sleep(400)}
// ---- /trash chest while looking away -> pick mode
L('--- T5 /trash chest while not looking at one');
a.msgs=[];await say(a,'/trash chest',900);ok(/Now tap/.test(a.msgs.join(' '))&&!a.currentWindow,'/trash chest arms the tap: '+a.msgs.join(' | '));
await sleep(31000);p=winOpen(a);await tap(a,110,Y,100);w=await p;await sleep(300);
ok(w&&w.slots.length!==90,'after 30 s the tap expires: plain chest opens');if(w){a.closeWindow(w);await sleep(400)}
// ---- help command
m=await say(a,'/trash help',900);ok(/Trash help/.test(m)&&/tap OK again|OK twice|again to delete/.test(m),'/trash help: '+m.slice(0,200));
// ---- switch with items in trash refused
w=await openCmd(a,'/trash');await give('TrC','dirt',3);await sleep(400);await click(a,inv(w,'dirt'));await click(a,0);a.msgs=[];await click(a,27,0,0,700);
ok(!!a.currentWindow&&wcount(w,0,27)===3,'chest button with items in trash slots: refused, stays open');
a.closeWindow(w);await sleep(600);ok(cnt(a,'dirt')===3,'close without OK gives them back');
// ---- T6 death with the trash open (audit A2: keep-inventory plugins clear drops at HIGHEST)
L('--- T6 death with items in the trash slots');
const die=async()=>{await cons('minecraft:kill TrC',900,true);await sleep(600);a.respawn&&a.respawn();await sleep(1500)};
const ems=()=>a.inventory.items().filter(i=>i.name==='emerald').reduce((x,i)=>x+i.count,0);
await cons('gamerule keepInventory false',300,true);await cons('spawnpoint TrC 110 -60 90',300,true);
const sc=async()=>{let t=0;const o=await cons('execute as @e[type=item,nbt={Item:{id:"minecraft:emerald"}}] run data get entity @s Item.count',900,true);for(const m of o.replace(/\x1b\[[0-9;]*m/g,'').matchAll(/has the following entity data: (\d+)/g))t+=+m[1];return t};
const near=n=>Object.values(a.entities).filter(e=>(e.name==='item'||e.displayName==='Item')&&e.position&&e.position.distanceTo(a.entity.position)<6).map(e=>e.getDroppedItem&&e.getDroppedItem()).filter(i=>i&&i.name===n).reduce((x,i)=>x+i.count,0);
// (a) keep-inventory death: probe mimics a keep-inventory plugin (HIGHEST: keepInventory true + drops cleared)
await cons('probe protect on',300,true);await fresh(a);await cons('minecraft:tp TrC 110.5 -60 90.5 180 0',800,true);
w=await openCmd(a,'/trash');await give('TrC','emerald',7);await sleep(400);await click(a,inv(w,'emerald'));await click(a,0);await sleep(300);
ok(wcount(w,0,27,'emerald')===7,'7 emeralds in the trash slots');a.msgs=[];
await die();await cons('minecraft:kill @e[type=item]',300,true);
ok(ems()===7,'keep-inventory death: the 7 trash-slot emeralds are back in the kept inventory (had '+ems()+')');
ok(/You died with 7 items in the trash slots\. Nothing was deleted/.test(a.msgs.join(' ')),'chat after keep-inventory death: '+a.msgs.filter(m=>/died with/.test(m)).join(' | '));
ok(!/TRASH player=TrC[^\n]*EMERALD/.test(await lastlog()),'nothing logged as trashed');
// (b) keep-inventory death, bag full: given on respawn
await fresh(a);await cons('minecraft:tp TrC 110.5 -60 90.5 180 0',800,true);
w=await openCmd(a,'/trash');await give('TrC','emerald',7);await sleep(400);await click(a,inv(w,'emerald'));await click(a,0);await sleep(300);
await give('TrC','stone',64*36);await sleep(600);a.msgs=[];
await cons('minecraft:kill @e[type=item]',300,true);await cons('minecraft:kill TrC',900,true);await sleep(600);a.respawn&&a.respawn();await sleep(1800);
const g=await sc();
ok(ems()+g===7&&/Your trash-slot items are back/.test(a.msgs.join(' ')),'full bag: emeralds given on respawn (inv '+ems()+', ground '+g+'): '+a.msgs.filter(m=>/trash-slot/.test(m)).join(' | '));
// (c) unkeep-inventory death: emeralds drop with the death drops (0.2.0 behavior)
await cons('probe protect off',300,true);await fresh(a);await cons('minecraft:kill @e[type=item]',300,true);await cons('clear TrC',400,true);// (b) left 7 at spawn: do not let the bot pick them up
await cons('minecraft:tp TrC 110.5 -60 90.5 180 0',800,true);
w=await openCmd(a,'/trash');await give('TrC','emerald',7);await sleep(400);await click(a,inv(w,'emerald'));await click(a,0);await sleep(300);
await cons('minecraft:kill TrC',900,true);await sleep(1200);
const g2=await sc();ok(g2===7,'normal death: the 7 emeralds drop with the death drops (ground '+g2+')');
a.respawn&&a.respawn();await sleep(1200);ok(ems()===0,'normal death: not duplicated into the inventory');
const S=H.summary();a.quit();process.exit(S.fail?1:0);
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
