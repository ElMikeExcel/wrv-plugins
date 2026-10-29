/* Excel WRV maintained source; original attribution and license preserved. */ package wrv.makersmark.magic;
import java.util.*;
/** One immutable native getLore generation. NEVER construct from stored ItemMeta or a previous decorated result. */
public final class FreshMagicLore {
 private final List<String> base;
 private FreshMagicLore(List<String> freshlyGenerated){base=List.copyOf(freshlyGenerated);}
 /** Integration seam only: exact Wand.updateLore immediately after its native getLore call. */
 public static FreshMagicLore fromNativeGetLore(List<String> freshlyGenerated){return new FreshMagicLore(freshlyGenerated);}
 public record Result(List<String> lines,boolean changed,String status){}
 public Result unchanged(String status){return new Result(new ArrayList<>(base),false,status);}
 /** Repeated rendering starts from the same immutable native base, never from decorated/stored lore. */
 public Result render(List<String> history){
  if(history==null||history.size()>4)return unchanged("UNAVAILABLE_OR_UNBOUNDED_SUMMARY");
  List<String> out=new ArrayList<>(base);for(String line:history){if(line==null)return unchanged("INVALID_SUMMARY");String clean=clean(line);if(clean.codePointCount(0,clean.length())>160)return unchanged("UNBOUNDED_LINE");out.add("§7"+clean+"§r");}
  return new Result(out,!out.equals(base),"FRESH_GENERATION_ONLY");
 }
 private static String clean(String text){String plain=text.replaceAll("(?i)§[0-9a-fk-orx]","");StringBuilder b=new StringBuilder();plain.codePoints().filter(c->Character.getType(c)!=Character.CONTROL&&Character.getType(c)!=Character.FORMAT&&Character.getType(c)!=Character.LINE_SEPARATOR&&Character.getType(c)!=Character.PARAGRAPH_SEPARATOR&&c!='§').forEach(b::appendCodePoint);return b.toString();}
}
