package wrv.magic.retention;

import java.io.*;import java.nio.channels.*;import java.nio.file.*;import java.security.*;import java.util.*;
import static java.nio.file.StandardOpenOption.*;
/** Offline one-use-per-consumer authority. No gameplay, currency or automatic lifecycle inference. */
public final class AuthenticatedWindow implements AutoCloseable {
 public enum Result { SETTLED, RATE_LIMITED, DAILY_CAPPED, CLOSED_NO_EFFECT, REVIEW }
 public record Token(UUID lineage,long epoch,long sequence,UUID caster,String spell,String sourceHash,UUID capability){}
 public record Key(long epoch,long sequence){}
 public record View(Key key,boolean closed,boolean useful,Map<String,Result> consumers){}
 public interface Fault {void at(String stage)throws IOException;}
 private static final Fault NONE=s->{};
 private static final class Row {
  Token token;boolean closed,useful;Map<String,Result> results=new TreeMap<>();Map<String,String> actions=new TreeMap<>();
  Row copy(){Row r=new Row();r.token=token;r.closed=closed;r.useful=useful;r.results.putAll(results);r.actions.putAll(actions);return r;}
 }
 private static final class State {
  UUID lineage=UUID.randomUUID();long epoch=0,next=1,watermark=0,retiredEpoch=0;byte[] secret=new byte[32];{new SecureRandom().nextBytes(secret);}TreeMap<Key,Row> rows=new TreeMap<>(Comparator.comparingLong(Key::epoch).thenComparingLong(Key::sequence));
  State copy(){State s=new State();s.lineage=lineage;s.epoch=epoch;s.next=next;s.watermark=watermark;s.retiredEpoch=retiredEpoch;s.secret=secret.clone();rows.forEach((k,r)->s.rows.put(k,r.copy()));return s;}
 }
 private final Path path;private final FileChannel lockChannel;private final FileLock lock;private final Set<String> consumers;private final int capacity,maxActions;private final Fault fault;
 private State state;private byte[] expected;private boolean closed,uncertain;
 public AuthenticatedWindow(Path path,Set<String> consumers,int capacity,int maxActions)throws IOException{this(path,consumers,capacity,maxActions,NONE);}
 public AuthenticatedWindow(Path path,Set<String> consumers,int capacity,int maxActions,Fault fault)throws IOException{
  if(consumers.isEmpty()||consumers.size()>128||capacity<1||maxActions<1)throw new IllegalArgumentException("explicit positive bounds/consumers required");
  for(String c:consumers)valid(c);this.consumers=Set.copyOf(consumers);this.capacity=capacity;this.maxActions=maxActions;this.fault=Objects.requireNonNull(fault);this.path=path.toAbsolutePath();Files.createDirectories(this.path.getParent());
  lockChannel=FileChannel.open(Path.of(this.path+".lock"),CREATE,WRITE);FileLock taken=null;
  try{taken=lockChannel.tryLock();if(taken==null)throw new IOException("another issuer owns file");lock=taken;expected=Files.exists(this.path)?Files.readAllBytes(this.path):null;state=expected==null?new State():decode(expected);if(state.rows.size()>capacity)throw new IOException("retained rows exceed configured bound");
   State next=state.copy();next.epoch=Math.addExact(next.epoch,1);next.next=1;next.watermark=0;
   for(Row r:next.rows.values()){r.closed=true;for(String c:consumers)if(!r.results.containsKey(c))r.results.put(c,Result.REVIEW);}
   compact(next);commit(next);
  }catch(Throwable e){if(taken!=null)taken.release();lockChannel.close();if(e instanceof IOException x)throw x;throw new IOException("issuer unavailable",e);}
 }
 private static void valid(String s){if(s==null||!s.matches("[a-z0-9_.:-]{1,128}"))throw new IllegalArgumentException("canonical identifier required");}
 private void ready()throws IOException{if(closed||uncertain)throw new IOException("closed or persistence uncertain");byte[] current=Files.exists(path)?Files.readAllBytes(path):null;if(!Arrays.equals(expected,current)){uncertain=true;throw new IOException("external state change; reload/reconcile required");}}
 public synchronized Token issue(UUID caster,String spell,String sourceHash)throws IOException{
  ready();Objects.requireNonNull(caster);valid(spell);if(sourceHash==null||!sourceHash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("source hash required");
  if(state.rows.size()>=capacity||state.next-state.watermark>capacity)throw new IOException("evidence capacity; gameplay must continue without credit token");
  State s=state.copy();long seq=s.next;s.next=Math.addExact(s.next,1);Token t=new Token(s.lineage,s.epoch,seq,caster,spell,sourceHash,capability(s,s.epoch,seq,caster,spell,sourceHash));Row r=new Row();r.token=t;s.rows.put(new Key(t.epoch,t.sequence),r);commit(s);return t;
 }
 private Row row(State s,Token t)throws IOException{if(t==null||!t.lineage.equals(s.lineage)||t.epoch!=s.epoch||t.sequence<=s.watermark)throw new IOException("unknown/closed epoch token");Row r=s.rows.get(new Key(t.epoch,t.sequence));if(r==null||!r.token.equals(t)||r.closed)throw new IOException("forged, unknown or closed token");return r;}
 /** The caller must already have verified a useful effect. Partial/ineligible evidence does not call this. */
 public synchronized boolean useful(Token t,String actionId,String fingerprint)throws IOException{
  ready();valid(actionId);if(fingerprint==null||!fingerprint.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("receipt digest required");State s=state.copy();Row r=row(s,t);String prior=r.actions.get(actionId);if(prior!=null){if(!prior.equals(fingerprint))throw new IOException("conflicting receipt");return false;}if(r.actions.size()>=maxActions)throw new IOException("action evidence bound");r.actions.put(actionId,fingerprint);r.useful=true;commit(s);return true;
 }
 public synchronized boolean settle(Token t,String consumer,Result result)throws IOException{
  ready();if(!consumers.contains(consumer)||result==null||result==Result.CLOSED_NO_EFFECT)throw new IllegalArgumentException("consumer/result");State s=state.copy();Row r=row(s,t);if(!r.useful)throw new IOException("no verified useful effect");Result old=r.results.get(consumer);if(old!=null){if(old!=result)throw new IOException("conflicting settlement");return false;}r.results.put(consumer,result);commit(s);return true;
 }
 /** Trusted lifecycle proof only; root finish is NOT automatically such proof. */
 public synchronized void closeCast(Token t)throws IOException{
  ready();State s=state.copy();Row r=row(s,t);r.closed=true;for(String c:consumers)r.results.putIfAbsent(c,r.useful?Result.REVIEW:Result.CLOSED_NO_EFFECT);compact(s);commit(s);
 }
 /** Explicit reviewed resolution. No award is executed. For uncertain external mutation do not choose SETTLED without evidence. */
 public synchronized void resolveReview(Key key,String consumer,Result result)throws IOException{
  ready();if(!consumers.contains(consumer)||result==null||result==Result.REVIEW)throw new IllegalArgumentException();State s=state.copy();Row r=s.rows.get(key);if(r==null||!r.closed||r.results.get(consumer)!=Result.REVIEW)throw new IOException("not a closed review");r.results.put(consumer,result);compact(s);commit(s);
 }
 private boolean terminal(Row r){return r.closed&&r.results.keySet().containsAll(consumers)&&!r.results.containsValue(Result.REVIEW);}
 private void compact(State s){s.rows.entrySet().removeIf(e->e.getKey().epoch<s.epoch&&terminal(e.getValue()));while(true){Key k=new Key(s.epoch,s.watermark+1);Row r=s.rows.get(k);if(r==null||!terminal(r))break;s.rows.remove(k);s.watermark++;}long oldest=s.rows.keySet().stream().filter(k->k.epoch<s.epoch).mapToLong(Key::epoch).min().orElse(s.epoch);s.retiredEpoch=Math.max(s.retiredEpoch,oldest-1);}
 public synchronized List<View> retained()throws IOException{ready();return state.rows.entrySet().stream().map(e->new View(e.getKey(),e.getValue().closed,e.getValue().useful,Map.copyOf(e.getValue().results))).toList();}
 public synchronized boolean isOpenUseful(Token token,String actionId,String fingerprint)throws IOException{ready();valid(actionId);if(fingerprint==null||!fingerprint.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("receipt digest required");Row r=row(state,token);return r.useful&&fingerprint.equals(r.actions.get(actionId));}
 public synchronized Set<String> consumerIds()throws IOException{ready();return consumers;}
 public synchronized long watermark()throws IOException{ready();return state.watermark;}
 private void commit(State next)throws IOException{
  ready();byte[] bytes=encode(next);Path tmp=Files.createTempFile(path.getParent(),"issuer-",".tmp");try{
   fault.at("before-write");try(FileChannel ch=FileChannel.open(tmp,WRITE,TRUNCATE_EXISTING)){java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(bytes);while(b.hasRemaining())ch.write(b);ch.force(true);}fault.at("before-move");ready();Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);fault.at("after-move");try(FileChannel dir=FileChannel.open(path.getParent(),READ)){dir.force(true);}if(!Arrays.equals(bytes,Files.readAllBytes(path)))throw new IOException("readback mismatch");expected=bytes;state=next;
  }catch(IOException|RuntimeException e){uncertain=true;throw new IOException("durable issuance uncertain; close/reconcile",e);}finally{Files.deleteIfExists(tmp);}
 }
 private byte[] encode(State s)throws IOException{
  ByteArrayOutputStream raw=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(raw)){out.writeInt(2);out.writeUTF(s.lineage.toString());out.write(s.secret);out.writeLong(s.retiredEpoch);out.writeLong(s.epoch);out.writeLong(s.next);out.writeLong(s.watermark);out.writeInt(consumers.size());for(String c:new TreeSet<>(consumers))out.writeUTF(c);out.writeInt(s.rows.size());for(Row r:s.rows.values()){Token t=r.token;out.writeLong(t.epoch);out.writeLong(t.sequence);out.writeUTF(t.caster.toString());out.writeUTF(t.spell);out.writeUTF(t.sourceHash);out.writeUTF(t.capability.toString());out.writeBoolean(r.closed);out.writeBoolean(r.useful);out.writeInt(r.results.size());for(var e:r.results.entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue().name());}out.writeInt(r.actions.size());for(var e:r.actions.entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue());}}}byte[] payload=raw.toByteArray();raw.write(hash(payload));return raw.toByteArray();
 }
 private State decode(byte[] bytes)throws IOException{
  if(bytes.length<32)throw new IOException("truncated");byte[] payload=Arrays.copyOf(bytes,bytes.length-32);if(!MessageDigest.isEqual(hash(payload),Arrays.copyOfRange(bytes,bytes.length-32,bytes.length)))throw new IOException("checksum");
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload))){if(in.readInt()!=2)throw new IOException("schema");State s=new State();s.lineage=UUID.fromString(in.readUTF());in.readFully(s.secret);s.retiredEpoch=in.readLong();s.epoch=in.readLong();s.next=in.readLong();s.watermark=in.readLong();if(s.retiredEpoch<0||s.retiredEpoch>=s.epoch||s.epoch<0||s.next<1||s.watermark<0||s.watermark>=s.next)throw new IOException("counter");Set<String> saved=new TreeSet<>();int nc=bounded(in.readInt(),128);for(int i=0;i<nc;i++)if(!saved.add(in.readUTF()))throw new IOException("duplicate consumer");if(!saved.equals(consumers))throw new IOException("consumer migration required");int nr=bounded(in.readInt(),capacity);for(int i=0;i<nr;i++){Row r=new Row();long epoch=in.readLong(),seq=in.readLong();if(epoch<=s.retiredEpoch||epoch<1||epoch>s.epoch||seq<1||(epoch==s.epoch&&(seq<=s.watermark||seq>=s.next)))throw new IOException("row range");r.token=new Token(s.lineage,epoch,seq,UUID.fromString(in.readUTF()),in.readUTF(),in.readUTF(),UUID.fromString(in.readUTF()));if(!authentic(s,r.token))throw new IOException("unauthenticated row");valid(r.token.spell);if(!r.token.sourceHash.matches("[a-f0-9]{64}"))throw new IOException("source");r.closed=in.readBoolean();r.useful=in.readBoolean();if(epoch<s.epoch&&!r.closed)throw new IOException("old epoch still open");int results=bounded(in.readInt(),consumers.size());for(int j=0;j<results;j++){String c=in.readUTF();if(!consumers.contains(c)||r.results.put(c,Result.valueOf(in.readUTF()))!=null)throw new IOException("consumer result");}int na=bounded(in.readInt(),maxActions);for(int j=0;j<na;j++){String a=in.readUTF(),f=in.readUTF();valid(a);if(!f.matches("[a-f0-9]{64}")||r.actions.put(a,f)!=null)throw new IOException("action");}if(s.rows.put(new Key(epoch,seq),r)!=null)throw new IOException("duplicate row");}if(in.available()!=0)throw new IOException("trailing");return s;}catch(IllegalArgumentException e){throw new IOException("malformed state",e);}
 }
 /** Exact authentic issued token plus durable all-consumer terminal boundary. */
 public synchronized boolean isRetired(Token token)throws IOException{ready();if(!authentic(state,token))throw new IOException("unknown token authority");return token.epoch<=state.retiredEpoch||token.epoch==state.epoch&&token.sequence<=state.watermark;}
 private static boolean authentic(State s,Token t){return t!=null&&t.lineage!=null&&t.lineage.equals(s.lineage)&&t.epoch>0&&t.epoch<=s.epoch&&t.sequence>0&&t.caster!=null&&t.spell!=null&&t.sourceHash!=null&&t.capability!=null&&capability(s,t.epoch,t.sequence,t.caster,t.spell,t.sourceHash).equals(t.capability);}
 private static UUID capability(State s,long epoch,long sequence,UUID caster,String spell,String source){try{var raw=new ByteArrayOutputStream();try(var out=new DataOutputStream(raw)){out.writeUTF("wrv-issued-token-v2");out.writeUTF(s.lineage.toString());out.writeLong(epoch);out.writeLong(sequence);out.writeUTF(caster.toString());out.writeUTF(spell);out.writeUTF(source);}var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(s.secret,"HmacSHA256"));var bytes=java.nio.ByteBuffer.wrap(mac.doFinal(raw.toByteArray()));return new UUID(bytes.getLong(),bytes.getLong());}catch(Exception e){throw new IllegalStateException("token authentication unavailable",e);}}
 private static int bounded(int n,int max)throws IOException{if(n<0||n>max)throw new IOException("bound");return n;}
 private static byte[] hash(byte[] data){try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
 @Override public synchronized void close()throws IOException{if(!closed){closed=true;try{lock.release();}finally{lockChannel.close();}}}
}
