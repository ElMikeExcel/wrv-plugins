const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,say}=H;
(async()=>{L('=== T22 part B (0.1.0 running on the 0.2.0 file)');const a=await join('TrA');
const m=await say(a,'/trash undo',1200);ok(cnt(a,'dirt')===0,'0.1.0 ignores the 0.2.0 batch: no items returned, so no dupe (msg: '+m+')');
const m2=await say(a,'/trash undo',1200);ok(cnt(a,'dirt')===0,'still nothing on a second try');a.quit();await sleep(500);H.summary();process.exit(0)})();
