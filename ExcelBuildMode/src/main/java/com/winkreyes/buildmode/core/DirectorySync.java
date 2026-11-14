package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.READ;

/**
 * Best-effort directory durability sync (1.3.5). The durable stores fsync
 * every file they write (mandatory, unchanged) and then fsync the containing
 * directory so the directory entry itself is durable. Some platforms refuse
 * directory channels entirely — Java NIO on Windows throws
 * AccessDeniedException when opening a directory — which previously failed
 * store saves on the Windows test rig even though the file-level sync had
 * already succeeded. Directory sync is therefore best-effort: it runs
 * exactly as before wherever the platform supports it (including the live
 * Linux server) and is skipped, never fatal, where it does not.
 */
final class DirectorySync {
    private DirectorySync() { }
    static void bestEffort(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, READ)) { channel.force(true); }
        catch (IOException refused) { /* platform refuses directory channels; the file-level sync already landed */ }
    }
}
