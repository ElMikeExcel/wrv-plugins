// Part 2: run AFTER part 1 and a rig restart (tests/rig-restart.sh). Checks persistence and /vaults reload.
const src=require('fs').readFileSync(__dirname+'/rig-test.js','utf8');
const head=src.slice(0,src.indexOf('(async()=>{try{')).replace("fs.writeFileSync(OUT,'');","");
eval(head+`
(async()=>{try{
 await rconConnect();L('=== ExcelVaults rig test part 2 (after restart) '+new Date().toString());
 const boot=fs.readFileSync(D+'/logs/latest.log','utf8');const en=(/ExcelVaults enabled:[^\\n]*/.exec(boot)||[''])[0];L('boot: '+en);
 ok(/1 cooldowns on 1 vaults/.test(en),'restart: the cooldown was loaded from data.yml');
 ok(!fs.readdirSync(D+'/plugins/ExcelVaults').some(f=>/\\.tmp$|unreadable/.test(f)),'no temp or unreadable data files left behind');
 const bot=await join('VtJava');await sleep(1500);const U=bot.player.uuid;await cons('minecraft:tp VtJava 7.5 -60 8.5 180 0',300);await sleep(1500);
 let st=await cons('vaults status VtJava');ok(/world 5 -60 5/.test(st)&&/2[23]h \\d+m/.test(st),'status after restart still shows about 23h left');
 ok(await isRew(N,U),'vault still locked for the bot after restart');
 const click=async(pos,sneak=false)=>{const [x,y,z]=pos.split(' ').map(Number);if(sneak){bot.sneak(true);await sleep(400)}bot.use(x,y,z);await sleep(sneak?900:1200);if(sneak){bot.sneak(false);await sleep(300)}};
 await cons('minecraft:item replace entity VtJava weapon.mainhand with minecraft:air',250);await sleep(3200);bot.msgs.length=0;await click(N);
 ok(has(bot,/opens again for you in 2[23]h \\d+m/)&&has(bot,/for \\$720\\./),'hint after restart: about 23h left, $720 (750 x 23/24 to nearest $5)');dump(bot);
 // reload with a changed price, then revert
 const cf=D+'/plugins/ExcelVaults/config.yml';const orig=fs.readFileSync(cf,'utf8');
 fs.writeFileSync(cf,orig.replace(/full-price: 750/,'full-price: 1000'));let r=await cons('vaults reload',500);ok(/reload/i.test(r),'/vaults reload answers');
 await sleep(3200);bot.msgs.length=0;await click(N);ok(has(bot,/for \\$960\\./),'after reload with full-price 1000: $960 (1000 x 23/24)');dump(bot);
 fs.writeFileSync(cf,orig);await cons('vaults reload',500);await sleep(3200);bot.msgs.length=0;await click(N);ok(has(bot,/for \\$720\\./),'reverted config + reload: back to $720');dump(bot);
 bot.msgs.length=0;bot.chat('/vaults reload');await sleep(1000);ok(has(bot,/No permission/),'non-op: /vaults reload refused');
 bot.quit();await sleep(500);
 L('=== TOTAL part 2: '+pass+' pass, '+fail+' fail');rc.end();process.exit(fail?1:0);
}catch(e){L('ERROR '+e.stack);L('=== TOTAL part 2: '+pass+' pass, '+fail+' fail');process.exit(2)}})();`);
