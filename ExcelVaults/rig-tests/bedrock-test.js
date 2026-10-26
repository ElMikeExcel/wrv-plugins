// Bedrock path for ExcelVaults 0.1.1: bedrock-protocol 3.59.0 (lf-tests copy, offline) through Geyser + floodgate
// on the box rig /workspace/excelvaults-rig (Bedrock port 19761). Console via RCON.
// Checks: vault opens from a Bedrock click, hint text, sneak + tap -> Floodgate modal confirm with the current price,
// Cancel = no charge, /vaults reopen form -> pay, /vaults -> SimpleForm list -> tap -> confirm at the scaled price.
// Also records what Geyser sends the client for the vault block (update_block / block_entity_data).
const bp=require('/workspace/lf-tests/node_modules/bedrock-protocol');const fs=require('fs');const net=require('net');
const D='/workspace/excelvaults-rig',S='/workspace/batch-next/parity-fixes-20261026/vaults';
const OUT=S+'/tests/bedrock-test.log';fs.writeFileSync(OUT,'');
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let rc,rbuf=Buffer.alloc(0),rid=10,waiters={};
function rconConnect(){return new Promise((res,rej)=>{rc=net.connect(26262,'127.0.0.1',()=>{send(1,3,process.env.RIG_RCON_PASSWORD||'');waiters[1]=()=>res()});rc.on('error',rej);
 rc.on('data',d=>{rbuf=Buffer.concat([rbuf,d]);while(rbuf.length>=4){const n=rbuf.readInt32LE(0);if(rbuf.length<4+n)break;const id=rbuf.readInt32LE(4);const body=rbuf.slice(12,4+n-2).toString();rbuf=rbuf.slice(4+n);if(waiters[id]){const w=waiters[id];delete waiters[id];w(body)}}})})}
function send(id,type,body){const b=Buffer.from(body,'utf8');const p=Buffer.alloc(14+b.length);p.writeInt32LE(10+b.length,0);p.writeInt32LE(id,4);p.writeInt32LE(type,8);b.copy(p,12);rc.write(p)}
const strip=s=>String(s).replace(/§./g,'');
const cons=async(c,ms=600)=>{const id=++rid;const r=await new Promise(res=>{waiters[id]=res;send(id,2,c)});await sleep(ms);L('$ '+c);if(r.trim())L('   '+strip(r).trim().replace(/\n/g,'\n   '));return strip(r)};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const N='.VtBed',V='21 -60 5',VX=21,VY=-60,VZ=5;
const bal=async()=>{const r=await cons('balance '+N,300);const m=/\$([\d,\.]+)/.exec(r);return m?Number(m[1].replace(/,/g,'')):NaN};
let U=null;const intArr=u=>{const h=u.replace(/-/g,'');const a=[];for(let i=0;i<4;i++){let v=parseInt(h.substr(i*8,8),16);if(v>0x7fffffff)v-=0x100000000;a.push(v)}return a};
const isRew=async()=>{const r=await cons('data get block '+V+' server_data.rewarded_players',200);return r.includes(U.join(', '))};
(async()=>{try{
 await rconConnect();L('=== ExcelVaults Bedrock test '+new Date().toString());
 await cons('setblock '+V+' minecraft:air',200);await cons('setblock '+V+' minecraft:vault',300);
 let rtid=0n,pos=null,tick=0n,held={network_id:0};
 let c=null;for(let attempt=1;attempt<=4&&!c;attempt++){
  const cc=bp.createClient({host:'127.0.0.1',port:19761,username:'VtBed',offline:true,version:process.argv[2]||'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
  cc.on('start_game',p=>{rtid=p.runtime_entity_id;pos={...p.player_position};tick=BigInt(p.current_tick||0)});cc.on('move_player',p=>{if(String(p.runtime_id)===String(rtid))pos={...p.position}});
  cc.on('inventory_content',p=>{if(p.window_id==='inventory'||p.window_id===0)held=p.input[0]});cc.on('inventory_slot',p=>{if((p.window_id==='inventory'||p.window_id===0)&&p.slot===0)held=p.item});
  cc.on('error',e=>L('ERR',e.message));cc.on('kick',p=>L('KICK',JSON.stringify(p)));
  try{await new Promise((res,rej)=>{const t=setTimeout(()=>rej(new Error('bedrock spawn timeout')),45000);cc.once('spawn',()=>{clearTimeout(t);res()});cc.once('kick',p=>{clearTimeout(t);rej(new Error('kicked '+JSON.stringify(p)))})});c=cc}
  catch(e){L('join attempt '+attempt+' failed: '+e.message);try{cc.close()}catch(_){}await sleep(4000)}}
 if(!c)throw new Error('could not join over Bedrock');
 const texts=[],forms=[],blk=[],bent=[];
 c.on('text',p=>texts.push(strip(p.message)));
 c.on('modal_form_request',p=>{forms.push(p);L('FORM id='+p.form_id+' '+strip(p.data).slice(0,600))});
 c.on('update_block',p=>{if(p.position&&p.position.x===VX&&p.position.y===VY&&p.position.z===VZ)blk.push(p.block_runtime_id)});
 c.on('block_entity_data',p=>{if(p.position&&p.position.x===VX&&p.position.y===VY&&p.position.z===VZ)bent.push(JSON.stringify(p.nbt,(k,v)=>typeof v==='bigint'?v.toString():v).slice(0,400))});
 const authIn=(flags)=>{tick++;c.queue('player_auth_input',{pitch:30,yaw:180,position:pos,move_vector:{x:0,z:0},head_yaw:180,input_data:flags&&flags.length?flags:undefined,input_mode:'touch',play_mode:'normal',interaction_model:'touch',interact_rotation:{x:0,z:0},tick,delta:{x:0,y:0,z:0},transaction_presence:false,transaction:undefined,item_stack_request_presence:false,item_stack_request:undefined,block_action_presence:false,block_action:undefined,vehicle_rotation_presence:false,vehicle_rotation:undefined,predicted_vehicle_presence:false,predicted_vehicle:undefined,analogue_move_vector:{x:0,z:0},camera_orientation:{x:0,y:-0.5,z:-0.86},raw_move_vector:{x:0,z:0}})};
 const tap=()=>L('   tap holding network_id '+(held&&held.network_id))||c.queue('inventory_transaction',{transaction:{legacy:{legacy_request_id:0},transaction_type:'item_use',actions:[],transaction_data:{action_type:'click_block',trigger_type:'player_input',block_position:{x:VX,y:VY,z:VZ},face:3,hotbar_slot:0,hand:'main_hand',held_item:held,player_pos:{x:pos.x,y:pos.y,z:pos.z},click_pos:{x:0.5,y:0.5,z:1},block_runtime_id:0,client_prediction:'success',client_cooldown_state:'off'}}});
 const click=async(sneak=false)=>{if(sneak){authIn(['start_sneaking','sneaking','sneak_down']);for(let k=0;k<6;k++){await sleep(50);authIn(['sneaking','sneak_down'])}}
  tap();for(let k=0;k<10;k++){await sleep(50);authIn(sneak?['sneaking','sneak_down']:[])}if(sneak){authIn(['stop_sneaking']);for(let k=0;k<4;k++){await sleep(50);authIn([])}}await sleep(1200)};
 const cmd=async t=>{c.queue('command_request',{command:t,origin:{type:'player',uuid:'00000000-0000-0000-0000-000000000000',request_id:'',player_entity_id:0n},internal:false,version:'52'});await sleep(2000)};
 await sleep(3000);L('spawned, runtime id '+rtid+' pos '+JSON.stringify(pos));
 if(process.env.DBG){const seen={};c.on('packet',pk=>{const n=pk.data&&pk.data.name;if(n&&!/move|level_chunk|network_chunk|set_entity_motion|update_attributes|set_time|tick_sync|level_event|level_sound|subchunk|animate|player_list|set_entity_data|update_abilities|network_stack/.test(n)){seen[n]=(seen[n]||0)+1}});await cmd('/vaults help');await sleep(1500);L('packets: '+JSON.stringify(seen));L('texts: '+texts.join(' | '));c.close();process.exit(0)}
 const ur=await cons('data get entity '+N+' UUID',200);const m=/\[I;\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]/.exec(ur);U=m?[m[1],m[2],m[3],m[4]].map(Number):null;ok(!!U,'Bedrock player is on the Java server through Geyser');
 for(const x of ['vaults reset '+N,'gamemode survival '+N,'clear '+N,'eco set '+N+' 5000','minecraft:tp '+N+' 21.5 -60 7.5 180 30'])await cons(x,400);
 await sleep(2000);for(let k=0;k<10;k++){authIn([]);await sleep(50)}
 // 1 open with a key from Bedrock
 await cons('minecraft:item replace entity '+N+' weapon.mainhand with minecraft:trial_key 1',500);blk.length=0;bent.length=0;await click();await sleep(1500);
 ok(await isRew(),'Bedrock tap with a trial key opens the vault (vanilla rewards the player)');
 let st=await cons('vaults status '+N,300);ok(/21 -60 5/.test(st),'open recorded (24h cooldown)');
 L('Geyser -> client for the vault after the open: update_block x'+blk.length+' '+JSON.stringify(blk.slice(-4))+', block_entity_data x'+bent.length);for(const b of bent.slice(-2))L('   '+b);
 // 2 hint (0.1.1: Bedrock wording + plain tap opens the reopen window)
 await cons('minecraft:item replace entity '+N+' weapon.mainhand with minecraft:air',400);await sleep(3200);texts.length=0;forms.length=0;const b00=await bal();await click();await sleep(1200);
 L('texts: '+texts.join(' | '));ok(texts.some(t=>/opens again for you in 2[34]h \d+m/.test(t))&&texts.some(t=>/\$750/.test(t)),'Bedrock hint: time left and $750');
 ok(texts.some(t=>/Sneak \+ tap it, or type \/vaults, to reopen it now for \$750\./.test(t))&&!texts.some(t=>/right-click|\[Reopen now/i.test(t)),'0.1.1 Bedrock hint: "Sneak + tap it, or type /vaults", no right-click, no chat button');
 ok(texts.some(t=>/A window opened: tap Reopen to pay, or Cancel\. Nothing is charged until you tap Reopen\./.test(t)),'0.1.1 Bedrock: note that the window opened');
 const ft=forms[0]?JSON.parse(forms[0].data):null;
 ok(ft&&ft.type==='modal'&&/Reopen for \$750/.test(ft.button1)&&/Cancel/.test(ft.button2),'0.1.1 plain tap on a locked vault opens the reopen window (Reopen for $750 / Cancel), the tappable [Reopen]');
 ok(await bal()===b00,'tap window shown: no charge');
 if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'false',has_cancel_reason:false});await sleep(1200);
 ok(await bal()===b00&&await isRew(),'tap window Cancel: no charge, still locked');
 await sleep(3200);texts.length=0;forms.length=0;await click();await sleep(1500);
 ok(texts.some(t=>/opens again for you in/.test(t))&&forms.length===0,'second plain tap within 30 s: hint again, window not reopened (cooldown)');
 texts.length=0;await cmd('/vaults help');await sleep(800);L('help: '+texts.join(' | '));
 ok(texts.some(t=>/Sneak \+ tap it \(or type \/vaults\) to pay and reopen it now\. On a controller: sneak, then press LT\./.test(t))&&texts.some(t=>/Tap a used vault/.test(t))&&!texts.some(t=>/click/i.test(t)),'0.1.1 Bedrock /vaults help: tap wording, controller line, no click words');
 // 3 sneak + tap -> modal
 await sleep(3200);forms.length=0;const b0=await bal();await click(true);await sleep(1500);
 const f1=forms[0]?JSON.parse(forms[0].data):null;
 ok(f1&&f1.type==='modal'&&/Reopen for \$750/.test(f1.button1)&&/Cancel/.test(f1.button2),'Bedrock sneak + tap: modal form, "Reopen for $750" / "Cancel"');
 ok(f1&&/opens again for you in/.test(strip(f1.content))&&/The price drops as the timer runs down \(lowest \$50\)\./.test(strip(f1.content))&&/Your balance: \$5,000/.test(strip(f1.content))&&!/\u2014/.test(f1.content),'Bedrock confirm body: time left, price-drops line, balance, no em dash');
 ok(await bal()===b0,'form shown: no charge yet');
 if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'false',has_cancel_reason:false});await sleep(1500);
 ok(await bal()===b0&&await isRew(),'Bedrock Cancel: no charge, still locked');
 // 4 /vaults reopen -> form -> pay
 forms.length=0;await cmd('/vaults reopen world 21 -60 5');await sleep(800);
 const f2=forms[0]?JSON.parse(forms[0].data):null;ok(f2&&/Reopen for \$750/.test(f2.button1),'/vaults reopen from Bedrock: same modal');
 blk.length=0;bent.length=0;
 if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'true',has_cancel_reason:false});await sleep(2500);
 ok(await bal()===b0-750&&!(await isRew()),'Bedrock tap Reopen: charged $750, vault reopened');
 ok(texts.some(t=>/Vault reopened for \$750/.test(t)),'Bedrock: confirmation chat');
 L('Geyser -> client for the vault after the paid reopen: update_block x'+blk.length+' '+JSON.stringify(blk.slice(-4))+', block_entity_data x'+bent.length);for(const b of bent.slice(-2))L('   '+b);
 const pc=fs.readFileSync(D+'/plugins/ExcelVaults/purchases.csv','utf8').trim().split('\n');ok(/,bedrock,normal,world 21 -60 5,750\.00,/.test(pc[pc.length-1]),'purchases.csv: platform bedrock');
 // 5 /vaults list -> SimpleForm -> tap -> confirm at $375
 await cons('minecraft:item replace entity '+N+' weapon.mainhand with minecraft:trial_key 1',500);await click();await sleep(1500);ok(await isRew(),'opened again');
 await cons('vaults age '+N+' 720',300);await cons('minecraft:item replace entity '+N+' weapon.mainhand with minecraft:air',300);
 forms.length=0;await cmd('/vaults');await sleep(800);const f3=forms[0]?JSON.parse(forms[0].data):null;
 ok(f3&&f3.type==='form'&&f3.buttons&&f3.buttons.some(b=>/world 21 -60 5/.test(strip(b.text))&&/12h 0m \| \$375/.test(strip(b.text))),'/vaults on Bedrock: SimpleForm list, button shows 12h 0m | $375');
 ok(f3&&/Tap a vault below to reopen it now\. Or sneak \+ tap a locked vault\./.test(strip(f3.content))&&!/\[Reopen\]|right-click/.test(f3.content),'0.1.1 Bedrock list form text: tap a vault below, no [Reopen] or right-click');
 if(forms[0]){const fid=forms[0].form_id;const idx=f3.buttons.findIndex(b=>/world 21 -60 5/.test(strip(b.text)));forms.length=0;c.queue('modal_form_response',{form_id:fid,has_response_data:true,data:String(Math.max(0,idx)),has_cancel_reason:false});await sleep(2000);
  const f4=forms[0]?JSON.parse(forms[0].data):null;ok(f4&&f4.type==='modal'&&/Reopen for \$375/.test(f4.button1),'tap list entry: confirm at $375 (half the cooldown left)');
  if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'false',has_cancel_reason:false});await sleep(1000)}
 await sleep(200);
 L('done');c.close();
}catch(e){L('ERROR '+e.stack);fail++}
 L('RESULT pass='+pass+' fail='+fail);process.exit(fail?1:0)})();
