const H=require('./h.js');const {L,cons,sleep,join,ok,cnt,chest,peek}=H;
(async()=>{L('=== T21 part B (after restart)');await cons('forceload add 170 90 190 110',800,true);const c=await chest(180,-60,100);
const a=await join('TrA');await sleep(500);ok(c.diamond===3&&c.gold_ingot===4,'after stop: diamonds back in the chest, nothing lost or duped: '+JSON.stringify({d:c.diamond,g:c.gold_ingot}));
ok(cnt(a,'emerald')===2&&cnt(a,'diamond')===0,'after stop: my emeralds back in my inventory (emerald='+cnt(a,'emerald')+', diamond='+cnt(a,'diamond')+')');
const pk=await peek('TrA');ok(/none/.test(pk),'nothing was deleted: '+pk);a.quit();await sleep(500);H.summary();process.exit(0)})();
