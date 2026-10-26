// ExcelWhatsNew Bedrock flow (bedrock-protocol client through Geyser + Floodgate on the box rig).
process.on('uncaughtException',e=>{console.log('UNCAUGHT '+e.message);process.exit(5)});
const bp=require('/workspace/batch-next/whatsnew/tests/rig/node_modules/bedrock-protocol');const {execSync}=require('child_process');const fs=require('fs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const rc=c=>execSync(`python3 /workspace/whatsnew-rig/rcon.py ${JSON.stringify(c)}`).toString().replace(/§./g,'').trim();
const OUT='/workspace/batch-next/whatsnew-newer-20261026/tests/rig/bedrock-newer.out';const L=(...a)=>{const s=new Date().toTimeString().slice(0,8)+' '+a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const U='WNNewerB';const N='.'+U;
function connect(){return new Promise((res,rej)=>{
 const c=bp.createClient({host:'127.0.0.1',port:19581,username:U,offline:true,version:'1.26.40',raknetBackend:(process.env.RAK||'jsp-raknet'),skipPing:true});
 c.msgs=[];c.forms=[];c.other=[];c.moving=false;let pos,tick=0n;
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
 c.on('start_game',p=>{pos={...p.player_position};});
 c.on('move_player',p=>{if(pos&&p.runtime_id!==undefined&&String(p.runtime_id)===String(c.rid))pos={...p.position}});
 c.on('start_game',p=>{c.rid=p.runtime_entity_id});
 c.on('text',p=>c.msgs.push((p.message||'').replace(/§./g,'')));
 c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};const f={id:p.form_id,d,at:Date.now()};
  if((d.title||'').startsWith("What's New")){c.forms.push(f);L('FORM ours',JSON.stringify(d).slice(0,200))}
  else if(c.keepOther){c.other.push(f);L('FORM other (kept)',JSON.stringify(d).slice(0,200))}
  else{L('FORM other (closing)',JSON.stringify(d).slice(0,120));c.queue('modal_form_response',{form_id:p.form_id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'})}});
 let k=0;c.mover=setInterval(()=>{if(!c.moving||!pos)return;k++;const dx=(k%40<20?0.05:-0.05);pos.x+=dx;tick++;
  try{c.queue('player_auth_input',{pitch:0,yaw:0,position:pos,move_vector:{x:0,z:1},head_yaw:0,input_data:undefined,input_mode:'mouse',play_mode:'normal',interaction_model:'crosshair',interact_rotation:{x:0,z:0},tick,delta:{x:dx,y:0,z:0},transaction_presence:false,transaction:undefined,item_stack_request_presence:false,item_stack_request:undefined,block_action_presence:false,block_action:undefined,vehicle_rotation_presence:false,vehicle_rotation:undefined,predicted_vehicle_presence:false,predicted_vehicle:undefined,analogue_move_vector:{x:0,z:1},camera_orientation:{x:0,y:0,z:1},raw_move_vector:{x:0,z:1}})}catch(e){if(k<3)L('move err '+e.message)}},50);
 c.take=()=>c.msgs.splice(0).join('\n');
 c.cmd=s=>c.queue('command_request',{command:s,origin:{type:'player',uuid:'00000000-0000-0000-0000-000000000000',request_id:'',player_entity_id:0n},internal:false,version:'52'});
 c.answer=(f,btn)=>c.queue('modal_form_response',{form_id:f.id,has_response_data:true,data:JSON.stringify(btn),has_cancel_reason:false});
 c.closeForm=f=>c.queue('modal_form_response',{form_id:f.id,has_response_data:false,has_cancel_reason:true,cancel_reason:'closed'});
 c.waitForm=async ms=>{const t=Date.now();while(Date.now()-t<ms){if(c.forms.length)return c.forms.shift();await sleep(250)}return null};
 c.waitOther=async ms=>{const t=Date.now();while(Date.now()-t<ms){if(c.other.length)return c.other.shift();await sleep(250)}return null};
 c.bye=async()=>{clearInterval(c.mover);try{c.disconnect('bye')}catch(e){try{c.close()}catch(e2){}}await sleep(3000)};
 c.once('spawn',()=>{L('spawned');res(c)});setTimeout(()=>rej(new Error('no spawn')),60000);
});}

setTimeout(()=>{L('TIMEOUT');process.exit(3)},300000);
const btns=f=>(f.d.buttons||[]).map(x=>x.text.replace(/§./g,''));
(async()=>{
 L('=== bedrock newer '+new Date().toString());
 L(rc('whatsnew reset '+N));
 let c=await connect();await sleep(4000);
 // standing still: no join form; open entry 3 by command
 c.cmd('/whatsnew 3');let f=await c.waitForm(15000);ok(!!f,'Bedrock: /whatsnew 3 opens a form');
 if(f){L('entry 3 buttons '+JSON.stringify(btns(f)));ok(JSON.stringify(btns(f))===JSON.stringify(['Newer news','Older news','Close']),'entry 3 buttons: Newer news, Older news, Close');
  const t3=f.d.content; c.answer(f,0);let g=await c.waitForm(15000);ok(!!g,'tap Newer news opens another form');
  if(g){L('entry 2 buttons '+JSON.stringify(btns(g)));ok(g.d.content!==t3&&JSON.stringify(btns(g))===JSON.stringify(['Newer news','Older news','Close']),'Newer news went to entry 2 (different text, same 3 buttons)');
   c.answer(g,1);let h=await c.waitForm(15000);ok(h&&h.d.content===t3,'Older news from entry 2 goes back to entry 3 (same text)');
   if(h){c.answer(h,0);g=await c.waitForm(15000);c.answer(g,0);let n=await c.waitForm(15000);
    if(n){L('newest buttons '+JSON.stringify(btns(n)));ok(JSON.stringify(btns(n))===JSON.stringify(['OK','Older news']),'Newer twice reaches the newest: OK, Older news (no Newer)');ok(/Mend/i.test(n.d.content),'newest form is the Mend news');
     c.answer(n,0);await sleep(1500);ok(/Got it/.test(c.take()),'OK on the newest still says Got it');}
    else ok(false,'newest form after two Newer taps');}}}
 c.cmd('/whatsnew 8');f=await c.waitForm(15000);if(f){L('entry 8 buttons '+JSON.stringify(btns(f)));ok(JSON.stringify(btns(f))===JSON.stringify(['Newer news','Close']),'last entry: Newer news, Close');
  c.answer(f,1);const extra=await c.waitForm(4000);ok(!extra,'Close (button 1 on the last entry) opens nothing');}else ok(false,'entry 8 form');
 c.cmd('/whatsnew');f=await c.waitForm(15000);if(f){L('newest after OK buttons '+JSON.stringify(btns(f)));ok(JSON.stringify(btns(f))===JSON.stringify(['Older news','Close']),'newest after OK: Older news, Close');c.closeForm(f);await sleep(800);}
 c.take();c.cmd('/whatsnew list');await sleep(2000);const lt=c.take();L('list tail: '+lt.split('\n').slice(-1)[0]);
 ok(/Type \/whatsnew 2 to \/whatsnew 8 to read an older one/.test(lt)&&!/or 3/.test(lt),'Bedrock list hint: /whatsnew 2 to /whatsnew 8');
 await c.bye();
 L('RESULT bedrock-newer '+pass+' passed '+fail+' failed');process.exit(0)
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
