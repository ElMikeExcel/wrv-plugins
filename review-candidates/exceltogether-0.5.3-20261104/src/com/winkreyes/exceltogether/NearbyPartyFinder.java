package com.winkreyes.exceltogether;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Real finder: another online, non-spectator player in the same world within the radius. */
public final class NearbyPartyFinder implements PartyFinder {
    private final PartyBonus bonus;

    public NearbyPartyFinder(PartyBonus bonus) { this.bonus = bonus; }

    @Override
    public boolean hasPartner(OfflinePlayer earner) {
        Player me = earner.getPlayer();
        if (me == null || !me.isOnline()) return false;
        Location a = me.getLocation();
        if (a.getWorld() == null) return false;
        for (Player other : a.getWorld().getPlayers()) {
            if (other.getUniqueId().equals(me.getUniqueId())) continue;
            if (!other.isOnline() || other.getGameMode() == GameMode.SPECTATOR) continue;
            Location b = other.getLocation();
            if (bonus.within(a.getWorld().equals(b.getWorld()),
                    a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ())) {
                return true;
            }
        }
        return false;
    }
}
