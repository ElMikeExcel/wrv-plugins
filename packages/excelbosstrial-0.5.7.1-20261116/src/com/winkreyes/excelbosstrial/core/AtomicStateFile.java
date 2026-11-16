/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Same-directory atomic replacement. No non-atomic fallback: the temp-file
 *  fsync is mandatory and any failure before or during the move fails the
 *  write. The post-move directory force is best-effort ONLY where the
 *  platform refuses directory channels outright (Windows NIO throws
 *  AccessDeniedException on FileChannel.open of a directory): there the
 *  already-forced file carries the write. Any other directory-force failure
 *  still fails the write, exactly as before. */
public final class AtomicStateFile {
    private AtomicStateFile() {}
    @FunctionalInterface public interface Probe { void at(String phase) throws IOException; }
    @FunctionalInterface interface DirectoryForce { void force(Path directory) throws IOException; }
    public static void write(Path target, String text) throws IOException { write(target,text,p -> {}); }
    public static void write(Path target, String text, Probe probe) throws IOException {
        write(target,text,probe,AtomicStateFile::forceDirectory);
    }
    static void write(Path target, String text, Probe probe, DirectoryForce directoryForce) throws IOException {
        Path parent=target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp=Files.createTempFile(parent,".recovery-state-",".tmp");
        try {
            probe.at("before-write");
            try(FileChannel out=FileChannel.open(temp,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer data=StandardCharsets.UTF_8.encode(text);
                while(data.hasRemaining())out.write(data);
                out.force(true);
            }
            probe.at("before-move");
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            probe.at("after-move");
            try { directoryForce.force(parent); }
            catch(AccessDeniedException platformRefusesDirectoryChannels) { /* file fsync already carried this write */ }
            probe.at("after-force");
        } finally { Files.deleteIfExists(temp); }
    }
    private static void forceDirectory(Path directory) throws IOException {
        try(FileChannel channel=FileChannel.open(directory,StandardOpenOption.READ)){channel.force(true);}
    }
}
