package com.winkreyes.excelexpansions.core;
import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
/** The releaser's flip engine for ExcelExpansions 0.1.1 - DESIGN section 5 step 1 (Enable)
 *  and the mirror retirement flip (section 4.2). A flip sets the anchored isEnabled flag in
 *  each of a row's content-package files - the exact mechanism the expansion rig proved by
 *  hand on the mirror fixture (catacombs_lair.yml false -> true, next boot clean).
 *  Facts of the format (G4 pull, all 94 content-package files): the flag is a line of its own
 *  anchored at column 0, "isEnabled: true|false", the first line of the file. Comment lines
 *  such as "# isEnabled: false" deeper in FMM files are NOT the flag and are never touched.
 *  Safety shape: every changed file first gets a sibling backup (<file>.bak-excelexpansions);
 *  writes go through a temp file + move; a file already at the target value is ALREADY_SET
 *  (idempotent - a re-tap after a partial flip converges instead of erroring); a missing file
 *  or a file with no anchored flag FAILS the flip as a whole, and the caller leaves the row
 *  proposed so the failure is reported, never half-released (DESIGN 5 preconditions). */
public final class Releaser {
 public enum Status{ FLIPPED, ALREADY_SET, MISSING, NO_FLAG, ERROR }
 public record FileResult(String relPath, Status status, String detail){}
 public record Result(boolean success, List<FileResult> files){}
 public static final String BACKUP_SUFFIX=".bak-excelexpansions";
 /** Flip the anchored isEnabled value inside one file's text. Returns the new text, the
  *  SAME text when already at the target value, or null when no anchored flag line exists. */
 public static String flipText(String text, boolean enable){
  String[] lines=text.split("\n",-1);
  for(int i=0;i<lines.length;i++){ String core=lines[i].endsWith("\r")? lines[i].substring(0,lines[i].length()-1) : lines[i];
   if(core.startsWith("isEnabled:")){ String v=core.substring("isEnabled:".length()).trim();
    if(!v.equals("true")&&!v.equals("false")) return null;
    if(v.equals(Boolean.toString(enable))) return text;
    lines[i]="isEnabled: "+enable+(lines[i].endsWith("\r")? "\r" : ""); return String.join("\n",lines); } }
  return null; }
 public Result flip(Path pluginsDir, List<String> relPaths, boolean enable){
  List<FileResult> out=new ArrayList<>();
  for(String rel: relPaths){ Path f=pluginsDir.resolve(rel);
   if(!Files.isRegularFile(f)){ out.add(new FileResult(rel,Status.MISSING,"file not found under plugins/")); continue; }
   try{ String orig=Files.readString(f,StandardCharsets.UTF_8); String flipped=flipText(orig,enable);
    if(flipped==null){ out.add(new FileResult(rel,Status.NO_FLAG,"no anchored isEnabled line")); continue; }
    if(flipped.equals(orig)){ out.add(new FileResult(rel,Status.ALREADY_SET,"already isEnabled: "+enable)); continue; }
    Files.copy(f, f.resolveSibling(f.getFileName()+BACKUP_SUFFIX), StandardCopyOption.REPLACE_EXISTING);
    Path tmp=f.resolveSibling(f.getFileName()+".tmp-excelexpansions"); Files.writeString(tmp,flipped,StandardCharsets.UTF_8);
    try{ Files.move(tmp,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
    catch(AtomicMoveNotSupportedException x){ Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING); }
    out.add(new FileResult(rel,Status.FLIPPED,"isEnabled -> "+enable)); }
   catch(Exception e){ out.add(new FileResult(rel,Status.ERROR,e.toString())); } }
  boolean ok=!relPaths.isEmpty() && out.stream().allMatch(fr->fr.status()==Status.FLIPPED||fr.status()==Status.ALREADY_SET);
  return new Result(ok,List.copyOf(out)); }
}
