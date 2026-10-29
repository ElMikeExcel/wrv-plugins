// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Optional global no-op-session proof. Never reads journal paths: callers supply proofs from
 * their already locked channels after complete validation. Not an economic replay mechanism.
 * Caller must fence ALL side effects and coordinate BOTH journals before enabling recovery.
 */
public final class GlobalSessionGuard implements AutoCloseable {
    public record Fingerprint(long length, String sha256, boolean validTerminal, String markerSha256) {
        /** Unknown marker identity is never sufficient for no-op recovery. */
        public Fingerprint(long length, String sha256, boolean validTerminal) { this(length,sha256,validTerminal,""); }
        public Fingerprint {
            if (length < 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Invalid journal fingerprint");
            if (markerSha256 == null || (!markerSha256.isEmpty() && !markerSha256.matches("[0-9a-f]{64}")))
                throw new IllegalArgumentException("Invalid marker fingerprint");
        }
    }
    public enum Decision { CLEAN_START, NO_OP_RECOVERY_ELIGIBLE, BLOCKED }
    private static final Set<String> KEYS = Set.of("checkout", "legacy-checkout");
    private static final String HEADER = "ExcelShops global session 2";
    private final Path root, envelope, fence;
    private final DataFileLock lock;
    private final boolean enabled;
    private String session;
    private Map<String,Fingerprint> assessed;
    private String assessedEnvelopeBody;
    private Decision decision;
    private boolean ready, fenced, uncertain, closed;

    public static GlobalSessionGuard open(Path root, boolean enabled) throws IOException {
        return new GlobalSessionGuard(root, enabled);
    }
    private GlobalSessionGuard(Path path, boolean enabled) throws IOException {
        this.enabled=enabled; this.root=path.toAbsolutePath().normalize();
        envelope=root.resolve("global-session-v2"); fence=root.resolve("global-session-v2.fence");
        if (!enabled) { lock=null; return; }
        Files.createDirectories(root);
        if (root.getFileName()==null || root.getParent()==null) throw new IOException("Plugin root must have a parent");
        // Migration snapshots every file inside the plugin directory. Keep the lifetime lock
        // outside that tree: opening/closing another descriptor would violate POSIX lock safety.
        Path lockPath=root.resolveSibling("."+root.getFileName()+"-global-session-v2.lock");
        lock=DataFileLock.open(lockPath, true);
    }
    /** Legacy markers may be bypassed ONLY by a matching prior v2 READY proof, never by age. */
    public synchronized Decision assess(Map<String,Fingerprint> proofs, boolean legacyMarkersPresent) throws IOException {
        requireOpen();
        if (!enabled) return Decision.BLOCKED;
        requireProofs(proofs); assessed=Map.copyOf(proofs); assessedEnvelopeBody=null; decision=Decision.BLOCKED;
        if (ready || uncertain || present(fence)) return decision;
        if (!present(envelope)) {
            if (!legacyMarkersPresent) decision=Decision.CLEAN_START;
            return decision;
        }
        try {
            String priorBody=readChecked(envelope);
            String[] lines=priorBody.split("\n", -1);
            if (lines.length!=6 || !HEADER.equals(lines[0]) || !"READY".equals(lines[2]) || !rootBinding().equals(lines[3])) return decision;
            UUID.fromString(lines[1]);
            if (proofs.values().stream().allMatch(f -> !f.markerSha256().isEmpty())
                    && line("checkout", proofs.get("checkout")).equals(lines[4])
                    && line("legacy-checkout", proofs.get("legacy-checkout")).equals(lines[5]))
                { decision=Decision.NO_OP_RECOVERY_ELIGIBLE; assessedEnvelopeBody=priorBody; }
        } catch (IllegalArgumentException | IOException failure) { decision=Decision.BLOCKED; }
        return decision;
    }
    public synchronized void recordReady(Map<String,Fingerprint> proofs) throws IOException { recordReady(proofs,false); }
    /** Recovery is opt-in and requires an immediately preceding assessment of these exact proofs. */
    public synchronized void recordReady(Map<String,Fingerprint> proofs, boolean allowNoOpRecovery) throws IOException {
        requireOpen(); if (!enabled) return; requireProofs(proofs);
        if (ready || uncertain || assessed==null || !assessed.equals(proofs)
                || decision==Decision.BLOCKED || decision==null
                || (decision==Decision.NO_OP_RECOVERY_ELIGIBLE && !allowNoOpRecovery)
                || present(fence)) throw new IOException("Session proof not eligible");
        session=UUID.randomUUID().toString();
        String body=HEADER+"\n"+session+"\nREADY\n"+rootBinding()+"\n"+line("checkout",proofs.get("checkout"))+"\n"+line("legacy-checkout",proofs.get("legacy-checkout"));
        try {
            if (decision==Decision.NO_OP_RECOVERY_ELIGIBLE) {
                String oldBody=readChecked(envelope);
                if (!oldBody.equals(assessedEnvelopeBody)) throw new IOException("Session evidence changed after assessment");
                archiveRecoveryEvidence(oldBody);
            } else if (present(envelope)) throw new IOException("Unexpected session envelope appeared");
            writeAtomic(envelope, body); ready=true;
        }
        catch (IOException failure) { uncertain=true; throw failure; }
    }
    /** Must return successfully BEFORE the first possible external or persistent side effect. */
    public synchronized void beforeMutation(String reason) throws IOException {
        requireOpen(); if (!enabled) return;
        if (!ready) { markUncertain("Mutation attempted before READY: "+reason); throw new IOException("Global session is not ready"); }
        if (uncertain) throw new IOException("Global session is uncertain");
        if (fenced) return;
        try {
            if (present(fence)) throw new IOException("Unexpected existing mutation fence");
            writeNew(fence,HEADER+"\n"+session+"\nMUTATION\n"+safeReason(reason));
            fenced=true;
        } catch (IOException failure) { uncertain=true; throw failure; }
    }
    /** Also latches in RAM if storage fails; subsequent operations must remain disabled. */
    public synchronized void markUncertain(String reason) throws IOException {
        requireOpen(); if (!enabled) return;
        uncertain=true;
        if (!present(fence))
            writeNew(fence,HEADER+"\n"+(session==null?"UNSTARTED":session)+"\nUNCERTAIN\n"+safeReason(reason==null||reason.isBlank()?"unspecified uncertainty":reason.substring(0,Math.min(reason.length(),2048))));
        fenced=true;
    }
    /** Caller has already durably saved player/world/economy state and closed both journals cleanly. */
    public synchronized void closeClean(boolean durabilityVerified) throws IOException {
        requireOpen(); if (!enabled) { close(); return; }
        if (!durabilityVerified || !ready || uncertain) throw new IOException("Clean boundary not established");
        try {
            // Envelope first: an interrupted cleanup can only leave a blocking orphan fence.
            Files.deleteIfExists(envelope); syncDirectory();
            Files.deleteIfExists(fence); syncDirectory();
        } finally { close(); }
    }
    public synchronized void closeUnclean() throws IOException { close(); }
    @Override public synchronized void close() throws IOException {
        if (!closed) { closed=true; if (lock!=null) lock.close(); }
    }
    private static boolean present(Path p) throws IOException {
        try { Files.readAttributes(p, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); return true; }
        catch (NoSuchFileException absent) { return false; }
    }
    private void requireOpen() throws IOException { if(closed) throw new IOException("Session guard closed"); }
    private static void requireProofs(Map<String,Fingerprint> p) throws IOException {
        if(p==null || !p.keySet().equals(KEYS) || p.values().stream().anyMatch(f->f==null||!f.validTerminal()))
            throw new IOException("Both fully validated terminal journal proofs required");
    }
    private String rootBinding() { return "root\t"+Base64.getUrlEncoder().withoutPadding().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8)); }
    private static String line(String key,Fingerprint f) { return key+"\t"+f.length()+"\t"+f.sha256()+"\t"+f.markerSha256(); }
    private static String safeReason(String s) throws IOException {
        if(s==null||s.isBlank()||s.length()>2048) throw new IOException("Invalid mutation reason");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    private static String digest(byte[] b) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private String readChecked(Path p) throws IOException {
        var a=DataFileLock.checkedAttributes(p); if(!a.isRegularFile()||a.size()>16384)throw new IOException("Invalid envelope");
        byte[] bytes=Files.readAllBytes(p); String s=new String(bytes,StandardCharsets.UTF_8);
        int cut=s.lastIndexOf('\n',s.length()-2);
        if(!s.endsWith("\n")||cut<0)throw new IOException("Truncated envelope");
        String body=s.substring(0,cut), hash=s.substring(cut+1,s.length()-1);
        if(!digest(body.getBytes(StandardCharsets.UTF_8)).equals(hash))throw new IOException("Envelope checksum mismatch");
        return body;
    }
    /** Preserve the exact validated previous proof before any replacement. Never overwrites an audit. */
    private void archiveRecoveryEvidence(String oldBody) throws IOException {
        Path audit=root.resolve("global-session-v2-audit");
        if (!present(audit)) { Files.createDirectory(audit); syncDirectory(); }
        if (!DataFileLock.checkedAttributes(audit).isDirectory()) throw new IOException("Recovery audit path is not a directory");
        Path receipt=audit.resolve("no-op-"+UUID.randomUUID()+".proof");
        writeNew(receipt,oldBody);
        // writeNew forces the file and plugin root; this parent also needs its own durable entry.
        try(FileChannel dir=FileChannel.open(audit,StandardOpenOption.READ)){dir.force(true);}
    }
    private void writeAtomic(Path target,String body) throws IOException {
        Path tmp=root.resolve("global-session-v2.tmp-"+UUID.randomUUID());
        writeNew(tmp,body);
        try { Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); syncDirectory(); }
        finally { Files.deleteIfExists(tmp); }
    }
    private void writeNew(Path target,String body) throws IOException {
        byte[] bytes=(body+"\n"+digest(body.getBytes(StandardCharsets.UTF_8))+"\n").getBytes(StandardCharsets.UTF_8);
        try(FileChannel out=FileChannel.open(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer b=ByteBuffer.wrap(bytes); while(b.hasRemaining())out.write(b); out.force(true);
        }
        syncDirectory();
    }
    private void syncDirectory() throws IOException {
        try(FileChannel dir=FileChannel.open(root,StandardOpenOption.READ)){dir.force(true);}
    }
}
