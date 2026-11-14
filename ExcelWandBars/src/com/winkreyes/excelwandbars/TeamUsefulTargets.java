package com.winkreyes.excelwandbars;

import org.bukkit.entity.*;
import wrv.magic.evidence.EffectObserver;

/** Proposed narrow team target policy, supplied explicitly to prepared factory. No activation. */
final class TeamUsefulTargets {
    static boolean hostile(LivingEntity target) {
        return target!=null&&!(target instanceof Player)&&!(target instanceof Tameable pet&&pet.isTamed())
            &&(target instanceof Enemy||target instanceof Monster||target instanceof EnderDragon);
    }
    static boolean eligible(EffectObserver.Receipt receipt,LivingEntity target) {
        if(receipt==null||receipt.kind()==null||target==null||receipt.recipient()==null
                ||!receipt.recipient().equals(target.getUniqueId())||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0)return false;
        // Do not demand alive/registered target for lethal damage: native receipt already proves exact loss.
        return switch(receipt.kind()) {
            case DIRECT_DAMAGE,DIRECT_CONTROL -> hostile(target);
            case DIRECT_HEAL -> target instanceof Player player&&player.isOnline()&&!player.isDead();
            case DIRECT_CLEANSE -> target instanceof Player player&&player.isOnline()&&!player.isDead()&&receipt.cleanse()!=null;
        };
    }
    private TeamUsefulTargets(){}
}
