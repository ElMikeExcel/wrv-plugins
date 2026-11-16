package com.winkreyes.exceltogether;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * ExcelTogether 0.5.1 - turns a party's member UUIDs into the players a
 * party-wide effect may touch: online, in the caster's world, and (when
 * maxRange > 0) within maxRange blocks of the caster. The caster is
 * always first and always included - a solo caster's roster is himself,
 * so a party spell cast alone still repairs the caster.
 *
 * Cross-world members are skipped: the roster is the targeting decision
 * for the party-repair action (see REPAIREQUIP-REDESIGN.md §6-B, which
 * specifies online + same-world), and Magic's per-entity step works in
 * the caster's world context.
 *
 * Pure Bukkit-interface logic (Player / World are interfaces), so the
 * selection rules are unit-testable headless. Excel - Wink Reyes Valley.
 */
public final class PartyRoster {
    private PartyRoster() {}

    /**
     * @param caster       the casting player (always included)
     * @param memberIds    the party roster including the caster; may be null
     * @param maxRange     distance cap in blocks; 0 or negative means no cap
     * @param onlinePlayer resolves a UUID to the online Player, or null
     * @return the selected players, caster first, then roster order
     */
    public static List<Player> select(Player caster, Collection<UUID> memberIds, double maxRange,
                                      Function<UUID, Player> onlinePlayer) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        ids.add(caster.getUniqueId());
        if (memberIds != null) ids.addAll(memberIds);
        World world = caster.getWorld();
        Location origin = caster.getLocation();
        double capSquared = maxRange * maxRange;
        List<Player> out = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            boolean self = id.equals(caster.getUniqueId());
            Player p = self ? caster : onlinePlayer.apply(id);
            if (p == null || !p.isOnline()) continue;
            if (!world.equals(p.getWorld())) continue;
            if (!self && maxRange > 0
                    && p.getLocation().distanceSquared(origin) > capSquared) continue;
            out.add(p);
        }
        return out;
    }
}
