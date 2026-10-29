package com.winkreyes.excelwandbars;

import java.util.*;
import java.util.function.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import com.elmakers.mine.bukkit.api.magic.*;
import com.elmakers.mine.bukkit.api.wand.Wand;
import wrv.magic.evidence.*;

/** Prepared read-only adapter. Native callback capabilities, never held-item or ID/template lookup. */
final class NativePracticeAdmission implements AutoCloseable {
    private final MagicAPI magic;
    private final Set<String> managed;
    private final Predicate<Wand> eligibleRank;
    private final Predicate<World> skippedWorld;
    private final BiPredicate<UUID,String> floorCoverage;
    private final BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,sharedTargetPolicy;
    private boolean closed;
    NativePracticeAdmission(MagicAPI magic, Set<String> managed, Predicate<Wand> eligibleRank,
            Predicate<World> skippedWorld, BiPredicate<UUID,String> floorCoverage,
            BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy) {
        this(magic,managed,eligibleRank,skippedWorld,floorCoverage,targetPolicy,targetPolicy);
    }
    NativePracticeAdmission(MagicAPI magic, Set<String> managed, Predicate<Wand> eligibleRank,
            Predicate<World> skippedWorld, BiPredicate<UUID,String> floorCoverage,
            BiPredicate<EffectObserver.Receipt,LivingEntity> targetPolicy,
            BiPredicate<EffectObserver.Receipt,LivingEntity> sharedTargetPolicy) {
        this.magic=Objects.requireNonNull(magic);this.managed=Set.copyOf(managed);
        this.eligibleRank=Objects.requireNonNull(eligibleRank);this.skippedWorld=Objects.requireNonNull(skippedWorld);
        this.floorCoverage=Objects.requireNonNull(floorCoverage);this.targetPolicy=Objects.requireNonNull(targetPolicy);this.sharedTargetPolicy=Objects.requireNonNull(sharedTargetPolicy);
        if(this.managed.isEmpty())throw new IllegalArgumentException("explicit managed spells required");
    }
    private boolean source(UUID caster,String spell,EffectObserver.SourceSnapshot snapshot,Mage mage,Wand wand,boolean requireFloor) {
        if(closed||!Bukkit.isPrimaryThread()||caster==null||!managed.contains(spell)||snapshot==null||mage==null||wand==null)return false;
        if(!(mage.getEntity() instanceof Player player)||!player.isOnline()||!caster.equals(player.getUniqueId())
                ||magic.getController().getRegisteredMage(player)!=mage||skippedWorld.test(player.getWorld())
                ||!eligibleRank.test(wand)||(requireFloor&&!floorCoverage.test(caster,spell)))return false;
        if(snapshot.wandId()==null||snapshot.templateKey()==null||!snapshot.ownerKnown()
                ||!Boolean.TRUE.equals(snapshot.bound())||!caster.equals(snapshot.ownerId())
                ||!snapshot.wandId().equals(wand.getId())||!snapshot.templateKey().equals(wand.getTemplateKey())
                ||!wand.isBound()||!wand.canUse(player)||!wand.hasSpell(spell)||snapshot.issuedAtMillis()<0)return false;
        try {
            Object owner=wand.getClass().getMethod("getOwnerId").invoke(wand);
            return !closed&&owner instanceof String id&&caster.toString().equalsIgnoreCase(id);
        } catch (ReflectiveOperationException|RuntimeException unknown) { return false; }
    }
    boolean originEligible(EffectObserver.RootEvidence root) {
        if(root==null)return false;
        try {
            var authority=EffectObserver.currentRootAuthority(root).orElse(null);
            return authority!=null&&source(root.caster(),root.spell(),root.source(),authority.mage(),authority.wand(),true)
                &&EffectObserver.currentRootAuthority(root).isPresent()&&!closed;
        } catch(RuntimeException|LinkageError unknown){return false;}
    }
    boolean targetEligible(EffectObserver.Receipt receipt) {return targetEligible(receipt,true);}
    /** SP retains its own RankUpHook authority and does not require practice-floor migration. */
    boolean sharedTargetEligible(EffectObserver.Receipt receipt) {return targetEligible(receipt,false);}
    private boolean targetEligible(EffectObserver.Receipt receipt,boolean requireFloor) {
        if(receipt==null||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0)return false;
        try {
            var authority=EffectObserver.currentReceiptAuthority(receipt).orElse(null);
            return authority!=null&&authority.target()!=null&&receipt.recipient().equals(authority.target().getUniqueId())
                &&source(receipt.caster(),receipt.spell(),receipt.source(),authority.mage(),authority.wand(),requireFloor)
                &&(requireFloor?targetPolicy:sharedTargetPolicy).test(receipt,authority.target())
                &&source(receipt.caster(),receipt.spell(),receipt.source(),authority.mage(),authority.wand(),requireFloor)
                &&EffectObserver.currentReceiptAuthority(receipt).isPresent()&&!closed;
        }catch(RuntimeException|LinkageError unknown){return false;}
    }
    PracticeConsumerAckV2.Admission resolve(EffectObserver.Receipt receipt) {
        if(!targetEligible(receipt))return null;
        return new PracticeConsumerAckV2.Admission(receipt.cast(),receipt.caster(),receipt.spell(),receipt.source(),
            receipt.caster().toString(),receipt.source().issuedAtMillis(),PracticeConsumerAckV2.TimeBasis.CAST_ISSUANCE);
    }
    @Override public void close(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");closed=true;}
}
