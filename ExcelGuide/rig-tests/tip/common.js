const lib=require('../lib');const {sleep,L,cons,join,fs,D}=lib;
let pass=0,fail=0;const ok=(c,w)=>{if(c){pass++;L('PASS '+w)}else{fail++;L('FAIL '+w)}};
const take=b=>b.msgs.splice(0).join('\n');
const takeRaw=b=>b.raw.splice(0).join('\n');
const say=async(all,b,c,ms=1500)=>{all.forEach(x=>{x.msgs.splice(0);x.raw.splice(0)});b.chat(c);await sleep(ms);L('> '+b.username+': '+c);};
const done=()=>{L('RESULT pass='+pass+' fail='+fail);process.exit(fail?1:0)};
const file=f=>{try{return fs.readFileSync(D+'/plugins/ExcelGuide/'+f,'utf8')}catch(e){return null}};
module.exports={...lib,ok,take,takeRaw,say,done,file};
