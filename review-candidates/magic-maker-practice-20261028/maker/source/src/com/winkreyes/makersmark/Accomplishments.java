package com.winkreyes.makersmark;
import java.util.*;
import org.bukkit.entity.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.winkreyes.makersmark.history.HistoryLedger;
/** Records observed custom names, never claims a canonical external-plugin boss ID. */
final class Accomplishments {
 static final Set<String> BOSSES=Set.of("WITHER","ENDER_DRAGON","WARDEN","ELDER_GUARDIAN");
 static HistoryLedger.Key observed(LivingEntity entity){
  if(entity instanceof Player||entity instanceof ArmorStand)return null;
  String name=entity.customName()==null?null:PlainTextComponentSerializer.plainText().serialize(entity.customName());
  return observed(entity.getType().name(),name,entity.getPersistentDataContainer().getKeys().stream().map(k->k.getNamespace()).filter(n->!Set.of("minecraft","excelrewardguard","excelbosstrial").contains(n)).distinct().sorted().toList());
 }
 static HistoryLedger.Key observed(String type,String name,List<String> provenance){
  boolean boss=BOSSES.contains(type);String clean=name==null?"":name.replaceAll("(?i)§[0-9a-fk-orx]","").strip();
  // Status-only numeric/health bars are not names. Keep genuine observed names intact, including player name tags.
  boolean meaningful=clean.codePoints().anyMatch(Character::isLetter)&&!clean.matches("(?iu)^(?:hp|health|level|lvl|lv|boss)?[\\s\\d.,/%❤♥♡|▌█▉▊▋▍▎▏+\\-:()\\[\\]]*(?:hp|health)?$");
  if(!meaningful){if(!boss)return null;clean=com.winkreyes.makersmark.core.Trophies.pretty(type);}
  String kind=(boss?"BOSS:":"NAMED:")+type;
  if(!provenance.isEmpty())kind+=";observed-source="+String.join(",",provenance);
  return new HistoryLedger.Key(kind,clean);
 }
}
