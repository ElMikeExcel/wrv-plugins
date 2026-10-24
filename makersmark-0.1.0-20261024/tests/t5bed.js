// Bedrock (Geyser + Floodgate) check on mm-rig: /mark toggle pop-up, tip pop-up OK. TEST RIG ONLY.
const bp=require('/workspace/tips-tests/node_modules/bedrock-protocol');const T=require('./lib.js');const {L,cons,sleep,fs}=T;
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('  PASS '+w)}else{fail++;L('  FAIL '+w)}};
const U=process.env.MMB||'MkBed1';const N='.'+U;const YML='/workspace/mm-rig/plugins/ExcelMakersMark/players.yml';
const c=bp.createClient({host:'127.0.0.1',port:19975,username:U,offline:true,version:'1.26.40',raknetBackend:'jsp-raknet',skipPing:true});
c.on('error',e=>L('ERR',e.message));c.on('kick',p=>L('KICK',JSON.stringify(p)));
let msgs=[],forms=[];
c.on('text',p=>{msgs.push((p.message||'').replace(/§./g,''))});
c.on('modal_form_request',p=>{let d;try{d=JSON.parse(p.data)}catch(e){d={}};forms.push({id:p.form_id,d});L('   FORM '+JSON.stringify(d).slice(0,300))});
const take=()=>msgs.splice(0).join(' | ');
const cmd=s=>c.queue('command_request',{command:s,origin:{type:'player',uuid:'00000000-0000-0000-0000-000000000000',request_id:'',player_entity_id:0n},internal:false,version:'52'});
const answer=(f,btn)=>c.queue('modal_form_response',{form_id:f.id,has_response_data:true,data:JSON.stringify(btn),has_cancel_reason:false});
const waitForm=async(pred,ms)=>{const t=Date.now();while(Date.now()-t<ms){const i=forms.findIndex(f=>pred(f.d));if(i>=0)return forms.splice(i,1)[0];await sleep(250)}return null};
const isToggle=d=>d.title==="Maker's mark"&&Array.isArray(d.buttons)&&d.buttons.length===3&&/^Maker's mark: /.test(d.buttons[0].text);
const isTip=d=>Array.isArray(d.buttons)&&d.buttons.some(b=>b.text==='OK, got it!');
const prefsOf=()=>{const y=fs.readFileSync(YML,'utf8');const u=(T.since(),null);return y};
setTimeout(()=>{L('TIMEOUT');L(`RESULT pass=${pass} fail=${fail}`);process.exit(3)},240000);
c.once('spawn',async()=>{
 L('=== Bedrock check ('+N+') '+new Date().toString());await sleep(4000);
 await cons('mark tipreset '+N,600,true);take();
 // 1 /mark opens the toggle pop-up; chat tip uses Bedrock wording
 cmd('/mark');let f=await waitForm(isToggle,8000);await sleep(1500);let t=take();L('   chat: '+t.slice(0,300));
 ok(!!f,'/mark on Bedrock opens the "Maker\'s mark" pop-up');
 ok(f&&/Maker's mark: ON\nTap to turn off/.test(f.d.buttons[0].text)&&/Gear stats: ON\nTap to turn off/.test(f.d.buttons[1].text)&&f.d.buttons[2].text==='Close','pop-up shows both switches ON plus Close');
 ok(/\[Tip\]/.test(t)&&/Type \/mark ok to hide this tip/.test(t)&&!/\[OK\]/.test(t),'chat tip uses Bedrock wording (no click [OK])');
 if(f){answer(f,0);await sleep(2500);t=take();L('   after tap: '+t.slice(0,200));ok(/Maker's mark is OFF/.test(t),'tapping the first switch turns Maker\'s mark OFF');}
 await sleep(2600);let y=fs.readFileSync(YML,'utf8');ok(new RegExp('mark: false').test(y),'players.yml saved mark: false for the Bedrock player');
 // 2 tip pop-up arrives at a calm moment, OK hides it
 let tf=await waitForm(isTip,20000);ok(!!tf,'tip pop-up arrives with "OK, got it!" and "Remind me later"');
 if(tf){ok(tf.d.title==="Maker's mark"&&tf.d.buttons.length===2&&tf.d.buttons[1].text==='Remind me later','tip pop-up layout');answer(tf,0);await sleep(1800);t=take();L('   '+t);ok(/That tip will not show again/.test(t),'tapping OK hides the tip');}
 // 3 second /mark: state shows OFF, flip stats
 cmd('/mark');f=await waitForm(isToggle,8000);ok(f&&/^Maker's mark: OFF\nTap to turn on/.test(f.d.buttons[0].text),'pop-up now shows Maker\'s mark: OFF');
 if(f){answer(f,1);await sleep(2000);t=take();ok(/Gear stats are OFF/.test(t),'tapping the second switch turns Gear stats OFF');}
 await sleep(1500);take();cmd('/mark');f=await waitForm(isToggle,8000);
 if(f){ok(/Gear stats: OFF/.test(f.d.buttons[1].text),'stats switch shows OFF');answer(f,2);await sleep(1500);t=take();ok(!/is (ON|OFF)/.test(t),'Close changes nothing');}
 await sleep(1000);const notip=!forms.some(x=>isTip(x.d))&&!/\[Tip\]/.test(take());ok(notip,'no tip after OK');
 cmd('/mark on');await sleep(800);cmd('/mark stats on');await sleep(2800);y=fs.readFileSync(YML,'utf8');L('   players.yml: '+y.replace(/\n/g,' ').slice(0,400));
 const pp=await cons(`papi parse ${N} M=%makersmark_mark% S=%makersmark_stats%`,900,true);ok(/M=ON S=ON/.test(pp),'typed /mark on and /mark stats on work on Bedrock: '+((pp.match(/M=\S+ S=\S+/)||[''])[0]));
 L(`RESULT pass=${pass} fail=${fail}`);c.close();await sleep(500);process.exit(0);
});
