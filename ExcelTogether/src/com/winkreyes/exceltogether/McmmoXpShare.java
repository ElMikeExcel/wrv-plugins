package com.winkreyes.exceltogether;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Reflection bridge from mcMMO's XP gain event to XpShareCore. If any mcMMO piece is missing it reports why and stays off. */
public final class McmmoXpShare {
    private final XpShareCore core; private final PartyFinder finder; private final Function<UUID, Player> online;
    private Method modXp, togetherMult; private Object srcSelf; private final double partyMult;
    public boolean receiverBoosts = false; private boolean plain = false;
    private Method evPlayer, evSkill, evXp, evReason, uGet, applyXp, mpParty, partyMode;
    private Object partyMembersSource;
    public final List<String> missing = new ArrayList<>();
    public Class<? extends Event> eventClass;
    public int indicators = 0;
    private final java.util.function.BiConsumer<OfflinePlayer, String> bar; private final String barText;

    public McmmoXpShare(ClassLoader cl, PartyFinder finder, double percent, Function<UUID, Player> online,
                        java.util.function.BiConsumer<OfflinePlayer, String> bar, String barText, double partyMult) {
        this.barText = barText; this.partyMult = partyMult;
        this.finder = finder; this.online = online; this.bar = bar;
        resolve(cl);
        this.core = new XpShareCore(this::apply, percent);
    }
    public boolean available() { return missing.isEmpty(); }
    public XpShareCore core() { return core; }

    @SuppressWarnings("unchecked")
    private void resolve(ClassLoader cl) {
        try {
            eventClass = (Class<? extends Event>) Class.forName("com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent", false, cl);
            Class<?> exp = Class.forName("com.gmail.nossr50.events.experience.McMMOPlayerExperienceEvent", false, cl);
            evPlayer = eventClass.getMethod("getPlayer"); evSkill = exp.getMethod("getSkill"); evReason = exp.getMethod("getXpGainReason");
            evXp = eventClass.getMethod("getRawXpGained");
            Class<?> user = Class.forName("com.gmail.nossr50.util.player.UserManager", false, cl);
            uGet = user.getMethod("getPlayer", Player.class);
            Class<?> mmo = Class.forName("com.gmail.nossr50.datatypes.player.McMMOPlayer", false, cl);
            Class<?> skill = Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType", false, cl);
            Class<?> reason = Class.forName("com.gmail.nossr50.datatypes.experience.XPGainReason", false, cl);
            Class<?> src = Class.forName("com.gmail.nossr50.datatypes.experience.XPGainSource", false, cl);
            for (Method m : mmo.getMethods())
                if (m.getName().equals("applyXpGain") && m.getParameterCount() == 4 && m.getParameterTypes()[1] == float.class
                        && m.getParameterTypes()[0].getName().equals(skill.getName()) && m.getParameterTypes()[3].getName().equals(src.getName())) applyXp = m;
            if (applyXp == null) { StringBuilder sb = new StringBuilder(); for (Method m : mmo.getMethods()) if (true) sb.append(java.util.Arrays.toString(new Object[0])); for (Method m : mmo.getMethods()) if (m.getName().equals("applyXpGain")) sb.append(java.util.Arrays.toString(m.getParameterTypes())).append(" skill=").append(skill.getName()); sb.append(" mmoFrom=").append(mmo.getProtectionDomain().getCodeSource()).append(" n=").append(mmo.getMethods().length); throw new NoSuchMethodException("applyXpGain candidates: " + sb); }
            partyMembersSource = Enum.valueOf((Class<Enum>) src.asSubclass(Enum.class), "PARTY_MEMBERS");
            mpParty = mmo.getMethod("getParty");
            Class<?> party = Class.forName("com.gmail.nossr50.datatypes.party.Party", false, cl);
            partyMode = party.getMethod("getXpShareMode");
            srcSelf = Enum.valueOf((Class<Enum>) src.asSubclass(Enum.class), "SELF");
            // Optional: lets the receiver get THEIR OWN mcMMO multipliers (global /xprate, e.g. ExcelWeekendXP, perks, skill modifiers).
            // modifyXpGain is package-private in mcMMO, so this needs setAccessible; if anything is missing we fall back to a plain copy.
            try {
                Method mx = mmo.getDeclaredMethod("modifyXpGain", skill, float.class); mx.setAccessible(true); modXp = mx;
                try { togetherMult = Class.forName("com.gmail.nossr50.party.PartyTogetherBonus", false, cl).getMethod("getSkillXpMultiplier", mmo, src); }
                catch (Throwable t) { togetherMult = null; }          // upstream mcMMO has no Together bonus: multiplier is 1
                receiverBoosts = true;
            } catch (Throwable t) { modXp = null; receiverBoosts = false; }
        } catch (Throwable t) { missing.add(String.valueOf(t)); }
    }

    private boolean apply(UUID receiver, Object skill, float xp, Object reason) throws Exception {
        Player p = online.apply(receiver);
        if (p == null) return false;
        Object mmo = uGet.invoke(null, p);
        if (mmo == null) return false;                       // mcMMO profile not loaded yet
        float amount = xp;
        if (receiverBoosts && !plain) {
            // xp is the earner's amount with the earner's own multipliers removed; the receiver gets their own, then the party multiplier
            float mine = (Float) modXp.invoke(mmo, skill, xp);
            amount = (float) (mine * partyMult);   // mine already includes the receiver's rate (e.g. weekend), perks and skill modifiers
        }
        if (!(amount > 0) || Float.isInfinite(amount)) return false;
        applyXp.invoke(mmo, skill, amount, reason, partyMembersSource);
        if (bar != null) { indicators++; } if (bar != null) bar.accept(p, barText);
        return true;
    }

    /** Earner's final gain divided by the earner's own multipliers (mcMMO modifiers/rate/perks and the Together bonus), giving the base amount. */
    float baseXp(Object earnerMmo, Object skill, float finalXp) {
        try {
            double mine = (Float) modXp.invoke(earnerMmo, skill, 1f);
            double together = togetherMult == null ? 1.0 : ((Number) togetherMult.invoke(null, earnerMmo, srcSelf)).doubleValue();
            return XpShareCore.baseOf(finalXp, mine, together);   // NaN = unusable: caller shares the final amount as a plain copy
        } catch (Throwable t) { return Float.NaN; }
    }

    /** Call from the (MONITOR, ignoreCancelled) XP gain event. Never throws. */
    public void onGain(Event e) {
        try {
            if (!eventClass.isInstance(e)) return;
            Player p = (Player) evPlayer.invoke(e);
            if (p == null) return;
            float xp = (Float) evXp.invoke(e);
            boolean mmoShares = false;
            Object mmo = uGet.invoke(null, p);
            if (mmo != null) { Object party = mpParty.invoke(mmo); mmoShares = party != null && "EQUAL".equals(String.valueOf(partyMode.invoke(party))); }
            Object skill = evSkill.invoke(e);
            float base = xp; plain = true;
            if (receiverBoosts && mmo != null) { float b = baseXp(mmo, skill, xp); if (!Float.isNaN(b)) { base = b; plain = false; } }
            core.onGain(p.getUniqueId(), skill, base, evReason.invoke(e), mmoShares, () -> {
                List<UUID> ids = new ArrayList<>();
                for (OfflinePlayer o : finder.sharers(p)) ids.add(o.getUniqueId());
                return ids;
            });
        } catch (Throwable t) { /* sharing must never break the earner's XP */ }
    }
}
