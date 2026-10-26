// ExcelBedrockMenu 1.5.20 Bedrock flow: bedrock-protocol client through Geyser + Floodgate on the box rig (fake player).
process.on('uncaughtException',e=>{console.log('UNCAUGHT '+e.message);process.exit(5)});
const bp=require('bedrock-protocol');const {execSync}=require('child_process');const fs=require('fs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const rc=c=>execSync(`python3 /workspace/ebm120-rig/rcon.py ${JSON.stringify(c)}`).toString().replace(/§./g,'').trim();
const OUT=__dirname+'/'+(process.env.OUTN||'bed120.out');fs.writeFileSync(OUT,'');const L=(...a)=>{const s=new Date().toTimeString().slice(0,8)+' '+a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const U='EbmBed';const N='00000000-0000-0000-0000-000000000000';
function connect(){return new Promise((res,rej)=>{
 const c=bp.createClient({host:'localhost',port:19591,username:U,offline:true,version:'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
 c.msgs=[];c.forms=[];c.opens=[];
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
 c.on('text',p=>c.msgs.push((p.message||'').replace(/§./g,'')));
 c.on('container_open',p=>{c.opens.push(p);L('CONTAINER_OPEN',JSON.stringify(p,(k,v)=>typeof v==='bigint'?v.toString():v).slice(0,200))});
 c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};const f={id:p.form_id,d};
  if((d.title||'').startsWith("What's New")&&!c.keepAll){L('FORM news (closing)');c.queue('modal_form_response',{form_id:p.form_id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});return}
  c.forms.push(f);L('FORM',JSON.stringify(d).replace(/§./g,'').slice(0,300))});
 c.take=()=>c.msgs.splice(0).join('\n');
 c.cmd=s=>c.queue('command_request',{command:s,origin:{type:'player',uuid:'00000000-0000-0000-0000-000000000000',request_id:'',player_entity_id:0n},internal:false,version:'52'});
 c.answer=(f,btn)=>c.queue('modal_form_response',{form_id:f.id,has_response_data:true,data:JSON.stringify(btn),has_cancel_reason:false});
 c.closeForm=f=>c.queue('modal_form_response',{form_id:f.id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});
 c.waitForm=async ms=>{const t=Date.now();while(Date.now()-t<ms){if(c.forms.length)return c.forms.shift();await sleep(250)}return null};
 c.bye=async()=>{try{c.disconnect('bye')}catch(e){try{c.close()}catch(e2){}}await sleep(3000)};
 c.once('spawn',()=>{L('spawned');res(c)});setTimeout(()=>rej(new Error('no spawn')),60000);
});}
setTimeout(()=>{L('TIMEOUT');process.exit(3)},300000);
const want=["Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","Adventurers Guild","Jobs & Money","mcMMO Stats","Skills & Perks","Leaderboard","Lost & Found","Trash Bin","Server Info","Server Commands","Server Website","What's New","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Auto-open on join"];
async function openMenu(c){await sleep(500);c.forms.length=0;c.cmd('/menu');const f=await c.waitForm(8000);return f}
(async()=>{
 L('=== bed120 '+new Date().toString());
 L(rc('lp user '+N+' permission unset exceldeathrecord.lostfound'));L(rc('lp user '+N+' permission unset exceltrash.use'));
 let c=await connect();await sleep(6000);c.forms.length=0;
 let mf=await openMenu(c);ok(!!mf,'compass /menu opens a form');
 const labels=mf?(mf.d.buttons||[]).map(b=>b.text.replace(/§./g,'')):[];L('menu buttons: '+labels.join(' | '));
 fs.writeFileSync(__dirname+'/bedrock-menu-form-captured.json',JSON.stringify(mf?mf.d:{},null,2));
 ok(mf&&mf.d.title==='Server Menu','form title Server Menu');
 ok(JSON.stringify(labels)===JSON.stringify(want),'button list is exactly the 1.5.20 order (20 buttons)');
 ok(labels.indexOf('Lost & Found')===11&&labels.indexOf('Trash Bin')===12,'Lost & Found 12th, Trash Bin 13th, right after Leaderboard');
 ok(mf&&(mf.d.buttons||[]).every(b=>!b.image),'no button images (same as before)');
 // Lost & Found tap
 c.take();c.answer(mf,11);await sleep(2500);
 let lf=await c.waitForm(4000);const lfm=c.take();L('after Lost & Found tap: form='+(lf?JSON.stringify(lf.d).replace(/§./g,'').slice(0,400):'none')+' chat='+lfm.slice(0,400));
 ok(!!lf||/lost|found|price|nothing/i.test(lfm),'Lost & Found tap reaches ExcelDeathRecord /lostfound (form or chat answer)');
 ok(!/Unknown command|isn't available|can't use/i.test(lfm),'no unknown-command or guard message');
 if(lf)c.closeForm(lf);await sleep(1000);
 // Trash Bin tap
 mf=await openMenu(c);c.opens.length=0;c.take();c.answer(mf,12);await sleep(3000);
 const tm=c.take();L('after Trash Bin tap chat='+tm.slice(0,300));
 ok(c.opens.length>0,'Trash Bin tap opens a container (the /trash bin inventory)');
 ok(!/Unknown command|isn't available|can't use/i.test(tm),'no unknown-command or guard message');
 await c.bye();L('rejoin after the bin (waiting 65 s for the login throttle)');await sleep(65000);c=await connect();await sleep(6000);c.forms.length=0;c.take();
 // permission denied path
 L(rc('lp user '+N+' permission set exceldeathrecord.lostfound false'));L(rc('lp user '+N+' permission set exceltrash.use false'));L(rc('lp user '+N+' permission check exceltrash.use'));await sleep(3000);
 mf=await openMenu(c);c.take();c.answer(mf,11);await sleep(2000);let d1=c.take();L('denied LF chat='+d1);ok(/can't use Lost & Found/.test(d1),'without exceldeathrecord.lostfound: plain denied message');
 let x=await c.waitForm(1500);ok(!x,'no Lost & Found form when denied');
 mf=await openMenu(c);c.opens.length=0;c.take();c.answer(mf,12);await sleep(2000);let d2=c.take();L('denied trash chat='+d2);ok(/can't use the Trash Bin/.test(d2)&&c.opens.length===0,'without exceltrash.use: plain denied message, no bin opens');
 L(rc('lp user '+N+' permission unset exceldeathrecord.lostfound'));L(rc('lp user '+N+' permission unset exceltrash.use'));
 // an unchanged button still works: Leaderboard index 10 and What's New 16
 mf=await openMenu(c);c.take();c.answer(mf,10);await sleep(2000);let lb=c.take();const lbf=await c.waitForm(1500);L('leaderboard chat='+lb.slice(0,200)+' form='+(lbf?lbf.d.title:'none'));ok(lb.length>0||!!lbf,'Leaderboard button (unchanged) still answers');if(lbf)c.closeForm(lbf);
 c.keepAll=true;mf=await openMenu(c);c.answer(mf,16);const wn=await c.waitForm(5000);ok(!!wn&&/What's New/.test(wn.d.title||''),'What\'s New button (moved 2 down) still opens the news');if(wn)c.closeForm(wn);
 await c.bye();
 L('RESULT bed120 '+pass+' passed '+fail+' failed');process.exit(0);
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
