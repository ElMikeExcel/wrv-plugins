package com.winkreyes.exceltogether;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelTogether 0.5.1 - the bridge between the plugin and its two Magic
 * action classes (ExcelPartyRepairAction / ExcelRepairItemAction).
 *
 * Magic instantiates action classes itself (no-arg, via ActionFactory),
 * so the actions cannot receive the PartyManager by constructor. The
 * plugin binds it here at enable time instead - the same roster of
 * record MagicLevelPartyLookup publishes - and unbinds at disable.
 * Unbound (Magic link off, or any time outside enable), the roster of
 * any player degrades to {self}: the actions then repair the caster
 * alone rather than failing.
 *
 * 0.5.2: the bridge also carries the RepairGate (the confirmation gate
 * in front of the repair) with its cooldown store. Unbound, there is no
 * gate and ExcelPartyRepairAction falls back to the 0.5.1 behaviour -
 * the cast repairs at once. Excel - Wink Reyes Valley.
 */
public final class PartyRepairBridge {
    private PartyRepairBridge() {}

    private static volatile PartyManager parties;
    private static volatile Logger logger;
    private static volatile RepairGate repairGate;
    private static volatile RepairCooldownStore cooldownStore;
    private static volatile File cooldownFile;

    public static void bind(PartyManager partyManager, Logger log) {
        parties = partyManager;
        logger = log;
    }

    /** 0.5.2: bind the confirmation gate and where its lockouts persist. */
    public static void bindGate(RepairGate gate, RepairCooldownStore store, File file) {
        repairGate = gate;
        cooldownStore = store;
        cooldownFile = file;
    }

    public static void unbind() {
        parties = null;
        logger = null;
        repairGate = null;
        cooldownStore = null;
        cooldownFile = null;
    }

    public static boolean bound() { return parties != null; }

    public static Logger logger() { return logger; }

    /** 0.5.2: the confirmation gate, or null when it is off / unbound. */
    public static RepairGate gate() { return repairGate; }

    /** 0.5.2: write the gate's live lockouts to disk (no-op when unbound). */
    public static void persistCooldowns() {
        RepairGate gate = repairGate;
        RepairCooldownStore store = cooldownStore;
        File file = cooldownFile;
        if (gate == null || store == null || file == null) return;
        store.save(file, gate.snapshotConfirmed(System.currentTimeMillis()), logger);
    }

    /**
     * The party roster for a player, leader first, the player included -
     * or a list of just the player when he is solo or the bridge is
     * unbound. Mirrors MagicLevelPartyLookup.partyMembers, in roster
     * order. Offline-grace members are included here; the caller's
     * selection (PartyRoster) gates on online / same-world.
     */
    public static List<UUID> partyMembersOf(UUID player) {
        PartyManager pm = parties;
        if (pm == null) return List.of(player);
        PartyManager.Party party = pm.partyOf(player);
        return party == null ? List.of(player) : party.members;
    }
}
