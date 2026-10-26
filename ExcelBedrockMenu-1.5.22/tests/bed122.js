// ExcelBedrockMenu 1.5.22 Bedrock flow (Java-parity buttons): bedrock-protocol client through Geyser + Floodgate on the box rig (fake player).
process.on('uncaughtException',e=>{console.log('UNCAUGHT '+e.message);process.exit(5)});
const bp=require('bedrock-protocol');const {execSync}=require('child_process');const fs=require('fs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const rc=c=>execSync(`python3 /workspace/ebm122-rig/rcon.py ${JSON.stringify(c)}`).toString().replace(/§./g,'').trim();
const PH=process.env.PHASE||'a';
const OUT=__dirname+'/bed122-'+PH+'.out';fs.writeFileSync(OUT,'');const L=(...a)=>{const s=new Date().toTimeString().slice(0,8)+' '+a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const U='EbmBed';const N='<floodgate-test-uuid>';
function connect(){return new Promise((res,rej)=>{
 const c=bp.createClient({host:'127.0.0.1',port:19592,username:U,offline:true,version:'1.26.40',raknetBackend:(process.env.RAK||'jsp-raknet'),skipPing:true});
 c.msgs=[];c.forms=[];c.opens=[];
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
 c.on('text',p=>c.msgs.push((p.message||'').replace(/§./g,'')));
 c.on('container_open',p=>{c.opens.push(p);L('CONTAINER_OPEN')});
 c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};const f={id:p.form_id,d};
  if((d.title||'').startsWith("What's New")&&!c.keepAll){L('FORM news (closing)');c.queue('modal_form_response',{form_id:p.form_id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});return}
  c.forms.push(f);L('FORM',JSON.stringify(d).replace(/§./g,'').slice(0,400))});
 c.take=()=>c.msgs.splice(0).join('\n');
 c.cmd=s=>c.queue('command_request',{command:s,origin:{type:'player',uuid:N,request_id:'',player_entity_id:0n},internal:false,version:'52'});
 c.answer=(f,v)=>c.queue('modal_form_response',{form_id:f.id,has_response_data:true,data:JSON.stringify(v),has_cancel_reason:false});
 c.closeForm=f=>c.queue('modal_form_response',{form_id:f.id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});
 c.waitForm=async ms=>{const t=Date.now();while(Date.now()-t<ms){if(c.forms.length)return c.forms.shift();await sleep(250)}return null};
 c.bye=async()=>{try{c.disconnect('bye')}catch(e){try{c.close()}catch(e2){}}await sleep(3000)};
 c.once('spawn',()=>{L('spawned');res(c)});setTimeout(()=>rej(new Error('no spawn')),60000);
});}
setTimeout(()=>{L('TIMEOUT');process.exit(3)},300000);
const want=["Spawn","Teleport to a player","Party","Utility Pouch","Spellbook","Excel Shop","My Cosmetics","Adventurers Guild","Jobs & Money","Skills & Perks","Leaderboard","Lost & Found","Trash Bin","Server Info","Server Commands","Server Website","What's New","Ask the Guide","Wand controls: Classic or Sneak bars","Auto-sort my chests: on/off","Maker's mark and Gear stats","Auto-open on join"];
const IX=n=>want.indexOf(n);
async function openMenu(c){await sleep(500);c.forms.length=0;c.cmd('/menu');return await c.waitForm(8000)}
const guard=/Unknown command|isn't available|can't use|can't ask/i;
(async()=>{
 L('=== bed122 phase '+PH+' '+new Date().toString());
 let c=await connect();await sleep(6000);c.forms.length=0;c.take();let mf;
 if(PH==='a'){ // menu list + colors + My Cosmetics
  for(const p of ['excelcosmetics.use','excelguide.ask','excelmakersmark.use'])L(rc('lp user '+N+' permission unset '+p));
  mf=await openMenu(c);ok(!!mf,'compass /menu opens a form');
  const labels=mf?(mf.d.buttons||[]).map(b=>b.text.replace(/§./g,'')):[];L('menu buttons: '+labels.join(' | '));
  fs.writeFileSync(__dirname+'/bedrock-menu-form-captured.json',JSON.stringify(mf?mf.d:{},null,2));
  ok(mf&&mf.d.title==='Server Menu','form title Server Menu');
  ok(JSON.stringify(labels)===JSON.stringify(want),'button list (codes stripped) is exactly the 1.5.22 order (22 buttons)');
  const raw=mf?(mf.d.buttons||[]).map(b=>b.text):[];
  const G={'1':['Spawn','Teleport to a player','Party','Adventurers Guild'],'5':['Utility Pouch','Spellbook','Jobs & Money','Skills & Perks','Leaderboard','Lost & Found','Trash Bin'],'2':['Excel Shop','My Cosmetics'],'n':['Server Info','Server Commands','Server Website',"What's New",'Ask the Guide'],'8':['Wand controls: Classic or Sneak bars','Auto-sort my chests: on/off',"Maker's mark and Gear stats",'Auto-open on join']};
  let gok=raw.length===22;for(const k in G)for(const n of G[k]){const r=raw[IX(n)];if(!r||!r.startsWith('\u00a7'+k)){gok=false;L('  color wrong for '+n)}}
  ok(gok,'raw labels carry the group color: Travel 1, Me 5, Shops 2 (incl. My Cosmetics), Help n (incl. Ask the Guide), Settings 8 (incl. Maker\'s mark)');
  ok(raw.every((t,i)=>t.slice(2)===want[i]),'under the 2-char code each label text is the plain label');
  ok(!labels.includes('mcMMO Stats'),'no separate mcMMO Stats button (matches Java)');
  c.take();c.opens.length=0;c.answer(mf,IX('My Cosmetics'));await sleep(2500);
  const cf=await c.waitForm(3000);const cm=c.take();L('after My Cosmetics tap: form='+(cf?JSON.stringify(cf.d).replace(/§./g,'').slice(0,400):'none')+' chat='+cm.slice(0,300)+' containers='+c.opens.length);
  ok((!!cf||cm.length>0||c.opens.length>0)&&!guard.test(cm),'My Cosmetics runs /cosmetics (ExcelCosmetics answers), no unknown-command or guard message');
  ok(!!cf&&c.opens.length===0,'with a cosmetic owned (rig grant: Pink color), ExcelCosmetics answers the Bedrock player with its own form, not a chest');
  if(cf)fs.writeFileSync(__dirname+'/bedrock-cosmetics-form-captured.json',JSON.stringify(cf.d,null,2));
  if(cf)c.closeForm(cf);
 }
 if(PH==='b'){ // Ask the Guide
  mf=await openMenu(c);c.take();c.answer(mf,IX('Ask the Guide'));
  const af=await c.waitForm(5000);L('ask form: '+(af?JSON.stringify(af.d):'none'));
  if(af)fs.writeFileSync(__dirname+'/bedrock-ask-form-captured.json',JSON.stringify(af.d,null,2));
  ok(!!af&&af.d.type==='custom_form'&&af.d.title==='Ask the Guide','Ask the Guide opens a Bedrock text-box form titled Ask the Guide');
  ok(!!af&&af.d.content&&af.d.content.length===1&&af.d.content[0].type==='input'&&/How do I claim land/.test(af.d.content[0].placeholder||''),'form has one text box with an example question');
  if(af){c.answer(af,['How do I claim land?']);await sleep(4000)}
  const am=c.take();L('guide answer chat='+am.slice(0,600));
  ok(am.length>0&&!guard.test(am)&&!/Ask me anything: \/ask <question>/.test(am),'Send runs /ask <question> and the Guide answers in chat');
  mf=await openMenu(c);c.take();c.answer(mf,IX('Ask the Guide'));const af2=await c.waitForm(5000);
  if(af2){c.answer(af2,['   ']);await sleep(1500)}const em=c.take();L('empty send chat='+em);
  ok(/No question typed/.test(em),'blank Send gives the plain how-to message, runs nothing');
  mf=await openMenu(c);c.take();c.answer(mf,IX('Ask the Guide'));const af3=await c.waitForm(5000);
  if(af3){c.closeForm(af3);await sleep(1500)}const cl=c.take();L('closed form chat='+JSON.stringify(cl));
  ok(cl.trim().length===0,'closing the text box does nothing');
 }
 if(PH==='c'){ // Maker's mark + Skills & Perks
  mf=await openMenu(c);c.take();c.answer(mf,IX("Maker's mark and Gear stats"));
  const mk=await c.waitForm(5000);const mkm=c.take();L('mark form: '+(mk?JSON.stringify(mk.d).replace(/§./g,''):'none')+' chat='+mkm.slice(0,200));
  if(mk)fs.writeFileSync(__dirname+'/bedrock-mark-form-captured.json',JSON.stringify(mk.d,null,2));
  const mb=mk?(mk.d.buttons||[]).map(b=>b.text.replace(/§./g,'')):[];
  ok(!!mk&&mk.d.title==="Maker's mark"&&mb.length===3&&/^Maker's mark: (ON|OFF)/.test(mb[0])&&/^Gear stats: (ON|OFF)/.test(mb[1])&&mb[2]==='Close',"Maker's mark opens ExcelMakersMark's own two-switch form (Maker's mark, Gear stats, Close)");
  ok(!guard.test(mkm),'no unknown-command or guard message');
  if(mk){const was=/ON/.test(mb[0]);c.take();c.answer(mk,0);await sleep(2000);const t1=c.take();L('mark switch tap chat='+t1.slice(0,200));
   mf=await openMenu(c);c.answer(mf,IX("Maker's mark and Gear stats"));const mk2=await c.waitForm(5000);const mb2=mk2?(mk2.d.buttons||[]).map(b=>b.text.replace(/§./g,'')):[];
   ok(!!mk2&&(/ON/.test(mb2[0])!==was),'tapping the Maker\'s mark switch flips it (same as Java /mark on|off)');
   if(mk2){c.answer(mk2,0);await sleep(1500);c.take()}}
  await sleep(800);
  mf=await openMenu(c);c.take();c.answer(mf,IX('Skills & Perks'));
  const sp=await c.waitForm(5000);L('skills form: '+(sp?JSON.stringify(sp.d).replace(/§./g,''):'none'));
  if(sp)fs.writeFileSync(__dirname+'/bedrock-skillsperks-form-captured.json',JSON.stringify(sp.d,null,2));
  const sb=sp?(sp.d.buttons||[]).map(b=>b.text):[];
  ok(JSON.stringify(sb)===JSON.stringify(['My mcMMO stats','Skill Trees on the Website','Close']),'Skills & Perks buttons: My mcMMO stats, Skill Trees on the Website, Close');
  ok(!!sp&&/Magic is awake: 1 of 5 seals broken\./.test(sp.d.content)&&/Old Quartermaster in the spawn village/.test(sp.d.content)&&!/sealed for now/.test(sp.d.content),'Magic line says awake, 1 of 5 (rig GuildGate has 1 seal broken, like live), first wand from the Old Quartermaster');
  if(sp){c.take();c.answer(sp,0);await sleep(2500)}
  const ms=c.take();L('mcstats chat='+ms.slice(0,300));
  ok(/mcMMO|Stats|Level|skill/i.test(ms)&&!guard.test(ms),'My mcMMO stats runs /mcstats (mcMMO answers)');
 }
 if(PH==='d'){ // denied paths
  for(const p of ['excelcosmetics.use','excelguide.ask','excelmakersmark.use'])L(rc('lp user '+N+' permission set '+p+' false'));await sleep(3000);
  // ExcelCosmetics puts no permission on /cosmetics (excelcosmetics.use is not checked), so there is no
  // denied path to test: the button runs /cosmetics exactly like the Java button does. Logged only.
  mf=await openMenu(c);c.take();c.answer(mf,IX('My Cosmetics'));await sleep(2000);let d1=c.take();const x1=await c.waitForm(1200);L('cosmetics with excelcosmetics.use=false (info only): chat='+d1.slice(0,120)+' form='+(x1?x1.d.title:'none'));if(x1)c.closeForm(x1);
  mf=await openMenu(c);c.take();c.answer(mf,IX('Ask the Guide'));await sleep(2000);let d2=c.take();const x2=await c.waitForm(1200);L('denied ask chat='+d2);
  ok(/can't ask the Guide/.test(d2)&&!x2,'without excelguide.ask: plain denied message, no text box');
  mf=await openMenu(c);c.take();c.answer(mf,IX("Maker's mark and Gear stats"));await sleep(2000);let d3=c.take();const x3=await c.waitForm(1200);L('denied mark chat='+d3);
  ok(/can't use Maker's mark/.test(d3)&&!x3,"without excelmakersmark.use: plain denied message, no form");
  for(const p of ['excelcosmetics.use','excelguide.ask','excelmakersmark.use'])L(rc('lp user '+N+' permission unset '+p));
 }
 if(PH==='e'){ // optional Java skills_perks.yml fix, as ExcelMenu renders it (Bedrock form of the Java chest)
  c.forms.length=0;L(rc('excelmenu open skills_perks .EbmBed'));const jf=await c.waitForm(6000);
  const js=jf?JSON.stringify(jf.d).replace(/§./g,''):'';L('ExcelMenu skills_perks form: '+js.slice(0,900));
  if(jf)fs.writeFileSync(__dirname+'/excelmenu-skills-perks-form-captured.json',JSON.stringify(jf.d,null,2));
  ok(/Magic \(Awake\)/.test(js)&&!/Sealed/.test(js),'fixed Java skills_perks.yml: Magic item reads Magic (Awake)');
  ok(/Seals broken: 1 of 5/.test(js)&&!/%guildgate/.test(js),'seal count placeholder resolves (1 of 5 on the rig, like live)');
  ok(/Old Quartermaster/.test(js),'first-wand line present');
  if(jf)c.closeForm(jf);
 }
 await c.bye();
 L('RESULT bed122 phase '+PH+' '+pass+' passed '+fail+' failed');process.exit(0);
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
