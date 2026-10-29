package wrv.magic.evidence;
import com.elmakers.mine.bukkit.spell.ActionSpell;import com.elmakers.mine.bukkit.action.*;import java.util.*;import java.lang.reflect.*;
/** Exact resolved runtime action classes only. Unknown branches/subclasses fail closed. */
public final class StrictActionGraph {
 private static final Set<String> LEAVES=Set.of("HealAction","DamageAction","PotionEffectAction","DelayAction","MessageAction");
 private static final Set<String> COMPOUNDS=Set.of("RepeatAction","CustomProjectileAction","AreaOfEffectAction","AsynchronousAction","ChangeContextAction","CheckModifiersAction");
 public record Proof(String shape){}
 public static Optional<Proof> inspect(Object spell){try{
  if(spell==null||spell.getClass()!=ActionSpell.class)return Optional.empty();
  var field=ActionSpell.class.getDeclaredField("actions");field.setAccessible(true);Object raw=field.get(spell);if(!(raw instanceof Map<?,?> map)||!map.keySet().equals(Set.of("cast")))return Optional.empty();
  var out=new StringBuilder();var seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());int[] count={0};walk(map.get("cast"),out,seen,count,(ActionSpell)spell,((ActionSpell)spell).getWorkingParameters());
  Field current=ActionSpell.class.getDeclaredField("currentHandler");current.setAccessible(true);Object active=current.get(spell);if(active!=null){var check=new StringBuilder();walk(active,check,Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()),new int[]{0},(ActionSpell)spell,((ActionSpell)spell).getWorkingParameters());if(!out.toString().equals(check.toString()))return Optional.empty();}
  return Optional.of(new Proof(out.toString()));
 }catch(Throwable unavailable){return Optional.empty();}}
 private static void walk(Object handler,StringBuilder out,Set<Object> seen,int[] count,ActionSpell spell,org.bukkit.configuration.ConfigurationSection parameters)throws Exception{
  if(handler==null||handler.getClass()!=ActionHandler.class||!seen.add(handler)||++count[0]>512)throw new IllegalArgumentException("unknown/cyclic graph");out.append('[');
  for(ActionContext context:((ActionHandler)handler).getActions()){
   if(context==null||context.getClass()!=ActionContext.class)throw new IllegalArgumentException();var effective=context.getEffectiveParameters(parameters);Object action=context.getAction();if(action==null)throw new IllegalArgumentException();String name=action.getClass().getSimpleName();if(!action.getClass().getName().equals("com.elmakers.mine.bukkit.action.builtin."+name))throw new IllegalArgumentException();
   out.append(name).append(';');if(COMPOUNDS.contains(name)){Field f=CompoundAction.class.getDeclaredField("handlers");f.setAccessible(true);Object raw=f.get(action);if(!(raw instanceof Map<?,?> children))throw new IllegalArgumentException();var keys=new TreeSet<String>();for(Object key:children.keySet()){if(!(key instanceof String s))throw new IllegalArgumentException();keys.add(s);}for(String key:keys){out.append(key).append(':');walk(children.get(key),out,seen,count,spell,effective);}}
   else if(name.equals("ModifierAction")){checkFocus(effective,spell,out);}
   else if(!LEAVES.contains(name))throw new IllegalArgumentException("unreviewed action");
  }out.append(']');seen.remove(handler);
 }

 private static Object field(Class<?> owner,String name,Object target)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 private static void checkFocus(org.bukkit.configuration.ConfigurationSection effective,ActionSpell spell,StringBuilder out)throws Exception{
  var removes=com.elmakers.mine.bukkit.utility.ConfigurationUtils.getStringList(effective,"remove_modifiers");if(removes!=null&&!removes.isEmpty())throw new IllegalArgumentException("modifier removals unreviewed");
  var adds=effective.getConfigurationSection("add_modifiers");var names=adds==null?com.elmakers.mine.bukkit.utility.ConfigurationUtils.getStringList(effective,"add_modifiers"):new ArrayList<String>(adds.getKeys(false));if(names==null||names.isEmpty()||!Set.of("wrv_focus","wrv_renewed","wrv_warded").containsAll(names))throw new IllegalArgumentException("unreviewed modifier");
  for(String name:new TreeSet<String>(names)){
   if(adds!=null){var override=adds.getConfigurationSection(name);if(override!=null){if(!Set.of("duration").containsAll(override.getKeys(false)))throw new IllegalArgumentException("modifier overrides");out.append(override.getValues(false));}}
   Object template=spell.getController().getModifierTemplate(name);if(template==null||template.getClass()!=com.elmakers.mine.bukkit.magic.ModifierTemplate.class||((com.elmakers.mine.bukkit.magic.ModifierTemplate)template).hasParent())throw new IllegalArgumentException("unknown/parent modifier");
   Object config=field(com.elmakers.mine.bukkit.magic.BaseMagicProperties.class,"configuration",template);if(!(config instanceof org.bukkit.configuration.ConfigurationSection c))throw new IllegalArgumentException();var values=new TreeMap<String,Object>(c.getValues(false));
   var keys=name.equals("wrv_focus")?Set.of("name","description","hidden","mana_regeneration_boost"):Set.of("name","description","hidden");if(!keys.containsAll(values.keySet()))throw new IllegalArgumentException("modifier may contain indirect effects");
   if(name.equals("wrv_focus")){Object boost=values.get("mana_regeneration_boost");if(!(boost instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=-0.79)throw new IllegalArgumentException("focus template changed");}
   for(var e:values.entrySet())if(!(e.getValue() instanceof String||e.getValue() instanceof Boolean||e.getValue() instanceof Number))throw new IllegalArgumentException();out.append(name).append(values);
  }
 }
 private StrictActionGraph(){}
}
