package com.winkreyes.excelskillspanel.provider.mcmmo;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.config.RankConfig;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.winkreyes.excelskillspanel.core.PanelModel;
import com.winkreyes.excelskillspanel.core.PerkLadder;
import com.winkreyes.excelskillspanel.core.SkillMath;
import com.winkreyes.excelskillspanel.provider.SkillsProvider;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provider #1 — mcMMO (the commercial core). Maps the 19
 * PrimarySkillTypes to panel rows through mcMMO's PUBLIC API
 * (com.gmail.nossr50.api.ExperienceAPI) behind a softdepend: no
 * mcMMO code is copied into this plugin, so mcMMO updates carry
 * the panel. This class is only ever loaded when the mcMMO
 * plugin is present and enabled (registration is guarded in the
 * plugin's onEnable).
 *
 * Read semantics, verified against the WRV fork's ExperienceAPI
 * source: getLevel(Player, PrimarySkillType); getXP(Player,
 * String) = XP into the current level; getXPToNextLevel(Player,
 * String) = the level's TOTAL XP span; getPowerLevel(Player);
 * getPowerLevelCap(). The String forms resolve the skill by its
 * display name (SkillTools.matchSkill).
 *
 * The optional rank label comes from mcMMO's own PlaceholderAPI
 * surface (%mcmmo_rank_&lt;skill&gt;%, DESIGN §7.2) read through
 * the WRV provider package's PAPI reflection helper; absent PAPI,
 * the row simply carries no rank — never an invented one.
 *
 * 0.2.0 (Item 8, D32): each skill's detail view carries its
 * perk ladder — but only after LIVE verification. For each
 * skill the provider re-reads every perk's rank count and
 * unlock levels from the fork's own surfaces
 * (SubSkillType.getNumRanks + RankConfig.getSubSkillUnlockLevel,
 * which reads the live skillranks.yml in the server's current
 * mode) and compares them against the verified embedded table
 * (PerkLadder.matchesLive). A match renders the ladder at the
 * player's level (the DERIVED level for the child skills,
 * exactly the level ExperienceAPI.getLevel returns for them);
 * any mismatch or failed read fails soft to the standing
 * website pointer line for that skill alone, with a WARNING —
 * an unverified ladder is never rendered.
 *
 * A skill whose read throws is skipped with a WARNING (logged,
 * never silent); the remaining skills still render.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class McMMoProvider implements SkillsProvider {

    /** The 19 primary skills in PrimarySkillType declaration
     *  order, with display names and Java icon material names
     *  (resolved by name at render time; an unknown name falls
     *  back to NAME_TAG in the presentation). */
    public record SkillDef(PrimarySkillType type, String displayName, String iconKey) {}

    public static final List<SkillDef> SKILLS = List.of(
            new SkillDef(PrimarySkillType.ACROBATICS, "Acrobatics", "FEATHER"),
            new SkillDef(PrimarySkillType.ALCHEMY, "Alchemy", "BREWING_STAND"),
            new SkillDef(PrimarySkillType.ARCHERY, "Archery", "BOW"),
            new SkillDef(PrimarySkillType.AXES, "Axes", "IRON_AXE"),
            new SkillDef(PrimarySkillType.CROSSBOWS, "Crossbows", "CROSSBOW"),
            new SkillDef(PrimarySkillType.EXCAVATION, "Excavation", "IRON_SHOVEL"),
            new SkillDef(PrimarySkillType.FISHING, "Fishing", "FISHING_ROD"),
            new SkillDef(PrimarySkillType.HERBALISM, "Herbalism", "WHEAT"),
            new SkillDef(PrimarySkillType.MACES, "Maces", "MACE"),
            new SkillDef(PrimarySkillType.MINING, "Mining", "IRON_PICKAXE"),
            new SkillDef(PrimarySkillType.REPAIR, "Repair", "ANVIL"),
            new SkillDef(PrimarySkillType.SALVAGE, "Salvage", "GOLD_INGOT"),
            new SkillDef(PrimarySkillType.SMELTING, "Smelting", "FURNACE"),
            new SkillDef(PrimarySkillType.SPEARS, "Spears", "IRON_SPEAR"),
            new SkillDef(PrimarySkillType.SWORDS, "Swords", "IRON_SWORD"),
            new SkillDef(PrimarySkillType.TAMING, "Taming", "BONE"),
            new SkillDef(PrimarySkillType.TRIDENTS, "Tridents", "TRIDENT"),
            new SkillDef(PrimarySkillType.UNARMED, "Unarmed", "LEATHER_CHESTPLATE"),
            new SkillDef(PrimarySkillType.WOODCUTTING, "Woodcutting", "OAK_LOG"));

    private final Logger log;

    public McMMoProvider(Logger log) {
        this.log = log;
    }

    @Override
    public String id() { return "mcmmo"; }

    @Override
    public ProviderData collect(Player player) {
        List<PanelModel.SkillRow> rows = new ArrayList<>();
        for (SkillDef def : SKILLS) {
            try {
                int level = ExperienceAPI.getLevel(player, def.type());
                String rank = com.winkreyes.excelskillspanel.provider.papi.PapiReads.get(
                        player, "%mcmmo_rank_" + def.type().name().toLowerCase() + "%");
                String skillId = def.type().name().toLowerCase();
                List<String> detailTail = ladderTail(skillId, def.displayName(), level);
                if (def.type().isChildSkill()) {
                    // Child skills (Salvage, Smelting) have a level
                    // but no XP: the XP reads throw
                    // UnsupportedOperationException for them
                    // (rig-proven). Level-only row, no bar. The
                    // ladder's earned state uses this same level —
                    // for a child skill getLevel returns the
                    // DERIVED level (LADDER-DATA §4).
                    rows.add(SkillMath.childRow(skillId,
                            def.displayName(), def.iconKey(), level, rank, detailTail));
                    continue;
                }
                int xpInto = ExperienceAPI.getXP(player, def.displayName());
                int xpSpan = ExperienceAPI.getXPToNextLevel(player, def.displayName());
                rows.add(SkillMath.mcmmoRow(skillId,
                        def.displayName(), def.iconKey(), level, xpInto, xpSpan, rank,
                        detailTail));
            } catch (Throwable t) {
                log.warning("ExcelSkillsPanel: mcMMO read failed for " + def.displayName()
                        + " (" + player.getName() + "): " + t.getClass().getSimpleName()
                        + ": " + t.getMessage() + " — row omitted, never faked");
            }
        }
        List<PanelModel.HeaderBlock> headers = new ArrayList<>();
        try {
            headers.add(SkillMath.powerHeader(ExperienceAPI.getPowerLevel(player),
                    ExperienceAPI.getPowerLevelCap()));
        } catch (Throwable t) {
            log.warning("ExcelSkillsPanel: mcMMO Power Level read failed for "
                    + player.getName() + ": " + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + " — header block omitted");
        }
        return new ProviderData(headers, rows, List.of());
    }

    /** The detail tail for one skill: its verified ladder lines
     *  at {@code level} when the live mcMMO data matches the
     *  verified table exactly, otherwise the pointer line. */
    private List<String> ladderTail(String skillId, String displayName, int level) {
        List<PerkLadder.Perk> perks = PerkLadder.perksFor(skillId);
        if (perks.isEmpty()) return List.of(SkillMath.PERK_POINTER);
        try {
            Map<String, int[]> live = new HashMap<>();
            RankConfig rankConfig = RankConfig.getInstance();
            for (PerkLadder.Perk perk : perks) {
                if (perk.subSkillEnum() == null) continue; // Graceful Roll: facet of Roll
                SubSkillType subSkill = SubSkillType.valueOf(perk.subSkillEnum());
                int numRanks = subSkill.getNumRanks();
                int[] unlocks = new int[numRanks];
                for (int rank = 1; rank <= numRanks; rank++) {
                    // The two-arg form reads the live skillranks.yml
                    // in the server's CURRENT mode — a mode change
                    // or a drifted file therefore mismatches the
                    // verified Retro table and fails soft below.
                    unlocks[rank - 1] = rankConfig.getSubSkillUnlockLevel(subSkill, rank);
                }
                live.put(perk.subSkillEnum(), unlocks);
            }
            if (PerkLadder.matchesLive(skillId, live)) {
                return PerkLadder.ladderLines(skillId, level);
            }
            log.warning("ExcelSkillsPanel: live mcMMO ladder for " + displayName
                    + " does not match the verified table — detail keeps the"
                    + " website pointer for this skill, ladder not rendered");
        } catch (Throwable t) {
            log.warning("ExcelSkillsPanel: live mcMMO ladder read failed for "
                    + displayName + ": " + t.getClass().getSimpleName() + ": "
                    + t.getMessage() + " — detail keeps the website pointer"
                    + " for this skill, ladder not rendered");
        }
        return List.of(SkillMath.PERK_POINTER);
    }
}
