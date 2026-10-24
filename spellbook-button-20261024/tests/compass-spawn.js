// Compass Spawn fix rig test. PHASE=old (live menu file, shows the bug) | new (fixed file).
// Bots: Op (sets spawn), Ex (existing player), Nw<sfx> (brand-new player). Bedrock path = the exact console
// command ExcelBedrockMenu 1.5.18 teleportToSpawn runs ("spawn <name>"), sent from the console.
const T=require('./compass-lib');const {L,cons,sleep,join}=T;
const R=[];const P=(ok,n,d)=>{const s=(ok?'PASS ':'FAIL ')+n+(d?' | '+d:'');R.push(s);L(s)};
const PH=process.env.PHASE||'new';const SFX=process.env.SFX||'1';const SP=(process.env.SP||'120 70 140').split(' ').map(Number);
const waitWin=async(b,ms=4000)=>{const t=Date.now();while(!b.currentWindow&&Date.now()-t<ms)await sleep(100);await sleep(600);return b.currentWindow};
const layout=w=>{if(!w)return 'nowin';const o=[];for(let i=0;i<w.inventoryStart;i++){const it=w.slots[i];if(it)o.push(i+':'+it.name)}return o.join(',')};
const pos=async n=>{const o=await cons('data get entity '+n+' Pos',700,true);const m=/\[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]/.exec(o);return m?[Math.floor(+m[1]),Math.floor(+m[2]),Math.floor(+m[3])]:null};
const dim=async n=>{const o=await cons('data get entity '+n+' Dimension',700,true);const m=/"(minecraft:[a-z_]+)"/.exec(o);return m?m[1]:o.slice(-80)};
const holdCompass=async b=>{for(let i=36;i<45;i++){const it=b.inventory.slots[i];if(it&&it.name==='compass'){b.setQuickBarSlot(i-36);await sleep(400);return true}}return false};
const away=async n=>{await cons(`execute in minecraft:overworld run tp ${n} ${SP[0]+400} 90 ${SP[2]-300}`,1200,true)};
async function compassSpawn(b,clickYes=true){await cons('clear '+b.username,300,true);b.chat('/compass');await sleep(1500);await holdCompass(b);
 b.activateItem();let w=await waitWin(b);const main=layout(w);if(!w)return {main,confirm:'nowin'};
 await b.clickWindow(10,0,0);await sleep(400);if(b.currentWindow===w){await sleep(1200)}w=await waitWin(b);const conf=layout(w);
 if(w){await b.clickWindow(clickYes?3:5,0,0);await sleep(2000)}if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(500)}return {main,confirm:conf}}
const same=(a,b)=>a&&b&&a[0]===b[0]&&a[2]===b[2]&&Math.abs(a[1]-b[1])<=1;
(async()=>{L('=== compass spawn rig, phase '+PH+' sfx '+SFX+' spawn '+SP.join(',')+' '+new Date().toString());
 const OP='Op'+SFX;await cons('op '+OP,500,true);const O=await join(OP);
 await cons(`execute in minecraft:overworld run tp ${OP} ${SP.join(' ')}`,1200,true);await cons(`execute in minecraft:overworld run setblock ${SP[0]} ${SP[1]-1} ${SP[2]} minecraft:stone`,300,true);
 O.chat('/setspawn');await sleep(1200);O.chat('/setspawn newbies');await sleep(1200);
 const sp=await pos(OP);L('  spawn set at '+sp);
 // /spawn reference
 const E=await join('Ex'+SFX);await away(E.username);E.chat('/spawn');await sleep(2000);const ref=await pos(E.username);
 P(same(ref,sp),'/spawn (player) goes to the Essentials spawn',JSON.stringify([ref,sp]));
 // existing player, compass, twice
 for(const k of [1,2]){await away(E.username);const r=await compassSpawn(E);const p=await pos(E.username);const d=await dim(E.username);
  if(k===1){L('  main menu: '+r.main);L('  confirm: '+r.confirm)}
  P(/10:compass/.test(r.main)&&/20:writable_book/.test(r.main)&&/21:hopper/.test(r.main),'compass opens the server menu (Spawn 10, Spellbook 20, Auto-sort 21) #'+k);
  P(/3:lime_dye,4:compass,5:red_dye/.test(r.confirm),'Spawn opens the Yes/Cancel confirm #'+k,r.confirm);
  if(PH==='old'){P(p[0]===-784&&p[2]===-336,'OLD file: compass Yes lands at the old village -784,-336, not /spawn (the bug) #'+k,JSON.stringify(p))}
  else{P(same(p,ref)&&d==='minecraft:overworld','existing player: compass Yes lands on the same spot as /spawn #'+k,JSON.stringify([p,ref,d]))}}
 // Cancel does not teleport
 await away(E.username);const before=await pos(E.username);await compassSpawn(E,false);const after=await pos(E.username);P(same(before,after),'Cancel keeps the player where they are',JSON.stringify([before,after]));
 // from the Nether
 if(PH==='new'){await cons(`execute in minecraft:the_nether run tp ${E.username} 10 80 10`,1500,true);await compassSpawn(E);const p=await pos(E.username);const d=await dim(E.username);P(same(p,ref)&&d==='minecraft:overworld','from the Nether: compass Yes still lands on the /spawn spot',JSON.stringify([p,d]))}
 // brand-new player
 const NN='Nw'+SFX+Date.now()%10000;const N=await join(NN);await sleep(1500);const first=await pos(NN);L('  new player first-join position '+first);
 await away(NN);const rn=await compassSpawn(N);const pn=await pos(NN);
 if(PH==='old')P(pn[0]===-784&&pn[2]===-336,'OLD file: new player compass lands at the old spot',JSON.stringify(pn));
 else P(same(pn,ref),'new player: compass Yes lands on the same spot as /spawn',JSON.stringify([pn,ref]));
 await away(NN);N.chat('/spawn');await sleep(2000);const pn2=await pos(NN);P(same(pn2,ref),'new player: /spawn lands on the same spot',JSON.stringify(pn2));
 // Bedrock path (ExcelBedrockMenu teleportToSpawn = console "spawn <name>")
 await away(NN);await cons('spawn '+NN,2000,true);const pb=await pos(NN);P(same(pb,ref),'Bedrock Spawn button path (console spawn <name>) lands on the same spot',JSON.stringify(pb));
 // commands menu /spawn entry still OK
 E.chat('/servercommands');await sleep(800);
 O.quit();E.quit();N.quit();await sleep(800);const f=R.filter(x=>x.startsWith('FAIL')).length;L('=== phase '+PH+': '+(R.length-f)+' pass, '+f+' fail');process.exit(0)})().catch(e=>{L('ERR '+e.stack);process.exit(1)});
