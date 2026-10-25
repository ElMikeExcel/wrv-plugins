// Bedrock path for ExcelClaimFly 0.1.0: a bedrock-protocol client (offline) through Geyser 2.12.0-SNAPSHOT + floodgate 2.2.5
// Note: uses the lf-tests copy of bedrock-protocol 3.59.0; the bedrock-cast-rig copy timed out at RakNet connect on this rig (run 1 and 2, 2:38 PM and 2:45 PM CDT).
// on the box rig /workspace/claimfly-rig (Bedrock port 19661). Checks the tappable confirm form, purchase, title/chat,
// the may_fly ability Geyser sends, a flying request, and the hit pause.
const bp=require('/workspace/lf-tests/node_modules/bedrock-protocol');const fs=require('fs');
const D='/workspace/claimfly-rig',IN=D+'/rigconsole',LOG=D+'/boot.log',S='/workspace/batch-next/claimfly-20261025';
const OUT=S+'/tests/bedrock-test.log';fs.writeFileSync(OUT,'');
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const o=t.slice(mark);mark=t.length;return o};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*/,'');
const cons=async(c,ms=1200)=>{since();fs.appendFileSync(IN,c+'\n');await sleep(ms);const o=since().split('\n').filter(x=>x.trim()).map(clean);L('$ '+c);for(const x of o)L('   '+x);return o.join('\n')};
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const N='.CfBed';const info=async()=>{const o=await cons('buildfly info '+N,900);const m=/ClaimFly info .*/.exec(o);return m?m[0]:''};
const strip=s=>String(s).replace(/§./g,'');
(async()=>{try{
 L('=== ExcelClaimFly Bedrock test '+new Date().toString());
 const c=bp.createClient({host:'127.0.0.1',port:19661,username:'CfBed',offline:true,version:process.argv[2]||'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
 const texts=[],titles=[],forms=[],abil=[];let rid=0n,uid=0n;
 c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));c.on('disconnect',p=>L('DISCONNECT',JSON.stringify(p)));
 c.on('text',p=>texts.push(strip(p.message)));c.on('set_title',p=>titles.push(p.type+':'+strip(p.text)));
 c.on('modal_form_request',p=>{forms.push(p);L('FORM id='+p.form_id+' '+strip(p.data).slice(0,700))});
 c.on('update_abilities',p=>{const b=p.abilities.find(x=>x.type==='base')||p.abilities[0];abil.push({allowed:b.allowed,enabled:b.enabled});});
 let pos=null,tick=0n;c.on('start_game',p=>{rid=p.runtime_entity_id;uid=p.entity_id;pos={...p.player_position};tick=BigInt(p.current_tick||0)});
 c.on('move_player',p=>{if(String(p.runtime_id)===String(rid))pos={...p.position}});
 const authIn=(flags)=>{tick++;c.queue('player_auth_input',{pitch:0,yaw:0,position:pos,move_vector:{x:0,z:0},head_yaw:0,input_data:flags&&flags.length?flags:undefined,input_mode:'touch',play_mode:'normal',interaction_model:'touch',interact_rotation:{x:0,z:0},tick,delta:{x:0,y:0,z:0},transaction_presence:false,transaction:undefined,item_stack_request_presence:false,item_stack_request:undefined,block_action_presence:false,block_action:undefined,vehicle_rotation_presence:false,vehicle_rotation:undefined,predicted_vehicle_presence:false,predicted_vehicle:undefined,analogue_move_vector:{x:0,z:0},camera_orientation:{x:0,y:0,z:1},raw_move_vector:{x:0,z:0}})};
 await new Promise((res,rej)=>{const t=setTimeout(()=>rej(new Error('bedrock spawn timeout')),45000);c.once('spawn',()=>{clearTimeout(t);res()})});
 await sleep(3000);L('spawned, runtime id '+rid);
 const mayFly=()=>{const a=abil[abil.length-1];return a?!!a.enabled.may_fly:undefined};const flyingAb=()=>{const a=abil[abil.length-1];return a?!!a.enabled.flying:undefined};
 let i=await info();ok(/bedrock=true/.test(i),'floodgate sees the client as Bedrock');
 for(const x of ['deleteallclaims '+N,'buildfly end '+N,'adjustbonusclaimblocks '+N+' 5000','gamemode survival '+N,'eco set '+N+' 3000','minecraft:tp '+N+' 800 -60 800'])await cons(x,900);
 // 1 no claim yet: the form explains, OK only, no charge
 forms.length=0;await cons('buildfly buy '+N,2500);
 const f0=forms[0]?JSON.parse(forms[0].data):null;ok(f0&&f0.type==='modal'&&/Make a claim first/.test(strip(f0.content))&&/OK/.test(f0.button1),'Bedrock no-claim: info form with OK, explains why');
 if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'true',has_cancel_reason:false});await sleep(800);
 // make a claim with the golden shovel from the Bedrock client
 await cons('minecraft:item replace entity '+N+' weapon.mainhand with golden_shovel');
 c.queue('command_request',{command:'/claim 10',origin:{type:'player',uuid:'00000000-0000-0000-0000-000000000000',request_id:'',player_entity_id:0n},internal:false,version:'52'});await sleep(2500);
 L('after /claim: '+texts.slice(-3).join(' | '));
 // 2 buy: the modal confirm, tap Buy
 forms.length=0;texts.length=0;titles.length=0;await cons('buildfly buy '+N,2500);
 const f1=forms[0]?JSON.parse(forms[0].data):null;
 ok(f1&&f1.type==='modal'&&/Buy 30 min for \$1,000/.test(f1.button1)&&/Cancel/.test(f1.button2),'Bedrock confirm: modal form, button1 "Buy 30 min for $1,000", button2 "Cancel"');
 ok(f1&&/Fly for 30 minutes, only inside your own claim\./.test(strip(f1.content))&&!/\u2014/.test(f1.content),'Bedrock confirm body: plain words, no em dash');
 let o=await cons('balance '+N,900);ok(/\$3,000/.test(o),'form shown: no charge yet');
 if(forms[0])c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'true',has_cancel_reason:false});await sleep(2500);
 o=await cons('balance '+N,900);ok(/\$2,000/.test(o),'tap Buy: charged $1,000');
 ok(texts.some(t=>/Build Flight pass: 30 minutes/.test(t))&&texts.some(t=>/Double-tap jump to fly/.test(t)),'Bedrock: how-it-works chat arrived');L('texts: '+texts.join(' | '));
 ok(titles.some(t=>/Build Flight on/.test(t))&&titles.some(t=>/Double-tap jump in your claim/.test(t)),'Bedrock: title + subtitle');L('titles: '+titles.join(' | '));
 await sleep(1000);i=await info();ok(/allowFlight=true/.test(i)&&/inOwnClaim=true/.test(i),'server: allow-flight on in own claim');
 ok(mayFly()===true,'Geyser sent update_abilities with may_fly=true (double-tap jump enabled on the client)');
 // 3 cancel path: second form, tap Cancel -> no charge
 forms.length=0;await cons('buildfly buy '+N,2500);if(forms[0]){c.queue('modal_form_response',{form_id:forms[0].form_id,has_response_data:true,data:'false',has_cancel_reason:false})}await sleep(1500);
 o=await cons('balance '+N,900);ok(/\$2,000/.test(o),'Bedrock Cancel: no charge');
 // 4 start flying the Bedrock way (request_ability flying=true, what a double-tap sends on older clients)
 await cons('minecraft:tp '+N+' 800 -50 800',1500);
 // 1.26.40 clients send a double-tap as player_auth_input with the start_flying flag (touch input mode here)
 for(let k=0;k<5;k++){authIn([]);await sleep(50)}authIn(['start_flying']);for(let k=0;k<20;k++){await sleep(50);authIn([])}await sleep(800);
 i=await info();const flew=/flying=true/.test(i);L('after request_ability: '+i);
 ok(flew,'Bedrock double-tap (player_auth_input start_flying, touch) reaches the server as flying=true');
 // 5 hit while flying (or with flight allowed): paused, message, may_fly removed on client
 texts.length=0;await cons('summon zombie 802 -60 800 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["cf"]}');
 await cons('damage '+N+' 1 minecraft:mob_attack by @e[type=zombie,tag=cf,limit=1]',2000);
 i=await info();ok(/allowFlight=false/.test(i)&&/combat=true/.test(i),'Bedrock hit: flight paused on server');
 ok(mayFly()===false,'Bedrock hit: Geyser removed may_fly on the client');
 if(flew)ok(texts.some(t=>/Flight paused, you got hit\. Land safe\./.test(t)),'Bedrock hit: "Flight paused, you got hit. Land safe."');
 await cons('minecraft:kill @e[tag=cf]');await sleep(11500);
 i=await info();ok(/allowFlight=true/.test(i)&&mayFly()===true,'Bedrock: after 10 s quiet, may_fly back');L('texts: '+texts.join(' | '));
 // 6 leave claim: may_fly removed
 await cons('minecraft:tp '+N+' 900 -60 900',2000);i=await info();ok(/allowFlight=false/.test(i)&&mayFly()===false,'Bedrock leave claim: may_fly removed');
 await cons('buildfly end '+N);
 c.close();
}catch(e){L('ERROR '+e.stack);fail++}
 L('RESULT pass='+pass+' fail='+fail);process.exit(fail?1:0)})();
