package excelemfixes.skillnudge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.SkillType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The ONLY skill-nudge class that touches EliteMobs types —
 * the same read path EliteMobs' own Skills page uses:
 * PlayerData's public static getters over EM's in-memory
 * player data (which EM itself loaded from its SQLite store,
 * plugins/EliteMobs/data/player_data.db, PlayerData table:
 * SkillXP_<TYPE> columns + the SkillBonusSelections JSON
 * string). Strictly read-only: no setter is ever called, no
 * file is opened, and every read is guarded so an EM hiccup
 * degrades to "no data" (and therefore no nudge) instead of
 * an error in a player's face.
 *
 * Skill LEVEL comes from EM itself (PlayerData.getSkillLevel,
 * backed by SkillXPCalculator), never from a reimplemented XP
 * curve — if MagmaGuy retunes the curve, the nudge stays
 * correct on its own.
 */
public final class EmSkillData {

    private EmSkillData() {}

    /** Skill type name -> EM-computed skill level. Empty when EM has no data for the player. */
    public static Map<String, Integer> readLevels(UUID uuid, Logger log) {
        Map<String, Integer> out = new LinkedHashMap<>();
        try {
            if (PlayerData.getPlayerData(uuid) == null) return out;
            for (SkillType type : SkillType.values()) {
                out.put(type.name(), PlayerData.getSkillLevel(uuid, type));
            }
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Skill Bonus Nudge: EM level read failed for " + uuid + ": " + t);
            }
            return Map.of();
        }
        return out;
    }

    /**
     * Skill type name -> number of selected bonuses, parsed
     * from EM's SkillBonusSelections JSON ({"ARMOR":["armor_evasion",...]}).
     * Returns null when the stored string cannot be parsed —
     * the caller treats null as "unknown", never as "empty",
     * so a format change in EM silences the nudge instead of
     * crying wolf at players who did claim their bonuses.
     */
    public static Map<String, Integer> readSelectedCounts(UUID uuid, Logger log) {
        String raw;
        try {
            raw = PlayerData.getSkillBonusSelections(uuid);
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Skill Bonus Nudge: EM selections read failed for " + uuid + ": " + t);
            }
            return null;
        }
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) return null;
            JsonObject obj = parsed.getAsJsonObject();
            Map<String, Integer> out = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (e.getValue().isJsonArray()) {
                    out.put(e.getKey().toUpperCase(), e.getValue().getAsJsonArray().size());
                } else {
                    return null; // unexpected shape — treat the whole read as unknown
                }
            }
            return out;
        } catch (Throwable t) {
            if (log != null) {
                log.warning("Skill Bonus Nudge: SkillBonusSelections parse failed for "
                        + uuid + ": " + t);
            }
            return null;
        }
    }
}
