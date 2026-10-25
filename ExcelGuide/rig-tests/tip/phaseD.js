// forward again to 0.3.0
const {sleep,L,cons,join,ok,take,say,done,file,fs,D}=require('./common');
(async()=>{
 const boot=fs.readFileSync(D+'/boot.log','utf8');
 ok(/1 approved player answers/.test(boot),'0.3.0 again: approved answer still there');
 const p=await join('RigFreshOne');const all=[p];
 await say(all,p,'/ask how do i tame a fox?',2500);const t=take(p);L('   fresh1: '+t.replace(/\n/g,' / '));
 ok(/A player said/.test(t)&&!/Tip: answers/.test(t),'answer served, OK still remembered');
 p.quit();await sleep(1000);done();
})();
