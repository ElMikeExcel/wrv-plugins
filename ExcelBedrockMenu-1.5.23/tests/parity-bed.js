// Parity fixes 2026-10-26, Bedrock side: bedrock-protocol client through Geyser + Floodgate on the box rig /workspace/parity-rig (UDP 19621).
// Phase a: compass Wand controls, Server Commands group 8 (Your Items & Help) + /cosmetics, stats map rows.
// Phase b: Guide Bedrock note (ExcelGuide 0.3.3) and What's New Bedrock hints (paired changelog).
process.on('uncaughtException',e=>{console.log('UNCAUGHT '+e.message);process.exit(5)});
const NM='/workspace/batch-next/bedrock-menu-parity-1522-20261026/tests/rig/node_modules/';
const bp=require(NM+'bedrock-protocol');const {execSync}=require('child_process');const fs=require('fs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const D='/workspace/parity-rig';
const rc=c=>execSync(`python3 ${D}/rcon.py ${JSON.stringify(c)}`).toString().replace(/§./g,'').trim();
const PH=process.env.PHASE||'a';
const OUT=__dirname+'/parity-bed-'+PH+'.out';fs.writeFileSync(OUT,'');const L=(...a)=>{const s=new Date().toTimeString().slice(0,8)+' '+a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const U='PtBed';const N='00000000-0000-0000-0000-000000000000';
function connect(keepNews){return new Promise((res,rej)=>{
 const c=bp.createClient({host:'127.0.0.1',port:19621,username:U,offline:true,version:'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
 c.msgs=[];c.forms=[];c.opens=[];
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
 c.on('text',p=>c.msgs.push((p.message||'').replace(/§./g,'')));
 c.on('container_open',p=>{c.opens.push(p);L('CONTAINER_OPEN')});
 c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};const f={id:p.form_id,d};
  if((d.title||'').startsWith("What's New")&&!c.keepNews){L('FORM news (closing)');c.queue('modal_form_response',{form_id:p.form_id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});return}
  c.forms.push(f);L('FORM',JSON.stringify(d).replace(/§./g,'').slice(0,500))});
 c.take=()=>c.msgs.splice(0).join('\n');
 c.cmd=s=>c.queue('command_request',{command:s,origin:{type:'player',uuid:N,request_id:'',player_entity_id:0n},internal:false,version:'52'});
 c.answer=(f,v)=>c.queue('modal_form_response',{form_id:f.id,has_response_data:true,data:JSON.stringify(v),has_cancel_reason:false});
 c.closeForm=f=>c.queue('modal_form_response',{form_id:f.id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});
 c.waitForm=async ms=>{const t=Date.now();while(Date.now()-t<ms){if(c.forms.length)return c.forms.shift();await sleep(250)}return null};
 c.bye=async()=>{try{c.disconnect('bye')}catch(e){try{c.close()}catch(e2){}}await sleep(3000)};
 c.once('spawn',()=>{L('spawned');res(c)});setTimeout(()=>rej(new Error('no spawn')),60000);
});}
setTimeout(()=>{L('TIMEOUT');process.exit(3)},240000);
const btns=f=>f?(f.d.buttons||[]).map(b=>(b.text||'').replace(/§./g,'')):[];
const guard=/Unknown command|isn't available|can't use|can't ask/i;
const csv=()=>fs.readFileSync(D+'/plugins/ExcelMenuStats/clicks.csv','utf8').trim().split('\n');
async function cmdForm(c,s){await sleep(500);c.forms.length=0;c.take();c.cmd(s);return await c.waitForm(8000)}
async function openGroup8(c){const g=await cmdForm(c,'/servercommands');const gb=btns(g);const i=gb.findIndex(t=>/Your Items & Help/.test(t));
 if(i<0)return {g,gb,f:null};c.answer(g,i);const f=await c.waitForm(6000);return {g,gb,f}}
(async()=>{
 L('=== parity-bed phase '+PH+' '+new Date().toString());
 let c=await connect();c.keepNews=false;await sleep(9000);c.forms.length=0;c.take();
 if(PH==='a'){
  const c0=csv().length;
  let mf=await cmdForm(c,'/menu');const mb=btns(mf);L('menu: '+mb.join(' | '));
  const wi=mb.indexOf('Wand controls: Classic or Sneak bars');ok(wi>=0,'compass has "Wand controls: Classic or Sneak bars" (button '+(wi+1)+')');
  c.take();c.answer(mf,wi);await sleep(2500);const wf=await c.waitForm(2500);const wm=c.take();
  L('after Wand controls: form='+(wf?JSON.stringify(wf.d).replace(/§./g,'').slice(0,300):'none')+' chat='+wm.slice(0,300));
  ok((!!wf||/wand|style|Classic|Sneak/i.test(wm))&&!guard.test(wm),'tap runs /wandstyle (ExcelWandBars answers)');if(wf)c.closeForm(wf);
  // Server Commands group list
  const {g,gb,f}=await openGroup8(c);L('groups: '+gb.join(' | '));
  fs.writeFileSync(__dirname+'/bedrock-servercommands-groups-captured.json',JSON.stringify(g?g.d:{},null,2));
  ok(g&&g.d.title==='Server Commands','/servercommands opens the Server Commands form');
  ok(gb.filter(t=>t!=='Close'&&!/^Back/.test(t)).length>=8&&/Your Items & Help/.test(gb[7]||''),'8 groups, Your Items & Help is group 8 (button 8)');
  const fb=btns(f);L('group 8: '+(f?JSON.stringify(f.d).replace(/§./g,'').slice(0,700):'none'));
  if(f)fs.writeFileSync(__dirname+'/bedrock-group8-captured.json',JSON.stringify(f.d,null,2));
  const pos=['/lostfound','/trash','/mark','/ask'].map(k=>fb.findIndex(t=>t.startsWith(k)));
  ok(pos.every(p=>p>=0)&&pos.join()==='0,1,2,3','group 8 buttons in order: /lostfound, /trash, /mark, /ask ('+fb.join(' | ')+')');
  // /lostfound
  if(f){c.take();c.answer(f,pos[0]);await sleep(2500);const x=await c.waitForm(2500);const m=c.take();L('lostfound: form='+(x?x.d.title:'none')+' chat='+m.slice(0,200));
   ok((!!x||m.length>0)&&!guard.test(m),'/lostfound runs (ExcelDeathRecord answers)');if(x)c.closeForm(x)}
  // /trash
  let r=await openGroup8(c);if(r.f){c.opens.length=0;c.take();c.answer(r.f,pos[1]);await sleep(2500);const m=c.take();const x=await c.waitForm(1500);
   L('trash: containers='+c.opens.length+' form='+(x?x.d.title:'none')+' chat='+m.slice(0,200));ok((c.opens.length>0||!!x)&&!guard.test(m),'/trash opens the trash bin');if(x)c.closeForm(x);
   if(c.opens.length){c.queue('container_close',{window_id:c.opens[0].window_id,window_type:c.opens[0].window_type,server:false});await sleep(1200)}}
  // /mark -> Maker's mark form
  r=await openGroup8(c);if(r.f){c.take();c.answer(r.f,pos[2]);const x=await c.waitForm(5000);const m=c.take();L('mark: '+(x?JSON.stringify(x.d).replace(/§./g,'').slice(0,300):'none')+' chat='+m.slice(0,200));
   ok(!!x&&x.d.title==="Maker's mark"&&!guard.test(m),"/mark opens the Maker's mark form (no typing)");if(x)c.closeForm(x)}
  // /ask -> Ask the Guide text box
  r=await openGroup8(c);if(r.f){c.take();c.answer(r.f,pos[3]);const x=await c.waitForm(5000);L('ask: '+(x?JSON.stringify(x.d).replace(/§./g,'').slice(0,300):'none'));
   ok(!!x&&x.d.type==='custom_form'&&x.d.title==='Ask the Guide','/ask opens the Ask the Guide text box');if(x)c.closeForm(x)}
  // /cosmetics in Shops & Trading
  const g2=await cmdForm(c,'/servercommands');const si=btns(g2).findIndex(t=>/Shops/.test(t));c.answer(g2,si);const sf=await c.waitForm(6000);const sb=btns(sf);L('shops group: '+sb.join(' | '));
  ok(sb.some(t=>t.startsWith('/cosmetics')),'/cosmetics is listed in Shops & Trading');
  const ci=sb.findIndex(t=>t.startsWith('/cosmetics'));if(sf&&ci>=0){c.take();c.opens.length=0;c.answer(sf,ci);await sleep(2500);const x=await c.waitForm(2500);const m=c.take();
   L('cosmetics: form='+(x?x.d.title:'none')+' chat='+m.slice(0,200)+' containers='+c.opens.length);ok((!!x||m.length>0||c.opens.length>0)&&!guard.test(m),'/cosmetics runs (ExcelCosmetics answers)');if(x)c.closeForm(x)}
  await sleep(2500);const t=csv().slice(c0);L('clicks.csv new rows:\n   '+t.join('\n   '));const tj=t.join('\n');
  ok(/,bedrock,server_menu,wandstyle,/.test(tj),'stats: Bedrock Wand controls counted as server_menu > wandstyle');
  for(const k of ['yi_lostfound','yi_trash','yi_mark','yi_ask','st_cosmetics'])ok(new RegExp(',bedrock,commands_menu_page2,'+k+',').test(tj),'stats: Bedrock tap counted as commands_menu_page2 > '+k);
 }
 if(PH==='b'){
  c.take();c.cmd('/ask How do I claim land?');await sleep(5000);const a=c.take();L('guide answer (bedrock): '+a.slice(0,700));
  ok(/right-click/i.test(a)&&/On a phone or tablet: right-click means press and hold, left-click means tap, and sneak means the crouch button\. On a controller: right-click is LT and left-click is RT\./.test(a),'Bedrock reader: same answer plus the "On a phone or tablet" note');
  c.keepNews=true;const nf=await cmdForm(c,'/whatsnew 4');const nt=nf?JSON.stringify(nf.d).replace(/§./g,''):'';L('whatsnew 4 (bedrock): '+nt.slice(0,2500));
  if(nf)fs.writeFileSync(__dirname+'/bedrock-whatsnew4-captured.json',JSON.stringify(nf.d,null,2));
  ok(/No typing: tap Trash Bin on the compass\./.test(nt),'Bedrock Oct 24 entry: /trash has "No typing: tap Trash Bin on the compass."');
  ok(!/Or click /.test(nt),'no Java "Or click" hints on Bedrock');
  if(nf)c.closeForm(nf);
  const n3=await cmdForm(c,'/whatsnew 2');const t3=n3?JSON.stringify(n3.d).replace(/§./g,''):'';L('whatsnew 2 (bedrock): '+t3.slice(0,1200));
  ok(/sneak \+ tap it on a phone or console/.test(t3),'trial vaults entry: platform-neutral reopen line');if(n3)c.closeForm(n3);
 }
 await c.bye();L('RESULT parity-bed phase '+PH+' '+pass+' passed '+fail+' failed');process.exit(0);
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
