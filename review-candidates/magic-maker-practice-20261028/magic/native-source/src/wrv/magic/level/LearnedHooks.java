package wrv.magic.level;
import com.elmakers.mine.bukkit.api.spell.*;import com.elmakers.mine.bukkit.api.wand.Wand;import com.elmakers.mine.bukkit.api.magic.Mage;import org.bukkit.entity.Player;
/** Inactive by default. Read-only learned spell selection; never reads held/active wand or writes rank. */
public final class LearnedHooks{
 private static volatile LearnedLevelResolver resolver;
 private static Object generation;
 private static volatile java.util.Set<String> retiredKeys=java.util.Set.of();
 private static boolean retired(String nativeKey){if(nativeKey==null)return false;try{return retiredKeys.contains(new SpellKey(nativeKey).getBaseKey());}catch(RuntimeException invalid){return !retiredKeys.isEmpty();}}
 public static synchronized void requireManaged(java.util.Set<String> keys){if(resolver!=null)throw new IllegalStateException("active learned owner");var copy=java.util.Set.copyOf(keys);for(String key:copy)if(!key.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("canonical managed keys");var union=new java.util.HashSet<>(retiredKeys);union.addAll(copy);retiredKeys=java.util.Set.copyOf(union);}
 private static void retire(){if(resolver!=null){var keys=new java.util.HashSet<>(retiredKeys);keys.addAll(resolver.managedKeys());retiredKeys=java.util.Set.copyOf(keys);}resolver=null;generation=null;}
 public static synchronized void install(LearnedLevelResolver value){if(value==null)throw new IllegalArgumentException();if(resolver!=null)throw new IllegalStateException("already installed");resolver=value;generation=new Object();}
 public static synchronized AutoCloseable installOwned(LearnedLevelResolver value){
  install(value);Object mine=generation;return ()->{synchronized(LearnedHooks.class){if(generation==mine){retire();}}};
 }
 public static synchronized void detach(){retire();}
 public static MageSpell resolve(Mage mage,String nativeKey,Wand original){
  var r=resolver;if(r==null)return retired(nativeKey)?null:mage.getSpell(nativeKey);
  SpellKey key;try{key=new SpellKey(nativeKey);}catch(Throwable invalid){return null;}if(!r.manages(key.getBaseKey()))return retired(nativeKey)?null:mage.getSpell(nativeKey);
  try{
   var decision=decision(r,mage,original,key.getBaseKey(),key.getLevel());if(decision==null||decision.status()!=LearnedLevelResolver.Status.RESOLVED)return null;
   MageSpell spell=mage.getSpell(new SpellKey(key.getBaseKey(),decision.effectiveLearnedLevel()).getKey());
   if(spell==null||!key.getBaseKey().equals(spell.getSpellKey().getBaseKey())||spell.getSpellKey().getLevel()!=decision.effectiveLearnedLevel())return null;return spell;
  }catch(Throwable unavailable){return null;}
 }
 private static LearnedLevelResolver.Decision decision(LearnedLevelResolver r,Mage mage,Wand wand,String base,int stored){
  if(wand==null||mage==null||!(mage.getEntity() instanceof Player player)||!wand.hasSpell(base)||!wand.canUse(player))return null;
  var request=new LearnedLevelResolver.Request(player.getUniqueId(),wand.getId(),base,stored);
  return OriginalWandContext.withOriginal(request,mage,wand,()->r.resolve(request));
 }
 public static boolean allowCast(MageSpell spell,Wand original){
  var r=resolver;if(r==null){try{return !retired(spell.getSpellKey().getKey());}catch(RuntimeException unavailable){return retiredKeys.isEmpty();}}
  try{var key=spell.getSpellKey();if(!r.manages(key.getBaseKey()))return !retired(key.getKey());if(original==null)return false;
   var d=decision(r,spell.getMage(),original,key.getBaseKey(),original.getSpellLevel(key.getBaseKey()));return d!=null&&d.status()==LearnedLevelResolver.Status.RESOLVED&&key.getLevel()>=1&&key.getLevel()==d.effectiveLearnedLevel();
  }catch(Throwable unavailable){return false;}
 }
 /** Read-only current-generation query for canonical native spell keys; inactive means unchanged. */
 public static boolean managesKey(String nativeKey){
  var r=resolver;if(nativeKey==null)return false;if(r==null)return retired(nativeKey);
  try{if(!nativeKey.matches("[a-z0-9_.-]+(?:\\|[1-9][0-9]*)?"))return false;int delimiter=nativeKey.indexOf('|');if(delimiter>=0)Integer.parseInt(nativeKey.substring(delimiter+1));var key=new SpellKey(nativeKey);return nativeKey.equals(key.getKey())&&key.getBaseKey().matches("[a-z0-9_.-]+")&&key.getLevel()>=1&&(r.manages(key.getBaseKey())||retiredKeys.contains(key.getBaseKey()));}
  catch(RuntimeException invalid){return false;}
 }
 public static boolean managed(Spell spell){var r=resolver;if(r==null){try{return retired(spell.getSpellKey().getKey());}catch(RuntimeException invalid){return !retiredKeys.isEmpty();}}try{return r.manages(spell.getSpellKey().getBaseKey())||retired(spell.getSpellKey().getKey());}catch(Throwable unavailable){return true;}}
 public static boolean allowAuto(boolean original,SpellKey key){var r=resolver;if(r==null)return retiredKeys.isEmpty()?original:key!=null&&original&&!retiredKeys.contains(key.getBaseKey());try{return original&&!r.manages(key.getBaseKey())&&!retiredKeys.contains(key.getBaseKey());}catch(Throwable unavailable){return false;}}
 private LearnedHooks(){}
}
