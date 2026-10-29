package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.*;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
import com.elmakers.mine.bukkit.api.magic.*;import com.elmakers.mine.bukkit.api.wand.Wand;import com.elmakers.mine.bukkit.api.wand.WandTemplate;import com.elmakers.mine.bukkit.api.wand.WandUpgradePath;import com.elmakers.mine.bukkit.api.spell.SpellKey;import com.elmakers.mine.bukkit.api.spell.PrerequisiteSpell;
import wrv.magic.level.*;import wrv.magic.retention.*;
/** Current loaded Magic authority only. No held-item substitution, offline rank guessing or spell granting. */
final class CurrentPlayerPracticeProviders {
 private final MagicAPI magic;private final Map<String,BoundedPractice> ledgers;
 private final Map<String,LearnedLevelResolver.Binding> bindings;
 private final BiPredicate<UUID,String> reviewedFloorCoverage;
 private final Function<LearnedLevelResolver.Request,Boolean> beforeUse;
 CurrentPlayerPracticeProviders(MagicAPI magic,Map<String,BoundedPractice> ledgers,
       Map<String,LearnedLevelResolver.Binding> bindings,BiPredicate<UUID,String> reviewedFloorCoverage){
  this(magic,ledgers,bindings,reviewedFloorCoverage,r->true);
 }
 CurrentPlayerPracticeProviders(MagicAPI magic,Map<String,BoundedPractice> ledgers,Map<String,LearnedLevelResolver.Binding> bindings,
       BiPredicate<UUID,String> reviewedFloorCoverage,Function<LearnedLevelResolver.Request,Boolean> beforeUse){
  this.beforeUse=Objects.requireNonNull(beforeUse);
  this.magic=Objects.requireNonNull(magic);this.ledgers=Map.copyOf(ledgers);this.bindings=Map.copyOf(bindings);
  this.reviewedFloorCoverage=Objects.requireNonNull(reviewedFloorCoverage);
  if(!this.ledgers.keySet().equals(this.bindings.keySet()))throw new IllegalArgumentException("matching explicit managed spell sets required");
 }
 private record Subject(Player player,Mage mage,Wand wand){}
 private Subject subject(LearnedLevelResolver.Request r){
  if(!Bukkit.isPrimaryThread()||r==null||r.player()==null||r.wandId()==null)return null;
  Player p=Bukkit.getPlayer(r.player());if(p==null||!p.isOnline())return null;
  var authority=OriginalWandContext.current(r).orElse(null);if(authority==null)return null;
  Mage found=authority.mage();Wand w=authority.wand();
  if(found==null||!(found.getEntity() instanceof Player originalPlayer)||!r.player().equals(originalPlayer.getUniqueId()))return null;
  int exact=0;for(Mage candidate:magic.getMages())if(candidate.getEntity() instanceof Player mp&&mp.getUniqueId().equals(r.player())){if(candidate!=found)return null;exact++;}
  if(exact!=1)return null;
  if(w==null||!r.wandId().equals(w.getId())||!w.isBound()||!w.canUse(p)||!w.hasSpell(r.spell()))return null;
  return new Subject(p,found,w);
 }
 /** Same pathless rank source as the spell book, but unknown/cyclic registry chains fail closed. */
 WandUpgradePath currentRankPath(Wand wand){
  try{
   var direct=wand.getPath();if(direct!=null)return direct;
   String key=wand.getTemplateKey();WandTemplate first=wand.getTemplate();
   if(key==null||key.isBlank()||first==null||!key.equals(first.getKey())||magic.getController().getWandTemplate(key)!=first)return null;
   Set<WandTemplate> seen=Collections.newSetFromMap(new IdentityHashMap<>());List<WandTemplate> chain=new ArrayList<>();
   WandTemplate next=first;
   while(next!=null){
    if(chain.size()>=8||!seen.add(next)||next.getKey()==null||magic.getController().getWandTemplate(next.getKey())!=next)return null;
    chain.add(next);next=next.getParent();
   }
   for(var t:chain){
    var cfg=t.getConfiguration();String rank=cfg==null?null:cfg.getString("level_spells_to_path");
    if(rank!=null&&!rank.isBlank()){
     var result=magic.getController().getPath(rank);
     return result!=null&&rank.equals(result.getKey())?result:null;
    }
   }
   var result=magic.getController().getPath(key);
   return result!=null&&key.equals(result.getKey())?result:null;
  }catch(RuntimeException|LinkageError unavailable){return null;}
 }
 /** Requirements belong to each previous level transition. Reviewed earned floors are not re-earned. */
 boolean transitionAllows(Wand wand,String spell,int targetLevel,int reviewedFloor){
  for(int previous=Math.max(1,reviewedFloor);previous<targetLevel;previous++){
   String key=new SpellKey(spell,previous).getKey();var prior=magic.getController().getSpellTemplate(key);
   if(prior==null||!key.equals(prior.getKey()))return false;
   String nextKey=new SpellKey(spell,previous+1).getKey();var next=magic.getController().getSpellTemplate(nextKey);
   if(next==null||!nextKey.equals(next.getKey())||!PrerequisiteSpell.hasPrerequisites(wand,next))return false;
   String required=prior.getRequiredUpgradePath();var tags=prior.getRequiredUpgradeTags();
   var path=currentRankPath(wand);
   if(required!=null&&!required.isEmpty()&&(path==null||!path.hasPath(required)))return false;
   if(tags!=null&&!tags.isEmpty()&&(path==null||!path.hasAllTags(tags)))return false;
  }
  return true;
 }
 com.winkreyes.excelwandbars.core.PracticeDisplay display(PracticeDisplayBridge.Request display){
  var unavailable=com.winkreyes.excelwandbars.core.PracticeDisplay.unavailable();
  if(display==null||display.subject()==null||display.originalMage()==null||display.originalWand()==null||!Bukkit.isPrimaryThread())return unavailable;
  var request=display.subject();
  try{return OriginalWandContext.withOriginal(request,display.originalMage(),display.originalWand(),()->{
   var decision=create().resolve(request);
   if(decision.status()!=LearnedLevelResolver.Status.RESOLVED)return unavailable;
   var ledger=ledgers.get(request.spell());if(ledger==null)return unavailable;
   var view=ledger.snapshot(request.player().toString());int active=decision.effectiveLearnedLevel();
   String requirement="";
   if(active<view.learnedLevel()){
    var needs=new ArrayList<String>();String key=new SpellKey(request.spell(),active).getKey();var prior=magic.getController().getSpellTemplate(key);
    if(prior!=null&&key.equals(prior.getKey())){
     String path=prior.getRequiredUpgradePath();if(path!=null&&!path.isBlank())needs.add(path+" wand rank");
     var tags=prior.getRequiredUpgradeTags();if(tags!=null&&!tags.isEmpty())needs.add("tags "+String.join(", ",new TreeSet<>(tags)));
    }
    needs.add("current seal, cast permission and prerequisites");requirement=String.join("; ",needs);
   }
   return new com.winkreyes.excelwandbars.core.PracticeDisplay(true,active,view.learnedLevel(),view.maxLevel(),view.usefulCount(),view.nextCumulativeThreshold(),requirement);
  });}catch(RuntimeException|LinkageError failure){return unavailable;}
 }
 LearnedLevelResolver create(){return new LearnedLevelResolver(bindings,r->{
   if(subject(r)==null||!Boolean.TRUE.equals(beforeUse.apply(r))||subject(r)==null||!reviewedFloorCoverage.test(r.player(),r.spell()))return null;
   return new LearnedLevelResolver.Scope(r,r.player().toString());
  },scope->{
   if(!scope.ownerScope().equals(scope.subject().player().toString()))return null;
   var ledger=ledgers.get(scope.subject().spell());if(ledger==null)return null;
   var v=ledger.snapshot(scope.ownerScope());
   return new LearnedLevelResolver.View(v.ownerScope(),v.spellKey(),v.learnedLevel(),v.maxLevel(),v.usefulCount(),v.nextCumulativeThreshold(),v.migrationFloor(),v.policyId());
  },scope->{
   var r=scope.subject();var s=subject(r);var binding=bindings.get(r.spell());
   if(s==null||binding==null||!reviewedFloorCoverage.test(r.player(),r.spell()))return null;
   var ledger=ledgers.get(r.spell());if(ledger==null)return null;
   int reviewedFloor=ledger.snapshot(scope.ownerScope()).migrationFloor();
   Set<Integer> levels=new TreeSet<>();
   for(int level=1;level<=binding.maxLevel();level++){
    String key=new SpellKey(r.spell(),level).getKey();var template=magic.getController().getSpellTemplate(key);
    // Exact key equality prevents Magic fallback from inventing an absent native level.
    if(template!=null&&key.equals(template.getKey())&&template.hasCastPermission(s.player())&&transitionAllows(s.wand(),r.spell(),level,reviewedFloor))levels.add(level);
   }
   return new LearnedLevelResolver.Availability(r,scope.ownerScope(),binding.policyId(),
       "loaded-native-registry:"+r.spell()+":"+levels,true,levels);
  });}
}
