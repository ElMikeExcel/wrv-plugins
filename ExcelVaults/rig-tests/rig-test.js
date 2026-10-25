// Rig test for ExcelVaults 0.1.0. Box rig /workspace/excelvaults-rig: Paper 26.3, offline, flat world,
// EssentialsX economy via Vault, ExcelGuildGate 0.3.3, ExcelAuditLog 0.2.0, Geyser + floodgate, Via for the Java bot.
// Console via RCON (port 26262). Plugin log lines read from boot.log.
const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');const {Vec3}=require('/workspace/ship1025-tests/node_modules/vec3');
const fs=require('fs'),net=require('net');
const D='/workspace/excelvaults-rig',LOG=D+'/boot.log',S='/workspace/batch-next/excelvaults-20261025';
const OUT=S+'/tests/rig-test.log';fs.writeFileSync(OUT,'');
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const o=t.slice(mark);mark=t.length;return o.replace(/\x1b\[[0-9;]*m/g,'')};
// --- minimal RCON
let rc,rbuf=Buffer.alloc(0),rid=10,waiters={};
function rconConnect(){return new Promise((res,rej)=>{rc=net.connect(26262,'127.0.0.1',()=>{send(1,3,process.env.RIG_RCON_PASSWORD||'');waiters[1]=()=>res()});rc.on('error',rej);
 rc.on('data',d=>{rbuf=Buffer.concat([rbuf,d]);while(rbuf.length>=4){const n=rbuf.readInt32LE(0);if(rbuf.length<4+n)break;const id=rbuf.readInt32LE(4);const body=rbuf.slice(12,4+n-2).toString();rbuf=rbuf.slice(4+n);if(waiters[id]){const w=waiters[id];delete waiters[id];w(body)}}})})}
function send(id,type,body){const b=Buffer.from(body,'utf8');const p=Buffer.alloc(14+b.length);p.writeInt32LE(10+b.length,0);p.writeInt32LE(id,4);p.writeInt32LE(type,8);b.copy(p,12);rc.write(p)}
const strip=s=>s.replace(/\u00a7./g,'');
const cons=async(c,ms=700)=>{since();const id=++rid;const r=await new Promise(res=>{waiters[id]=res;send(id,2,c)});await sleep(ms);const lg=since().split('\n').filter(x=>/ExcelVaults|AuditLog|GuildGate/.test(x));L('$ '+c);const out=strip(r);if(out.trim())L('   '+out.trim().replace(/\n/g,'\n   '));for(const x of lg)L('   log: '+x.trim());return out+'\n'+lg.join('\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
function mk(n){const b=mf.createBot({host:'127.0.0.1',port:26261,username:n,version:'1.21.11',auth:'offline'});b.msgs=[];b.physicsEnabled=false;
 const ow=b._client.write.bind(b._client);b._client.write=(nm,p)=>{if(nm==='position'||nm==='look'||nm==='position_look'||nm==='player_input'&&!p.__ok)return;if(p&&p.__ok)delete p.__ok;return ow(nm,p)};
 b.sneak=on=>ow('player_input',{inputs:{shift:!!on}});b.seq=1;
 b.use=(x,y,z)=>{const q=b.entity.position;const dx=x+0.5-q.x,dy=y+0.5-(q.y+1.62),dz=z+0.5-q.z;const yaw=-Math.atan2(dx,dz)*180/Math.PI,pitch=-Math.atan2(dy,Math.hypot(dx,dz))*180/Math.PI;ow('position_look',{x:q.x,y:q.y,z:q.z,yaw,pitch,flags:{onGround:true,hasHorizontalCollision:false}});ow('tick_end',{});const face=Math.abs(dx)>Math.abs(dz)?(dx>0?4:5):(dz>0?2:3);ow('block_place',{hand:0,location:{x,y,z},direction:face,cursorX:face==4?0:face==5?1:0.5,cursorY:0.5,cursorZ:face==2?0:face==3?1:0.5,insideBlock:false,worldBorderHit:false,sequence:b.seq++})};
 b.on('message',(j,pos)=>{if(pos==='game_info')return;b.msgs.push(j.toString())});b.on('kicked',r=>L(n+' kicked '+JSON.stringify(r)));b.on('error',e=>L(n+' err '+e));return b}
const join=n=>new Promise((res,rej)=>{const b=mk(n);const t=setTimeout(()=>rej(new Error('join timeout '+n)),30000);b.once('spawn',()=>{clearTimeout(t);res(b)})});
const has=(b,re)=>b.msgs.some(m=>re.test(m));
const win=(b,ms=5000)=>new Promise((res,rej)=>{const t=setTimeout(()=>rej(new Error('no window')),ms);b.once('windowOpen',w=>{clearTimeout(t);res(w)})});
const iname=it=>{if(!it)return 'empty';let c=it.customName;try{if(c&&typeof c!=='string')c=JSON.stringify(c)}catch(e){};return it.name+(c?' "'+String(c).replace(/\\u00a7./g,'')+'"':'')};
const dump=b=>{L('--- '+b.username+' chat:\n   '+b.msgs.filter(m=>m.trim()).join('\n   '));b.msgs.length=0};
const bal=async n=>{const r=await cons('balance '+n,300);const m=/\$([\d,\.]+)/.exec(r);return m?Number(m[1].replace(/,/g,'')):NaN};
const intArr=u=>{const h=u.replace(/-/g,'');const a=[];for(let i=0;i<4;i++){let v=parseInt(h.substr(i*8,8),16);if(v>0x7fffffff)v-=0x100000000;a.push(v)}return a};
const rewarded=async(x,y,z)=>{const r=await cons(`data get block ${x} ${y} ${z} server_data.rewarded_players`,200);return /found no elements|Found no elements/.test(r)?[]:r};
const isRew=async(pos,u)=>{const r=await cons(`data get block ${pos} server_data.rewarded_players`,200);const a=intArr(u);return r.includes(a.join(', '))||r.includes(a.join(','))};
const N='5 -60 5',O='9 -60 5',P='13 -60 5',Q='17 -60 5';
(async()=>{try{
 await rconConnect();
 L('=== ExcelVaults rig test '+new Date().toString());
 for(const c of ['difficulty peaceful','time set noon','gamerule advance_time false','gamerule spawn_mobs false','forceload add 0 0 31 31','guildgate reset','vaults reload'])await cons(c,500);
 for(const pos of [N,P,Q])await cons(`setblock ${pos} minecraft:air`,200);await cons(`setblock ${O} minecraft:air`,200);
 await cons(`setblock ${N} minecraft:vault`,300);
 await cons(`setblock ${O} minecraft:vault[ominous=true]{config:{key_item:{id:"minecraft:ominous_trial_key",count:1},loot_table:"minecraft:chests/trial_chambers/reward_ominous"}}`,300);
 await cons(`setblock ${P} minecraft:vault`,300);await cons(`setblock ${Q} minecraft:vault`,300);
 const bot=await join('VtJava');await sleep(1500);const U=bot.player.uuid;L('bot uuid '+U);
 const other=await join('VtOther');await sleep(800);
 await cons('vaults reset VtJava',300);await cons('gamemode survival VtJava',200);await cons('clear VtJava',200);await cons('eco set VtJava 10000',300);
 await cons('minecraft:tp VtJava 7.5 -60 8.5 180 0',300);await cons('minecraft:tp VtOther 30 -60 30',300);await sleep(1500);
 const click=async(pos,sneak=false)=>{const [x,y,z]=pos.split(' ').map(Number);const b=bot.blockAt(new Vec3(x,y,z));if(!b||b.name!=='vault'){L('no vault block seen at '+pos+': '+(b&&b.name));return}
  if(sneak){bot.sneak(true);await sleep(400)}bot.use(x,y,z);await sleep(sneak?900:1200);if(sneak){bot.sneak(false);await sleep(300)}};
 const key=async(k='trial_key')=>{await cons(`minecraft:item replace entity VtJava weapon.mainhand with minecraft:${k} 1`,250);await sleep(300)};
 const empty=async()=>{await cons('minecraft:item replace entity VtJava weapon.mainhand with minecraft:air',250);await sleep(300)};

 // ---- 1 vanilla open is recorded
 ok(!(await isRew(N,U)),'fresh vault: bot not rewarded');
 await empty();bot.msgs.length=0;await click(N);ok(bot.msgs.length===0,'unused vault + empty hand: no hint (vanilla)');
 await key();since();await click(N);await sleep(1500);let lg=since();L('   log: '+lg.split('\n').filter(x=>/ExcelVaults/.test(x)).join(' | '));
 ok(await isRew(N,U),'open with trial key: vanilla rewards the bot');ok(/Vault opened: VtJava at world 5 -60 5, reopens in 24h 0m/.test(lg),'open recorded: "reopens in 24h 0m"');
 let st=await cons('vaults status VtJava');ok(/world 5 -60 5 normal left=2[34]h/.test(st),'status shows the 24h cooldown');
 // ---- 2 hint
 await empty();bot.msgs.length=0;await click(N);dump(bot);
 // re-collect (dump cleared); click again after hint cooldown
 await sleep(3200);await click(N);
 ok(has(bot,/This vault opens again for you in 2[34]h \d+m\./),'hint: "This vault opens again for you in 23h 59m"');
 ok(has(bot,/Sneak \+ right-click it to reopen it now for \$750\./),'hint: pay option $750 (just locked = full price)');
 ok(has(bot,/\[Reopen now: \$750\]/),'hint: clickable Java button');
 dump(bot);
 await sleep(1000);bot.msgs.length=0;await click(N);ok(!has(bot,/opens again/),'hint is rate-limited (3 s)');
 // ---- 3 sneak prompt (Java GUI), cancel = no charge
 await sleep(3200);let w=win(bot);await click(N,true);let W=null;try{W=await w}catch(e){L('no window: '+e)}
 ok(W&&/Reopen trial vault/.test(JSON.stringify(W.title)),'sneak + right-click: "Reopen trial vault" screen');
 if(W){L('window: 11='+iname(W.slots[11])+' 13='+iname(W.slots[13])+' 15='+iname(W.slots[15]));
  ok(W.slots[11]&&W.slots[11].name==='lime_concrete'&&/Reopen for \$750/.test(iname(W.slots[11])),'Java confirm: lime "Reopen for $750"');
  ok(await bal('VtJava')===10000,'screen open: no charge yet');await bot.clickWindow(15,0,0);await sleep(1200);}
 ok(await bal('VtJava')===10000&&await isRew(N,U),'cancel: no charge, vault still locked');
 // ---- 4 not enough money
 await cons('eco set VtJava 500',300);bot.msgs.length=0;await sleep(3200);w=win(bot);await click(N,true);try{W=await w;await bot.clickWindow(11,0,0);await sleep(1500)}catch(e){L('no window '+e)}
 ok(has(bot,/You need \$750 to reopen this vault\. You have \$500\./),'poor: refused with price and balance');ok(await bal('VtJava')===500&&await isRew(N,U),'poor: no charge, still locked');dump(bot);
 // ---- 5 buy
 await cons('eco set VtJava 10000',300);await sleep(3200);w=win(bot);await click(N,true);since();try{W=await w;await bot.clickWindow(11,0,0);await sleep(2000)}catch(e){L('no window '+e)}
 lg=since();ok(await bal('VtJava')===9250,'buy: charged $750 (10,000 -> 9,250)');ok(!(await isRew(N,U)),'buy: bot removed from rewarded list (Paper API)');
 ok(has(bot,/Vault reopened for \$750\. Use a trial key on it to open it\./),'buy: confirmation message');ok(/Reopen-now REOPENED: VtJava paid \$750/.test(lg),'buy: console line');dump(bot);
 const pc=fs.readFileSync(D+'/plugins/ExcelVaults/purchases.csv','utf8').trim().split('\n');L('purchases.csv last: '+pc[pc.length-1]);
 ok(/,VtJava,.*,java,normal,world 5 -60 5,750\.00,14\d\d,9250\.00,REOPENED$/.test(pc[pc.length-1]),'purchases.csv line (type, vault, price, minutes skipped, balance, result)');
 const day=new Date().toLocaleDateString('en-CA',{timeZone:'America/Chicago'});const af=D+'/plugins/ExcelAuditLog/audit-'+day+'.csv';
 let al=[];for(let t=0;t<10;t++){al=fs.existsSync(af)?fs.readFileSync(af,'utf8').split('\n').filter(x=>/,vault,/.test(x)&&/for \$750\.00/.test(x)):[];if(al.length)break;await sleep(1000)}L('audit vault lines: '+al.slice(-1).join(''));
 ok(al.some(x=>/VtJava.*,vault,.*reopen-now reopened normal vault world 5 -60 5 for \$750\.00/.test(x)),'ExcelAuditLog CSV has the "vault" purchase line');
 
 await cons('op VtJava',300);bot.msgs.length=0;bot.chat('/audit VtJava 1 all');await sleep(3000);const aum=bot.msgs.join('\n');L('   /audit chat: '+aum.split('\n').filter(x=>/vault|reopen/i.test(x)).slice(0,3).join(' | '));
 ok(/reopen-now reopened/.test(aum),'/audit VtJava 1 all (as op) shows the vault purchase');await cons('deop VtJava',300);bot.msgs.length=0;
 // ---- 6 open again, scaled price at 12h left, buy through /vaults reopen
 await key();await click(N);await sleep(1200);ok(await isRew(N,U),'reopened vault opens again with a key');
 await cons('vaults age VtJava 720',300);await empty();bot.msgs.length=0;await sleep(3200);await click(N);
 ok(has(bot,/opens again for you in 12h 0m/)&&has(bot,/for \$375\./),'12h left: hint says 12h 0m and $375 (half price)');dump(bot);
 w=win(bot);bot.chat('/vaults reopen world 5 -60 5');try{W=await w;ok(/Reopen for \$375/.test(iname(W.slots[11])),'/vaults reopen opens the same screen at $375');await bot.clickWindow(11,0,0);await sleep(1500)}catch(e){ok(false,'/vaults reopen screen '+e)}
 ok(await bal('VtJava')===8875&&!(await isRew(N,U)),'/vaults reopen buy: charged $375, reopened');dump(bot);
 // ---- 7 timer reopen by the sweep (no click)
 await key();await click(N);await sleep(1200);ok(await isRew(N,U),'opened again');await empty();
 bot.msgs.length=0;await cons('vaults age VtJava 1441',300);await sleep(7000);
 ok(!(await isRew(N,U)),'24h passed: sweep re-allowed it without a click');ok(has(bot,/A trial vault is ready for you again at world 5 -60 5\. Bring a trial key\./),'ready message');dump(bot);
 st=await cons('vaults status VtJava');ok(/0 cooldowns/.test(st),'cooldown record cleared after reopen');
 // ---- 8 /vaults list
 await key();await click(N);await sleep(1200);bot.msgs.length=0;bot.chat('/vaults');await sleep(1200);
 ok(has(bot,/Your trial vaults waiting to reopen:/)&&has(bot,/world 5 -60 5 \(normal\) opens in 2[34]h \d+m \[Reopen now: \$(750|745)\]/),'/vaults lists the vault with time and price');dump(bot);
 // ---- 9 vault locked before install: timer starts on first click, nothing mass-unlocked
 const a=intArr(U);await cons(`data modify block ${P} server_data.rewarded_players set value [[I;${a.join(',')}]]`,300);
 const ao=intArr(other.player.uuid);await cons(`data modify block ${Q} server_data.rewarded_players set value [[I;${ao.join(',')}]]`,300);
 ok(await isRew(P,U),'pre-install lock set on vault P');await sleep(6000);ok(await isRew(P,U)&&await isRew(Q,other.player.uuid),'no mass unlock: pre-locked vaults stay locked (sweep ignores unknown)');
 await empty();await cons('minecraft:tp VtJava 13.5 -60 8.5 180 0',300);await sleep(1500);bot.msgs.length=0;since();await click(P);lg=since();
 ok(/Cooldown started on first click after install: VtJava at world 13 -60 5/.test(lg),'first click on a pre-locked vault starts its timer');
 ok(has(bot,/opens again for you in 24h 0m/),'pre-locked vault hint: 24h 0m');ok(await isRew(P,U),'pre-locked vault stays locked');dump(bot);
 // ---- 10 ominous: gated before seal 2
 await cons('minecraft:tp VtJava 9.5 -60 8.5 180 0',300);await sleep(1500);await key('ominous_trial_key');since();await click(O);await sleep(1500);lg=since();
 ok(await isRew(O,U),'ominous vault opens (vanilla) with an ominous key');ok(/Vault opened: VtJava at world 9 -60 5 \(ominous\), reopens in 72h 0m once seal 2 is broken/.test(lg),'ominous open recorded silently, waits for seal 2');
 await empty();bot.msgs.length=0;await sleep(3200);await click(O);ok(has(bot,/ominous vault is used up for now\. Ominous vaults start reopening after the second seal falls\./),'gated note on click');ok(!has(bot,/\$/),'gated: no price offered');
 await sleep(3200);bot.msgs.length=0;let gotWin=false;bot.once('windowOpen',()=>gotWin=true);await click(O,true);ok(!gotWin,'gated: sneak does not open a pay screen');
 bot.msgs.length=0;bot.chat('/vaults reopen world 9 -60 5');await sleep(1200);ok(has(bot,/used up for now/)&&await bal('VtJava')===8875,'gated: /vaults reopen refused, no charge');
 await cons('vaults age VtJava 5000',300);await sleep(6500);ok(await isRew(O,U),'gated: 83h later still locked (seal 0)');
 await cons('guildgate grant ender_dragon',800);await sleep(6500);ok(await isRew(O,U),'1 seal: still locked');
 bot.msgs.length=0;await cons('guildgate grant wither',800);await sleep(7000);ok(!(await isRew(O,U)),'seal 2 broken: sweep re-allows the ominous vault (time already up)');ok(has(bot,/ready for you again at world 9 -60 5\. Bring a ominous trial key/),'ominous ready message');dump(bot);
 // ---- 11 ominous pay + daily limit 2
 for(let i=1;i<=3;i++){await key('ominous_trial_key');await click(O);await sleep(1200);await empty();await sleep(3200);bot.msgs.length=0;const b0=await bal('VtJava');w=win(bot);await click(O,true);
  try{W=await w;L('ominous window 11='+iname(W.slots[11]));if(i===1)ok(/Reopen for \$3,000/.test(iname(W.slots[11])),'ominous just locked: $3,000');await bot.clickWindow(11,0,0);await sleep(1500)}catch(e){L('no window '+e)}
  const b1=await bal('VtJava');L('ominous buy '+i+': '+b0+' -> '+b1);
  if(i<3)ok(b0-b1===3000&&!(await isRew(O,U)),'ominous paid reopen '+i+' of 2: charged $3,000');
  else ok(b1===b0&&has(bot,/You already reopened 2 ominous vaults with money today\. The limit is 2 a day\./)&&await isRew(O,U),'3rd ominous paid reopen today refused (limit 2), no charge');
  if(i===1)await cons('eco set VtJava 20000',300);dump(bot)}
 // ---- 12 reset (admin), permission
 bot.msgs.length=0;bot.chat('/vaults reset VtJava');await sleep(1000);ok(has(bot,/No permission/),'non-op: /vaults reset refused');
 st=await cons('vaults reset VtJava',300);await sleep(6000);ok(!(await isRew(N,U))&&!(await isRew(O,U))&&!(await isRew(P,U)),'admin reset: all recorded vaults reopen (normal, ominous at seal 2, pre-locked)');
 ok(await isRew(Q,other.player.uuid),'admin reset of one player leaves others alone');
 // ---- 13 persistence across restart
 await cons('minecraft:tp VtJava 7.5 -60 8.5 180 0',300);await sleep(1500);await key();await click(N);await sleep(1200);ok(await isRew(N,U),'restart prep: vault opened again (fresh 24h cooldown)');await cons('vaults age VtJava 60',300);st=await cons('vaults status VtJava');const before=(/left=(\S+ \S+)/.exec(st)||[])[1];
 bot.quit();other.quit();await sleep(800);await cons('save-all',1500);
 fs.writeFileSync(S+'/tests/.restart','1');L('restart requested; before='+before);
 L(`=== TOTAL part 1: ${pass} pass, ${fail} fail`);rc.end();process.exit(fail?1:0);
}catch(e){L('ERROR '+e.stack);L(`=== TOTAL part 1: ${pass} pass, ${fail} fail`);process.exit(2)}})();
