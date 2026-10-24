// after a restart on 0.3.0: persistence
const {sleep,L,cons,join,ok,take,say,done,file,fs,D}=require('./common');
(async()=>{
 const boot=fs.readFileSync(D+'/boot.log','utf8');
 ok(/1 approved player answers/.test(boot),'boot reports 1 approved answer');
 ok(!/(WARN|ERROR)\]: \[ExcelGuide\]/.test(boot),'no ExcelGuide WARN/ERROR at boot');
 const admin=await join('RigAdmin'),f1=await join('RigFreshOne'),f2=await join('RigFreshTwo');const all=[admin,f1,f2];
 await say(all,f1,'guide, how do i tame a fox?',2500);let t=take(f1);L('   fresh1: '+t.replace(/\n/g,' / '));
 ok(/A player said/.test(t)&&!/Tip: answers/.test(t),'approved answer + OK both survived the restart');
 await sleep(21000);
 await say(all,f2,'/ask how do i tame a fox?',2500);t=take(f2);L('   fresh2: '+t.replace(/\n/g,' / '));
 ok(/A player said/.test(t)&&/Tip: answers/.test(t),'unacked player still gets the tip (/ask door)');
 await say(all,admin,'/guide review approved',1500);t=take(admin);L('   approved: '+t.replace(/\n/g,' / '));
 ok(/1 approved/.test(t),'approved list persisted');
 all.forEach(b=>b.quit());await sleep(1000);done();
})();
