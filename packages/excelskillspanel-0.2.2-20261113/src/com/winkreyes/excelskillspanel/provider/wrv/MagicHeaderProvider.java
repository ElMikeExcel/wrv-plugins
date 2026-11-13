package com.winkreyes.excelskillspanel.provider.wrv;

import com.winkreyes.excelskillspanel.core.MagicLines;
import com.winkreyes.excelskillspanel.core.PanelModel;
import com.winkreyes.excelskillspanel.provider.SkillsProvider;
import com.winkreyes.excelskillspanel.provider.papi.PapiReads;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Provider #2 — the WRV-only Magic module (DESIGN §7.3: excluded
 * from any commercial build). Supplies the FULL Magic header
 * (Mike's Q2 ruling) from verified reads only:
 *
 * - Magic Level + XP bar + total XP + mana-pool boost %: the
 *   ExcelMagicLevel PlaceholderAPI surface
 *   (%excelmagiclevel_level/_xp/_next/_total_xp/_mana_boost%).
 * - Rank name/key, next rank, absolute mana pool and regen:
 *   the Magic API chain in MagicApiReads (verified run 273,
 *   rig-proven — see RIG-RESULTS.md; 0.2.2 resolves the wand
 *   from the player's carried items, DIAGNOSIS.md). The API
 *   slice carries the wand's FRAME max/regen; this provider
 *   raises them to the EFFECTIVE pool with the level boosts
 *   (MagicLines.effectiveManaMax/effectiveManaRegen) — the live
 *   wand's mana is synced to that pool, so the frame alone
 *   understates it. The no-wand mage reads are already
 *   effective and are never boosted twice (wandFrame gate).
 * - Regen / cast-cost boost %: ExcelMagicLevel's reward-table
 *   formula over its live config fractions (MagicLevelStoreReads).
 * - Credited casts: MagicGate's ledger (MagicGateReads) — the
 *   rank-driving count. ExcelMagicLevel's totalCasts is a
 *   different counter and is never read again (0.2.2, Item 13).
 * - "Guild seals: N of 5": the GuildGate reflective read.
 *
 * 0.2.2 (Item 12): the "Class: Mage" line is gone — the field
 * no longer exists in the DTO.
 *
 * Composition is pure (MagicLines): every field that fails to
 * read is omitted from the header — the panel never fakes a
 * field. Failures log at WARNING (never silent).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicHeaderProvider implements SkillsProvider {

    private final Logger log;

    public MagicHeaderProvider(Logger log) {
        this.log = log;
    }

    @Override
    public String id() { return "magic"; }

    @Override
    public ProviderData collect(Player player) {
        // --- ExcelMagicLevel PAPI surface ---
        Integer level = PapiReads.getInt(player, "%excelmagiclevel_level%");
        Long xpInto = PapiReads.getLong(player, "%excelmagiclevel_xp%");
        Long xpToNext = PapiReads.getLong(player, "%excelmagiclevel_next%");
        Long totalXp = PapiReads.getLong(player, "%excelmagiclevel_total_xp%");
        String manaBoost = PapiReads.get(player, "%excelmagiclevel_mana_boost%");
        if (level == null && pluginPresent("ExcelMagicLevel")) {
            log.warning("ExcelSkillsPanel: ExcelMagicLevel is enabled but its "
                    + "PlaceholderAPI surface returned no level for " + player.getName()
                    + " — Magic Level lines omitted (check PlaceholderAPI)");
        }

        // --- Magic API chain (rank + wand-frame mana) ---
        String rankName = null, rankKey = null, nextRankName = null;
        Float mana = null;
        Integer manaMax = null, manaRegen = null;
        boolean wandFrame = false;
        java.util.List<String> wandLore = null;
        if (pluginPresent("Magic")) {
            try {
                MagicApiReads.MagicApiData api = MagicApiReads.read(player);
                if (api != null) {
                    rankName = api.rankName();
                    rankKey = api.rankKey();
                    nextRankName = api.nextRankName();
                    mana = api.mana();
                    manaMax = api.manaMax();
                    manaRegen = api.manaRegen();
                    wandFrame = api.wandFrame();
                    wandLore = api.wandLore();
                } else {
                    log.warning("ExcelSkillsPanel: Magic is enabled but no mage read "
                            + "for " + player.getName() + " — rank/mana lines omitted");
                }
            } catch (Throwable t) {
                log.warning("ExcelSkillsPanel: Magic API read failed for " + player.getName()
                        + ": " + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + " — rank/mana lines omitted, never faked");
            }
        }

        // --- regen / cost boost % via the reward-table formula ---
        String regenBoost = null, costReduction = null;
        double regenBoostFraction = 0.0;
        if (level != null && level >= 2) {
            double[] perLevel = MagicLevelStoreReads.perLevelBoosts();
            if (perLevel != null) {
                regenBoostFraction = perLevel[0] * (level - 1);
                regenBoost = MagicLines.percentOf(regenBoostFraction);
                costReduction = MagicLines.percentOf(perLevel[1] * (level - 1));
            }
        }

        // --- effective mana pool (wand frame + level boosts) ---
        // Only when the numbers came from a wand frame: the
        // no-wand mage reads are already effective and must NOT
        // be boosted a second time (wandFrame gate). Current mana
        // is clamped to the effective max so the line never reads
        // over-full. At Mike's level 26 this is a no-op (150 ->
        // 150, 25 -> 25); at rig level 136 it is 150 -> 154.
        if (wandFrame) {
            if (manaMax != null) manaMax = MagicLines.effectiveManaMax(manaMax, manaBoost);
            if (manaRegen != null && regenBoostFraction > 0) {
                manaRegen = MagicLines.effectiveManaRegen(manaRegen, regenBoostFraction);
            }
            if (mana != null && manaMax != null && mana > manaMax) mana = manaMax.floatValue();
        }

        Long casts = MagicGateReads.creditedCasts(player.getUniqueId(), log, wandLore);
        Integer seals = GuildSealsReads.sealsBrokenCount();

        MagicLines.MagicData data = new MagicLines.MagicData(
                level, xpInto, xpToNext, totalXp,
                rankName, rankKey, nextRankName,
                mana, manaMax, manaRegen,
                manaBoost, regenBoost, costReduction,
                casts, seals);
        PanelModel.HeaderBlock block = MagicLines.headerBlock(data);
        if (block == null) return ProviderData.empty();
        return new ProviderData(List.of(block), List.of(), MagicLines.detailLines(data));
    }

    private static boolean pluginPresent(String name) {
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        return p != null && p.isEnabled();
    }
}
