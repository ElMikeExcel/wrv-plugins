package com.winkreyes.excelwandbars;
import java.util.*;import java.nio.file.*;import java.nio.channels.*;import java.nio.*;import java.io.*;import java.security.*;import java.nio.charset.StandardCharsets;
import wrv.magic.retention.BoundedPractice;
/** Offline transaction core only. No native adapter, runtime hook, default activation or historical-provenance claim. */
final class FirstObservationFloor implements AutoCloseable {
 enum Input { SAVED_ITEM, TEMPLATE, NEW_PROVISION }
 record Capture(UUID owner,String wand,String spell,int nativeRank,Input input,Object exactAuthority){}
 interface Authority { boolean exactSavedCapture(Capture capture)throws Exception; boolean exactBoundTouch(Capture capture)throws Exception; }
 interface Fault {void at(String stage)throws IOException;}
 record Policy(String epoch,boolean enabled,int capacity){Policy{if(epoch==null||!epoch.matches("[a-zA-Z0-9_.-]{1,100}")||capacity<1)throw new IllegalArgumentException();}}
 record Key(UUID owner,String wand,String spell,String epoch){}
 record Entry(Key key,int rank,boolean committed){}
 record Observation(int capturedRank,boolean committed){}
 Optional<Observation> observed(UUID owner,String wand,String spell)throws IOException{ready();var e=entries.get(new Key(owner,wand,spell,policy.epoch()));return e==null?Optional.empty():Optional.of(new Observation(e.rank,e.committed));}
 enum Status{CONFIRMED,PREPARED,REVIEW}
 record Result(Status status,int floor,String reason){}
 private final Path file;private final Policy policy;private final Authority authority;private final Map<String,BoundedPractice> ledgers;private final Fault fault;private final Thread thread=Thread.currentThread();
 private final Map<Key,Entry> entries=new LinkedHashMap<>();private FileChannel lockChannel;private FileLock lock;private byte[] disk;private boolean closed,uncertain;
 FirstObservationFloor(Path file,Policy policy,Map<String,BoundedPractice> ledgers,Authority authority)throws IOException{this(file,policy,ledgers,authority,s->{});}
 FirstObservationFloor(Path file,Policy policy,Map<String,BoundedPractice> ledgers,Authority authority,Fault fault)throws IOException{
  this.file=file.toAbsolutePath();this.policy=Objects.requireNonNull(policy);this.ledgers=Map.copyOf(ledgers);this.authority=Objects.requireNonNull(authority);this.fault=Objects.requireNonNull(fault);
  Files.createDirectories(this.file.getParent());try{lockChannel=FileChannel.open(this.file.resolveSibling(this.file.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);lock=lockChannel.tryLock();if(lock==null)throw new IOException("journal already owned");disk=Files.exists(this.file)?Files.readAllBytes(this.file):null;if(disk!=null)read(disk);}catch(Exception e){closeQuiet();throw new IOException("journal unavailable",e);}
 }
 boolean covered(UUID owner,String spell)throws IOException{ready();int required=0;for(var e:entries.values())if(e.committed&&e.key.owner.equals(owner)&&e.key.spell.equals(spell))required=Math.max(required,e.rank);if(required==0)return false;var ledger=ledgers.get(spell);return ledger!=null&&ledger.snapshot(owner.toString()).migrationFloor()>=required;}
 private Key key(Capture c){return new Key(c.owner(),c.wand(),c.spell(),policy.epoch());}
 private void ready()throws IOException{if(closed||uncertain||Thread.currentThread()!=thread)throw new IOException("journal fenced");byte[] now=Files.exists(file)?Files.readAllBytes(file):null;if(!Arrays.equals(now,disk)){uncertain=true;throw new IOException("journal externally changed");}}
 private BoundedPractice validate(Capture c,boolean touch)throws Exception{
  ready();if(!policy.enabled()||c==null||c.owner()==null||c.wand()==null||c.wand().isBlank()||c.wand().length()>200||c.spell()==null||!c.spell().matches("[a-z0-9_.-]{1,200}")||c.input()!=Input.SAVED_ITEM||c.exactAuthority()==null||!(touch?authority.exactBoundTouch(c):authority.exactSavedCapture(c)))throw new IOException("exact saved owner/rank authority unavailable");ready();
  var ledger=ledgers.get(c.spell());if(ledger==null||c.nativeRank()<1||c.nativeRank()>ledger.snapshot(c.owner().toString()).maxLevel())throw new IOException("unreviewed spell/rank");
  for(var e:entries.values())if(e.key.wand.equals(c.wand())&&e.key.epoch.equals(policy.epoch())&&!e.key.owner.equals(c.owner()))throw new IOException("wand owner changed");return ledger;
 }
 /** Explicit first observation: same identity cannot be prepared again with a different initial payload. */
 Result prepare(Capture c){try{validate(c,false);var k=key(c);var old=entries.get(k);if(old!=null){if(old.rank!=c.nativeRank())return review("conflicting first observation");return new Result(old.committed?Status.CONFIRMED:Status.PREPARED,0,"existing immutable observation");}if(entries.size()>=policy.capacity())return review("journal capacity");var proposed=new LinkedHashMap<>(entries);proposed.put(k,new Entry(k,c.nativeRank(),false));persist(proposed,"prepare");return new Result(Status.PREPARED,0,"saved current native rank, no historical claim");}catch(Exception e){return review(e.getMessage());}}
 /** Before each managed touch: committed entries never re-import ordinary subsequent native growth. Pending recovery uses saved rank, not current input. */
 Result beforeTouch(Capture c){try{var ledger=validate(c,true);var e=entries.get(key(c));if(e==null)return review("saved pre-mutation capture missing");
   var before=ledger.snapshot(c.owner().toString());if(e.committed){if(before.migrationFloor()<e.rank)return review("committed floor missing");return new Result(Status.CONFIRMED,before.migrationFloor(),"already observed; no re-import");}
   // Authority is checked again immediately before durable import. Pending immutable rank survives a retry.
   if(!authority.exactBoundTouch(c))return review("authority changed");ready();
   ledger.importFloor(c.owner().toString(),e.rank,receipt(e.key));fault.at("after-floor");var after=ledger.snapshot(c.owner().toString());if(after.migrationFloor()<e.rank||after.usefulCount()!=before.usefulCount())return review("floor reconciliation required");
   var proposed=new LinkedHashMap<>(entries);proposed.put(e.key,new Entry(e.key,e.rank,true));persist(proposed,"commit");return new Result(Status.CONFIRMED,after.migrationFloor(),"durable current-rank floor; zero historical casts");
  }catch(Exception failure){uncertain=true;return review(failure.getMessage());}}
 private static Result review(String why){return new Result(Status.REVIEW,0,why);}
 private String receipt(Key k)throws Exception{var out=new ByteArrayOutputStream();try(var d=new DataOutputStream(out)){d.writeUTF(k.owner.toString());d.writeUTF(k.wand);d.writeUTF(k.spell);d.writeUTF(k.epoch);}return "first-current-v1-"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(out.toByteArray()));}
 private void persist(Map<Key,Entry> proposed,String phase)throws IOException{
  ready();Path temp=null;try{fault.at(phase+"-before-write");var bytes=new ByteArrayOutputStream();try(var d=new DataOutputStream(bytes)){d.writeUTF("FIRST-CURRENT-1");d.writeUTF(policy.epoch());d.writeInt(proposed.size());for(var e:proposed.values()){d.writeUTF(e.key.owner.toString());d.writeUTF(e.key.wand);d.writeUTF(e.key.spell);d.writeInt(e.rank);d.writeBoolean(e.committed);}}
   byte[] payload=bytes.toByteArray();byte[] raw;try{var envelope=new ByteArrayOutputStream();try(var e=new DataOutputStream(envelope)){e.writeInt(payload.length);e.write(payload);e.write(MessageDigest.getInstance("SHA-256").digest(payload));}raw=envelope.toByteArray();}catch(NoSuchAlgorithmException impossible){throw new IOException(impossible);}temp=Files.createTempFile(file.getParent(),".first-current-",".tmp");try(var ch=FileChannel.open(temp,StandardOpenOption.WRITE)){var b=ByteBuffer.wrap(raw);while(b.hasRemaining())ch.write(b);ch.force(true);}fault.at(phase+"-before-move");ready();Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);try(var dir=FileChannel.open(file.getParent(),StandardOpenOption.READ)){dir.force(true);}fault.at(phase+"-after-move");if(!Arrays.equals(raw,Files.readAllBytes(file)))throw new IOException("journal readback mismatch");disk=raw;entries.clear();entries.putAll(proposed);
  }catch(IOException e){uncertain=true;throw e;}finally{if(temp!=null)Files.deleteIfExists(temp);}
 }
 private void read(byte[] raw)throws IOException{byte[] payload;try(var outer=new DataInputStream(new ByteArrayInputStream(raw))){int length=outer.readInt();if(length<0||length!=raw.length-36)throw new IOException("journal envelope length");payload=outer.readNBytes(length);byte[] checksum=outer.readNBytes(32);try{if(!MessageDigest.isEqual(checksum,MessageDigest.getInstance("SHA-256").digest(payload)))throw new IOException("journal checksum mismatch");}catch(NoSuchAlgorithmException impossible){throw new IOException(impossible);}}try(var d=new DataInputStream(new ByteArrayInputStream(payload))){if(!d.readUTF().equals("FIRST-CURRENT-1")||!d.readUTF().equals(policy.epoch()))throw new IOException("journal version/epoch mismatch");int n=d.readInt();if(n<0||n>policy.capacity())throw new IOException("journal capacity");for(int i=0;i<n;i++){var k=new Key(UUID.fromString(d.readUTF()),d.readUTF(),d.readUTF(),policy.epoch());int rank=d.readInt();boolean committed=d.readBoolean();if(k.wand.isBlank()||k.wand.length()>200||!k.spell.matches("[a-z0-9_.-]{1,200}")||rank<1||rank>6||entries.putIfAbsent(k,new Entry(k,rank,committed))!=null)throw new IOException("invalid observation");for(var other:entries.keySet())if(other.wand.equals(k.wand)&&!other.owner.equals(k.owner))throw new IOException("conflicting owner");}if(d.available()!=0)throw new IOException("trailing bytes");}}
 private void closeQuiet(){try{if(lock!=null)lock.release();}catch(Exception ignored){}try{if(lockChannel!=null)lockChannel.close();}catch(Exception ignored){}}
 @Override public void close(){closed=true;closeQuiet();}
}
