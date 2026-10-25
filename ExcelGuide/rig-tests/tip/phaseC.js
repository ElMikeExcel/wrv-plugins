// rolled back to 0.2.3
const {sleep,L,cons,join,ok,take,say,done,file,fs,D}=require('./common');
(async()=>{
 const boot=fs.readFileSync(D+'/boot.log','utf8');
 ok(/Enabling ExcelGuide v0\.2\.3/.test(boot),'0.2.3 enabled after rollback');
 ok(!/ERROR\]: \[ExcelGuide\]|Error occurred while enabling ExcelGuide/.test(boot),'no ExcelGuide errors on 0.2.3');
 L('   0.2.3 warnings: '+(boot.match(/WARN\]: \[ExcelGuide\].*/g)||[]).join(' | '));
 const p=await join('RigFreshTwo');const all=[p];
 await say(all,p,'/ask how do i claim land?',2500);const t=take(p);L('   0.2.3 answer: '+t.replace(/\n/g,' / ').slice(0,200));
 ok(/claim|golden shovel/i.test(t),'0.2.3 Guide answers normally');
 p.quit();await sleep(1000);done();
})();
