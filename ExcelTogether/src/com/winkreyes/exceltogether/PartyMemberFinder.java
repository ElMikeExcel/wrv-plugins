package com.winkreyes.exceltogether;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Bonus partner finder: honors BonusMode, using party membership and/or distance. Also drives sharing and the status line. */
public final class PartyMemberFinder implements PartyFinder {
    private final PartyBonus bonus; private final PartyManager mgr; private final BonusMode mode;
    private final Predicate<UUID> active;
    public PartyMemberFinder(PartyBonus bonus, PartyManager mgr, BonusMode mode) { this(bonus, mgr, mode, id -> true); }
    /** @param active false for a player who has been idle too long (sharing only). */
    public PartyMemberFinder(PartyBonus bonus, PartyManager mgr, BonusMode mode, Predicate<UUID> active) {
        this.bonus = bonus; this.mgr = mgr; this.mode = mode; this.active = active;
    }

    private boolean near(Player me, Player other) {
        if (other.getUniqueId().equals(me.getUniqueId()) || !other.isOnline() || other.getGameMode() == GameMode.SPECTATOR) return false;
        Location a = me.getLocation(), b = other.getLocation();
        if (a.getWorld() == null || b.getWorld() == null) return false;
        return bonus.within(a.getWorld().equals(b.getWorld()), a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    @Override public boolean hasPartner(OfflinePlayer earner) {
        Player me = earner.getPlayer();
        if (me == null || !me.isOnline() || me.getLocation().getWorld() == null) return false;
        boolean party = false, any = false;
        for (Player other : me.getLocation().getWorld().getPlayers()) {
            if (!near(me, other)) continue;
            any = true;
            if (mgr.sameParty(me.getUniqueId(), other.getUniqueId())) { party = true; break; }
        }
        return mode.counts(party, any);
    }

    /**
     * How many party members are in bonus range of me right now, not counting me (WRV-068 indicator).
     * Uses the same near() test as hasPartner(), so in PARTY mode count > 0 exactly when the bonus
     * is paying: the indicator can never disagree with the payout. The idle (anti-leech) predicate
     * is NOT applied here, just as hasPartner() does not apply it; it only gates sharing.
     */
    public int partyMembersInRange(Player me) {
        if (me == null || !me.isOnline() || me.getLocation().getWorld() == null) return 0;
        int n = 0;
        for (Player other : me.getLocation().getWorld().getPlayers())
            if (near(me, other) && mgr.sameParty(me.getUniqueId(), other.getUniqueId())) n++;
        return n;
    }

    @Override public String partnerName(OfflinePlayer earner) {
        Player me = earner.getPlayer();
        if (me == null) return null;
        List<String> names = new ArrayList<>(); boolean any = false;
        for (Player other : me.getLocation().getWorld().getPlayers()) {
            if (!near(me, other)) continue;
            any = true;
            if (mgr.sameParty(me.getUniqueId(), other.getUniqueId())) names.add(shortName(other));
        }
        if (!names.isEmpty()) return String.join(", ", names);
        return any ? "a nearby player" : null;
    }

    @Override public List<OfflinePlayer> sharers(OfflinePlayer earner) {
        List<OfflinePlayer> out = new ArrayList<>();
        Player me = earner.getPlayer();
        if (me == null || me.getLocation().getWorld() == null) return out;
        for (Player other : me.getLocation().getWorld().getPlayers())
            if (near(me, other) && mgr.sameParty(me.getUniqueId(), other.getUniqueId()) && active.test(other.getUniqueId())) out.add(other);
        return out;
    }

    private static String shortName(Player p) {
        String n = p.getName();
        try { n = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(p.displayName()); } catch (Throwable ignored) { }
        return n.startsWith(".") ? n.substring(1) : n;
    }

    /** Plain-words status for /excelparty status: whether the bonus is on right now and, if not, why. */
    public List<String> status(Player me) {
        List<String> out = new ArrayList<>();
        int pct = (int) Math.round((bonus.multiplier() - 1.0) * 100.0);
        PartyManager.Party pt = mgr.partyOf(me.getUniqueId());
        if (pt == null && mode == BonusMode.PARTY) { out.add("Party bonus is OFF: you are not in a party. Invite someone with /excelparty invite."); return out; }
        if (hasPartner(me)) {
            out.add("Party bonus is ON: +" + pct + "% Jobs money and XP with " + partnerName(me) + ".");
            return out;
        }
        if (pt == null) { out.add("Party bonus is OFF: nobody is within " + (int) bonus.radius() + " blocks."); return out; }
        int others = 0;
        for (UUID u : pt.members) {
            if (u.equals(me.getUniqueId())) continue;
            others++;
            String n = mgr.nameOf(u); if (n.startsWith(".")) n = n.substring(1);
            Player o = org.bukkit.Bukkit.getPlayer(u);
            if (o == null || !o.isOnline()) { out.add("Party bonus is OFF: " + n + " is offline."); continue; }
            if (o.getGameMode() == GameMode.SPECTATOR) { out.add("Party bonus is OFF: " + n + " is in spectator mode."); continue; }
            Location a = me.getLocation(), b = o.getLocation();
            if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) { out.add("Party bonus is OFF: " + n + " is in another world."); continue; }
            double d = a.distance(b);
            out.add("Party bonus is OFF: " + n + " is " + (int) Math.ceil(d) + " blocks away (needs " + (int) bonus.radius() + " or closer).");
        }
        if (others == 0) out.add("Party bonus is OFF: you are the only one in your party.");
        return out;
    }
}
