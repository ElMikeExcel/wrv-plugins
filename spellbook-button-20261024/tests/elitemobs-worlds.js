// RIG ONLY. Spawn teleports from every world type, with real EliteMobs (MagmaGuy) 10.9.10 loaded.
// Paths per world: Java compass > Spawn > Yes (ExcelMenu), console "spawn <name>" (what ExcelBedrockMenu sends),
// the real ExcelBedrockMenu 1.5.18 teleportToSpawn method (incl. its Guild-world 1-tick check), and its villageFallback.
const T=require('./elitemobs-worlds-lib');const {L,cons,sleep,join}=T;
const R=[];const P=(ok,n,d)=>{const s=(ok?'PASS ':'FAIL ')+n+(d?' | '+d:'');R.push(s);L(s)};
const SFX=process.env.SFX||'1';const SP=(process.env.SP||'120 70 140').split(' ').map(Number);
const waitWin=async(b,ms=4000)=>{const t=Date.now();while(!b.currentWindow&&Date.now()-t<ms)await sleep(100);await sleep(600);return b.currentWindow};
const layout=w=>{if(!w)return 'nowin';const o=[];for(let i=0;i<w.inventoryStart;i++){const it=w.slots[i];if(it)o.push(i+':'+it.name)}return o.join(',')};
const where=async n=>{const o=await cons('rigem where '+n,800,true);const m=/RIGWHERE \S+ world=(\S+) env=(\S+) pos=(-?\d+),(-?\d+),(-?\d+) feet=(\S+) head=(\S+) below=(\S+) feetPassable=(\S+) headPassable=(\S+)/.exec(o);
 return m?{w:m[1],env:m[2],p:[+m[3],+m[4],+m[5]],feet:m[6],head:m[7],below:m[8],fp:m[9]==='true',hp:m[10]==='true'}:{raw:o.slice(-200)}};
const holdCompass=async b=>{for(let i=36;i<45;i++){const it=b.inventory.slots[i];if(it&&it.name==='compass'){b.setQuickBarSlot(i-36);await sleep(400);return true}}return false};
async function compassSpawn(b){await cons('clear '+b.username,300,true);b.chat('/compass');await sleep(1500);await holdCompass(b);
 b.activateItem();let w=await waitWin(b);const main=layout(w);if(!w)return {main,confirm:'nowin'};
 await b.clickWindow(10,0,0);await sleep(400);if(b.currentWindow===w){await sleep(1200)}w=await waitWin(b);const conf=layout(w);
 if(w){await b.clickWindow(3,0,0);await sleep(2500)}if(b.currentWindow){b.closeWindow(b.currentWindow);await sleep(500)}return {main,confirm:conf}}
const same=(a,b)=>a&&b&&a[0]===b[0]&&a[2]===b[2]&&Math.abs(a[1]-b[1])<=1;
const EXPW={'minecraft:em_adventurers_guild':'em_adventurers_guild','minecraft:em_id_test_dungeon':'em_id_test_dungeon','minecraft:the_nether':'world_nether','minecraft:the_end':'world_the_end','minecraft:overworld':'world'};
const WORLDS=[ // label, dimension key, x y z inside it
 ['EliteMobs Adventurers Guild (em_adventurers_guild, loaded by EliteMobs)','minecraft:em_adventurers_guild',208,88,236],
 ['EliteMobs-style dungeon name (em_id_test_dungeon, NOT registered by EliteMobs)','minecraft:em_id_test_dungeon',40,-50,40],
 ['Nether','minecraft:the_nether',10,80,10],
 ['End, dragon alive','minecraft:the_end',0,90,60],
 ['End, after dragon killed','minecraft:the_end',0,90,60],
 ['Overworld (away from spawn)','minecraft:overworld',SP[0]+400,90,SP[2]-300]];
let ref;
const put=async(n,W)=>{await cons(`execute in ${W[1]} run forceload add ${W[2]} ${W[4]}`,800,true);await cons(`execute in ${W[1]} run setblock ${W[2]} ${W[3]-1} ${W[4]} minecraft:stone`,300,true);await cons(`execute in ${W[1]} run fill ${W[2]} ${W[3]} ${W[4]} ${W[2]} ${W[3]+1} ${W[4]} minecraft:air`,300,true);
 await cons(`execute in ${W[1]} run minecraft:tp ${n} ${W[2]}.5 ${W[3]} ${W[4]}.5`,1500,true);return where(n)};
const check=(lbl,path,r,log)=>{const at=r.w==='world'&&same(r.p,ref.p);const old=r.p&&r.p[0]===-784&&(r.p[2]===-336||r.p[2]===-272);
 const cancelled=/RIGTP \S+ .*cancelled=true/.test(log);const emerr=/\[EliteMobs\].*(ERROR|Exception)|ERROR.*EliteMobs|EliteMobs.*Exception/i.test(log)||/Exception/.test(log);
 P(at&&!old&&r.fp&&r.hp&&!cancelled&&!emerr,`${lbl}: ${path} -> overworld /spawn block`,JSON.stringify({w:r.w,p:r.p,feet:r.feet,head:r.head,below:r.below,cancelled,err:emerr}));};
async function doWorld(b,W,lbl){const n=b.username;let s=await put(n,W);
 if(W[1]==='minecraft:em_adventurers_guild'){await cons(`execute in minecraft:overworld run minecraft:tp ${n} ${SP[0]+400} 90 ${SP[2]-300}`,1200,true);T.since();b.chat('/ag');await sleep(4000);const lg=T.since();const a=await where(n);
  P(a.w==='em_adventurers_guild','entering the Guild the player way: /ag (EliteMobs command) puts the player in em_adventurers_guild',JSON.stringify({w:a.w,p:a.p,feet:a.feet,below:a.below})+' '+((/RIGTP .*/.exec(lg)||[''])[0]));
  if(a.w==='em_adventurers_guild')s=a;else s=await put(n,W);}
 if(s.w!==EXPW[W[1]]){P(false,lbl+': could not enter world',JSON.stringify(s));return}
 L(`  -- ${lbl}: in ${s.w}/${s.env} at ${s.p}`);
 T.since();const c=await compassSpawn(b);let log=T.since();let r=await where(n);
 if(!/10:compass/.test(c.main)||!/3:lime_dye/.test(c.confirm))P(false,lbl+': compass menu/confirm did not open',JSON.stringify(c));
 check(lbl,'Java compass > Spawn > Yes',r,log);
 await put(n,W);T.since();await cons('spawn '+n,2000,true);log=T.since();r=await where(n);check(lbl,'Bedrock: console spawn <name>',r,log);
 await put(n,W);T.since();const inv=await cons('rigem ebmspawn '+n,2500,true);log=inv+T.since();r=await where(n);check(lbl,'Bedrock: ExcelBedrockMenu teleportToSpawn (real method)',r,log);
 await put(n,W);T.since();await cons('rigem ebmfallback '+n,2000,true);r=await where(n);
 const old=r.p&&r.p[0]===-784;P(r.w==='world'&&same(r.p,ref.p),`${lbl}: ExcelBedrockMenu villageFallback (only runs if spawn failed) -> /spawn block`,JSON.stringify({w:r.w,p:r.p,feet:r.feet,head:r.head,oldVillage:old}));}
(async()=>{L('=== EliteMobs/world-type spawn rig, sfx '+SFX+' spawn '+SP.join(',')+' '+new Date().toString());
 await cons('rigem worlds',600);
 const OP='Op'+SFX;await cons('op '+OP,500,true);const O=await join(OP);
 await cons(`execute in minecraft:overworld run setblock ${SP[0]} ${SP[1]-1} ${SP[2]} minecraft:stone`,300,true);await cons(`execute in minecraft:overworld run fill ${SP[0]} ${SP[1]} ${SP[2]} ${SP[0]} ${SP[1]+1} ${SP[2]} minecraft:air`,300,true);
 await cons(`execute in minecraft:overworld run minecraft:tp ${OP} ${SP[0]}.5 ${SP[1]} ${SP[2]}.5`,1200,true);
 O.chat('/setspawn');await sleep(1200);O.chat('/setspawn newbies');await sleep(1200);
 ref=await where(OP);L('  spawn set at '+JSON.stringify(ref));
 const E=await join('Ex'+SFX);await cons('effect give '+E.username+' minecraft:resistance infinite 255 true',300,true);
 await cons(`execute in minecraft:overworld run minecraft:tp ${E.username} ${SP[0]+400} 90 ${SP[2]-300}`,1200,true);E.chat('/spawn');await sleep(2000);const r0=await where(E.username);
 P(r0.w==='world'&&same(r0.p,ref.p),'/spawn reference lands on the set spawn',JSON.stringify(r0));
 for(const W of WORLDS){let lbl=W[0];
  if(/dragon alive/.test(lbl)){await put(E.username,W);await sleep(6000);await cons('rigem dragon',800);const d=await cons('rigem dragon',800,true);
   if(!/dragon=true/.test(d)){await cons('execute in minecraft:the_end run summon minecraft:ender_dragon 0 100 0 {DragonPhase:0}',2000);}
   const d2=await cons('rigem dragon',800,true);lbl+=' ['+(/RIGDRAGON.*/.exec(d2)||[''])[0].replace(/^RIGDRAGON /,'')+']';}
  if(/after dragon/.test(lbl)){await cons('execute in minecraft:the_end run minecraft:kill @e[type=minecraft:ender_dragon]',1000);await sleep(14000);const d=await cons('rigem dragon',800,true);lbl+=' ['+(/RIGDRAGON.*/.exec(d)||[''])[0].replace(/^RIGDRAGON /,'')+']';}
  await doWorld(E,W,lbl)}
 // brand-new player, first thing they do is leave from the Guild world
 const NN='Nw'+SFX+Date.now()%10000;const N=await join(NN);await sleep(1500);await cons('effect give '+NN+' minecraft:resistance infinite 255 true',300,true);
 const g=WORLDS[0];const sg=await put(NN,g);P(sg.w==='em_adventurers_guild','new player is inside em_adventurers_guild before testing',sg.w);T.since();await compassSpawn(N);let log=T.since();check('new player in Guild world','Java compass > Spawn > Yes',await where(NN),log);
 await put(NN,g);T.since();await cons('rigem ebmspawn '+NN,2500,true);log=T.since();check('new player in Guild world','Bedrock: ExcelBedrockMenu teleportToSpawn (real method)',await where(NN),log);
 const errs=T.fs.readFileSync(T.LOG,'utf8').split('\n').map(T.clean).filter(x=>/EliteMobs/.test(x)&&/(ERROR|WARN|Exception)/.test(x)&&!/outdated|FreeMinecraftModels/.test(x));
 L('  EliteMobs WARN/ERROR lines in whole log (minus known boot notices): '+errs.length);errs.slice(0,10).forEach(x=>L('    '+x.slice(0,220)));
 O.quit();E.quit();N.quit();await sleep(800);const f=R.filter(x=>x.startsWith('FAIL')).length;L('=== sfx '+SFX+': '+(R.length-f)+' pass, '+f+' fail');process.exit(0)})().catch(e=>{L('ERR '+e.stack);process.exit(1)});
