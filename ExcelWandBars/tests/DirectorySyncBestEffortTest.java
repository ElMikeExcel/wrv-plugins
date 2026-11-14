package com.winkreyes.excelwandbars;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import wrv.magic.retention.AuthenticatedWindow;
import wrv.magic.retention.BoundedPractice;

/** 0.3.18: directory fsync is best-effort. Windows refuses directory
 *  channels (AccessDeniedException on FileChannel.open, proven by the
 *  Nov 14, 2026 mirror diagnosis), so a save must still succeed on the
 *  file-level fsync alone. This suite simulates exactly that refusal:
 *  every FileChannel.open on a directory throws, while file opens run
 *  for real. It covers EVERY directory-force site in the tree:
 *  AuthenticatedWindow.commit, BoundedPractice.persist and
 *  PracticeIdentityJournal.persist. On the 0.3.17 classes each refused
 *  scenario fails at the first directory sync; on 0.3.18 all pass.
 *  Plain-main suite in the house style; Mockito is used only to stub
 *  the static FileChannel.open, as in the ExcelShops 1.3.9 suite. */
public class DirectorySyncBestEffortTest {
 static int n;
 static void ok(boolean b) { if (!b) throw new AssertionError("case " + n); n++; }

 private static MockedStatic<FileChannel> refuseDirectoryChannels(boolean unsupported) {
  MockedStatic<FileChannel> channels = Mockito.mockStatic(FileChannel.class, CALLS_REAL_METHODS);
  channels.when(() -> FileChannel.open(any(Path.class), any(OpenOption[].class)))
      .thenAnswer(invocation -> {
       Path path = invocation.getArgument(0);
       if (path != null && Files.isDirectory(path)) {
        if (unsupported) throw new UnsupportedOperationException("simulated directory refusal");
        throw new AccessDeniedException(path.toString());
       }
       return invocation.callRealMethod();
      });
  return channels;
 }

 private static final String HASH = "a".repeat(64);

 private static void authenticatedWindow(Path dir) throws Exception {
  Path file = dir.resolve("practice-issuer");
  try (AuthenticatedWindow w = new AuthenticatedWindow(file, Set.of("practice"), 16, 4)) {
   AuthenticatedWindow.Token t = w.issue(UUID.randomUUID(), "mend", HASH);
   ok(t != null && Files.isRegularFile(file));
  }
 }

 private static void boundedPractice(Path dir) throws Exception {
  Path file = dir.resolve("usage-ledger");
  BoundedPractice.Config cfg = new BoundedPractice.Config(
      List.of(5L), EnumSet.of(BoundedPractice.EffectiveKind.DIRECT_HEAL), 0, 0, 10);
  try (BoundedPractice bp = new BoundedPractice(file, "policy1", "mend", cfg)) {
   ok(bp.importFloor("playerone", 1, "receiptone") && Files.isRegularFile(file));
  }
 }

 private static void identityJournal(Path dir) throws Exception {
  Path file = dir.resolve("practice-originals");
  String id = UUID.randomUUID().toString();
  try (PracticeIdentityJournal j = new PracticeIdentityJournal(file, 16)) {
   PracticeIdentityJournal.Entry e = j.prepare(id, UUID.randomUUID().toString(),
       "wand_template", "raw-data", "test", Map.of("mend", 1));
   ok(e != null && j.get(id) != null && Files.isRegularFile(file));
  }
 }

 private static Path fresh(Path root, String name) throws Exception {
  Path dir = root.resolve(name);
  Files.createDirectories(dir);
  return dir;
 }

 public static void main(String[] args) throws Exception {
  Path root = Files.createTempDirectory("wandbars-dirfsync-");

  // Refused with AccessDeniedException (the proven Windows refusal).
  try (MockedStatic<FileChannel> ignored = refuseDirectoryChannels(false)) {
   authenticatedWindow(fresh(root, "ad-aw"));
   boundedPractice(fresh(root, "ad-bp"));
   identityJournal(fresh(root, "ad-pij"));
  }
  // Refused with UnsupportedOperationException (the other platform refusal).
  try (MockedStatic<FileChannel> ignored = refuseDirectoryChannels(true)) {
   authenticatedWindow(fresh(root, "uo-aw"));
   boundedPractice(fresh(root, "uo-bp"));
   identityJournal(fresh(root, "uo-pij"));
  }
  // No refusal: normal Linux behaviour is unchanged.
  authenticatedWindow(fresh(root, "ok-aw"));
  boundedPractice(fresh(root, "ok-bp"));
  identityJournal(fresh(root, "ok-pij"));

  // Each helper contributes: AW 1, BP 1, PIJ 1 per scenario = 9 checks.
  // One extra structural check: the refused AW file reopens cleanly.
  try (AuthenticatedWindow w = new AuthenticatedWindow(
       root.resolve("ad-aw").resolve("practice-issuer"), Set.of("practice"), 16, 4)) {
   ok(w.retained() != null);
  }

  System.out.println("PASS " + n + " directory-sync best-effort checks");
 }
}
