// ExcelWhatsNew By date: Bedrock flow (bedrock-protocol client through Geyser +
// Floodgate on the box rig). WRITTEN FOR 0.1.2 against the Nov 13, 2026 snapshot
// (96 entries). FOR THE 0.1.3 RIG (later leg, see BUILD-RECORD.md): re-pin counts
// to the rig-day snapshot, AND flip the day walk: a day tap opens the day's OLDEST
// entry, the form title carries the position "(1/M)" stepping to "(M/M)", buttons
// at walk start are [Newer news, Back, Close] (the pending-OK case moves to the
// walk END form: [OK, Older news, Back]), and the walk advances via Newer news.
// Section 4 and the Oct 26 part of section 7 change direction; the rest stands.
process.on('uncaughtException',e=>{console.log('UNCAUGHT '+e.message);process.exit(5)});
const bp=require('/workspace/batch-next/whatsnew/tests/rig/node_modules/bedrock-protocol');const {execSync}=require('child_process');const fs=require('fs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const rc=c=>execSync(`python3 /workspace/whatsnew-rig/rcon.py ${JSON.stringify(c)}`).toString().replace(/§./g,'').trim();
const OUT='/workspace/batch-next/whatsnew-bydate-20261113/tests/rig/bedrock-bydate.out';const L=(...a)=>{const s=new Date().toTimeString().slice(0,8)+' '+a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const U='WNByDateB';const N='.'+U;
function connect(){return new Promise((res,rej)=>{
 const c=bp.createClient({host:'127.0.0.1',port:19581,username:U,offline:true,version:'1.26.40',raknetBackend:(process.env.RAK||'jsp-raknet'),skipPing:true});
 c.msgs=[];c.forms=[];c.other=[];c.moving=false;let pos,tick=0n;
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
 c.on('start_game',p=>{pos={...p.player_position};});
 c.on('move_player',p=>{if(pos&&p.runtime_id!==undefined&&String(p.runtime_id)===String(c.rid))pos={...p.position}});
 c.on('start_game',p=>{c.rid=p.runtime_entity_id});
 c.on('text',p=>c.msgs.push((p.message||'').replace(/§./g,'')));
 c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};const f={id:p.form_id,d,at:Date.now()};
  if((d.title||'').startsWith("What's New")||(d.title||'')==='Recent news'||/2026$/.test(d.title||'')){c.forms.push(f);L('FORM ours',JSON.stringify(d).slice(0,220))}
  else if(c.keepOther){c.other.push(f);L('FORM other (kept)',JSON.stringify(d).slice(0,120))}
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
 L('=== bedrock bydate '+new Date().toString());
 L(rc('whatsnew reset '+N));
 let c=await connect();await sleep(4000);
 // 1. Compass path: /whatsnew opens the newest entry WITH a By date button (B1).
 c.cmd('/whatsnew');let f=await c.waitForm(15000);ok(!!f,'Bedrock: /whatsnew opens the newest entry form');
 if(f){L('newest buttons '+JSON.stringify(btns(f)));
  ok(JSON.stringify(btns(f))===JSON.stringify(['OK','Older news','By date']),'newest entry buttons: OK, Older news, By date');
  // 2. By date -> months: exactly November 72, October 24, Back.
  c.answer(f,2);let m=await c.waitForm(15000);ok(!!m,'By date opens the months form');
  if(m){L('months title '+m.d.title+' buttons '+JSON.stringify(btns(m)));
   ok(m.d.title==='What\'s New — By date','months form title is What\'s New — By date');
   ok(JSON.stringify(btns(m))===JSON.stringify(['November 2026 — 72 changes','October 2026 — 24 changes','Back']),'months: November 72, October 24, Back');
   // 3. November -> days newest first, counts, no Nov 14; Back -> months.
   c.answer(m,0);let dy=await c.waitForm(15000);ok(!!dy,'November opens the days form');
   if(dy){L('nov days title '+dy.d.title+' buttons '+JSON.stringify(btns(dy)));
    ok(dy.d.title==='November 2026','days form title is November 2026');
    const db=btns(dy);
    ok(db[0]==='Fri, Nov 13 — 5 changes','days: Fri, Nov 13 — 5 changes first');
    ok(db[db.length-2]==='Sat, Nov 1 — 4 changes'&&db[db.length-1]==='Back','days: Nov 1 last before Back');
    ok(!db.some(x=>x.includes('Nov 14')),'days: no Nov 14 row');
    // 4. Nov 13 -> newest entry of the day, with Back; walk Older through all 5.
    c.answer(dy,0);let e13=await c.waitForm(15000);ok(!!e13,'Nov 13 opens its newest entry');
    if(e13){L('nov13 buttons '+JSON.stringify(btns(e13)));
     ok(/section of the compass menu has its own colour/i.test(e13.d.content),'Nov 13 form is the section-colours entry');
     ok(JSON.stringify(btns(e13))===JSON.stringify(['OK','Older news','Back']),'Nov 13 newest (still the pending global newest): OK, Older news, Back (no Close)');
     let steps=0,cur=e13;while(true){const bb=btns(cur);const oi=bb.indexOf('Older news');if(oi<0)break;c.answer(cur,oi);cur=await c.waitForm(15000);if(!cur)break;steps++;if(steps>10)break}
     ok(steps===4,'Older walks all 5 Nov 13 entries and stops ('+steps+' steps)');
     if(cur){L('nov13 oldest buttons '+JSON.stringify(btns(cur)));
      ok(JSON.stringify(btns(cur))===JSON.stringify(['Newer news','Back','Close']),'Nov 13 oldest: Newer news, Back, Close (no Older)');
      // 5. Back out level by level: days -> months -> list.
      const bi=btns(cur).indexOf('Back');c.answer(cur,bi);let back1=await c.waitForm(15000);
      ok(!!back1&&back1.d.title==='November 2026','Back from the day entry returns to the November days');
      if(back1){const bbi=btns(back1).indexOf('Back');c.answer(back1,bbi);let back2=await c.waitForm(15000);
       ok(!!back2&&back2.d.title==="What's New — By date",'Back from the days returns to the months');
       if(back2){const b2i=btns(back2).indexOf('Back');c.answer(back2,b2i);let back3=await c.waitForm(15000);
        ok(!!back3&&back3.d.title==='Recent news','Back from the months returns to the flat list form');
        if(back3){L('list buttons '+JSON.stringify(btns(back3)));
         ok(btns(back3)[0]==='By date','list form: By date is the first button');
         ok(btns(back3).length===12,'list form: By date + 10 entries + Close (12 buttons)');
         ok(btns(back3)[btns(back3).length-1]==='Close','list form: Close last');
         // 6. List form: tap the first entry button -> newest entry.
         c.answer(back3,1);let e1=await c.waitForm(15000);
         ok(!!e1&&/section of the compass menu has its own colour/i.test(e1.d.content),'list form entry button opens the newest entry');
         if(e1)c.closeForm(e1);
        } // back3 (list form)
       } // back2 (months form)
      } // back1 (days form)
     } // cur (Nov 13 oldest)
    } // e13 (Nov 13 newest)
   } // dy (November days)
  } // m (months)
 } // f (newest entry)
   // 7. October days: no Oct 27; Oct 26 walks 10; Oct 24 single entry.
   c.cmd('/whatsnew bydate 2026-10');let od=await c.waitForm(15000);
   if(od){const ob=btns(od);
    ok(ob.some(x=>x==='Mon, Oct 26 — 10 changes'),'October days: Mon, Oct 26 — 10 changes');
    ok(!ob.some(x=>x.includes('Oct 27')),'October days: no Oct 27 row');
    const i26=ob.indexOf('Mon, Oct 26 — 10 changes');c.answer(od,i26);let o26=await c.waitForm(15000);
    if(o26){let steps=0,cur=o26;while(true){const bb=btns(cur);const oi=bb.indexOf('Older news');if(oi<0)break;c.answer(cur,oi);cur=await c.waitForm(15000);if(!cur)break;steps++;if(steps>12)break}
     ok(steps===9,'Oct 26: Older walks all 10 entries ('+steps+' steps)');
     if(cur)c.closeForm(cur);}
    c.cmd('/whatsnew bydate 2026-10-24');let o24=await c.waitForm(15000);
    if(o24){L('oct24 buttons '+JSON.stringify(btns(o24)));
     ok(JSON.stringify(btns(o24))===JSON.stringify(['Back','Close']),'Oct 24 single entry: Back, Close only');
     c.closeForm(o24);
    } // o24
   } // od (October days)
 // 8. Garbage date: no form (chat rendering carries the message), months list + did-not-match line, no error.
 c.take();c.cmd('/whatsnew bydate 2026-13');let gm=await c.waitForm(4000);
 ok(!gm,'garbage date opens no form (chat rendering carries the message)');
 await sleep(1000);const gt=c.take();
 ok(/did not match any news/.test(gt)&&/November 2026/.test(gt),'garbage date prints the did-not-match line + months list');
 // 9. Regression: /whatsnew 99 clamps; join popup still fires once (reset above).
 c.cmd('/whatsnew 99');let cl=await c.waitForm(15000);
 if(cl){ok(btns(cl).includes('Newer news')&&!btns(cl).includes('Older news'),'/whatsnew 99 clamps to entry 10 (Newer, no Older)');c.closeForm(cl);}
 await c.bye();
 L('RESULT bedrock-bydate '+pass+' passed '+fail+' failed');process.exit(0)
})().catch(e=>{L('CRASH '+e.stack);process.exit(2)});
