// Tip + What's New check for the Build Flight staged texts (box rig /workspace/claimfly-rig, staged tips.yml and changelog.yml
// loaded, ExcelTips 0.1.0 + ExcelWhatsNew 0.1.0 jars copied from the port-warning rig). Java bot via ViaVersion.
const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');const fs=require('fs');
const D='/workspace/claimfly-rig',IN=D+'/rigconsole',LOG=D+'/boot.log',S='/workspace/batch-next/claimfly-20261025';
const OUT=S+'/tests/tip-test.log';fs.writeFileSync(OUT,'');
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const o=t.slice(mark);mark=t.length;return o};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*/,'');
const cons=async(c,ms=1200)=>{since();fs.appendFileSync(IN,c+'\n');await sleep(ms);const o=since().split('\n').filter(x=>x.trim()).map(clean);L('$ '+c);for(const x of o)L('   '+x);return o.join('\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
(async()=>{try{
 L('=== Build Flight tip + What\'s New test '+new Date().toString());
 let o=await cons('tips reload',1500);ok(/24 tips/.test(o),'tips reload: 24 tips');
 o=await cons('whatsnew reload',1500);ok(/4 entries/.test(o)&&/2026-10-25-build-flight/.test(o),'whatsnew reload: 4 entries, newest build-flight');
 o=await cons('tips check',1500);ok(!/build-flight/.test(o),'tips check: no warning for build-flight');
 for(const c of ['deleteallclaims CfTip','tips reset CfTip','whatsnew reset CfTip'])await cons(c,900);
 const b=mf.createBot({host:'127.0.0.1',port:26061,username:'CfTip',version:'1.21.11',auth:'offline'});b.physicsEnabled=false;
 const ow=b._client.write.bind(b._client);b._client.write=(nm,p)=>{if(nm==='position'||nm==='look'||nm==='position_look')return;return ow(nm,p)}; // same as rig-test.js: no move packets from a physics-off bot
 b.on('kicked',r=>L('CfTip kicked '+JSON.stringify(r)));b.on('end',r=>L('CfTip end '+r));
 const msgs=[];b.on('message',(j,pos)=>{if(pos!=='game_info')msgs.push(j.toString())});
 await new Promise((res,rej)=>{const t=setTimeout(()=>rej(new Error('join timeout')),30000);b.once('spawn',()=>{clearTimeout(t);res()})});
 await sleep(2000);
 for(const c of ['gamemode survival CfTip','adjustbonusclaimblocks CfTip 5000','minecraft:tp CfTip 1000 -60 1000','minecraft:item replace entity CfTip weapon.mainhand with golden_shovel'])await cons(c,800);
 await sleep(12000);msgs.length=0; // past the join news + any join tips
 const t0=Date.now();b.chat('/claim 8');await sleep(4000);
 L('after /claim (4 s): '+msgs.join(' | '));
 ok(msgs.some(m=>/Claim created/.test(m)),'claim created');
 ok(!msgs.some(m=>/Want to fly while you build\?/.test(m)),'build-flight tip waits (not inside the 10 s tip gap)');
 await sleep(10000);L('after 14 s: '+msgs.join(' | '));
 ok(msgs.some(m=>/Want to fly while you build\? A Build Flight pass lets you fly inside your own claim for 30 minutes \(\$1,000\)\./.test(m)),'build-flight tip line 1 after making a claim');
 ok(msgs.some(m=>/Get it in the Spawn Shop, Travel & Animals\. Double-tap jump to fly\. \/buildfly shows your time left\./.test(m)),'build-flight tip line 2');
 o=await cons('tips status CfTip',1200);ok(/build-flight/.test(o),'tips status lists build-flight for the player');
 msgs.length=0;b.chat('/whatsnew');await sleep(2500);const wn=msgs.join('\n');L('--- /whatsnew (Java chat):\n'+wn);
 ok(/New: Build Flight pass/.test(wn),'What\'s New shows "New: Build Flight pass" first');
 ok(/\/buildfly/.test(wn)&&/30 minutes for \$1,000/.test(wn)&&/only inside your own claim/.test(wn),'What\'s New entry lines');
 ok(!/\u2014/.test(wn),'no em dash');
 msgs.length=0;b.chat('/buildfly');await sleep(1500);L('/buildfly: '+msgs.join(' | '));
 ok(msgs.some(m=>/You have no Build Flight pass\. Get one in the Spawn Shop, Travel & Animals, for \$1,000\./.test(m)),'/buildfly with no pass names the Spawn Shop');
 await cons('deleteallclaims CfTip',900);b.quit();await sleep(1000);
}catch(e){L('ERROR '+e.stack);fail++}
 L('RESULT pass='+pass+' fail='+fail);process.exit(fail?1:0)})();
