package com.winkreyes.exceltogether;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Bonus partner finder for 0.2.0: honors BonusMode, using party membership and/or distance. */
public final class PartyMemberFinder implements PartyFinder {
    private final PartyBonus bonus; private final PartyManager mgr; private final BonusMode mode;
    public PartyMemberFinder(PartyBonus bonus, PartyManager mgr, BonusMode mode) { this.bonus = bonus; this.mgr = mgr; this.mode = mode; }

    @Override public boolean hasPartner(OfflinePlayer earner) {
        Player me = earner.getPlayer();
        if (me == null || !me.isOnline()) return false;
        Location a = me.getLocation();
        if (a.getWorld() == null) return false;
        boolean party = false, any = false;
        for (Player other : a.getWorld().getPlayers()) {
            UUID id = other.getUniqueId();
            if (id.equals(me.getUniqueId()) || !other.isOnline() || other.getGameMode() == GameMode.SPECTATOR) continue;
            Location b = other.getLocation();
            if (!bonus.within(a.getWorld().equals(b.getWorld()), a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ())) continue;
            any = true;
            if (mgr.sameParty(me.getUniqueId(), id)) { party = true; break; }
        }
        return mode.counts(party, any);
    }
}
