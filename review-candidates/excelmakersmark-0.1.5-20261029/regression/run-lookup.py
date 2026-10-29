# Excel WRV synthetic regression harness: extracts actual find method, not rewritten algorithm.
from pathlib import Path
import subprocess, os
r=Path(__file__).resolve().parents[1]
outdir=Path(os.environ['BUILD_DIR']); (outdir/'tests').mkdir(parents=True, exist_ok=True)
for label,path in [('Baseline',r/'regression/baseline-find.java.txt'),('Candidate',r/'src/com/winkreyes/makersmark/MakerMagicBridge.java')]:
 s=path.read_text(); a=s.index(' private Slot find(UUID maker,String nativeId,boolean mutationBoundary)');b=s.index(' @Override public void observe',a);method=s[a:b]
 prefix='''import java.util.*;
class CLASS {
 static long clones,metaReads; boolean closed; Plugin plugin=new Plugin(); java.util.function.Supplier<Collection<Player>> players;
 record Slot(Player owner,int index,ItemStack item){}
 static class Bukkit {static boolean primary=true; static boolean isPrimaryThread(){return primary;}}
 static class PersistentDataType {static final Object STRING=new Object();}
 static class Meta {ItemStack item; Meta(ItemStack i){item=i;} Meta getPersistentDataContainer(){return this;} String get(Object k,Object t){if(item.bad)throw new IllegalArgumentException();return item.maker;}}
 static class ItemStack {String maker,nativeId;int amount=1;boolean bad,meta=true,eligible=true;int reads,throwAt;ItemStack(String m,String n){maker=m;nativeId=n;} int getAmount(){return amount;} boolean hasItemMeta(){return meta;} Meta getItemMeta(){metaReads++;if(++reads==throwAt)throw new IllegalStateException("synthetic transient metadata failure");return new Meta(this);} public ItemStack clone(){clones++;ItemStack i=new ItemStack(maker,nativeId);i.amount=amount;i.bad=bad;i.meta=meta;i.eligible=eligible;return i;}}
 static class Inv {List<ItemStack> items=new ArrayList<>();ItemStack[] getContents(){return items.toArray(ItemStack[]::new);}int getSize(){return items.size();}ItemStack getItem(int i){return items.get(i);}}
 static class Player {String name;Inv inv=new Inv(),ender=new Inv();ItemStack cursor;Player(String n){name=n;}Inv getInventory(){return inv;}Inv getEnderChest(){return ender;}ItemStack getItemOnCursor(){return cursor;}}
 static class Wand {static String getWandId(ItemStack i){return i.nativeId;}}
 static class Marks {Object kId=new Object();static class Data{String id;Data(String i){id=i;}}Data read(ItemStack i){if(i.bad)throw new IllegalArgumentException();return new Data(i.maker);}}
 static class Recorder {boolean quarantine,nativeQuarantine;List<String> events=new ArrayList<>();boolean quarantined(UUID u){return quarantine;}boolean nativeQuarantined(String s){return nativeQuarantine;}void observeAmbiguity(UUID u,int n){events.add("maker:"+u+":"+n);}void observeNativeAmbiguity(String s,int n){events.add("native:"+s+":"+n);}}
 static class Plugin {Recorder rec=new Recorder();Marks marks=new Marks();Recorder historyRecorder(){return rec;}Marks marks(){return marks;}boolean historyEligible(Player p,int index,ItemStack item){return item.eligible;}}
'''.replace('CLASS',label)
 suffix='''
 static UUID id=new UUID(1,2); static ItemStack target(){return new ItemStack(id.toString(),"wand");}
 static CLASS fixture(int mode,int count){CLASS b=new CLASS();List<Player> ps=new ArrayList<>();for(int j=0;j<count;j++){Player p=new Player("p"+j);for(int i=0;i<41;i++)p.inv.items.add(new ItemStack("unrelated","other"));ps.add(p);}if(count==0){b.players=()->ps;return b;}Player p=ps.get(0);p.inv.items.set(3,target());Player other=ps.get(ps.size()-1);
 switch(mode){case 1:other.ender.items.add(target());break;case 2:other.cursor=target();break;case 3:other.inv.items.add(target());break;case 4:p.inv.items.get(3).amount=2;break;case 5:p.inv.items.get(3).eligible=false;break;case 6:p.inv.items.get(3).maker="other";break;case 7:p.inv.items.get(3).nativeId="other";break;case 8:other.cursor=new ItemStack(null,"wand");break;case 9:other.cursor=new ItemStack("invalidUUID","wand");break;case 10:other.cursor=new ItemStack(id.toString(),"other");break;case 11:other.inv.items.get(0).bad=true;break;case 12:b.plugin.rec.quarantine=true;break;case 13:b.plugin.rec.nativeQuarantine=true;break;case 14:b.closed=true;break;case 15:p.inv.items.set(3,null);break;case 16:for(Player x:ps)for(ItemStack i:x.inv.items)if(i!=null&&!id.toString().equals(i.maker))i.meta=false;break;case 17:other.cursor=target();p.inv.items.set(3,null);break;case 18:other.ender.items.add(target());p.inv.items.set(3,null);break;case 19:p.inv.items.get(3).amount=0;break;case 20:p.inv.items.get(3).throwAt=2;break;case 21:other.cursor=target();other.inv.items.get(0).bad=true;break;}
 b.players=()->ps;return b;}
 public static void main(String[] args){for(int m=0;m<22;m++)for(boolean mutation:new boolean[]{false,true}){CLASS b=fixture(m,3);clones=0;Slot s=b.find(id,"wand",mutation);System.out.println(m+"/"+mutation+"="+(s==null?"null":s.owner.name+":"+s.index+":"+s.item.maker)+"/"+b.plugin.rec.events+" clones="+clones);}
 CLASS guards=fixture(0,2);Bukkit.primary=false;if(guards.find(id,"wand",true)!=null)throw new AssertionError("offthread");Bukkit.primary=true;if(guards.find(null,"wand",true)!=null||guards.find(id,null,true)!=null)throw new AssertionError("null guard");CLASS snap=fixture(0,2);Slot selected=snap.find(id,"wand",false);selected.owner.inv.items.get(selected.index).maker="changed-after-find";if(!id.toString().equals(selected.item.maker))throw new AssertionError("snapshot changed");System.out.println("guard and detached snapshot checks PASS");
 for(int size:new int[]{0,2,5,10,40})for(int mode:new int[]{0,15,1}){CLASS b=fixture(mode,size);for(int i=0;i<1000;i++)b.find(id,"wand",false);clones=0;metaReads=0;com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long alloc=bean.getThreadAllocatedBytes(Thread.currentThread().threadId());long start=System.nanoTime();for(int i=0;i<2000;i++)b.find(id,"wand",false);System.out.println("BENCH 2000 lookups players="+size+" mode="+mode+" clones="+clones+" metadataReads="+metaReads+" allocatedBytes="+(bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-alloc)+" nanos="+(System.nanoTime()-start));}}

}
'''.replace('CLASS',label)
 (outdir/'tests'/f'{label}.java').write_text(prefix+method+suffix)
jdk=os.environ['JAVA_HOME']+'/bin/'
subprocess.run([jdk+'javac',str(outdir/'tests/Baseline.java'),str(outdir/'tests/Candidate.java')],check=True)
outputs=[]
for name in ['Baseline','Candidate']:
 out=subprocess.check_output([jdk+'java','-Xmx256m','-cp',str(outdir/'tests'),name],text=True);(outdir/(name+'-results.txt')).write_text(out);outputs.append(out)
a,b=outputs
assert [s.split(' clones=')[0] for s in [x for x in a.splitlines() if not x.startswith('BENCH')]]==[s.split(' clones=')[0] for s in [x for x in b.splitlines() if not x.startswith('BENCH')]]
print('PASS: 44 exact-method synthetic baseline/candidate outputs and ambiguity effects identical')
print('Benchmark outputs saved in Baseline-results.txt / Candidate-results.txt')
