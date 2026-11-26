package excelemfixes.mcmmo;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

/**
 * Recognition of the entities that must never be a source of
 * mcMMO XP in either direction — the same set the mcMMO fork's
 * Layer-2 fix recognises (Nov 15, 2026), re-implemented here
 * because kill AWARDS are this module's own channel: a
 * recognised entity is excluded from awards before pricing,
 * exactly as the fork excludes it from per-damage pricing.
 * (Per-damage gains from these entities are already zeroed by
 * the fork itself; this class exists for the award path and
 * for deciding where hit tokens are pointless.)
 *
 * <p>Every check is a POSITIVE identification and every
 * failure mode is OPEN (not recognised), mirroring the fork:
 * <ul>
 *   <li>persistent-data stamps written by the owning plugins
 *       (ExcelTrainingDummy, ExcelRewardGuard's denial stamp,
 *       Magic's spawn/manage stamps);</li>
 *   <li>ExcelSummons registry membership, reached reflectively
 *       (the fork's pattern — this module compiles and runs
 *       identically without ExcelSummons);</li>
 *   <li>EliteMobs training-dummy boss filenames, via the
 *       {@link ElitePort} identity.</li>
 * </ul>
 *
 * <p>Also answers the Taming scoping question — whether a
 * wolf is a CAST summon (earns its owner nothing) versus an
 * adopted pet (earns normally) — by reading ExcelSummons'
 * summon record origin reflectively, exactly as the fork does.
 */
public final class NoXpRecognition {

    private static final NamespacedKey EXCEL_TRAINING_DUMMY_KEY =
            new NamespacedKey("exceltrainingdummy", "training");
    private static final NamespacedKey REWARD_GUARD_DENIED_KEY =
            new NamespacedKey("excelrewardguard", "denied_v1");
    private static final NamespacedKey MAGIC_SPAWNED_KEY =
            new NamespacedKey("magic", "magicspawned");
    private static final NamespacedKey MAGIC_MOB_KEY =
            new NamespacedKey("magic", "magicmob");

    private static final String SUMMONS_PLUGIN = "ExcelSummons";
    private static final String SUMMONS_API =
            "com.winkreyes.excelsummons.ExcelSummonsAPI";

    private final Logger logger;
    private final AtomicBoolean warned = new AtomicBoolean();

    // Probe states: 0 = untried, 1 = hooked, 2 = broken (fail open).
    // Plugin absence is never cached.
    private volatile int summonsProbeState = 0;
    private volatile Method isSummonMethod;
    private volatile Method getRecordMethod;

    public NoXpRecognition(Logger logger) {
        this.logger = logger;
    }

    /** True when the entity is positively recognised as no-XP. */
    public boolean isNoXpEntity(Entity entity, ElitePort.EliteInfo info) {
        if (entity == null) return false;
        if (hasNoXpStamp(entity)) return true;
        if (isSummon(entity)) return true;
        return info != null && info.customBoss()
                && DummyFiles.isTrainingDummyFilename(info.filename());
    }

    /** True when the entity is a registered ExcelSummons summon. */
    public boolean isSummon(Entity entity) {
        if (entity == null || !pluginEnabled(SUMMONS_PLUGIN)) return false;
        Method method = resolveIsSummon();
        if (method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(null, entity));
        } catch (Throwable t) {
            // Stale handle (plugin reloaded): drop it, fail open.
            isSummonMethod = null;
            summonsProbeState = 0;
            return false;
        }
    }

    /**
     * True only when the entity is positively a CAST summon
     * (a Magic cast the ExcelSummons bridge captured). Adopted
     * pets, unregistered wolves, and every probe failure answer
     * false — the Nov 15 scoping ruling blocks only positively
     * identified cast summons from earning their owner Taming.
     */
    public boolean isCastSummon(Entity entity) {
        if (entity == null || !pluginEnabled(SUMMONS_PLUGIN)) return false;
        Method method = resolveGetRecord();
        if (method == null) return false;
        try {
            Object record = method.invoke(null, entity);
            if (record == null) return false;
            Method origin = record.getClass().getMethod("origin");
            Object value = origin.invoke(record);
            return value != null && "CAST".equals(
                    ((Enum<?>) value).name());
        } catch (Throwable t) {
            getRecordMethod = null;
            summonsProbeState = 0;
            return false;
        }
    }

    private boolean hasNoXpStamp(Entity entity) {
        try {
            PersistentDataContainer container =
                    entity.getPersistentDataContainer();
            return container.has(EXCEL_TRAINING_DUMMY_KEY)
                    || container.has(REWARD_GUARD_DENIED_KEY)
                    || container.has(MAGIC_SPAWNED_KEY)
                    || container.has(MAGIC_MOB_KEY);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean pluginEnabled(String name) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            return plugin != null && plugin.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    private Method resolveIsSummon() {
        if (summonsProbeState == 2) return null;
        if (summonsProbeState == 1 && isSummonMethod != null) {
            return isSummonMethod;
        }
        synchronized (this) {
            try {
                Class<?> api = Class.forName(SUMMONS_API);
                isSummonMethod = api.getMethod("isSummon", Entity.class);
                getRecordMethod = api.getMethod("getRecord", Entity.class);
                summonsProbeState = 1;
                return isSummonMethod;
            } catch (Throwable t) {
                summonsProbeState = 2;
                warnOnce(t);
                return null;
            }
        }
    }

    private Method resolveGetRecord() {
        if (summonsProbeState == 2) return null;
        if (summonsProbeState == 1 && getRecordMethod != null) {
            return getRecordMethod;
        }
        resolveIsSummon();
        return getRecordMethod;
    }

    private void warnOnce(Throwable t) {
        if (warned.compareAndSet(false, true)) {
            logger.warning("mcMMO XP Connector: ExcelSummons is present "
                    + "but its API could not be resolved ("
                    + t.getClass().getSimpleName()
                    + "); summons will be treated as ordinary entities.");
        }
    }
}
