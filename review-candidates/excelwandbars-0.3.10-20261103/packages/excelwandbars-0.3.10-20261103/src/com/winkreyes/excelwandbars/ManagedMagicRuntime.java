package com.winkreyes.excelwandbars;

import java.util.*;
import java.util.function.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import wrv.magic.eligibility.*;
import wrv.magic.evidence.*;
import wrv.magic.level.*;
import wrv.sp.route.SingleSpRoute;

/** One explicitly prepared lifecycle owner. No defaults, config activation, migration or payout replay. */
final class ManagedMagicRuntime implements AutoCloseable {
    record Plan(Set<String> practiceSpells, Set<String> sharedSpSpells,
                EffectObserver.Sink practice, LearnedLevelResolver learned,
                Predicate<EffectObserver.Receipt> eligibility,
                SingleSpRoute.LateProof lateProof, int pendingCapacity,
                List<ConsumerFanout.Binding> otherConsumers,
                Consumer<ConsumerFanout.Failure> failures) {
        Plan {
            practiceSpells = Set.copyOf(practiceSpells);
            sharedSpSpells = Set.copyOf(sharedSpSpells);
            otherConsumers = List.copyOf(otherConsumers);
            Objects.requireNonNull(practice); Objects.requireNonNull(learned);
            Objects.requireNonNull(eligibility); Objects.requireNonNull(lateProof);
            Objects.requireNonNull(failures);
            if (practiceSpells.isEmpty() || (!sharedSpSpells.isEmpty()&&!sharedSpSpells.containsAll(practiceSpells))
                    || pendingCapacity < 1) throw new IllegalArgumentException("explicit scopes/capacity required");
            for (String key : sharedSpSpells)
                if (!key.matches("[a-z0-9_.-]{1,200}")) throw new IllegalArgumentException("canonical spell key");
            for (String key : practiceSpells)
                if (!learned.manages(key)) throw new IllegalArgumentException("practice must have matching learned resolver");
            for (var binding : otherConsumers)
                if (Set.of("practice", "shared-sp").contains(binding.id()))
                    throw new IllegalArgumentException("reserved consumer identity");
        }
    }
    private final RankUpHook rankUp;
    private boolean closed;
    private AutoCloseable spHandle, learnedHandle, observerHandle, healthHandle, sourceHandle, historyHandle, displayHandle;
    private Set<String> managedPracticeSpells=Set.of();
    private ManagedMagicRuntime(RankUpHook rankUp) { this.rankUp = Objects.requireNonNull(rankUp); }

    /** Explicit combined mode: history is a consumer of this owner, never a competing registration. */
    static ManagedMagicRuntime startSourceAwareWithHistory(RankUpHook rankUp, Plugin plugin,
            Function<Predicate<EffectObserver.Receipt>, Plan> planFactory, int receiptCapacity, int playerCapacity,
            Predicate<EffectObserver.Receipt> nonSelfEligibility) {
        var attachment=SharedHistoryAttachment.acquire();
        ManagedMagicRuntime runtime=null;
        try {
            var history=attachment.binding();
            runtime=startSourceAware(rankUp,plugin,gate->{
                Plan p=Objects.requireNonNull(planFactory.apply(gate));
                var others=new ArrayList<ConsumerFanout.Binding>(p.otherConsumers());
                if(others.stream().anyMatch(b->b.id().equals(history.id())))throw new IllegalArgumentException("duplicate history consumer");
                others.add(history);
                return new Plan(p.practiceSpells(),p.sharedSpSpells(),p.practice(),p.learned(),p.eligibility(),p.lateProof(),p.pendingCapacity(),others,p.failures());
            },receiptCapacity,playerCapacity,nonSelfEligibility);
            runtime.historyHandle=attachment;
            attachment.attachOwner(runtime::close);
            return runtime;
        } catch(Throwable failure) {
            try { if(runtime!=null)runtime.close();else attachment.close(); }
            catch(Throwable cleanup){failure.addSuppressed(cleanup);}
            throw failure;
        }
    }

    /** Practice factory must wire the supplied gate into its useful-effect predicate; optional managed SP uses the same gate; legacy SP remains outside this observer. */
    static ManagedMagicRuntime startSourceAware(RankUpHook rankUp, Plugin plugin,
            Function<Predicate<EffectObserver.Receipt>, Plan> planFactory, int receiptCapacity, int playerCapacity,
            Predicate<EffectObserver.Receipt> nonSelfEligibility) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("server owner thread required");
        var ledger = new CombatDeficitLedger(playerCapacity);
        var gate = new SelfHealReceiptGate(ledger, receiptCapacity, nonSelfEligibility);
        var tracker = new BukkitCombatHealthTracker(plugin, ledger);
        try {
            Plan plan = Objects.requireNonNull(planFactory.apply(gate));
            requireSharedGate(plan, gate);
            return startInternal(rankUp, plan, tracker, gate);
        } catch (Throwable failure) {
            // Factory/validation failures must fence any retained callback capability too.
            try { gate.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    static void requireSharedGate(Plan plan, SelfHealReceiptGate gate) {
        if (plan.eligibility() != gate) throw new IllegalArgumentException("practice must use exact supplied gate");
        if (!(plan.practice() instanceof SourceAwarePracticeBinding.Bound bound) || !bound.uses(gate))
            throw new IllegalArgumentException("practice must use exact shared gate binding");
    }

    /** Caller must finish explicit policy, current availability and reviewed floor preparation first. */
    static ManagedMagicRuntime start(RankUpHook rankUp, Plan plan) {
        return startInternal(rankUp, plan, null, null);
    }
    private static ManagedMagicRuntime startInternal(RankUpHook rankUp, Plan plan,
            BukkitCombatHealthTracker tracker, SelfHealReceiptGate gate) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("server owner thread required");
        Objects.requireNonNull(plan);
        ManagedMagicRuntime runtime = new ManagedMagicRuntime(rankUp);
        runtime.managedPracticeSpells=plan.practiceSpells();
        runtime.sourceHandle = gate == null ? null : gate::close; // owns it even if startup fails before the wrapping sink exists
        try {
            if (tracker != null) { runtime.healthHandle = tracker; tracker.start(); }
            var consumers = new ArrayList<ConsumerFanout.Binding>();
            consumers.add(new ConsumerFanout.Binding("practice", plan.practiceSpells(), plan.practice()));
            if (!plan.sharedSpSpells().isEmpty()) {
                var sp = rankUp.configureManagedEvidenceOwned(plan.sharedSpSpells(),
                        plan.lateProof(), plan.pendingCapacity(), plan.eligibility());
                runtime.spHandle = sp.handle();
                consumers.add(new ConsumerFanout.Binding("shared-sp", plan.sharedSpSpells(), sp.sink()));
            } // Empty means legacy SP untouched; practice/history still share one observer.
            consumers.addAll(plan.otherConsumers());
            ConsumerFanout fanout = new ConsumerFanout(consumers, plan.failures());
            runtime.learnedHandle = LearnedHooks.installOwned(plan.learned());
            EffectObserver.Sink observer = fanout;
            if (tracker != null) {
                var source = new SourceAwareReceiptSink(fanout, gate, new NativeSelfHealFrameProvider(tracker));
                runtime.sourceHandle = source; observer = source;
            }
            runtime.observerHandle = EffectObserver.installOwned(fanout.spells(), observer);
            EffectObserver.enableLifecycleProof(true);
            return runtime;
        } catch (Throwable failure) {
            // Roll back acquired registrations. Exclusive ownership is required until identity handles exist.
            try { runtime.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    /** Prepared bootstrap supplies the same current provider used by learned-level selection. */
    void attachDisplay(Set<String> keys, Function<PracticeDisplayBridge.Request,com.winkreyes.excelwandbars.core.PracticeDisplay> provider) {
        if(!Bukkit.isPrimaryThread()||closed)throw new IllegalStateException("active server owner required");
        if(displayHandle!=null||!managedPracticeSpells.equals(Set.copyOf(keys)))throw new IllegalArgumentException("exact managed display scope required");
        displayHandle=PracticeDisplayBridge.installOwned(keys,provider);
    }
    @Override public void close() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("server owner thread required");
        if (closed) return;
        closed = true;
        Throwable failure = null;
        try { if (displayHandle != null) displayHandle.close(); }
        catch (Throwable t) { failure=t; }
        finally { displayHandle=null; }
        try { if (observerHandle != null) observerHandle.close(); }
        catch (Throwable t) { if(failure==null)failure=t;else failure.addSuppressed(t); }
        finally { observerHandle = null; }
        try { if (learnedHandle != null) learnedHandle.close(); }
        catch (Throwable t) { if (failure == null) failure = t; else failure.addSuppressed(t); }
        finally { learnedHandle = null; }
        try { if (spHandle != null) spHandle.close(); }
        catch (Throwable t) { if (failure == null) failure = t; else failure.addSuppressed(t); }
        finally { spHandle = null; }
        try { if (sourceHandle != null) sourceHandle.close(); }
        catch (Throwable t) { if (failure == null) failure = t; else failure.addSuppressed(t); }
        finally { sourceHandle = null; }
        try { if (healthHandle != null) healthHandle.close(); }
        catch (Throwable t) { if (failure == null) failure = t; else failure.addSuppressed(t); }
        finally { healthHandle = null; }
        try { if (historyHandle != null) historyHandle.close(); }
        catch (Throwable t) { if (failure == null) failure = t; else failure.addSuppressed(t); }
        finally { historyHandle = null; }
        if (failure != null) throw new IllegalStateException("runtime cleanup incomplete; inspect before restart", failure);
        // Any configured managed-SP fence remains; legacy-only mode never acquired or altered that route.
    }
}
