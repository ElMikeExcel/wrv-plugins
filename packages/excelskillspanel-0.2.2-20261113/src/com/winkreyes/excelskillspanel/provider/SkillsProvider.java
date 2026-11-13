package com.winkreyes.excelskillspanel.provider;

import com.winkreyes.excelskillspanel.core.PanelModel;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The provider seam (DESIGN §7.3, kept minimal and honest): a
 * provider supplies header blocks and skill rows for one player,
 * computed from its own plugin's verified read surfaces. The
 * panel core knows only this interface.
 *
 * Two providers ship in the WRV build:
 * - provider-mcmmo ({@code provider.mcmmo}) — the commercial core:
 *   the 19 PrimarySkillTypes via mcMMO's public ExperienceAPI,
 *   plus the Power Level header. No mcMMO code is copied; reads
 *   go through the public API behind a softdepend.
 * - provider-magic ({@code provider.wrv}) — WRV-only: the Magic
 *   header block (ExcelMagicLevel + Magic API + GuildGate reads).
 *   Excluded from any commercial build by package boundary.
 *
 * The seam split for packaging is this package boundary plus
 * provider registration in the plugin's onEnable: the WRV build
 * compiles core + both providers into one jar; a commercial build
 * compiles core + provider-mcmmo only.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public interface SkillsProvider {

    /** Stable provider id ("mcmmo", "magic"). */
    String id();

    /** This provider's contribution for one player. A provider
     *  whose reads all fail returns an empty contribution — it
     *  never invents values. */
    ProviderData collect(Player player);

    /** One provider's contribution: header blocks, skill rows,
     *  and (WRV provider only) the Magic detail view's lines. */
    record ProviderData(List<PanelModel.HeaderBlock> headers,
                        List<PanelModel.SkillRow> rows,
                        List<String> magicDetailLines) {
        public ProviderData {
            headers = headers == null ? List.of() : List.copyOf(headers);
            rows = rows == null ? List.of() : List.copyOf(rows);
            magicDetailLines = magicDetailLines == null ? List.of() : List.copyOf(magicDetailLines);
        }

        public static ProviderData empty() {
            return new ProviderData(List.of(), List.of(), List.of());
        }
    }
}
