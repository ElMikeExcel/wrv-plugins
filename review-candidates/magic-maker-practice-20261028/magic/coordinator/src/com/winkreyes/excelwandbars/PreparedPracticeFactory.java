package com.winkreyes.excelwandbars;

import java.nio.file.Path;
import java.util.*;
import java.util.function.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.SpellKey;
import wrv.magic.evidence.*;
import wrv.magic.eligibility.*;
import wrv.magic.level.*;
import wrv.magic.retention.*;
import wrv.sp.route.SingleSpRoute;

/** Explicit prepared factory, not an onEnable hook or configuration default. */
final class PreparedPracticeFactory implements OwnedPracticeLifecycle.PreparedFactory {
    record SpellPolicy(String policyId,int nativeLevels,Set<BoundedPractice.EffectiveKind> acceptedKinds,
            long minimumIntervalMillis,int dailyCap,int receiptCapacity) {
        SpellPolicy {
            if(policyId==null||policyId.isBlank()||nativeLevels<2||nativeLevels>6)throw new IllegalArgumentException("reviewed multi-level policy required");
            acceptedKinds=Set.copyOf(acceptedKinds);
            new BoundedPractice.Config(ApprovedPracticeCurve.forNativeLevels(nativeLevels),acceptedKinds,minimumIntervalMillis,dailyCap,receiptCapacity);
        }
        BoundedPractice.Config config(){return new BoundedPractice.Config(ApprovedPracticeCurve.forNativeLevels(nativeLevels),acceptedKinds,minimumIntervalMillis,dailyCap,receiptCapacity);}
    }
    record Settings(Map<String,SpellPolicy> spells,int issuerCapacity,int pendingCapacity,int receiptCapacity,int playerCapacity,
            SingleSpRoute.LateProof lateProof) {
        Settings {
            spells=Map.copyOf(spells);Objects.requireNonNull(lateProof);
            if(spells.isEmpty()||issuerCapacity<1||pendingCapacity<1||receiptCapacity<1||playerCapacity<1)throw new IllegalArgumentException("explicit bounds required");
            for(String key:spells.keySet())if(!key.matches("[a-z0-9_.-]{1,200}"))throw new IllegalArgumentException("canonical spell key");
        }
    }
    private final ExcelWandBars plugin;private final MagicAPI magic;private final Path data;
    private final Settings settings;private final BiPredicate<UUID,String> coverage;
    private final FirstObservationFloor.Policy migrationPolicy;private final boolean preserveLegacySp;private final Supplier<Set<String>> activeFamilies;
    private final BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,sharedTargetPolicy;
    private final Consumer<Throwable> errors;
    PreparedPracticeFactory(ExcelWandBars plugin,MagicAPI magic,Path data,Settings settings,
            BiPredicate<UUID,String> coverage,BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,
            BiPredicate<EffectObserver.Receipt,LivingEntity> sharedTargetPolicy,Consumer<Throwable> errors){
        this(plugin,magic,data,settings,coverage,targetPolicy,sharedTargetPolicy,errors,null);
    }
    PreparedPracticeFactory(ExcelWandBars plugin,MagicAPI magic,Path data,Settings settings,
            BiPredicate<UUID,String> coverage,BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,
            BiPredicate<EffectObserver.Receipt,LivingEntity> sharedTargetPolicy,Consumer<Throwable> errors,FirstObservationFloor.Policy migrationPolicy){
        this(plugin,magic,data,settings,coverage,targetPolicy,sharedTargetPolicy,errors,migrationPolicy,false);
    }
    PreparedPracticeFactory(ExcelWandBars plugin,MagicAPI magic,Path data,Settings settings,
            BiPredicate<UUID,String> coverage,BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,
            BiPredicate<EffectObserver.Receipt,LivingEntity> sharedTargetPolicy,Consumer<Throwable> errors,FirstObservationFloor.Policy migrationPolicy,boolean preserveLegacySp){
        this(plugin,magic,data,settings,coverage,targetPolicy,sharedTargetPolicy,errors,migrationPolicy,preserveLegacySp,()->settings.spells().keySet());
    }
    PreparedPracticeFactory(ExcelWandBars plugin,MagicAPI magic,Path data,Settings settings,
            BiPredicate<UUID,String> coverage,BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,
            BiPredicate<EffectObserver.Receipt,LivingEntity> sharedTargetPolicy,Consumer<Throwable> errors,FirstObservationFloor.Policy migrationPolicy,boolean preserveLegacySp,Supplier<Set<String>> activeFamilies){
        this.activeFamilies=Objects.requireNonNull(activeFamilies);
        this.preserveLegacySp=preserveLegacySp;
        if(migrationPolicy!=null&&!migrationPolicy.enabled())throw new IllegalArgumentException("explicit enabled migration policy required; use strict constructor otherwise");
        this.migrationPolicy=migrationPolicy;
        this.plugin=Objects.requireNonNull(plugin);this.magic=Objects.requireNonNull(magic);this.data=Objects.requireNonNull(data);
        this.settings=Objects.requireNonNull(settings);this.coverage=Objects.requireNonNull(coverage);
        this.targetPolicy=Objects.requireNonNull(targetPolicy);this.sharedTargetPolicy=Objects.requireNonNull(sharedTargetPolicy);this.errors=Objects.requireNonNull(errors);
    }
    private void registry(Map<String,SpellPolicy> active){
        for(var entry:active.entrySet())for(int level=1;level<=entry.getValue().nativeLevels();level++){
            String key=new SpellKey(entry.getKey(),level).getKey();var nativeSpell=magic.getController().getSpellTemplate(key);
            if(nativeSpell==null||!key.equals(nativeSpell.getKey()))throw new IllegalStateException("exact reviewed native level unavailable: "+key);
        }
    }
    @Override public AutoCloseable open() throws Exception {
        if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");
        Set<String> selected=Set.copyOf(activeFamilies.get());
        if(!settings.spells().keySet().containsAll(selected))throw new IllegalStateException("unknown stage family");
        Map<String,SpellPolicy> active=new LinkedHashMap<>();for(String key:selected)active.put(key,settings.spells().get(key));
        if(active.isEmpty())return ()->{};
        registry(active);var resources=new Resources();
        try {
            Map<String,BoundedPractice> ledgers=new LinkedHashMap<>();Map<String,LearnedLevelResolver.Binding> bindings=new LinkedHashMap<>();
            for(String key:new TreeSet<>(active.keySet())){
                var policy=active.get(key);
                var ledger=new BoundedPractice(data.resolve("practice-"+key+".dat"),policy.policyId(),key,policy.config());
                resources.stores.add(ledger);ledgers.put(key,ledger);bindings.put(key,new LearnedLevelResolver.Binding(policy.policyId(),policy.nativeLevels()));
            }
            var issuer=new AuthenticatedWindow(data.resolve("practice-issuer"),Set.of("practice"),settings.issuerCapacity(),1);
            resources.stores.add(issuer);
            BiPredicate<UUID,String> effectiveCoverage=coverage;
            Function<LearnedLevelResolver.Request,Boolean> beforeUse=request->true;
            if(migrationPolicy!=null){
                Map<String,Integer> maximum=new LinkedHashMap<>();active.forEach((key,policy)->maximum.put(key,policy.nativeLevels()));
                var migration=new FirstObservationCoordinator(magic,new HashSet<>(plugin.rankUp().ladderOrder()),maximum,
                    key->{var template=magic.getController().getSpellTemplate(key);return template==null?null:new SpellKey(template.getKey()).getBaseKey();},
                    data.resolve("first-current-ranks.dat"),migrationPolicy,ledgers);
                resources.migration=migration;
                migration.start();
                resources.templateGuard=MigrationTouchGuard.installOwned(migration::beforeTemplateTouch);
                migration.bootstrapLoaded();
                effectiveCoverage=migration::covered;beforeUse=migration::beforeUse;
            }
            var current=new CurrentPlayerPracticeProviders(magic,ledgers,bindings,effectiveCoverage,beforeUse);
            var admission=new NativePracticeAdmission(magic,active.keySet(),plugin.rankUp()::isLadderWand,plugin::skipWorld,effectiveCoverage,targetPolicy,sharedTargetPolicy);
            resources.admission=admission;
            resources.runtime=ManagedMagicRuntime.startSourceAwareWithHistory(plugin.rankUp(),plugin,gate->{
                try {
                    if(!(gate instanceof SelfHealReceiptGate exact))throw new IllegalArgumentException("native shared gate required");
                    var practice=SourceAwarePracticeBinding.build(issuer,ledgers,admission::originEligible,admission::resolve,exact,e->errors.accept(e));
                    return new ManagedMagicRuntime.Plan(active.keySet(),preserveLegacySp?Set.of():active.keySet(),practice,current.create(),gate,
                        settings.lateProof(),settings.pendingCapacity(),List.of(),failure->errors.accept(failure.error()));
                }catch(java.io.IOException failed){throw new java.io.UncheckedIOException(failed);}
            },settings.receiptCapacity(),settings.playerCapacity(),preserveLegacySp?admission::targetEligible:admission::sharedTargetEligible);
            resources.runtime.attachDisplay(active.keySet(),current::display);
            return resources;
        }catch(Throwable failure){try{resources.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    private static final class Resources implements AutoCloseable {
        final List<AutoCloseable> stores=new ArrayList<>();NativePracticeAdmission admission;ManagedMagicRuntime runtime;FirstObservationCoordinator migration;AutoCloseable templateGuard;boolean closed;
        @Override public void close() throws Exception {
            if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");
            if(closed)return;closed=true;Throwable failure=null;
            // Fence callbacks first, then release exclusive observer/SP owner before closing durable files.
            try{if(admission!=null)admission.close();}catch(Throwable t){failure=t;}
            try{if(runtime!=null)runtime.close();}catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
            try{if(templateGuard!=null)templateGuard.close();}catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
            try{if(migration!=null)migration.close();}catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
            for(int i=stores.size()-1;i>=0;i--)try{stores.get(i).close();}catch(Throwable t){if(failure==null)failure=t;else failure.addSuppressed(t);}
            if(failure instanceof Exception e)throw e;if(failure instanceof Error e)throw e;
        }
    }
}
