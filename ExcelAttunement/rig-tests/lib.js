const mf=require('mineflayer');const fs=require('fs');const D='/workspace/ea016/rig';const IN=D+'/e16console';const LOG=D+'/boot.log';
const con=c=>fs.appendFileSync(IN,c+'\n');const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const OUT=process.env.OUT||'/workspace/ea016/tests/rig-run.log';
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const out=t.slice(mark);mark=t.length;return out};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*\[(\d\d:\d\d:\d\d) (INFO|WARN|ERROR)\]: /,'[$1] ');
const cons=async(c,ms=1000,quiet=false)=>{since();con(c);await sleep(ms);const o=since().split('\n').filter(x=>x.trim()).map(clean);if(!quiet){L('$ '+c);for(const x of o)L('   '+x)}return o.join('\n')};
function mk(n){const b=mf.createBot({host:'RIG_HOST',port:25897,username:n,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;const ow=b._client.write.bind(b._client);b._client.write=(n,p)=>{if(n==='position'||n==='look'||n==='position_look')return;return ow(n,p)};b.msgs=[];b.on('messagestr',(x,pos)=>{b.msgs.push((pos==='game_info'?'[AB] ':'')+x)});b._client.on('action_bar',d=>{const j=JSON.stringify(d);const m=j.match(/"text":\{"type":"string","value":"([^"]*)"/);b.msgs.push('[AB] '+(m?m[1]:j))});b.on('kicked',r=>L(n+' kicked '+r));b.on('error',e=>L(n+' err '+e));return b}
const join=async n=>{const b=mk(n);await new Promise(r=>b.once('spawn',r));await sleep(1500);return b};
const num=async(c)=>{const o=await cons(c,700,true);const m=o.match(/data: (-?[\d.]+)[fdbsL]?/);return m?parseFloat(m[1]):null};
const health=n=>num('data get entity '+n+' Health');
const food=n=>num('data get entity '+n+' foodLevel');
const sat=n=>num('data get entity '+n+' foodSaturationLevel');
const absorb=n=>num('data get entity '+n+' AbsorptionAmount');
module.exports={mf,fs,con,sleep,L,cons,since,clean,mk,join,num,health,food,sat,absorb,D,LOG};
