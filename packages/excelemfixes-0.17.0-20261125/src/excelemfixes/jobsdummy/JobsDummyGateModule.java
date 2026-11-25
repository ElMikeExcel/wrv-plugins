package excelemfixes.jobsdummy;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

/**
 * Jobs Dummy Gate module: EliteMobs' Adventurers Guild training
 * dummies pay nothing in Jobs.
 *
 * <p>The defect: the guild training dummies are EliteMobs custom
 * bosses whose entity type is HUSK, and they are regional bosses
 * — they respawn on the spot, forever. Jobs pays its Kill
 * actions by entity type, so every dummy death pays the killer
 * the full husk rate in money and Hunter XP, and a player can
 * stand at the guild and farm a "training" target indefinitely
 * (observed live: three kills in ten seconds, full pay each).
 * EliteMobs paying nothing itself does not help — Jobs pays on
 * the death event regardless of EliteMobs' drop flags.
 *
 * <p>The fix: a veto on Jobs' own payment events, keyed on
 * EliteMobs' identity for the entity (its boss filename, read
 * through EliteMobs' own tagger) — never on entity type, so a
 * real husk anywhere else keeps paying normally. The boss list
 * is a config knob; the shipped default is EliteMobs' own five
 * guild training dummies.
 *
 * <p>Jobs is a softdepend: with no Jobs plugin installed there
 * is nothing to veto, and the module idles without loading any
 * Jobs class.
 */
public final class JobsDummyGateModule implements FixModule {

    private ModuleContext ctx;
    private JobsDummyGate gate;
    private final AtomicReference<DummyBossList> list =
            new AtomicReference<>(DummyBossList.parse(null));
    private volatile boolean debug;

    @Override
    public String id() {
        return "jobs-dummy-gate";
    }

    @Override
    public String displayName() {
        return "Jobs Dummy Gate";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        load();
        if (Bukkit.getPluginManager().getPlugin("Jobs") == null) {
            ctx.logger().info("Jobs Dummy Gate: Jobs plugin not found — module idles "
                    + "(nothing to veto). The boss list still applies the moment "
                    + "Jobs is installed.");
            return;
        }
        gate = new JobsDummyGate(list::get, () -> debug, ctx.logger());
        Bukkit.getPluginManager().registerEvents(gate, ctx.plugin());
        ctx.logger().info("Jobs Dummy Gate ready — Jobs payments vetoed for "
                + list.get().size() + " listed boss file(s): " + list.get().ids());
        if (list.get().isEmpty()) {
            ctx.logger().info("Jobs Dummy Gate: boss-files list is empty — the gate "
                    + "vetoes nothing until bosses are listed.");
        }
    }

    @Override
    public void onDisable() {
        if (gate != null) {
            HandlerList.unregisterAll(gate);
            gate = null;
        }
    }

    @Override
    public void reload() {
        load();
        ctx.logger().info("Jobs Dummy Gate reloaded — " + list.get().size()
                + " listed boss file(s): " + list.get().ids());
    }

    private void load() {
        ConfigurationSection section = ctx.moduleConfig();
        debug = section != null && section.getBoolean("debug", false);
        list.set(DummyBossList.parse(
                section == null ? null : section.getList("boss-files")));
    }
}
