/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Same-directory atomic replacement. No non-atomic fallback or success on uncertain durability. */
public final class AtomicStateFile {
    private AtomicStateFile() {}
    @FunctionalInterface public interface Probe { void at(String phase) throws IOException; }
    public static void write(Path target, String text) throws IOException { write(target,text,p -> {}); }
    public static void write(Path target, String text, Probe probe) throws IOException {
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
            try(FileChannel directory=FileChannel.open(parent,StandardOpenOption.READ)){directory.force(true);}
            probe.at("after-force");
        } finally { Files.deleteIfExists(temp); }
    }
}
