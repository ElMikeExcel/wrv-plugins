// Rig test for ExcelClaimFly 0.1.0 (box rig /workspace/claimfly-rig: Paper 26.3, offline, GP 16.18.7, LP 5.5.87,
// Vault + EssentialsX economy, ExcelMenu 0.1.5 with the STAGED shop menus, floodgate + Geyser). Java bots via ViaVersion.
const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');const fs=require('fs');
const D='/workspace/claimfly-rig',IN=D+'/rigconsole',LOG=D+'/boot.log',S='/workspace/batch-next/claimfly-20261025';
const OUT=S+'/tests/rig-test.log';fs.writeFileSync(OUT,'');
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const o=t.slice(mark);mark=t.length;return o};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*/,'');
const cons=async(c,ms=1200)=>{since();fs.appendFileSync(IN,c+'\n');await sleep(ms);const o=since().split('\n').filter(x=>x.trim()).map(clean);L('$ '+c);for(const x of o)L('   '+x);return o.join('\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const info=async n=>{const o=await cons('buildfly info '+n,900);const m=/ClaimFly info .*/.exec(o);return m?m[0]:''};
function mk(n){const b=mf.createBot({host:'127.0.0.1',port:26061,username:n,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;b.writes=false;
 const ow=b._client.write.bind(b._client);b._client.write=(nm,p)=>{if(!b.writes&&(nm==='position'||nm==='look'||nm==='position_look'))return;return ow(nm,p)};
 b.live=on=>{b.writes=on;b.physicsEnabled=on};b.fly=()=>ow('abilities',{flags:2});
 b.msgs=[];b.titles=[];b.bars=[];
 b.on('message',(j,pos)=>{const s=j.toString();if(pos==='game_info'){b.bars.push(s);return}b.msgs.push(s)});
 b.on('actionBar',j=>b.bars.push(j.toString()));
 b._client.on('packet',(d,m)=>{if(m.name==='action_bar'||(m.name==='system_chat'&&(d.isActionBar||d.overlay))){const r=JSON.stringify(d.text??d.content??d);b.bars.push([...r.matchAll(/"text":\{"type":"string","value":"([^"]*)"\}/g)].map(x=>x[1]).join(''))}});
 b.on('title',(t,type)=>b.titles.push(type+':'+(typeof t==='string'?t:JSON.stringify(t))));
 b.on('kicked',r=>L(n+' kicked '+JSON.stringify(r)));b.on('error',e=>L(n+' err '+e));return b}
const join=n=>new Promise((res,rej)=>{const b=mk(n);const t=setTimeout(()=>rej(new Error('join timeout '+n)),30000);b.once('spawn',()=>{clearTimeout(t);res(b)})});
const has=(b,re)=>b.msgs.some(m=>re.test(m));const cnt=(b,re)=>b.msgs.filter(m=>re.test(m)).length;
const win=(b,ms=6000)=>new Promise((res,rej)=>{const t=setTimeout(()=>rej(new Error('no window')),ms);b.once('windowOpen',w=>{clearTimeout(t);res(w)})});
const iname=it=>{if(!it)return 'empty';let c=it.customName;try{if(c&&typeof c!=='string')c=JSON.stringify(c)}catch(e){};return it.name+(c?' "'+String(c).replace(/\\u00a7./g,'')+'"':'')};
const dump=b=>{L('--- '+b.username+' chat:\n   '+b.msgs.filter(m=>m.trim()).join('\n   '));L('--- titles: '+b.titles.join(' || '));b.msgs.length=0;b.titles.length=0};
const bal=async n=>{const r=await cons('balance '+n,900);const m=/\$([\d,\.]+)/.exec(r);return m?Number(m[1].replace(/,/g,'')):NaN};
const C={x:200,z:200};const G=-60;
const PL=D+'/plugins/ExcelClaimFly/purchases.log';const plMark=fs.existsSync(PL)?fs.readFileSync(PL,'utf8').length:0; // journal is append-only across runs; count only this run's lines
(async()=>{try{
 L('=== ExcelClaimFly rig test '+new Date().toString());
 for(const c of ['difficulty easy','time set noon','gamerule doDaylightCycle false','gamerule advance_time false','gamerule doMobSpawning false','gamerule spawn_mobs false','minecraft:kill @e[type=!player]','lp user CfOwner permission unset excelclaimfly.pass','buildfly reload'])await cons(c,900);
 let o=await cons('version ExcelClaimFly',1500);
 // ---- players
 const own=await join('CfOwner'),noc=await join('CfNoClaim'),poor=await join('CfPoor');await sleep(2000);
 for(const n of ['CfOwner','CfNoClaim','CfPoor']){await cons('lp user '+n+' permission unsettemp excelclaimfly.pass',700);await cons('adjustbonusclaimblocks '+n+' 5000',700);await cons('gamemode survival '+n,500);await cons('effect clear '+n,500)}
 await cons(`minecraft:tp CfOwner ${C.x} ${G} ${C.z}`);await cons(`minecraft:tp CfPoor 400 ${G} 400`);await cons(`minecraft:tp CfNoClaim 600 ${G} 600`);await sleep(1500);
 await cons('minecraft:item replace entity CfOwner weapon.mainhand with golden_shovel');await cons('minecraft:item replace entity CfPoor weapon.mainhand with golden_shovel');await sleep(800);own.chat('/claim 15');poor.chat('/claim 10');await sleep(2500);L('owner chat after /claim: '+own.msgs.join(' | '));L('poor chat after /claim: '+poor.msgs.join(' | '));own.msgs.length=0;poor.msgs.length=0;
 o=await cons('eco set CfOwner 2500');await cons('eco set CfNoClaim 5000');await cons('eco set CfPoor 500');
 // ---- 1 refusals, no charge
 noc.msgs.length=0;o=await cons('buildfly confirm CfNoClaim',1500);
 ok(has(noc,/Make a claim first/),'no claim: refused with the make-a-claim message');ok(await bal('CfNoClaim')===5000,'no claim: balance still $5,000');
 o=await cons('buildfly confirm CfPoor',1500);ok(has(poor,/\$1,000/)&&!has(poor,/Build Flight pass: 30 minutes/),'no money: refused, no pass');ok(await bal('CfPoor')===500,'no money: balance still $500');dump(noc);dump(poor);
 // ---- 2 buy through the STAGED shop menus: /shop -> Travel & Animals -> Build Flight Pass -> confirm chest -> Buy
 let w=win(own);own.chat('/shop');let W=await w;L('shop window: '+W.title+' slots0-8: '+W.slots.slice(0,9).map(iname).join(', '));
 w=win(own);await own.clickWindow(2,0,0);W=await w;L('travel window: '+W.title+' slots0-8: '+W.slots.slice(0,9).map(iname).join(', '));
 ok(W.slots[5]&&W.slots[5].name==='feather'&&/Build Flight Pass/.test(iname(W.slots[5])),'shop: Build Flight Pass feather in Travel & Animals slot 5');
 ok(W.slots[6]&&W.slots[6].name==='gold_nugget'&&W.slots[7]&&W.slots[7].name==='arrow','shop: balance moved to 6, back to 7');
 w=win(own);await own.clickWindow(5,0,0);W=await w;L('confirm window: '+W.title+' 11='+iname(W.slots[11])+' 13='+iname(W.slots[13])+' 15='+iname(W.slots[15]));
 ok(W.slots[11]&&W.slots[11].name==='lime_concrete'&&/Buy 30 min for \$1,000/.test(iname(W.slots[11])),'Java confirm: lime "Buy 30 min for $1,000" at slot 11');
 ok(await bal('CfOwner')===2500,'confirm screen open: no charge yet');
 await own.clickWindow(11,0,0);await sleep(2000);
 ok(await bal('CfOwner')===1500,'buy: charged $1,000 (2500 -> 1500)');
 ok(has(own,/Build Flight pass: 30 minutes/)&&has(own,/Double-tap jump to fly\. Double-tap again to stop\./)&&has(own,/You land safe, no fall damage\./),'buy: how-it-works chat lines');
 ok(own.titles.some(t=>/Build Flight on/.test(t))&&own.titles.some(t=>/Double-tap jump in your claim/.test(t)),'buy: title + subtitle');
 let i=await info('CfOwner');ok(/left=29:[0-5]\d/.test(i)&&/managed=true/.test(i)&&/allowFlight=true/.test(i)&&/inOwnClaim=true/.test(i),'after buy: 29:xx left, allow-flight on in claim');
 o=await cons('lp user CfOwner permission info',1500);ok(/excelclaimfly\.pass/.test(o)&&/expires|expiry|\d+m/i.test(o),'LuckPerms: temporary excelclaimfly.pass node');
 dump(own);
 // ---- 3 re-buy extends; cap
 await cons('eco set CfOwner 5000');o=await cons('buildfly confirm CfOwner',1500);
 ok(has(own,/Added 30 minutes to your Build Flight pass\. You now have (59 min|1 hr) left\./),'re-buy: extends to ~60 min');ok(own.titles.some(t=>/\+30 min/.test(t)),'re-buy: extended title');ok(await bal('CfOwner')===4000,'re-buy charged $1,000');
 await cons('buildfly give CfOwner 45');o=await cons('buildfly confirm CfOwner',1500);ok(await bal('CfOwner')===4000,'cap: ~105 min + 30 > 120 refused, no charge');L('cap msg: '+own.msgs.slice(-2).join(' | '));
 dump(own);
 // ---- 4 status
 own.chat('/buildfly');await sleep(1200);ok(has(own,/Build Flight: (1 hr 4\d min|1 hr 45 min) left \(1:4\d:\d\d\)/),'/buildfly status shows time left');L('status: '+own.msgs.join(' | '));own.msgs.length=0;
 // ---- 5 fly in claim, action bar
 await cons(`minecraft:tp CfOwner ${C.x} ${G+10} ${C.z}`);await sleep(600);own.fly();await sleep(1500);
 i=await info('CfOwner');ok(/flying=true/.test(i),'in claim: double-tap (abilities flying) accepted, flying=true');
 ok(own.bars.some(t=>/Build Flight 1:4\d:\d\d left/.test(t)),'action bar: "Build Flight h:mm:ss left" while flying');L('bars: '+[...new Set(own.bars)].slice(-3).join(' | '));
 // ---- 6 fall-damage control: a player with no pass falling 10 blocks gets hurt
 let h0=poor.health;await cons('damage CfPoor 7 minecraft:fall',1200);L('control fall health '+h0+' -> '+poor.health);ok(poor.health<h0,'control: 7 fall damage hurts a player with no safe landing');await cons('effect give CfPoor instant_health 1 5');
 // ---- 7 leave claim while flying: flight off, safe landing
 own.msgs.length=0;await cons(`minecraft:tp CfOwner ${C.x+60} ${G+10} ${C.z}`);await sleep(1500);
 ok(has(own,/You left your claim, so flight is off\. Land safe\./),'leave claim: message');i=await info('CfOwner');ok(/allowFlight=false/.test(i)&&/flying=false/.test(i)&&/safeLanding=true/.test(i),'leave claim: flight off, safe landing armed');
 h0=own.health;await cons('damage CfOwner 7 minecraft:fall',1200);L('leave-claim fall health '+h0+' -> '+own.health);ok(own.health>=h0,'leave claim: landing fall damage cancelled');
 i=await info('CfOwner');ok(/safeLanding=false/.test(i),'safe landing used up after one landing');
 h0=own.health;await cons('damage CfOwner 2 minecraft:fall',1200);ok(own.health<h0,'a later fall (no flight) hurts normally: one landing only');await cons('effect give CfOwner instant_health 1 5');
 // ---- 8 back in claim
 own.msgs.length=0;await cons(`minecraft:tp CfOwner ${C.x} ${G} ${C.z}`);await sleep(1500);ok(has(own,/You are in your claim\. Double-tap jump to fly\./),'re-enter: hint');i=await info('CfOwner');ok(/allowFlight=true/.test(i),'re-enter: allow-flight back');
 // ---- 9 mob hit while flying: pause, land safe, timer runs, resumes after quiet
 const up=async()=>{await cons(`minecraft:tp CfOwner ${C.x} ${G+10} ${C.z}`,600);own.fly();await sleep(1200)};
 await up();i=await info('CfOwner');ok(/flying=true/.test(i),'flying before mob hit');
 await cons(`summon zombie ${C.x+2} ${G} ${C.z} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["cf"]}`);
 own.msgs.length=0;own.bars.length=0;let hBefore=own.health;await cons('damage CfOwner 1 minecraft:mob_attack by @e[type=zombie,tag=cf,limit=1]',1500);
 ok(has(own,/^Flight paused, you got hit\. Land safe\.$/),'mob hit: "Flight paused, you got hit. Land safe."');
 i=await info('CfOwner');ok(/allowFlight=false/.test(i)&&/combat=true/.test(i)&&/safeLanding=true/.test(i),'mob hit: flight off, combat tagged, safe landing armed');
 ok(own.bars.some(t=>/Flight paused \(fight\) \| \d+:\d\d:\d\d left/.test(t)),'mob hit: paused action bar with timer still counting');
 let h1=own.health;await cons('damage CfOwner 7 minecraft:fall',1200);L('mob-hit health '+hBefore+' -> '+h1+' (hit) -> '+own.health+' (after landing)');ok(own.health>=h1,'mob hit: landing took NO fall damage');
 own.msgs.length=0;await sleep(11000);ok(has(own,/^Flight is back\. Double-tap jump to fly\.$/),'after 10 s quiet: "Flight is back"');i=await info('CfOwner');ok(/allowFlight=true/.test(i)&&/combat=false/.test(i),'after quiet: allow-flight back on');
 await cons('minecraft:kill @e[tag=cf]');
 // ---- 10 projectile hit (arrow shot by a skeleton)
 await up();await cons(`summon skeleton ${C.x+4} ${G} ${C.z} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["cf"]}`);await cons(`summon arrow ${C.x} ${G+11} ${C.z} {NoGravity:1b,Tags:["cf"]}`);
 own.msgs.length=0;await cons('damage CfOwner 1 minecraft:arrow by @e[type=arrow,tag=cf,limit=1] from @e[type=skeleton,tag=cf,limit=1]',1500);
 ok(has(own,/^Flight paused, you got hit\. Land safe\.$/),'arrow hit: paused with the same message');i=await info('CfOwner');ok(/allowFlight=false/.test(i)&&/safeLanding=true/.test(i),'arrow hit: flight off, safe landing');
 h1=own.health;await cons('damage CfOwner 7 minecraft:fall',1200);ok(own.health>=h1,'arrow hit: landing took NO fall damage');await cons('minecraft:kill @e[tag=cf]');await sleep(11000);
 // ---- 11 attacking a mob while flying also pauses
 await up();await cons(`summon zombie ${C.x+1} ${G+10} ${C.z} {NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b,Tags:["cf"]}`,1500);
 const z=Object.values(own.entities).find(e=>e.name==='zombie');own.msgs.length=0;if(z){own.attack(z);await sleep(1500)}
 ok(z&&has(own,/^Flight paused while you fight\. Land safe\.$/),'attacking a mob while flying: paused');h1=own.health;await cons('damage CfOwner 7 minecraft:fall',1200);ok(own.health>=h1,'attack pause: landing took NO fall damage');
 await cons('minecraft:kill @e[tag=cf]');await sleep(11000);
 // ---- 12 combat rule is one config flip
 const cfg=D+'/plugins/ExcelClaimFly/config.yml';const orig=fs.readFileSync(cfg,'utf8');fs.writeFileSync(cfg,orig.replace(/pause-flight: true/,'pause-flight: false'));
 o=await cons('buildfly reload');ok(/combat pause OFF/.test(o),'flip: reload says combat pause OFF');
 await up();await cons(`summon zombie ${C.x+2} ${G} ${C.z} {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["cf"]}`);own.msgs.length=0;
 await cons('damage CfOwner 1 minecraft:mob_attack by @e[type=zombie,tag=cf,limit=1]',1500);i=await info('CfOwner');
 ok(/flying=true/.test(i)&&!has(own,/Flight paused/),'flip OFF: hit does not stop flight');
 fs.writeFileSync(cfg,orig);o=await cons('buildfly reload');ok(/combat pause ON/.test(o),'flip back ON');await cons('minecraft:kill @e[tag=cf]');
 await cons(`minecraft:tp CfOwner ${C.x} ${G} ${C.z}`);
 // ---- 13 expiry with warnings (LP temp node 70 s)
 await cons('lp user CfOwner permission unsettemp excelclaimfly.pass');await sleep(1200);dump(own);
 await cons('lp user CfOwner permission settemp excelclaimfly.pass true 70s',1500);await sleep(1000);await up();
 await sleep(4000);ok(own.titles.some(t=>/Build Flight: 2 min left/.test(t))&&has(own,/Build Flight: 2 min left\. Find a spot to land\./),'warning: first warning (5-min rule) as title + chat');
 await sleep(14000);ok(has(own,/Build Flight: 1 min left\. Find a spot to land\./),'warning: 1-minute warning');
 await sleep(58000);ok(own.titles.some(t=>/Build Flight ended/.test(t))&&has(own,/Your Build Flight pass ran out\. Land safe\. Get another in the Spawn Shop\./),'expiry: title + chat');
 i=await info('CfOwner');ok(/allowFlight=false/.test(i)&&/safeLanding=true/.test(i),'expiry: flight off, safe landing armed');h1=own.health;await cons('damage CfOwner 7 minecraft:fall',1200);ok(own.health>=h1,'expiry: landing took NO fall damage');dump(own);
 // ---- 14 runs out while logged off (time keeps counting); stale allow-flight cleared on join
 await cons('lp user CfOwner permission settemp excelclaimfly.pass true 15s',1500);await sleep(1500);i=await info('CfOwner');ok(/allowFlight=true/.test(i),'15 s pass: allow-flight on');
 own.quit();await sleep(20000);const own2=await join('CfOwner');await sleep(2500);i=await info('CfOwner');
 ok(/allowFlight=false/.test(i),'rejoin after pass ran out offline: allow-flight cleared');ok(has(own2,/ran out/),'rejoin: told the pass ran out');dump(own2);
 // ---- 15 trusted builder in someone else's claim: not allowed by default
 await cons('lp user CfPoor permission settemp excelclaimfly.pass true 5m');await cons(`minecraft:tp CfPoor ${C.x+3} ${G} ${C.z}`);own2.chat('/trust CfPoor');await sleep(2000);i=await info('CfPoor');ok(/allowFlight=false/.test(i)&&/inOwnClaim=false/.test(i),'trusted builder in owner claim: no flight (owner-only default)');
 await cons('lp user CfPoor permission unsettemp excelclaimfly.pass');
 o=fs.readFileSync(PL,'utf8').slice(plMark);L('purchases.log:\n'+o.replace(/[0-9a-f-]{36}/g,'<uuid>'));ok((o.match(/\tBUY\t/g)||[]).length===1&&(o.match(/\tEXTEND\t/g)||[]).length===1,'journal: 1 BUY + 1 EXTEND');
 own2.quit();noc.quit();poor.quit();
}catch(e){L('ERROR '+e.stack);fail++}
 L('RESULT pass='+pass+' fail='+fail);process.exit(fail?1:0)})();
