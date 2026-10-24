const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');const fs=require('fs');const D='/workspace/guide-rig';const IN=D+'/rigconsole';const LOG=D+'/boot.log';
const con=c=>fs.appendFileSync(IN,c+'\n');const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const OUT=process.env.OUT||'/workspace/guide-rig-tests/run.log';
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let mark=0;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const out=t.slice(mark);mark=t.length;return out};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*\[(\d\d:\d\d:\d\d) (INFO|WARN|ERROR)\]: /,'[$1] ');
const cons=async(c,ms=1000,quiet=false)=>{const o=require('child_process').execFileSync('python3',['/workspace/guide-rig/rcon.py',c]).toString();await sleep(ms);if(!quiet)L('$ '+o.trim());return o};
function mk(n){const b=mf.createBot({host:'127.0.0.1',port:25993,username:n,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;const ow=b._client.write.bind(b._client);b._client.write=(n,p)=>{if(n==='position'||n==='look'||n==='position_look')return;return ow(n,p)};b.msgs=[];b.raw=[];
 b.on('message',(j,pos)=>{if(pos==='game_info')return;b.msgs.push(j.toString());b.raw.push(JSON.stringify(j.json))});b.on('kicked',r=>L(n+' kicked '+r));b.on('error',e=>L(n+' err '+e));return b}
const join=async n=>{const b=mk(n);await new Promise(r=>b.once('spawn',r));await sleep(1500);return b};
module.exports={mf,fs,con,sleep,L,cons,since,clean,mk,join,D,LOG};
