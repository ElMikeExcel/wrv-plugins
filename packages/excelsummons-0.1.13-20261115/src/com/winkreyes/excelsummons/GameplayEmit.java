package com.winkreyes.excelsummons;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GameplayEvent field shapes (0.1.12, ExcelProtect Audit Watch
 * emitter adoption — the follow-on named by the ExcelProtect
 * 0.1.10 P-PROTECT-WATCH-1 build): the pure half of the emitter
 * door. Every map built here follows the GameplayEvent contract
 * in ExcelProtect's api/GameplayEvent (0.1.10) EXACTLY, mirrored
 * against Protect's consumer (AuditWatch.onGameplay):
 *
 *  - kind is one of summon_spawn / summon_despawn / mana_reserve /
 *    summon_refusal (spell_cast is Protect's own Magic feed, never
 *    emitted here);
 *  - fields MUST carry player + player_uuid — a record with no
 *    player has no per-player home and Protect drops + counts it,
 *    so the door refuses incomplete maps before firing (see
 *    complete());
 *  - Protect copies only String / Number / Boolean field values
 *    into the audit record, so every value here is one of those
 *    (counted names travel as ONE comma-joined String, never a
 *    List);
 *  - source is stamped "ExcelSummons" on every record (the
 *    contract asks emitters to name themselves; Protect would
 *    stamp source=emitter if it were absent);
 *  - summon is the spell family BASE key, lowercased — the same
 *    normalisation Protect's door sync applies
 *    (AuditReserve.baseKey of the lowercased key).
 *
 * Reason vocabularies are the contract's: despawn reasons are
 * DespawnNotice's enum in lowercase (expired / dismissed / died /
 * unloaded — the bridge's TRUE reasons, which Protect's observed
 * feed cannot see); refusal reasons are cap (any CapPolicy
 * refusal, with cap_kind naming the exact one), reserve_ceiling
 * and reserve_floor. A refusal record carries the same numbers
 * the refusal message showed the player.
 *
 * This class is pure (no Bukkit, no Protect types) so the shapes
 * are unit-tested off-server; the Bukkit/Protect glue is
 * GameplayDoor + protect/ProtectGameplayGateway.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GameplayEmit {

    private GameplayEmit() {}

    public static final String SOURCE = "ExcelSummons";
    public static final String KIND_SPAWN = "summon_spawn";
    public static final String KIND_DESPAWN = "summon_despawn";
    public static final String KIND_RESERVE = "mana_reserve";
    public static final String KIND_REFUSAL = "summon_refusal";

    /** The consumer's drop rule, mirrored: player + player_uuid
     *  must both be present, non-blank Strings. */
    public static boolean complete(Map<String, Object> fields) {
        if (fields == null) return false;
        Object p = fields.get("player");
        Object u = fields.get("player_uuid");
        return p instanceof String ps && !ps.isBlank()
                && u instanceof String us && !us.isBlank();
    }

    private static Map<String, Object> base(String player, UUID playerUuid) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("player", player);
        m.put("player_uuid", playerUuid == null ? null : playerUuid.toString());
        m.put("source", SOURCE);
        return m;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    // ------------------------------------------------------------ reasons

    /** DespawnNotice reason -> the contract's despawn reason. */
    public static String despawnReason(DespawnNotice.Reason reason) {
        if (reason == null) return "caster_left";
        return switch (reason) {
            case EXPIRED -> "expired";
            case DISMISSED -> "dismissed";
            case DIED -> "died";
            case UNLOADED -> "unloaded";
        };
    }

    /** CapPolicy refusal -> the cap_kind detail (the record's
     *  reason is always "cap" for these); null for NONE. */
    public static String capKind(CapPolicy.Refusal refusal) {
        if (refusal == null) return null;
        return switch (refusal) {
            case SPELL_CAP -> "spell_cap";
            case TOTAL_CAP -> "total_cap";
            case SERVER_CEILING -> "server_ceiling";
            case NONE -> null;
        };
    }

    /** ReservePolicy refusal -> the contract's refusal reason;
     *  null for NONE. */
    public static String reserveReason(ReservePolicy.Refusal refusal) {
        if (refusal == null) return null;
        return switch (refusal) {
            case RESERVE_CEILING -> "reserve_ceiling";
            case RESERVE_FLOOR -> "reserve_floor";
            case NONE -> null;
        };
    }

    // ------------------------------------------------------------ records

    /** summon_spawn: summon (base key), entity_uuid, entity_type,
     *  branch, origin (cast|adopted) — the contract's field set,
     *  plus expires_ms for timed summons (Protect's observed feed
     *  carries the same field). */
    public static Map<String, Object> spawnFields(String player, UUID playerUuid,
                                                  String summonBase, UUID entityUuid,
                                                  String entityType, String branch,
                                                  String origin, Long expiresMs) {
        Map<String, Object> m = base(player, playerUuid);
        m.put("summon", lower(summonBase));
        m.put("entity_uuid", entityUuid == null ? null : entityUuid.toString());
        m.put("entity_type", lower(entityType));
        m.put("branch", lower(branch));
        m.put("origin", lower(origin));
        if (expiresMs != null) m.put("expires_ms", expiresMs);
        return m;
    }

    /** summon_despawn: summon, entity_uuid, reason — plus
     *  entity_type / origin when the glue knows them and
     *  hold_percent, the applied hold the record carried (0 for
     *  adopted / clamp-skipped), released by this despawn. */
    public static Map<String, Object> despawnFields(String player, UUID playerUuid,
                                                    String summonBase, UUID entityUuid,
                                                    String entityType, String origin,
                                                    String reason, int holdPercent) {
        Map<String, Object> m = base(player, playerUuid);
        m.put("summon", lower(summonBase));
        m.put("entity_uuid", entityUuid == null ? null : entityUuid.toString());
        if (entityType != null) m.put("entity_type", lower(entityType));
        if (origin != null) m.put("origin", lower(origin));
        m.put("reason", reason);
        m.put("hold_percent", holdPercent);
        return m;
    }

    /** mana_reserve: the bridge's OWN bookkeeping numbers (the
     *  follow-on's ask — Protect's derived lines are its replay;
     *  these are Summons' applied state at the spawn/despawn
     *  moment): this summon's applied hold_percent, the owner's
     *  applied held_total_percent, held_total in mana units
     *  (percent of mana_max, rounded), and the mage's live pool
     *  numbers — usable_mana and effective_mana_max are both the
     *  mage's effective max, exactly as Protect stamps them. */
    public static Map<String, Object> reserveFields(String player, UUID playerUuid,
                                                    String summonBase, int holdPercent,
                                                    int heldTotalPercent, int heldTotal,
                                                    int usableMana, int manaMax,
                                                    int effectiveManaMax) {
        Map<String, Object> m = base(player, playerUuid);
        m.put("summon", lower(summonBase));
        m.put("hold_percent", holdPercent);
        m.put("held_total_percent", heldTotalPercent);
        m.put("held_total", heldTotal);
        m.put("usable_mana", usableMana);
        m.put("mana_max", manaMax);
        m.put("effective_mana_max", effectiveManaMax);
        return m;
    }

    /** summon_refusal for a CapPolicy refusal: reason=cap with
     *  cap_kind naming the exact cap, plus the same numbers the
     *  refusal message showed the player (counted / cap / rank /
     *  counted names for the total cap; count / cap for the spell
     *  cap; the ceiling for the server ceiling) and the message
     *  text itself. stage is "prec_cast" or "capture_backstop". */
    public static Map<String, Object> capRefusalFields(String player, UUID playerUuid,
                                                       String summonBase, String spellKey,
                                                       String capKind, String rank,
                                                       Integer counted, Integer cap,
                                                       List<String> countedNames,
                                                       Integer serverCeiling,
                                                       String stage, String message) {
        Map<String, Object> m = base(player, playerUuid);
        m.put("summon", lower(summonBase));
        if (spellKey != null) m.put("spell", spellKey);
        m.put("reason", "cap");
        m.put("cap_kind", capKind);
        if (rank != null) m.put("rank", rank);
        if (counted != null) m.put("counted", counted);
        if (cap != null) m.put("cap", cap);
        if (countedNames != null && !countedNames.isEmpty()) {
            m.put("counted_names", String.join(", ", countedNames));
        }
        if (serverCeiling != null) m.put("server_ceiling", serverCeiling);
        if (stage != null) m.put("stage", stage);
        if (message != null && !message.isEmpty()) m.put("message", message);
        return m;
    }

    /** summon_refusal for a ReservePolicy refusal: reason is
     *  reserve_ceiling / reserve_floor, with the live numbers the
     *  message was built from (this spell's percent, the applied
     *  held percent, ceiling, floor) and the mage's live pool
     *  numbers at refusal time, plus the message text itself. */
    public static Map<String, Object> reserveRefusalFields(String player, UUID playerUuid,
                                                           String summonBase, String spellKey,
                                                           String reason, int spellPercent,
                                                           int heldPercent, int ceilingPercent,
                                                           int floorPercent,
                                                           int effectiveManaMax, int manaMax,
                                                           String message) {
        Map<String, Object> m = base(player, playerUuid);
        m.put("summon", lower(summonBase));
        if (spellKey != null) m.put("spell", spellKey);
        m.put("reason", reason);
        m.put("hold_percent", spellPercent);
        m.put("held_total_percent", heldPercent);
        m.put("ceiling_percent", ceilingPercent);
        m.put("floor_percent", floorPercent);
        m.put("effective_mana_max", effectiveManaMax);
        m.put("mana_max", manaMax);
        m.put("stage", "prec_cast");
        if (message != null && !message.isEmpty()) m.put("message", message);
        return m;
    }
}
