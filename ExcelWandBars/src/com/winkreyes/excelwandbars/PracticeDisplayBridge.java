package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.Function;
import com.winkreyes.excelwandbars.core.PracticeDisplay;
import wrv.magic.level.LearnedLevelResolver;
/** One generation-owned read-only provider. No mutation, lore registration, or currency handling. */
final class PracticeDisplayBridge {
 record Request(LearnedLevelResolver.Request subject,com.elmakers.mine.bukkit.api.wand.Wand originalWand,com.elmakers.mine.bukkit.api.magic.Mage originalMage){}
 private record Registration(Set<String> keys,Function<Request,PracticeDisplay> provider,Thread owner){}
 private static Registration current;private static final Set<String> previouslyManaged=new HashSet<>();
 static synchronized AutoCloseable installOwned(Set<String> keys,Function<Request,PracticeDisplay> provider){
  if(current!=null)throw new IllegalStateException("practice display already owned");
  if(keys.isEmpty()||keys.stream().anyMatch(k->!k.matches("[a-z0-9_.-]+")))throw new IllegalArgumentException("explicit keys");
  var owned=new Registration(Set.copyOf(keys),Objects.requireNonNull(provider),Thread.currentThread());current=owned;previouslyManaged.addAll(keys);
  return ()->{synchronized(PracticeDisplayBridge.class){if(Thread.currentThread()!=owned.owner())throw new IllegalStateException("display owner thread required");if(current==owned)current=null;}};
 }
 static synchronized PracticeDisplay read(Request request,boolean configuredManaged){
  if(request==null||request.subject()==null)return configuredManaged?PracticeDisplay.unavailable():null;
  var registration=current;
  if(registration==null||!registration.keys().contains(request.subject().spell()))return configuredManaged||previouslyManaged.contains(request.subject().spell())?PracticeDisplay.unavailable():null;
  if(Thread.currentThread()!=registration.owner())return PracticeDisplay.unavailable();
  try{var view=registration.provider().apply(request);return view==null?PracticeDisplay.unavailable():view;}catch(RuntimeException unavailable){return PracticeDisplay.unavailable();}
 }
}
