package excelemfixes.partydrops;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The party-drops death hook, ported in behavior verbatim from
 * the standalone ExcelPartyDrops 0.1.0 EmDeathListener (the only
 * 0.1.0 class that referenced EliteMobs API types). It is loaded
 * and registered solely when EliteMobs is present and enabled, so
 * the pack stays inert (and error-free) without EliteMobs — the
 * guarded pattern the standalone used.
 *
 * Listener shape per PARTY-DROPS-SPEC-2026-10-20 section 3a:
 * EliteMobs' public EliteMobDeathEvent at MONITOR, editing the
 * wrapped EntityDeathEvent's drop list after EliteMobs' own
 * handling has settled it — the mutation lands in the drop list
 * before Paper scatters it. Only the vanilla drop list is ever
 * touched: EM's generated gear is delivered by EM itself outside
 * the list, and coin-shower items never enter it.
 */
final class PartyDropsListener implements Listener {

    /** The quiet hint, in player words. Small, rare, action-bar only. */
    static final String HINT_TEXT = "Hunting together pays off \u2014 the group earned extra drops.";

    private final PartyDropsModule module;

    PartyDropsListener(PartyDropsModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteMobDeath(EliteMobDeathEvent event) {
        PartyDropsSettings s = module.settingsSnapshot();
        if (!s.enabled()) return;

        EliteEntity elite = event.getEliteEntity();
        if (elite == null) return;

        // --- origin gate (spec section 5): natural only, no anti-exploit flags.
        CreatureSpawnEvent.SpawnReason reason = elite.getSpawnReason();
        boolean eligible = OriginGate.eligible(true, elite.isNaturalEntity(),
                reason == null ? null : reason.name(), elite.isTriggeredAntiExploit());
        if (!eligible) {
            if (s.debug()) {
                module.logger().info("party drops: skipped elite (natural=" + elite.isNaturalEntity()
                        + ", reason=" + reason + ", antiExploit=" + elite.isTriggeredAntiExploit() + ")");
            }
            return;
        }

        EntityDeathEvent death = event.getEntityDeathEvent();
        if (death == null) return;
        List<ItemStack> drops = death.getDrops();
        if (drops == null || drops.isEmpty()) return;

        // --- participants (spec section 2): presence at the death, 50-block rule.
        // The range test is PartyRange.within — a COPY of ExcelTogether's
        // PartyBonus.within() semantics, copied not called (no Together
        // dependency; Together folds in programme wave 13 and the copy
        // stays a copy, documented as such in PartyRange).
        Location deathLoc = death.getEntity().getLocation();
        List<ParticipantCounter.Candidate> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location pl = p.getLocation();
            boolean sameWorld = pl.getWorld() != null && pl.getWorld().equals(deathLoc.getWorld());
            candidates.add(new ParticipantCounter.Candidate(
                    p.getUniqueId(), p.isOnline(), !p.isDead(),
                    p.getGameMode() == GameMode.SPECTATOR, sameWorld,
                    pl.getX() - deathLoc.getX(), pl.getY() - deathLoc.getY(), pl.getZ() - deathLoc.getZ()));
        }
        List<UUID> participants = ParticipantCounter.eligible(candidates, s.partyRadius(), s.participantCap());
        double multiplier = MultiplierMath.multiplier(participants.size(), s.stepPerExtra(), s.multiplierCap());
        if (multiplier <= 1.0) {
            if (s.debug()) {
                module.logger().info("party drops: elite died with " + participants.size()
                        + " participant(s) - multiplier 1.0, drops untouched");
            }
            return;
        }

        // --- scale the vanilla drop list (spec section 4): per-stack probabilistic rounding.
        MultiplierMath.RandomSource rng = () -> module.random().nextDouble();
        long before = 0L;
        long after = 0L;
        for (ItemStack stack : drops) {
            if (stack == null) continue;
            int amount = stack.getAmount();
            if (amount <= 0) continue;
            before += amount;
            int scaled = MultiplierMath.scaledAmount(amount, multiplier, rng);
            if (scaled != amount) stack.setAmount(scaled);
            after += scaled;
        }

        // --- audit (spec section 7): one console line per multiplied kill.
        module.logger().info("multiplied elite drops: type=" + death.getEntityType()
                + " level=" + elite.getLevel()
                + " participants=" + participants.size()
                + " multiplier=" + trim(multiplier)
                + " items " + before + " -> " + after);

        // --- the quiet hint (spec section 7): participants only, cooldown-gated.
        long now = System.currentTimeMillis();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            if (module.hintGate().tryShow(uuid, now)) {
                p.sendActionBar(Component.text(HINT_TEXT, NamedTextColor.GRAY));
            }
        }
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
