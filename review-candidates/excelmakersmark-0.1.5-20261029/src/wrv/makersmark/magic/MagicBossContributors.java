/* Excel WRV maintained source; original attribution and license preserved. */ package wrv.makersmark.magic;

import java.util.*;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import wrv.magic.evidence.EffectObserver;

/** Default-unwired evidence consumer. Never changes items, raw kills, credit, SP or rewards. */
public final class MagicBossContributors implements EffectObserver.Sink {
    public record Source(UUID item, String nativeWand, UUID actor) {}
    public record Accomplishment(UUID item, UUID victim, UUID actor, String nativeWand, String bossType, String observedName) {}
    @FunctionalInterface public interface SourceAuthority {
        /** Resolve the captured original item, never merely the currently held wand. */
        boolean verified(Source captured);
    }
    private static final Set<String> BOSSES=Set.of("WITHER","ENDER_DRAGON","ELDER_GUARDIAN","WARDEN");
    private static final Set<String> ENTITY_NAMESPACES=Set.of("minecraft","excelrewardguard","excelbosstrial");
    private final SourceAuthority authority;
    private final Consumer<String> diagnostics;
    private final Map<UUID, LinkedHashMap<UUID,Source>> contributors=new HashMap<>();
    private final Set<UUID> ambiguousItems=new HashSet<>();
    private final Set<UUID> ambiguousDeaths=new HashSet<>();
    private boolean paused;
    private record DeathObservation(EntityDeathEvent event,String type,String name) {}
    private final Map<UUID,DeathObservation> pendingDeaths=new HashMap<>();
    private final LinkedHashSet<UUID> finished=new LinkedHashSet<>();
    private final LinkedHashSet<UUID> dispatches=new LinkedHashSet<>();
    public MagicBossContributors(SourceAuthority authority,Consumer<String> diagnostics){this.authority=Objects.requireNonNull(authority);this.diagnostics=Objects.requireNonNull(diagnostics);}
    @Override public void observe(EffectObserver.Receipt ignored){} // Receipt alone lacks verified event binding.
    @Override public void damageCompleted(EffectObserver.DamageCompletion c){
        if(paused || !Bukkit.isPrimaryThread() || c==null || c.pending()==null || c.verdict()!=EffectObserver.DamageVerdict.VERIFIED)return;
        var p=c.pending();var r=c.evidence();var event=c.verifiedEvent();var snapshot=p.source();
        if(p.dispatch()==null||p.cast()==null||p.caster()==null||p.recipient()==null||p.spell()==null||p.spell().isBlank()
                ||snapshot==null||snapshot.observedMakerItemId()==null||snapshot.wandId()==null||snapshot.wandId().isBlank()
                ||event==null||event.isCancelled()||!Double.isFinite(event.getFinalDamage())||event.getFinalDamage()<=0
                ||!(event.getEntity() instanceof LivingEntity boss)||!eligible(boss)||!boss.getUniqueId().equals(p.recipient())
                ||r==null||r.kind()!=EffectObserver.Kind.DIRECT_DAMAGE||!Double.isFinite(r.effectiveAmount())||r.effectiveAmount()<=0
                ||!Objects.equals(r.cast(),p.cast())||!Objects.equals(r.caster(),p.caster())||!Objects.equals(r.spell(),p.spell())
                ||!Objects.equals(r.recipient(),p.recipient())||!Objects.equals(r.source(),snapshot)||finished.contains(p.recipient()))return;
        if(dispatches.contains(p.dispatch()))return;
        Source source=new Source(snapshot.observedMakerItemId(),snapshot.wandId(),p.caster());
        if(ambiguousItems.contains(source.item()))return;
        if(!authority.verified(source)){diagnostics.accept("Unverified captured wand source refused");return;}
        var items=contributors.get(p.recipient());
        if(items==null){if(contributors.size()>=256){diagnostics.accept("Boss contribution capacity reached; new encounter refused");return;}items=new LinkedHashMap<>();contributors.put(p.recipient(),items);}
        Source previous=items.get(source.item());
        if(previous!=null&&!previous.nativeWand().equals(source.nativeWand())){
            if(ambiguousItems.size()>=4096){paused=true;diagnostics.accept("Ambiguous source capacity reached; consumer paused");return;}
            ambiguousItems.add(source.item());if(ambiguousDeaths.size()>=256)paused=true;else ambiguousDeaths.add(p.recipient());diagnostics.accept("Conflicting native wand identity held");return;
        }
        if(previous==null&&contributors.values().stream().mapToInt(Map::size).sum()>=4096){diagnostics.accept("Boss contribution capacity reached; new item refused");return;}
        items.put(source.item(),source); // Existing per-weapon semantics: last verified contributor actor.
        dispatches.add(p.dispatch());if(dispatches.size()>4096)dispatches.remove(dispatches.iterator().next());
    }
    /** Called by actual death listener; finalization must run AFTER full event/native dispatch. */
    public boolean captureDeath(EntityDeathEvent event){
        if(!Bukkit.isPrimaryThread()||event==null||!eligible(event.getEntity())||finished.contains(event.getEntity().getUniqueId()))return false;
        UUID victim=event.getEntity().getUniqueId();var old=pendingDeaths.get(victim);
        if(old!=null&&old.event()!=event){if(ambiguousDeaths.size()>=256)paused=true;else ambiguousDeaths.add(victim);diagnostics.accept("Competing death observations held");return false;}
        if(old==null&&pendingDeaths.size()>=256){diagnostics.accept("Death observation capacity reached");return false;}
        if(old==null){String name=event.getEntity().customName()==null?null:net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getEntity().customName());
            pendingDeaths.put(victim,new DeathObservation(event,event.getEntity().getType().name(),name));}return true;
    }
    /** Read-only evidence handoff. Caller must durably admit each item/victim receipt; this is not a commit. */
    public List<Accomplishment> finishDeath(EntityDeathEvent event){
        if(paused||!Bukkit.isPrimaryThread()||event==null)return List.of();UUID victim=event.getEntity().getUniqueId();
        var observation=pendingDeaths.get(victim);if(observation==null||observation.event()!=event||ambiguousDeaths.contains(victim))return List.of();
        if(event.isCancelled()){pendingDeaths.remove(victim);return List.of();} // Retain valid earlier contributors across revival/phase continuation.
        if(!eligible(event.getEntity())){diagnostics.accept("Boss provenance changed; history held");return List.of();}
        var items=contributors.get(victim);
        if(items==null)return List.of();var output=new ArrayList<Accomplishment>();
        for(var source:items.values()){
            if(ambiguousItems.contains(source.item())||!authority.verified(source)){diagnostics.accept("Contributor handoff held for unresolved source");return List.of();}
            output.add(new Accomplishment(source.item(),victim,source.actor(),source.nativeWand(),observation.type(),observation.name()));
        }
        return List.copyOf(output);
    }
    /** Caller may acknowledge only AFTER every emitted award is durably admitted. */
    public boolean acknowledge(EntityDeathEvent event,List<Accomplishment> admitted){
        if(event==null||admitted==null||admitted.isEmpty())return false;
        var expected=finishDeath(event);if(expected.isEmpty()||!expected.equals(admitted))return false;
        UUID victim=event.getEntity().getUniqueId();pendingDeaths.remove(victim);contributors.remove(victim);
        finished.add(victim);if(finished.size()>4096)finished.remove(finished.iterator().next());return true;
    }
    private boolean eligible(LivingEntity entity){
        if(!BOSSES.contains(entity.getType().name()))return false;
        for(var key:entity.getPersistentDataContainer().getKeys())if(!ENTITY_NAMESPACES.contains(key.getNamespace()))return false;
        return true;
    }
    public boolean hasContributors(UUID victim){var items=contributors.get(victim);return items!=null&&!items.isEmpty();}
    public void discardOptedOut(UUID victim,java.util.function.Predicate<UUID> optedOut){var items=contributors.get(victim);if(items!=null)items.values().removeIf(source->optedOut.test(source.actor()));}
    public void retireEmptyDeath(EntityDeathEvent event){if(event==null||hasContributors(event.getEntity().getUniqueId()))return;var seen=pendingDeaths.get(event.getEntity().getUniqueId());if(seen!=null&&seen.event()==event){pendingDeaths.remove(event.getEntity().getUniqueId());contributors.remove(event.getEntity().getUniqueId());}}

    /** Shutdown/discard only, not durable quarantine recovery. */
    public void clear(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");contributors.clear();ambiguousItems.clear();ambiguousDeaths.clear();pendingDeaths.clear();finished.clear();dispatches.clear();paused=false;}
}
