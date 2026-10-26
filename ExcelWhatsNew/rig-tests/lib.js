const mf=require('/workspace/ship1025-tests/node_modules/mineflayer');const fs=require('fs');const D='/workspace/whatsnew-rig';const IN=D+'/rigconsole';const LOG=D+'/boot.log';
const con=c=>fs.appendFileSync(IN,c+'\n');const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const OUT=process.env.OUT||'/workspace/batch-next/whatsnew/tests/rig/run.log';
const L=(...a)=>{const s=a.join(' ');console.log(s);fs.appendFileSync(OUT,s+'\n')};
let mark=fs.readFileSync(LOG,'utf8').length;const since=()=>{const t=fs.readFileSync(LOG,'utf8');const out=t.slice(mark);mark=t.length;return out};
const clean=x=>x.replace(/\x1b\[[0-9;]*m/g,'').replace(/^>?\s*\[(\d\d:\d\d:\d\d) (INFO|WARN|ERROR)\]: /,'[$1] ');
const cons=async(c,ms=1000,quiet=false)=>{since();con(c);await sleep(ms);const o=since().split('\n').filter(x=>x.trim()).map(clean);if(!quiet){L('$ '+c);for(const x of o)L('   '+x)}return o.join('\n')};
function mk(n){const b=mf.createBot({host:'127.0.0.1',port:25981,username:n,version:'1.21.11',auth:'offline'});b.physicsEnabled=false;const ow=b._client.write.bind(b._client);b._client.write=(n,p)=>{if(n==='position'||n==='look'||n==='position_look')return;return ow(n,p)};b.msgs=[];b.raw=[];
 b.on('message',(j,pos)=>{if(pos==='game_info')return;b.msgs.push(j.toString());b.raw.push(JSON.stringify(j.json))});b.on('kicked',r=>L(n+' kicked '+r));b.on('error',e=>L(n+' err '+e));return b}
const join=async n=>{for(let i=0;i<4;i++){const b=mk(n);const r=await new Promise(res=>{const t=setTimeout(()=>res('timeout'),30000);b.once('spawn',()=>{clearTimeout(t);res('ok')});b.once('kicked',k=>{clearTimeout(t);res(String(k))});b.once('error',e=>{clearTimeout(t);res('error '+e.message)})});if(r==='ok'){await sleep(1500);return b}L('join retry: '+r.slice(0,120));try{b.quit()}catch(e){}const m=/wait (\d+) sec/.exec(r);await sleep(((m?+m[1]:15)+3)*1000)}throw new Error('could not join')};
module.exports={mf,fs,con,sleep,L,cons,since,clean,mk,join,D,LOG};
