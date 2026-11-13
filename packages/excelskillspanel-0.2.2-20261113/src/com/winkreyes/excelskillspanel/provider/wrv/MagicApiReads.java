package com.winkreyes.excelskillspanel.provider.wrv;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageClass;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.ProgressionPath;
import com.elmakers.mine.bukkit.api.wand.Wand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * The Magic API reads behind the FULL Magic header (Mike's Q2
 * ruling). Call chain verified desk-side in run 273 against the
 * real Magic-11.2.5.jar.
 *
 * 0.2.2 (menu programme Item 13 — Mike, Nov 13, 2026, ~12:38 PM
 * CST): the 0.2.1 read trusted exactly ONE wand source,
 * Mage.getActiveWand(), and fell back to the class base path on
 * null. The live panel then showed Rank: Beginner / Mana 60/60 /
 * regen 5 while Mike's Renewal wand reads Apprentice / 150 / 25.
 * Root cause, proven from the Magic 11.2.5 bytecode
 * (DIAGNOSIS.md): getActiveWand() returns a CACHED field that
 * Magic only refreshes from the player's hand on its own tick
 * (Mage.tick -> checkWand -> checkMainhandWand). The live 60/60
 * is the BEGINNER bound wand ACTIVE (frame 60 x 1.0059 -> 60) —
 * the class fallback would have read 50 at level 26 (the
 * beginner PATH's mana_max is 50; the rig's no-wand chain reads
 * effMax 51 at level 136 = trunc(50 x 1.0321)), so the live
 * trigger is the stale beginner wand being the cached active
 * wand, not the no-wand state. The not-held class fallback is a
 * second, real failure mode (Beginner / 50 / 5) that the 0.2.1
 * rig never exercised (it equipped the wand and let ticks pass
 * before every read) — which is why it passed while live failed.
 * Either way the 0.2.1 panel reported whatever the cached field
 * happened to hold as if it were the player's rank.
 *
 * The 0.2.2 resolution therefore reads the wand from the
 * player's CARRIED items, in this order — the panel shows the
 * player's rank whenever they own the wand, not only in the tick
 * window when it happens to be active:
 *   1. the held main-hand item, via controller.getIfWand
 *      (ground truth at open time; getIfWand ONLY — getWand
 *      wand-ifies ANY item into a phantom wand, per MagicGate's
 *      own rig-caught note, and is never used here);
 *   2. the cached active wand, but only while its live item is
 *      still carried in the player's inventory;
 *   3. the offhand item, same ground-truth read, else the
 *      cached offhand wand;
 *   4. an inventory scan: every carried item through
 *      getIfWand; when several real wands are carried, the one
 *      whose path ranks HIGHEST (master > apprentice > student
 *      > beginner) — never first-found, never the lowest. This
 *      is the step that fixes Mike's case with no wand in hand,
 *      and it guards the two-bound-wands registry state
 *      (DIAGNOSIS.md §2) deterministically;
 *   5. the mage's bound wands (Magic's own registry — a wand
 *      pouched or ender-chested is still the player's wand),
 *      highest path first;
 *   6. the class path, LAST — only when the player owns no wand
 *      anywhere. A wand-less mage's rank IS the class base;
 *      that fallback is honest there and only there.
 *
 * Rank and next rank come from the WRV rank order
 * (beginner -> student -> apprentice -> master), NEVER from
 * Magic's path.getNextPath(): Magic's own display name for the
 * master path is "Wizard" (messages.defaults.yml, paths.master.
 * name '&6Wizard'; "Survival Progression: ... Apprentice,
 * Wizard (AKA Master)"), so getNextPath() on an apprentice wand
 * returns a path named Wizard — the 0.2.1 read would have shown
 * "Next rank: Wizard" even with the wand held. The WRV ladder
 * has exactly four ranks (MagicGate RankFloor) and its wand lore
 * writes "Next: Master"; the panel matches the lore.
 * Mana: the current value is the resolved wand's live mana
 * (Magic syncs it to the effective pool); manaMax/manaRegen are
 * the wand's FRAME — the provider (MagicHeaderProvider) applies
 * the level boosts to reach the effective pool the player
 * actually has. The mage-effective reads survive ONLY as the
 * no-wand fallback (step 6's class standing), already effective
 * there, so the provider must NOT boost them a second time —
 * wandFrame says which source the numbers came from.
 *
 * Names arrive with legacy color codes embedded
 * ("§aApprentice"); the panel strips them — the name as
 * displayed, without formatting bytes that the Java chest's
 * component text would print literally.
 *
 * Every value that cannot be read stays null — the composer
 * omits the line; nothing is ever substituted. This class
 * references Magic API types directly and is only loaded when
 * the Magic plugin is present (guarded by the provider, which
 * catches linkage failures as a logged omission).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicApiReads {
    private MagicApiReads() {}

    /** The Magic-API slice of the header DTO; all fields nullable.
     *  wandLore carries the resolved wand's lore lines as plain
     *  text (for MagicGateReads' lore fallback — the number
     *  MagicGate itself writes on the wand the player sees).
     *  wandFrame is true when mana/manaMax/manaRegen came from a
     *  resolved wand's FRAME (the provider applies the level
     *  boosts to reach the effective pool); false when they came
     *  from the mage-effective fallback, which is already the
     *  effective value and must NOT be boosted again. */
    public record MagicApiData(String rankName, String rankKey, String nextRankName,
                               Float mana, Integer manaMax, Integer manaRegen,
                               java.util.List<String> wandLore, boolean wandFrame) {}

    /** WRV progression order, low to high. Unknown keys rank
     *  below beginner — an unrecognised path never outranks a
     *  known one, and is never guessed at. */
    public static int rankOrder(String key) {
        if (key == null) return -1;
        return switch (key.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "beginner" -> 0;
            case "student" -> 1;
            case "apprentice" -> 2;
            case "master" -> 3;
            default -> -1;
        };
    }

    /** The WRV rank order as keys, low to high — the single
     *  source for rank/next-rank display. Magic's own chain is
     *  NOT used: its master path displays as "Wizard"
     *  (DIAGNOSIS.md §4), which the WRV ladder never calls a
     *  rank. */
    static final String[] WRV_RANKS = {"beginner", "student", "apprentice", "master"};

    /** Display name for a WRV rank key — the title-cased key
     *  ("master" -> "Master", never "Wizard"). Null for an
     *  unknown key: the caller falls back to Magic's own name.
     *  Public: the unit tests pin this contract directly. */
    public static String wrvDisplayName(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase(java.util.Locale.ROOT);
        if (rankOrder(k) < 0) return null;
        return Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }

    /** The next WRV rank key after the given key, or null at
     *  master (top rank — the panel omits the Next line) and
     *  for unknown keys. Public: the unit tests pin this
     *  contract directly. */
    public static String wrvNextKey(String key) {
        int i = rankOrder(key);
        if (i < 0 || i + 1 >= WRV_RANKS.length) return null;
        return WRV_RANKS[i + 1];
    }

    /** A wand's rank key: its path key when set, else its
     *  template key when that is itself a rank key (a
     *  never-upgraded wand: beginner template IS the beginner
     *  rank — MagicGate's resolveRankKey rule, same order). */
    static String wandRankKey(Wand wand) {
        if (wand == null) return null;
        try {
            ProgressionPath path = wand.getPath();
            if (path != null && path.getKey() != null && rankOrder(path.getKey()) >= 0) {
                return path.getKey();
            }
        } catch (Throwable ignored) {
            // fall through to the template key
        }
        try {
            String template = wand.getTemplateKey();
            if (template != null && rankOrder(template) >= 0) return template;
        } catch (Throwable ignored) {
            // no rank source on this wand
        }
        return null;
    }

    public static MagicApiData read(Player player) {
        MageController controller = controller();
        if (controller == null) return null;
        Mage mage = controller.getMage(player);
        if (mage == null) return null;

        Wand wand = resolveWand(controller, mage, player);

        String rankName = null, rankKey = null, nextRankName = null;
        Float mana = null;
        Integer manaMax = null, manaRegen = null;
        if (wand != null) {
            try {
                ProgressionPath path = wand.getPath();
                if (path != null) {
                    rankName = stripLegacy(path.getName());
                    rankKey = path.getKey();
                }
            } catch (Throwable ignored) {
                // a wand that will not yield its path yields no rank
            }
            if (rankKey == null) rankKey = wandRankKey(wand);
            // Rank/next from the WRV order, never Magic's chain:
            // getNextPath() on apprentice returns the path Magic
            // displays as "Wizard" (its master name) — the WRV
            // ladder's next rank is Master, as the wand lore says.
            String wrvName = wrvDisplayName(rankKey);
            if (wrvName != null) rankName = wrvName;
            String nextKey = wrvNextKey(rankKey);
            nextRankName = nextKey == null ? null : wrvDisplayName(nextKey);
            try { mana = wand.getMana(); } catch (Throwable ignored) {}
            try { manaMax = wand.getManaMax(); } catch (Throwable ignored) {}
            try { manaRegen = wand.getManaRegeneration(); } catch (Throwable ignored) {}
        }

        if (rankKey == null) {
            // Step 6: no wand anywhere — the class's own standing.
            try {
                MageClass active = mage.getActiveClass();
                if (active != null) {
                    ProgressionPath path = active.getPath();
                    if (path != null) {
                        rankName = stripLegacy(path.getName());
                        rankKey = path.getKey();
                    }
                }
            } catch (Throwable ignored) {
                // omission is honest; the composer drops the line
            }
            // Same WRV-order rule for the class path (beginner ->
            // Student, never a Magic chain name).
            String wrvName = wrvDisplayName(rankKey);
            if (wrvName != null) rankName = wrvName;
            String nextKey = wrvNextKey(rankKey);
            nextRankName = nextKey == null ? null : wrvDisplayName(nextKey);
            if (wand == null) {
                try { mana = mage.getMana(); } catch (Throwable ignored) {}
                try { manaMax = mage.getEffectiveManaMax(); } catch (Throwable ignored) {}
                try { manaRegen = mage.getEffectiveManaRegeneration(); } catch (Throwable ignored) {}
            }
        }
        return new MagicApiData(rankName, rankKey, nextRankName, mana, manaMax, manaRegen,
                wandLoreLines(wand), wand != null);
    }

    /** The resolved wand's lore lines as plain text (null when
     *  there is no wand or no lore) — the display copy of the
     *  facts MagicGate writes on the wand. */
    static java.util.List<String> wandLoreLines(Wand wand) {
        if (wand == null) return null;
        try {
            ItemStack item = wand.getItem();
            if (item == null || !item.hasItemMeta()) return null;
            java.util.List<net.kyori.adventure.text.Component> lore =
                    item.getItemMeta().lore();
            if (lore == null || lore.isEmpty()) return null;
            java.util.List<String> out = new java.util.ArrayList<>();
            for (net.kyori.adventure.text.Component line : lore) {
                out.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(line));
            }
            return java.util.List.copyOf(out);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The carried-item wand resolution (DIAGNOSIS.md §5).
     *  Returns null only when the player carries and owns no
     *  wand at all. Never calls getWand(item) — that call
     *  wand-ifies ordinary items. */
    static Wand resolveWand(MageController controller, Mage mage, Player player) {
        // 1) the held main-hand item — ground truth at open time.
        try {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            Wand held = controller.getIfWand(mainHand);
            if (held != null) return held;
        } catch (Throwable ignored) {
            // fall through to the cached reads
        }
        // 2) the cached active wand, while its item is carried.
        try {
            Wand active = mage.getActiveWand();
            if (active != null && itemIsCarried(player, active)) return active;
        } catch (Throwable ignored) {
            // fall through
        }
        // 3) the offhand.
        try {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            Wand off = controller.getIfWand(offHand);
            if (off != null) return off;
        } catch (Throwable ignored) {
            // fall through
        }
        try {
            Wand offCached = mage.getOffhandWand();
            if (offCached != null && itemIsCarried(player, offCached)) return offCached;
        } catch (Throwable ignored) {
            // fall through
        }
        // 4) inventory scan — highest path wins.
        Wand best = null;
        int bestOrder = -2;
        try {
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null) continue;
                Wand candidate;
                try {
                    candidate = controller.getIfWand(stack);
                } catch (Throwable ignored) {
                    continue;
                }
                if (candidate == null) continue;
                String key = wandRankKey(candidate);
                int order = rankOrder(key);
                if (order > bestOrder) {
                    best = candidate;
                    bestOrder = order;
                }
            }
        } catch (Throwable ignored) {
            // a failed scan is not a failed read — fall through
        }
        if (best != null) return best;
        // 5) bound wands (pouched / ender-chested still owned).
        try {
            for (Wand bound : mage.getBoundWands()) {
                if (bound == null) continue;
                String key = wandRankKey(bound);
                int order = rankOrder(key);
                if (order > bestOrder) {
                    best = bound;
                    bestOrder = order;
                }
            }
        } catch (Throwable ignored) {
            // fall through to null
        }
        return best;
    }

    /** True when the wand's live item sits in the player's
     *  inventory (MagicGate's itemIsCarried rule, same shape). */
    static boolean itemIsCarried(Player player, Wand wand) {
        try {
            ItemStack item = wand.getItem();
            if (item == null || item.getType().isAir()) return false;
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null && (s == item || s.equals(item))) return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Remove legacy (§) formatting codes from a Magic display
     *  name: "§aApprentice" -&gt; "Apprentice". */
    static String stripLegacy(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) { i++; continue; }
            out.append(c);
        }
        return out.toString();
    }

    /** The controller route ExcelMagicLevel's MagicBridge proves:
     *  the MagicAPI service first, the plugin-instance cast as
     *  the fallback. */
    static MageController controller() {
        try {
            RegisteredServiceProvider<MagicAPI> rsp =
                    Bukkit.getServicesManager().getRegistration(MagicAPI.class);
            if (rsp != null && rsp.getProvider() != null) {
                return rsp.getProvider().getController();
            }
        } catch (Throwable ignored) {
            // fall through to the plugin-cast route
        }
        try {
            Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic instanceof MagicAPI api) return api.getController();
        } catch (Throwable ignored) {
            // no controller: caller omits the Magic API lines
        }
        return null;
    }
}
