/* Excel WRV synthetic regression fixture; original attribution retained. */ package com.winkreyes.makersmark;
// Excel: actual Prefs save() regression with real denied rename, synthetic choices.
import java.nio.file.*;import java.nio.file.attribute.*;import java.util.*;import java.lang.reflect.*;
public final class PrefsFailureTest {
 static int checks;
 static void ok(boolean b,String n){if(!b)throw new AssertionError(n);checks++;}
 static boolean dirty(Prefs p)throws Exception{Field f=Prefs.class.getDeclaredField("dirty");f.setAccessible(true);return f.getBoolean(p);}
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory(Path.of(args[0]),"excel-prefs-minimal-");Path file=dir.resolve("players.yml"),temp=dir.resolve("players.yml.tmp");
  UUID u=UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
  String original="players:\n  "+u+":\n    mark: false\n    stats: false\n    tip-ok: true\n    tip-shows: 7\n";
  Files.writeString(file,original);Files.writeString(temp,"precreated");
  Prefs p=new Prefs(new ExcelMakersMark(dir.toFile()));
  ok(!p.get(u).mark&&!p.get(u).stats&&p.get(u).tipOk&&p.get(u).tipShows==7,"loaded choices and acknowledgments");
  p.get(u).mark=true;p.changed();ok(dirty(p),"dirty set");
  Files.setPosixFilePermissions(dir,PosixFilePermissions.fromString("r-x------"));
  try {
   ok(!temp.toFile().renameTo(file.toFile()),"actual rename denied by parent mode");
   p.save();
   ok(Files.readString(file).equals(original),"authoritative bytes unchanged on denied rename");
   ok(dirty(p),"failed save retains dirty");
   ok(Files.readString(temp).contains("mark: true"),"complete candidate retained for later retry");
  }finally{Files.setPosixFilePermissions(dir,PosixFilePermissions.fromString("rwx------"));}
  p.save();ok(!dirty(p),"successful retry clears dirty");ok(!Files.exists(temp),"successful rename consumes temp");
  Prefs reload=new Prefs(new ExcelMakersMark(dir.toFile()));
  ok(reload.get(u).mark&&!reload.get(u).stats&&reload.get(u).tipOk&&reload.get(u).tipShows==7,"reload preserves all choices");
  byte[] saved=Files.readAllBytes(file);p.save();ok(Arrays.equals(saved,Files.readAllBytes(file)),"clean save no-op");
  System.out.println("PASS "+checks+" actual Prefs checks on "+args[0]);
 }
}
