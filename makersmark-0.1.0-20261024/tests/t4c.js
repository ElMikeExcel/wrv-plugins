// Rollback check: jar removed, server restarted. Mode ROLL=out (plugin gone) or ROLL=back (reinstalled). TEST RIG ONLY.
const C=require('./common.js');const {H,L,cons,sleep,join,ok,say,Y,P,items,openAt,put,take,close,tp}=C;
const fs=require('fs');const mode=process.env.ROLL;
const nbt=async slot=>{const o=await cons(`data get entity ${P} ${slot}`,700,true);return o};
(async()=>{L('=== rollback check ('+mode+') '+new Date().toString());
const boot=fs.readFileSync('/workspace/mm-rig/boot.log','utf8');
const pl=await cons('plugins',800,true);L('   '+(pl.split('\n').find(x=>/Paper Plugins|Bukkit Plugins|ExcelMakersMark|Server Plugins/.test(x))||'').slice(0,200));
const a=await join(P);await sleep(800);
const sel=await nbt('SelectedItem');const inv=await nbt('Inventory');L('   held item: '+sel.replace(/\x1b\[[0-9;]*m/g,'').slice(0,400));
if(mode==='out'){
  ok(!/ExcelMakersMark/.test(pl),'ExcelMakersMark not loaded');
  ok(!/ExcelMakersMark/.test(boot.split('\n').filter(x=>/ERROR|Exception/.test(x)).join('\n')),'no ExcelMakersMark errors in the boot log');
  ok(/Crafted by Keeper/.test(sel)&&/Kills: 3/.test(sel),'old items keep their lore as plain text; nothing lost or broken');
  const m=await say(a,'/mark on',800);L('   /mark: '+m);ok(!/Maker's mark is/.test(m),'/mark command gone');
  await cons('gamemode survival '+P,300,true);await tp(P,0.5,0.5,-90,0);await cons('minecraft:clear '+P+' iron_sword',300,true);await cons('minecraft:give '+P+' iron_ingot 2',300,true);await cons('minecraft:give '+P+' stick 1',300,true);
  let w=await openAt(a,2,Y,0);await put(a,w,'iron_ingot',2,true);await put(a,w,'iron_ingot',5,true);await put(a,w,'stick',8);await sleep(300);await take(a,w,0);await close(a);
  const it=a.inventory.items().find(i=>i.name==='iron_sword');const raw=JSON.stringify(it&&it.components||it&&it.nbt||{});L('   new craft components: '+raw.slice(0,200));
  ok(!!it&&!/Crafted by/.test(raw),'new crafts get no mark while the plugin is out');
}else{
  ok(/ExcelMakersMark/.test(pl),'ExcelMakersMark loaded again');
  const o=await cons(`mark inspect ${P} hand`,700,true);L('   '+(o.match(/\[MM-INSPECT\][^\n]*/)||[o])[0]);
  ok(/name=Keeper/.test(o)&&/mob=3/.test(o)&&/WITHER=1/.test(o)&&/Crafted by Keeper/.test(o),'reinstalled: the sword tag and lore are intact (mark, kills, trophy)');
}
H.summary();a.quit();await sleep(1500);process.exit(0)})().catch(e=>{L('ERROR '+e.stack);H.summary();process.exit(1)});
