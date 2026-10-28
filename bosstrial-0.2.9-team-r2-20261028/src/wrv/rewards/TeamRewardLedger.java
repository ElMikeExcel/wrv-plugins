package wrv.rewards;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

/** Single-writer durable roster/claims. Does NOT perform inventory mutations or determine combat eligibility. */
public final class TeamRewardLedger implements AutoCloseable {
 public enum State { OWED,PENDING,DELIVERED,REVIEW }
 public enum EncounterOutcome { ACTIVE,WON,LOST,ABORTED }
 public enum Outcome { DELIVERED,NOT_APPLIED,UNCERTAIN }
 public record RewardUnit(String unitId,String payload) { public RewardUnit { valid(unitId); Objects.requireNonNull(payload); if(payload.length()>20000)throw new IllegalArgumentException("payload too large"); } }
 public record Claim(String id,String encounterId,String planId,UUID player,List<RewardUnit> units,State state,String attempt) { public Claim {units=List.copyOf(units);} }
 private static final int MAGIC=0x54525731, LIMIT=100000;
 private static class Encounter { String id,plan=""; EncounterOutcome outcome=EncounterOutcome.ACTIVE; Set<UUID> roster=new HashSet<>(); Encounter(String id){this.id=id;} }
 private final Path file; private final FileChannel lockChannel; private final FileLock lock;
 private Map<String,Encounter> encounters=new TreeMap<>(); private Map<String,Claim> claims=new TreeMap<>();
 private Map<String,String> scopes=new TreeMap<>();
 private byte[] expected; private boolean failed,closed;
 public TeamRewardLedger(Path file) throws IOException {
  this.file=file.toAbsolutePath(); Files.createDirectories(this.file.getParent());
  lockChannel=FileChannel.open(this.file.resolveSibling(this.file.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  FileLock acquired; try{acquired=lockChannel.tryLock();}catch(Exception e){lockChannel.close();throw new IOException("ledger already owned",e);} if(acquired==null){lockChannel.close();throw new IOException("ledger already owned");} lock=acquired;
  try { expected=Files.exists(this.file)?Files.readAllBytes(this.file):null; if(expected!=null)read(expected);
   boolean dirty=false; for(var e:new ArrayList<>(claims.entrySet()))if(e.getValue().state==State.PENDING){Claim c=e.getValue();claims.put(e.getKey(),state(c,State.REVIEW,c.attempt));dirty=true;} if(dirty)persist();
  }catch(Exception e){close();throw new IOException("invalid or unwritable reward ledger",e);}
 }
 public synchronized void openEncounter(String id)throws IOException {ready();valid(id);if(encounters.containsKey(id))return;if(encounters.size()>=LIMIT)throw new IllegalStateException("encounter limit");change(()->encounters.put(id,new Encounter(id)));}
 /** Adapter must establish legitimate active-fight participation; late entry has no HP-percent restriction here. */
 public synchronized void qualify(String id,UUID player)throws IOException {ready();Objects.requireNonNull(player);Encounter e=encounter(id);if(e.outcome!=EncounterOutcome.ACTIVE)throw new IllegalStateException("already finalized");if(e.roster.contains(player))return;if(e.roster.size()>=1000)throw new IllegalStateException("roster limit");change(()->e.roster.add(player));}
 public synchronized void closeWithoutWin(String id,EncounterOutcome outcome)throws IOException {ready();if(outcome!=EncounterOutcome.LOST&&outcome!=EncounterOutcome.ABORTED)throw new IllegalArgumentException("not a loss/abort");Encounter e=encounter(id);if(e.outcome==outcome)return;if(e.outcome!=EncounterOutcome.ACTIVE)throw new IllegalStateException("conflicting terminal outcome");change(()->e.outcome=outcome);}
 public synchronized EncounterOutcome outcome(String id){ready();return encounter(id).outcome;}
 /** Atomically carries a credited component roster into a team run (elder trio), with no items. */
 public synchronized void creditComponentInto(String componentId,String teamId)throws IOException {
  ready();valid(teamId);valid("component-credit:"+teamId);if(componentId.equals(teamId))throw new IllegalArgumentException("component equals team");
  Encounter child=encounter(componentId);if(child.roster.isEmpty())throw new IllegalStateException("no component roster");
  Encounter existing=encounters.get(teamId);
  if(child.outcome==EncounterOutcome.WON){
   if(!child.plan.equals("component-credit:"+teamId)||existing==null||!existing.roster.containsAll(child.roster))throw new IllegalStateException("component conflict");return;
  }
  if(child.outcome!=EncounterOutcome.ACTIVE)throw new IllegalStateException("component not active");
  if(existing!=null&&existing.outcome!=EncounterOutcome.ACTIVE)throw new IllegalStateException("team finalized");
  if(existing==null&&encounters.size()>=LIMIT)throw new IllegalStateException("encounter limit");
  Set<UUID> union=new HashSet<>(child.roster);if(existing!=null)union.addAll(existing.roster);
  if(union.size()>1000)throw new IllegalStateException("roster limit");
  if((long)claims.size()+child.roster.size()>LIMIT)throw new IllegalStateException("claim limit");
  change(()->{
   Encounter team=encounters.computeIfAbsent(teamId,Encounter::new);team.roster.addAll(child.roster);
   child.outcome=EncounterOutcome.WON;child.plan="component-credit:"+teamId;
   for(UUID player:child.roster){String id=componentId+"/"+player;claims.put(id,new Claim(id,componentId,child.plan,player,List.of(),State.OWED,""));}
  });
 }
 public synchronized Set<UUID> roster(String id){ready();return Set.copyOf(encounter(id).roster);}
 /** Caller supplies the explicit, budgeted distribution. Missing qualified players require an explicit empty list. */
 public synchronized void win(String id,String planId,Map<UUID,List<RewardUnit>> distribution)throws IOException {winInternal(id,null,planId,distribution);}
 public synchronized void winOnceScope(String id,String scope,String planId,Map<UUID,List<RewardUnit>> distribution)throws IOException {valid(scope);winInternal(id,scope,planId,distribution);}
 public synchronized void importConsumedScope(String scope)throws IOException {ready();valid(scope);if(scopes.containsKey(scope))return;change(()->scopes.put(scope,"!IMPORTED"));}
 public synchronized Map<String,String> consumedScopes(){ready();return Map.copyOf(scopes);}
 private void winInternal(String id,String scope,String planId,Map<UUID,List<RewardUnit>> distribution)throws IOException {
  ready();valid(planId);Encounter e=encounter(id);Objects.requireNonNull(distribution);
  if(scope!=null&&scopes.containsKey(scope)&&!scopes.get(scope).equals(id))throw new IllegalStateException("payout scope already consumed");
  if(e.roster.isEmpty())throw new IllegalStateException("empty team");
  if(!distribution.keySet().equals(e.roster))throw new IllegalArgumentException("plan must explicitly cover frozen qualified roster");
  Map<String,Claim> proposed=new TreeMap<>();Set<String> units=new HashSet<>();
  for(UUID p:e.roster){List<RewardUnit> list=List.copyOf(distribution.get(p));if(list.size()>1000)throw new IllegalArgumentException("too many units");for(RewardUnit u:list)if(!units.add(u.unitId))throw new IllegalArgumentException("reward unit allocated twice");String cid=id+"/"+p;proposed.put(cid,new Claim(cid,id,planId,p,list,State.OWED,""));}
  if(e.outcome==EncounterOutcome.WON){if(scope!=null&&!id.equals(scopes.get(scope)))throw new IllegalStateException("cannot add scope after finalization");if(!e.plan.equals(planId))throw new IllegalStateException("different plan for finalized encounter");for(Claim c:proposed.values()){Claim old=claims.get(c.id);if(old==null||!old.units.equals(c.units))throw new IllegalStateException("changed immutable reward plan");}return;}
  if(e.outcome!=EncounterOutcome.ACTIVE)throw new IllegalStateException("encounter ended without win");
  if(claims.size()+proposed.size()>LIMIT)throw new IllegalStateException("claim limit");
  change(()->{e.outcome=EncounterOutcome.WON;e.plan=planId;claims.putAll(proposed);if(scope!=null)scopes.put(scope,id);});
 }
 public synchronized List<Claim> claims(UUID owner){ready();Objects.requireNonNull(owner);return claims.values().stream().filter(c->c.player.equals(owner)).toList();}
 public synchronized Claim beginDelivery(UUID owner,String id)throws IOException {ready();Claim c=owned(owner,id);if(c.state!=State.OWED)throw new IllegalStateException("not claimable: "+c.state);Claim next=state(c,State.PENDING,UUID.randomUUID().toString());change(()->claims.put(id,next));return next;}
 public synchronized void finishDelivery(UUID owner,String id,String attempt,Outcome result)throws IOException {ready();Claim c=owned(owner,id);Objects.requireNonNull(result);if(!c.attempt.equals(attempt)||attempt.isEmpty())throw new IllegalStateException("wrong attempt");State target=switch(result){case DELIVERED->State.DELIVERED;case NOT_APPLIED->State.OWED;case UNCERTAIN->State.REVIEW;};if(c.state==target&&target!=State.OWED)return;if(c.state!=State.PENDING)throw new IllegalStateException("not pending");change(()->claims.put(id,state(c,target,target==State.OWED?"":attempt)));}
 private Claim owned(UUID owner,String id){Objects.requireNonNull(owner);Claim c=claims.get(id);if(c==null||!c.player.equals(owner))throw new SecurityException("not owner");return c;}
 private static Claim state(Claim c,State s,String a){return new Claim(c.id,c.encounterId,c.planId,c.player,c.units,s,a);}
 private Encounter encounter(String id){Encounter e=encounters.get(id);if(e==null)throw new IllegalArgumentException("unknown encounter");return e;}
 private static void valid(String s){if(s==null||s.isBlank()||s.startsWith("!")||s.length()>200||s.contains("/")||s.indexOf('\0')>=0)throw new IllegalArgumentException("invalid ID");}
 private void ready(){if(closed||failed)throw new IllegalStateException("ledger closed or persistence uncertain");}
 private void change(Runnable operation)throws IOException {byte[] before=encode();try{operation.run();persist();}catch(Exception ex){failed=true;try{read(before);}catch(Exception ignored){}if(ex instanceof IOException io)throw io;throw ex;}}
 private void persist()throws IOException {
  byte[] current=Files.exists(file)?Files.readAllBytes(file):null;if(!Arrays.equals(current,expected))throw new IOException("external ledger change");byte[] bytes=encode();Path tmp=Files.createTempFile(file.getParent(),file.getFileName()+".",".tmp");
  try{try(FileChannel ch=FileChannel.open(tmp,StandardOpenOption.WRITE)){java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(bytes);while(b.hasRemaining())ch.write(b);ch.force(true);}Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);try(FileChannel dir=FileChannel.open(file.getParent(),StandardOpenOption.READ)){dir.force(true);}byte[] verify=Files.readAllBytes(file);if(!Arrays.equals(bytes,verify))throw new IOException("readback mismatch");expected=bytes;}finally{Files.deleteIfExists(tmp);}
 }
 private byte[] encode()throws IOException {ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeInt(MAGIC);d.writeInt(2);d.writeInt(encounters.size());for(Encounter e:encounters.values()){d.writeUTF(e.id);d.writeUTF(e.outcome.name());d.writeUTF(e.plan);d.writeInt(e.roster.size());for(UUID p:new TreeSet<>(e.roster))d.writeUTF(p.toString());}d.writeInt(claims.size());for(Claim c:claims.values()){d.writeUTF(c.id);d.writeUTF(c.encounterId);d.writeUTF(c.planId);d.writeUTF(c.player.toString());d.writeUTF(c.state.name());d.writeUTF(c.attempt);d.writeInt(c.units.size());for(RewardUnit u:c.units){d.writeUTF(u.unitId);d.writeUTF(u.payload);}}d.writeInt(scopes.size());for(var entry:scopes.entrySet()){d.writeUTF(entry.getKey());d.writeUTF(entry.getValue());}}byte[] body=b.toByteArray();if(body.length>64*1024*1024-32)throw new IOException("ledger capacity");b.write(digest(body));return b.toByteArray();}
 private static int count(DataInputStream d)throws IOException {int n=d.readInt();if(n<0||n>LIMIT)throw new IOException("invalid count");return n;}
 private static byte[] digest(byte[] bytes){try{return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
 private void read(byte[] bytes)throws IOException {if(bytes.length<40)throw new IOException("truncated ledger");byte[] body=Arrays.copyOf(bytes,bytes.length-32);if(!java.security.MessageDigest.isEqual(digest(body),Arrays.copyOfRange(bytes,bytes.length-32,bytes.length)))throw new IOException("checksum mismatch");if(bytes.length>64*1024*1024)throw new IOException("oversized ledger");Map<String,Encounter> es=new TreeMap<>();Map<String,Claim> cs=new TreeMap<>();Map<String,String> ss=new TreeMap<>();try(DataInputStream d=new DataInputStream(new ByteArrayInputStream(body))){if(d.readInt()!=MAGIC||d.readInt()!=2)throw new IOException("wrong format/version");int n=count(d);for(int i=0;i<n;i++){String id=d.readUTF();valid(id);Encounter e=new Encounter(id);e.outcome=EncounterOutcome.valueOf(d.readUTF());e.plan=d.readUTF();if(e.outcome==EncounterOutcome.WON)valid(e.plan);else if(!e.plan.isEmpty())throw new IOException("unfinished plan");int m=count(d);for(int j=0;j<m;j++)if(!e.roster.add(UUID.fromString(d.readUTF())))throw new IOException("duplicate roster");if(es.put(id,e)!=null)throw new IOException("duplicate encounter");}n=count(d);Map<String,Set<String>> allocated=new HashMap<>();for(int i=0;i<n;i++){String id=d.readUTF(),eid=d.readUTF(),pid=d.readUTF();UUID p=UUID.fromString(d.readUTF());State s=State.valueOf(d.readUTF());String a=d.readUTF();if(s==State.OWED?!a.isEmpty():a.isEmpty())throw new IOException("invalid attempt");if(!a.isEmpty())UUID.fromString(a);int m=count(d);if(m>1000)throw new IOException("too many reward units");List<RewardUnit> us=new ArrayList<>();Set<String> seen=allocated.computeIfAbsent(eid,k->new HashSet<>());for(int j=0;j<m;j++){RewardUnit u=new RewardUnit(d.readUTF(),d.readUTF());if(!seen.add(u.unitId))throw new IOException("duplicate unit");us.add(u);}Encounter e=es.get(eid);if(e==null||e.outcome!=EncounterOutcome.WON||!e.plan.equals(pid)||!e.roster.contains(p)||!id.equals(eid+"/"+p))throw new IOException("orphan claim");if(cs.put(id,new Claim(id,eid,pid,p,us,s,a))!=null)throw new IOException("duplicate claim");}n=count(d);for(int i=0;i<n;i++){String scope=d.readUTF(),owner=d.readUTF();valid(scope);if(!owner.equals("!IMPORTED")&&(!es.containsKey(owner)||es.get(owner).outcome!=EncounterOutcome.WON))throw new IOException("orphan scope");if(ss.put(scope,owner)!=null)throw new IOException("duplicate scope");}if(d.read()!=-1)throw new IOException("trailing bytes");for(Encounter e:es.values())if(e.outcome==EncounterOutcome.WON)for(UUID p:e.roster)if(!cs.containsKey(e.id+"/"+p))throw new IOException("missing claim");}encounters=es;claims=cs;scopes=ss;}
 @Override public synchronized void close()throws IOException {if(!closed){closed=true;try{lock.release();}finally{lockChannel.close();}}}
}
