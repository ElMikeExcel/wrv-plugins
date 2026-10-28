package com.winkreyes.excelguildgate;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;
/** Atomic replacement with forced bytes and directory acknowledgement. No non-atomic fallback. */
public final class RecoveryAtomicFile {
    public interface Fault {void at(String phase) throws IOException;}
    public static void write(Path target,byte[] bytes) throws IOException {write(target,bytes,p->{});}
    public static void write(Path target,byte[] bytes,Fault fault) throws IOException {
        Path absolute=target.toAbsolutePath(),parent=absolute.getParent();
        if(Files.isSymbolicLink(absolute))throw new IOException("refuse symlink state target");
        Files.createDirectories(parent);Path tmp=Files.createTempFile(parent,".state-recovery-",".tmp");
        try{
            fault.at("before-write");
            try(FileChannel ch=FileChannel.open(tmp,WRITE,TRUNCATE_EXISTING)){ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())ch.write(b);ch.force(true);}
            fault.at("before-move");
            Files.move(tmp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            fault.at("after-move");
            try(FileChannel dir=FileChannel.open(parent,READ)){dir.force(true);}
            fault.at("after-force");
        } finally {Files.deleteIfExists(tmp);}
    }
}
