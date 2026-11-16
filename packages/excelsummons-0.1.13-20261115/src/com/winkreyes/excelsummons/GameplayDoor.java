package com.winkreyes.excelsummons;

import com.winkreyes.excelsummons.protect.ProtectGameplayGateway;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The emitter door's holder (0.1.12): owns the bound GameplaySink
 * and the binding rule. Unbound (ExcelProtect absent or not yet
 * enabled) the sink is NOOP and every emit is a silent no-op —
 * Summons never depends on Protect being there. Binding is tried
 * at Summons enable and again whenever the ExcelProtect plugin
 * enables later (SummonsListener's PluginEnableEvent handler),
 * because neither plugin hard-depends on the other and enable
 * order is not guaranteed.
 *
 * emit() enforces the consumer's completeness rule BEFORE firing
 * (GameplayEmit.complete): a record with no player name has no
 * per-player home in Protect's audit log and would only be
 * dropped + counted there, so it is not fired at all.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GameplayDoor {

    private volatile GameplaySink sink = GameplaySink.NOOP;
    private volatile boolean bound;

    /** Bind when ExcelProtect is present + enabled. Idempotent;
     *  returns true when the door is bound after the call. */
    public boolean tryBind(Logger log) {
        if (bound) return true;
        Plugin protect = Bukkit.getPluginManager().getPlugin("ExcelProtect");
        if (protect == null || !protect.isEnabled()) return false;
        try {
            sink = new ProtectGameplayGateway(log);
            bound = true;
            log.info("ExcelSummons: Protect audit door bound — summon refusals, "
                    + "spawns, despawns and mana-reserve now record to the "
                    + "per-player audit log (ExcelProtect GameplayEvent).");
            return true;
        } catch (Throwable t) {
            log.warning("ExcelSummons: Protect audit door did not bind (" + t
                    + ") — gameplay records stay local; summons are unaffected.");
            return false;
        }
    }

    public boolean bound() { return bound; }

    /** Fire one gameplay fact, when the door is bound and the
     *  field map is complete (player + player_uuid present). */
    public void emit(String kind, Map<String, Object> fields) {
        if (!bound) return;
        if (!GameplayEmit.complete(fields)) return;
        sink.emit(kind, fields);
    }
}
