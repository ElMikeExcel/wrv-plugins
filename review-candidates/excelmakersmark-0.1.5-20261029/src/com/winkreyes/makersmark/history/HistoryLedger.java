/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.history;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32C;
import java.text.Normalizer;
import java.security.*;

/** Offline candidate archive core. No Bukkit hooks enabled. Single writer; corrupt/incomplete journals fail closed. */
public final class HistoryLedger implements AutoCloseable {
    private static final int MAGIC=0x4d4d4832, LIMIT=16*1024*1024;
    public record Key(String kind,String name) implements Comparable<Key> {
        public Key { Objects.requireNonNull(kind); Objects.requireNonNull(name); name=clean(name); if(kind.isBlank()||name.isBlank())throw new IllegalArgumentException("Empty trophy"); }
        private static String clean(String value){
            String plain=value.replaceAll("(?i)§[0-9a-fk-orx]", "");StringBuilder b=new StringBuilder();
            plain.codePoints().filter(c->Character.getType(c)!=Character.CONTROL&&Character.getType(c)!=Character.FORMAT).forEach(b::appendCodePoint);
            return Normalizer.normalize(b.toString(),Normalizer.Form.NFC).strip();
        }
        public int compareTo(Key k){int c=kind.compareTo(k.kind);return c==0?name.compareTo(k.name):c;}
    }
    public record State(UUID item,long revision,Map<Key,Long> names,Set<UUID> lineage) {
        public State { names=Collections.unmodifiableMap(new TreeMap<>(names));lineage=Set.copyOf(lineage); }
    }
    public record Expected(UUID item,long revision) {}
    public record Plan(UUID receipt,List<Expected> inputs,State output) {
        public Plan { inputs=List.copyOf(inputs); }
    }
    public record Award(UUID receipt,UUID item,UUID victim,UUID actor,Key key){}
    private final Map<UUID,Award> awards=new HashMap<>();
    public enum Status { PREPARED, COMMITTED, ABORTED }
    private final FileChannel channel;
    private final FileLock lock;
    private final Map<UUID,State> states=new HashMap<>();
    private final Map<UUID,Plan> plans=new HashMap<>(); // Pending only; committed snapshots are not retained.
    private record Receipt(UUID item,long revision,String digest){}
    private final Map<UUID,Receipt> receipts=new HashMap<>();
    private final Map<UUID,byte[]> pendingContexts=new HashMap<>();
    private final Map<UUID,String> contextHashes=new HashMap<>();
    private final Map<UUID,String> physicalProofs=new HashMap<>();
    private final Set<UUID> committed=new HashSet<>(), aborted=new HashSet<>(), consumed=new HashSet<>(), quarantined=new HashSet<>();
    private boolean failed;
    public enum Cut { AFTER_WRITE_BEFORE_FORCE, AFTER_FORCE_BEFORE_APPLY }
    @FunctionalInterface public interface FaultInjector { void at(byte operation,Cut cut)throws IOException; }
    private final FaultInjector fault;

    public HistoryLedger(Path file)throws IOException {this(file,(op,cut)->{});}
    /** Test instrumentation only; normal server construction must use the one-argument constructor. */
    public HistoryLedger(Path file,FaultInjector fault)throws IOException {
        this.fault=Objects.requireNonNull(fault);
        channel=FileChannel.open(file,StandardOpenOption.CREATE,StandardOpenOption.READ,StandardOpenOption.WRITE);
        FileLock acquired=null;
        try { acquired=channel.tryLock();if(acquired==null)throw new IOException("Archive already in use"); lock=acquired; replay();channel.force(true);forceDirectory(file.toAbsolutePath().getParent()); }
        catch(Throwable e){if(acquired!=null)acquired.release();channel.close();throw e;}
    }
    public synchronized State state(UUID id){if(failed)throw new IllegalStateException("Archive failed");return states.get(id);}
    public synchronized Status status(UUID receipt){return aborted.contains(receipt)?Status.ABORTED:committed.contains(receipt)?Status.COMMITTED:plans.containsKey(receipt)?Status.PREPARED:null;}
    public synchronized boolean consumed(UUID id){return consumed.contains(id);}
    public synchronized boolean quarantined(UUID id){healthyUnchecked();return quarantined.contains(id);}
    private void healthyUnchecked(){if(failed)throw new IllegalStateException("Archive failed");}
    public synchronized Set<UUID> unresolved(){Set<UUID> r=new HashSet<>(plans.keySet());r.removeAll(committed);return Set.copyOf(r);}
    /** Single-writer ingress persisted before identity registration or award preparation. */
    public synchronized void acceptAward(Award award)throws IOException {
        healthy();Objects.requireNonNull(award);if(committed.contains(award.receipt))return;
        Award prior=awards.get(award.receipt);if(prior!=null){if(!prior.equals(award))throw new IllegalStateException("Award identity conflict");return;}
        if(awards.size()>=4096)throw new IOException("Durable award inbox full");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);
        uuid(d,award.receipt);uuid(d,award.item);uuid(d,award.victim);uuid(d,award.actor);str(d,award.key.kind);str(d,award.key.name);
        append((byte)7,bytes.toByteArray());awards.put(award.receipt,award);
    }
    public synchronized Optional<Award> pendingAward(UUID receipt){if(failed)throw new IllegalStateException("Archive failed");return Optional.ofNullable(awards.get(receipt));}
    public synchronized List<Award> pendingAwards(){if(failed)throw new IllegalStateException("Archive failed");return List.copyOf(awards.values());}
    /** Pure preview: no journal write, no identity assignment, no archive mutation. */
    public synchronized Plan preview(UUID receipt,List<Expected> inputs,UUID output,Key earned){
        if(inputs.isEmpty()&&earned!=null)throw new IllegalArgumentException("Earned history requires established identity");
        checkInputs(inputs,null);
        Map<Key,Long> names=new TreeMap<>();Set<UUID> lineage=new HashSet<>();
        long rev=0;
        for(Expected e:inputs){State s=states.get(e.item);s.names.forEach((k,v)->names.merge(k,v,Math::max));lineage.addAll(s.lineage);if(!s.item.equals(output))lineage.add(s.item);if(s.item.equals(output))rev=s.revision;}
        if(states.containsKey(output)&&inputs.stream().noneMatch(e->e.item.equals(output)))throw new IllegalStateException("Output identity already exists");
        if(earned!=null)names.compute(earned,(k,v)->v==null?1:Math.addExact(v,1));
        return new Plan(receipt,inputs,new State(output,Math.addExact(rev,1),names,lineage));
    }
    /** Fsync the intended transaction BEFORE an adapter changes a committed item result. */
    public synchronized void prepare(Plan p)throws IOException {
        healthy();if(aborted.contains(p.receipt))throw new IllegalStateException("Aborted receipt cannot be reused");String digest=digest(p);Receipt old=receipts.get(p.receipt);if(old!=null){if(!old.digest.equals(digest))throw new IllegalStateException("Receipt conflict");return;}
        validatePlan(p);checkInputs(p.inputs,p.receipt);append((byte)1,encodeDelta(p));plans.put(p.receipt,p);receipts.put(p.receipt,new Receipt(p.output.item,p.output.revision,digest));
    }
    /** Optional private recovery fingerprints, forced before the actual pickup intent. No item text/NBT needed. */
    public synchronized void prepareWithContext(Plan p,byte[] context)throws IOException {
        healthy();Objects.requireNonNull(context);if(context.length>65536)throw new IOException("Recovery context too large");
        String hash=hash(context);String old=contextHashes.get(p.receipt);
        if(old!=null){if(!old.equals(hash))throw new IllegalStateException("Recovery context conflict");prepare(p);return;}
        if(receipts.containsKey(p.receipt))throw new IllegalStateException("Cannot invent context after an intent");
        validatePlan(p);checkInputs(p.inputs,p.receipt);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(bytes);uuid(d,p.receipt);d.writeInt(context.length);d.write(context);
        append((byte)5,bytes.toByteArray());pendingContexts.put(p.receipt,context.clone());contextHashes.put(p.receipt,hash);
        prepare(p);
    }
    public synchronized Optional<byte[]> pendingContext(UUID receipt){if(failed)throw new IllegalStateException("Archive failed");byte[] c=pendingContexts.get(receipt);return plans.containsKey(receipt)&&c!=null?Optional.of(c.clone()):Optional.empty();}
    /** Durable acknowledgement AFTER exact result and input-consumption checks, BEFORE commit. */
    public synchronized void markPhysicalVerified(UUID receipt,UUID item,long revision,String fingerprint,int copies,boolean consumedProof)throws IOException {
        healthy();Plan p=plans.get(receipt);if(p==null||!p.output.item.equals(item)||p.output.revision!=revision||copies!=1||!consumedProof||fingerprint==null||!fingerprint.matches("[0-9a-f]{64}"))throw new IllegalStateException("Unproven physical result");
        String old=physicalProofs.get(receipt);if(old!=null){if(!old.equals(fingerprint))throw new IllegalStateException("Physical receipt conflict");return;}
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);uuid(d,receipt);d.writeUTF(fingerprint);append((byte)6,b.toByteArray());physicalProofs.put(receipt,fingerprint);
    }
    public synchronized Optional<String> physicalProof(UUID receipt){if(failed)throw new IllegalStateException("Archive failed");return plans.containsKey(receipt)?Optional.ofNullable(physicalProofs.get(receipt)):Optional.empty();}
    /** Adapter must prove actual post-event result UUID/revision/receipt and input consumption. Never call from previews. */
    public synchronized void commit(UUID receipt,UUID observedItem,long observedRevision,boolean uniqueResult,boolean inputsSettled)throws IOException {
        healthy();if(aborted.contains(receipt))throw new IllegalStateException("Aborted receipt cannot commit");Receipt recorded=Objects.requireNonNull(receipts.get(receipt),"Unknown receipt");
        if(!recorded.item.equals(observedItem)||recorded.revision!=observedRevision||!uniqueResult||!inputsSettled||quarantined.contains(observedItem))throw new IllegalStateException("Unproven item transaction");
        if(committed.contains(receipt))return;
        Plan p=Objects.requireNonNull(plans.get(receipt),"Missing prepared transaction");
        if(quarantined.contains(observedItem)||p.inputs.stream().anyMatch(e->quarantined.contains(e.item))||!p.output.item.equals(observedItem)||p.output.revision!=observedRevision||!uniqueResult||!inputsSettled)throw new IllegalStateException("Unproven item transaction");
        if(committed.contains(receipt))return;
        append((byte)2,uuidBytes(receipt));apply(p);committed.add(receipt);awards.remove(receipt);plans.remove(receipt);pendingContexts.remove(receipt);physicalProofs.remove(receipt);
    }
    /** Caller must prove unchanged original input stacks AND no transferred/stamped output. */
    public synchronized void abortUnchanged(UUID receipt,boolean inputsUnchanged,boolean outputAbsent)throws IOException {
        healthy();if(!inputsUnchanged||!outputAbsent)throw new IllegalStateException("Unsafe abort evidence");
        if(committed.contains(receipt))throw new IllegalStateException("Committed history cannot abort");
        if(aborted.contains(receipt))return;
        if(!plans.containsKey(receipt))throw new IllegalStateException("No prepared intent");
        append((byte)4,uuidBytes(receipt));aborted.add(receipt);plans.remove(receipt);pendingContexts.remove(receipt);physicalProofs.remove(receipt);
    }
    public synchronized Set<UUID> pendingFor(UUID item){
        if(failed)throw new IllegalStateException("Archive failed");Set<UUID> found=new HashSet<>();
        for(Plan p:plans.values())if(p.output.item.equals(item)||p.inputs.stream().anyMatch(e->e.item.equals(item)))found.add(p.receipt);
        return Set.copyOf(found);
    }
    public synchronized Optional<Plan> pendingPlan(UUID receipt){if(failed)throw new IllegalStateException("Archive failed");return Optional.ofNullable(plans.get(receipt));}
    /** Reconciles an item token; does not guess which copy is genuine or overwrite rollback history. */
    /** Once a clone is actually observed, removing it from the current scan cannot undo quarantine. */
    public synchronized void recordObservedDuplicate(UUID item,int copies)throws IOException {
        healthy();Objects.requireNonNull(item);if(copies<2)throw new IllegalArgumentException("Duplicate evidence required");
        if(!quarantined.contains(item)){append((byte)3,uuidBytes(item));quarantined.add(item);}
    }
    public synchronized State reconcile(UUID item,long itemRevision,UUID receipt,int observedCopies)throws IOException {
        healthy();
        if(observedCopies>1&&!quarantined.contains(item)){append((byte)3,uuidBytes(item));quarantined.add(item);}
        if(observedCopies!=1||quarantined.contains(item))throw new IllegalStateException("Duplicate or unavailable item identity: quarantine");
        if(receipt==null||aborted.contains(receipt))throw new IllegalStateException("Missing or aborted archive receipt");
        if(receipt!=null){Receipt token=receipts.get(receipt);if(token==null||!token.item.equals(item)||token.revision!=itemRevision)throw new IllegalStateException("Unknown or mismatched item receipt");}
        if(receipt!=null&&plans.containsKey(receipt)&&!committed.contains(receipt))throw new IllegalStateException("Pending transaction requires verified inputs and explicit commit");
        State s=states.get(item);if(s==null||consumed.contains(item)||s.revision!=itemRevision)throw new IllegalStateException("Archive/item rollback mismatch: quarantine");
        return s;
    }
    /** Coherent archive-only checkpoint. Item/world state must be quiescent and backed up with matching manifest separately. */
    public synchronized void checkpoint(Path target)throws IOException {
        healthy();if(!unresolved().isEmpty())throw new IllegalStateException("Pending transactions prevent checkpoint");channel.force(true);
        Path absolute=target.toAbsolutePath(), parent=absolute.getParent();
        Path temporary=parent.resolve("."+absolute.getFileName()+"."+UUID.randomUUID()+".pending");
        try {
            try(FileChannel out=FileChannel.open(temporary,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
                long n=channel.size(),pos=0;while(pos<n){long wrote=channel.transferTo(pos,n-pos,out);if(wrote<=0)throw new IOException("Checkpoint stalled");pos+=wrote;}out.force(true);
            }
            // Atomic no-overwrite publication of fully forced bytes. Unsupported filesystems fail, not downgrade.
            Files.createLink(absolute,temporary);forceDirectory(parent);
            Files.delete(temporary);forceDirectory(parent);
        }catch(IOException|RuntimeException e){throw e;} // Preserve temp/target evidence on uncertain failure.
    }
    private static void forceDirectory(Path parent)throws IOException{
        try(FileChannel directory=FileChannel.open(parent,StandardOpenOption.READ)){directory.force(true);}
    }

    private void checkInputs(List<Expected> inputs,UUID own){
        Set<UUID> seen=new HashSet<>();
        for(Expected e:inputs){State s=states.get(e.item);if(!seen.add(e.item)||s==null||quarantined.contains(e.item)||consumed.contains(e.item)||s.revision!=e.revision)throw new IllegalStateException("Stale, duplicate, or missing input");
            for(Plan pending:plans.values())if(!pending.receipt.equals(own)&&!committed.contains(pending.receipt)&&pending.inputs.stream().anyMatch(x->x.item.equals(e.item)))throw new IllegalStateException("Input has pending transaction");}
    }
    private void validatePlan(Plan p){
        Objects.requireNonNull(p.receipt);Objects.requireNonNull(p.output.item);
        if(quarantined.contains(p.output.item))throw new IllegalStateException("Quarantined output");
        if(p.output.revision<1||p.output.names.values().stream().anyMatch(n->n<1))throw new IllegalArgumentException("Invalid output");
        Map<Key,Long> baseline=new TreeMap<>();Set<UUID> ancestors=new HashSet<>();long previous=0;
        for(Expected e:p.inputs){State source=states.get(e.item);if(source==null)throw new IllegalStateException("Missing parent");
            source.names.forEach((k,v)->baseline.merge(k,v,Math::max));ancestors.addAll(source.lineage);if(!source.item.equals(p.output.item))ancestors.add(source.item);if(source.item.equals(p.output.item))previous=source.revision;}
        if(p.output.revision!=Math.addExact(previous,1)||!p.output.lineage.equals(ancestors))throw new IllegalStateException("Invalid revision or lineage");
        long increments=0;
        for(var e:baseline.entrySet())if(p.output.names.getOrDefault(e.getKey(),0L)<e.getValue())throw new IllegalStateException("History loss");
        for(var e:p.output.names.entrySet()){long delta=e.getValue()-baseline.getOrDefault(e.getKey(),0L);if(delta<0||delta>1)throw new IllegalStateException("Invalid history increment");increments+=delta;}
        if(increments>1||(p.inputs.isEmpty()&&!p.output.names.isEmpty()))throw new IllegalStateException("Unproven history addition");
        if(states.containsKey(p.output.item)&&p.inputs.stream().noneMatch(x->x.item.equals(p.output.item)))throw new IllegalStateException("Output collision");
        for(Plan pending:plans.values())if(!committed.contains(pending.receipt)&&pending.output.item.equals(p.output.item))throw new IllegalStateException("Pending output collision");
    }
    private void apply(Plan p){for(Expected e:p.inputs)if(!e.item.equals(p.output.item))consumed.add(e.item);states.put(p.output.item,p.output);}
    private void healthy()throws IOException{if(failed)throw new IOException("Archive failed; reopen and inspect before proceeding");}
    private void append(byte op,byte[] payload)throws IOException {
        if(payload.length>LIMIT)throw new IOException("Transaction too large; no history discarded");
        CRC32C crc=new CRC32C();crc.update(op);crc.update(payload);ByteBuffer frame=ByteBuffer.allocate(13+payload.length).putInt(MAGIC).put(op).putInt(payload.length).put(payload).putInt((int)crc.getValue());frame.flip();
        try{channel.position(channel.size());while(frame.hasRemaining())channel.write(frame);fault.at(op,Cut.AFTER_WRITE_BEFORE_FORCE);channel.force(true);fault.at(op,Cut.AFTER_FORCE_BEFORE_APPLY);}catch(IOException e){failed=true;throw e;}
    }
    private void replay()throws IOException{
        channel.position(0);while(channel.position()<channel.size()){
            ByteBuffer h=ByteBuffer.allocate(9);readFully(h);h.flip();if(h.getInt()!=MAGIC)throw new IOException("Bad archive header");byte op=h.get();int n=h.getInt();if(n<0||n>LIMIT)throw new IOException("Invalid frame length");ByteBuffer b=ByteBuffer.allocate(n+4);readFully(b);b.flip();byte[] body=new byte[n];b.get(body);CRC32C crc=new CRC32C();crc.update(op);crc.update(body);if(b.getInt()!=(int)crc.getValue())throw new IOException("Archive checksum mismatch");
            if(op==1){Plan p=decodeDelta(body);if(receipts.containsKey(p.receipt))throw new IOException("Duplicate prepare frame");validatePlan(p);checkInputs(p.inputs,p.receipt);plans.put(p.receipt,p);receipts.put(p.receipt,new Receipt(p.output.item,p.output.revision,digest(p)));}
            else if(op==2){if(n!=16)throw new IOException("Invalid commit");UUID id=readUuid(new DataInputStream(new ByteArrayInputStream(body)));Plan p=plans.get(id);if(p==null||!committed.add(id))throw new IOException("Invalid commit sequence");apply(p);awards.remove(id);plans.remove(id);pendingContexts.remove(id);physicalProofs.remove(id);}
            else if(op==7){DataInputStream d=new DataInputStream(new ByteArrayInputStream(body));Award a=new Award(readUuid(d),readUuid(d),readUuid(d),readUuid(d),new Key(str(d),str(d)));if(d.available()!=0||committed.contains(a.receipt)||awards.putIfAbsent(a.receipt,a)!=null)throw new IOException("Invalid award inbox entry");}
            else if(op==6){DataInputStream d=new DataInputStream(new ByteArrayInputStream(body));UUID id=readUuid(d);String proof=d.readUTF();if(!plans.containsKey(id)||physicalProofs.containsKey(id)||!proof.matches("[0-9a-f]{64}")||d.available()!=0)throw new IOException("Invalid physical receipt");physicalProofs.put(id,proof);}
            else if(op==5){DataInputStream d=new DataInputStream(new ByteArrayInputStream(body));UUID id=readUuid(d);int size=count(d);if(size>65536||size!=d.available()||receipts.containsKey(id)||contextHashes.containsKey(id))throw new IOException("Invalid recovery context");byte[] context=d.readNBytes(size);pendingContexts.put(id,context);contextHashes.put(id,hash(context));}
            else if(op==4){if(n!=16)throw new IOException("Invalid abort");UUID id=readUuid(new DataInputStream(new ByteArrayInputStream(body)));if(plans.remove(id)==null||committed.contains(id)||!aborted.add(id))throw new IOException("Invalid abort sequence");pendingContexts.remove(id);physicalProofs.remove(id);}
            else if(op==3){if(n!=16)throw new IOException("Invalid quarantine");quarantined.add(readUuid(new DataInputStream(new ByteArrayInputStream(body))));}
            else throw new IOException("Unknown archive operation");
        }
    }
    private void readFully(ByteBuffer b)throws IOException{while(b.hasRemaining())if(channel.read(b)<0)throw new EOFException("Incomplete journal; preserve and recover explicitly");}
    private static String digest(Plan p)throws IOException{
        return hash(encodePlan(p));
    }
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private byte[] encodeDelta(Plan p)throws IOException{
        Map<Key,Long> baseline=new TreeMap<>();for(Expected e:p.inputs)states.get(e.item).names.forEach((k,v)->baseline.merge(k,v,Math::max));
        Key earned=null;for(var e:p.output.names.entrySet())if(e.getValue()>baseline.getOrDefault(e.getKey(),0L))earned=e.getKey();
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);uuid(d,p.receipt);d.writeInt(p.inputs.size());
        for(Expected e:p.inputs){uuid(d,e.item);d.writeLong(e.revision);}uuid(d,p.output.item);d.writeLong(p.output.revision);d.writeBoolean(earned!=null);
        if(earned!=null){str(d,earned.kind);str(d,earned.name);}return b.toByteArray();
    }
    private Plan decodeDelta(byte[] bytes)throws IOException{
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(bytes));UUID receipt=readUuid(d);int n=count(d);List<Expected> inputs=new ArrayList<>();
        for(int i=0;i<n;i++)inputs.add(new Expected(readUuid(d),d.readLong()));UUID output=readUuid(d);long revision=d.readLong();
        Key earned=d.readBoolean()?new Key(str(d),str(d)):null;if(d.available()!=0)throw new IOException("Trailing delta data");
        Plan p=preview(receipt,inputs,output,earned);if(p.output.revision!=revision)throw new IOException("Invalid delta revision");return p;
    }
    private static byte[] encodePlan(Plan p)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);uuid(d,p.receipt);d.writeInt(p.inputs.size());for(Expected e:p.inputs){uuid(d,e.item);d.writeLong(e.revision);}uuid(d,p.output.item);d.writeLong(p.output.revision);d.writeInt(p.output.names.size());for(var e:p.output.names.entrySet()){str(d,e.getKey().kind);str(d,e.getKey().name);d.writeLong(e.getValue());}List<UUID> lineage=new ArrayList<>(p.output.lineage);lineage.sort(Comparator.naturalOrder());d.writeInt(lineage.size());for(UUID id:lineage)uuid(d,id);return b.toByteArray();}
    private static int count(DataInputStream d)throws IOException{int n=d.readInt();if(n<0||n>LIMIT||n>d.available())throw new IOException("Invalid count");return n;}
    private static void str(DataOutputStream d,String s)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);d.writeInt(b.length);d.write(b);}
    private static String str(DataInputStream d)throws IOException{int n=count(d);if(n>d.available())throw new EOFException();return new String(d.readNBytes(n),StandardCharsets.UTF_8);}
    private static void uuid(DataOutputStream d,UUID id)throws IOException{d.writeLong(id.getMostSignificantBits());d.writeLong(id.getLeastSignificantBits());}
    private static UUID readUuid(DataInputStream d)throws IOException{return new UUID(d.readLong(),d.readLong());}
    private static byte[] uuidBytes(UUID id){return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();}
    public synchronized void close()throws IOException{lock.release();channel.close();}
}
