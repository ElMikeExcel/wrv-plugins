package com.winkreyes.excelclaims;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.1.3 corner pillars — Mike's ruling, Nov 8, 2026 ("Make claim
 * corners taller and more visible"), after Marv's player-proof
 * report that 0.1.2's single glowstone per corner hid behind
 * terrain (he saw 3 of 4 corners). Pins: the base-Y rule carried
 * verbatim from 0.1.2, the pillar shape (blocks per corner, cap
 * placement), world-ceiling truncation, the height clamp, and
 * the visualization config seams (defaults = shipped behavior;
 * junk values degrade to the defaults, never to no marker).
 */
public class CornerPillarTest {

    private static final int[][] CORNERS = {{0, 0}, {0, 9}, {9, 0}, {9, 9}};
    private static final int[] FLAT = {65, 65, 65, 65};

    // ------------------------------------------------------------ base Y (the 0.1.2 rule, verbatim)

    @Test
    void baseYIsOneAboveTheSurface() {
        assertEquals(71, CornerPillars.baseY(70, 64));
    }

    @Test
    void baseYNeverSitsAtOrBelowTheClaimBottom() {
        // Surface below the claim's clicked bottom: the marker
        // stands at the bottom + 1, as 0.1.2 placed it.
        assertEquals(65, CornerPillars.baseY(60, 64));
        assertEquals(65, CornerPillars.baseY(63, 64));
        assertEquals(65, CornerPillars.baseY(64, 64));
    }

    // ------------------------------------------------------------ pillar shape

    @Test
    void fourCornersAtHeightFourSendSixteenBlocks() {
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, FLAT, 4, 319);
        assertEquals(16, plan.size(), "4 corners x 4 high");
    }

    @Test
    void eachPillarIsBodyBelowAndExactlyOneCapOnTop() {
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, FLAT, 4, 319);
        for (int c = 0; c < 4; c++) {
            List<CornerPillars.Block> pillar = plan.subList(c * 4, c * 4 + 4);
            for (int i = 0; i < 4; i++) {
                CornerPillars.Block b = pillar.get(i);
                assertEquals(CORNERS[c][0], b.x());
                assertEquals(CORNERS[c][1], b.z());
                assertEquals(65 + i, b.y(), "pillar blocks stack bottom to top from the base Y");
                assertEquals(i == 3, b.cap(), "only the top block is the cap");
            }
        }
        assertEquals(4, plan.stream().filter(CornerPillars.Block::cap).count(), "one cap per corner");
    }

    @Test
    void eachCornerKeepsItsOwnBaseY() {
        int[] slopes = {65, 70, 60, 65};
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, slopes, 3, 319);
        assertEquals(12, plan.size());
        for (int c = 0; c < 4; c++) {
            assertEquals(slopes[c], plan.get(c * 3).y());
            assertEquals(slopes[c] + 2, plan.get(c * 3 + 2).y());
            assertTrue(plan.get(c * 3 + 2).cap());
        }
    }

    @Test
    void heightOneSendsALoneCapPerCorner() {
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, FLAT, 1, 319);
        assertEquals(4, plan.size());
        assertTrue(plan.stream().allMatch(CornerPillars.Block::cap));
    }

    @Test
    void heightBelowOneStillSendsTheCap() {
        // plan() itself is defensive; the config clamp is the
        // primary gate, but a 0 here must not mean "no corners".
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, FLAT, 0, 319);
        assertEquals(4, plan.size());
    }

    // ------------------------------------------------------------ world ceiling

    @Test
    void pillarTruncatesAtTheCeilingAndTheCapRidesDown() {
        int[] high = {318, 65, 65, 65};
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, high, 4, 319);
        // Corner 0: base 318, ceiling 319 -> two blocks, cap at 319.
        assertEquals(2 + 12, plan.size());
        assertEquals(318, plan.get(0).y());
        assertFalse(plan.get(0).cap());
        assertEquals(319, plan.get(1).y());
        assertTrue(plan.get(1).cap(), "the cap is the topmost SENT block, not the unsendable one");
    }

    @Test
    void cornerAboveTheCeilingContributesNothing() {
        int[] over = {320, 65, 65, 65};
        List<CornerPillars.Block> plan = CornerPillars.plan(CORNERS, over, 4, 319);
        assertEquals(12, plan.size(), "the other three corners still show");
    }

    // ------------------------------------------------------------ config seams

    @Test
    void pillarHeightClamp() {
        assertEquals(1, ClaimsConfig.sanitizePillarHeight(0));
        assertEquals(1, ClaimsConfig.sanitizePillarHeight(-5));
        assertEquals(1, ClaimsConfig.sanitizePillarHeight(1));
        assertEquals(4, ClaimsConfig.sanitizePillarHeight(4));
        assertEquals(16, ClaimsConfig.sanitizePillarHeight(16));
        assertEquals(16, ClaimsConfig.sanitizePillarHeight(17));
        assertEquals(16, ClaimsConfig.sanitizePillarHeight(1000));
    }

    @Test
    void absentVisualizationSectionYieldsTheShippedDefaults() {
        ClaimsConfig cfg = new ClaimsConfig();
        cfg.load(new YamlConfiguration());
        assertEquals(4, cfg.cornerPillarHeight);
        assertEquals(Material.GLOWSTONE, cfg.cornerBlockMaterial);
        assertEquals(Material.SEA_LANTERN, cfg.cornerCapMaterial);
    }

    @Test
    void visualizationSectionParses() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("visualization.corner-pillar-height", 6);
        y.set("visualization.corner-block-material", "GLOWSTONE");
        y.set("visualization.corner-cap-material", "GOLD_BLOCK");
        ClaimsConfig cfg = new ClaimsConfig();
        cfg.load(y);
        assertEquals(6, cfg.cornerPillarHeight);
        assertEquals(Material.GLOWSTONE, cfg.cornerBlockMaterial);
        assertEquals(Material.GOLD_BLOCK, cfg.cornerCapMaterial);
    }

    @Test
    void junkVisualizationValuesDegradeToDefaults() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("visualization.corner-pillar-height", 99);
        y.set("visualization.corner-block-material", "NOT_A_BLOCK");
        y.set("visualization.corner-cap-material", "ALSO_NOT_A_BLOCK");
        ClaimsConfig cfg = new ClaimsConfig();
        cfg.load(y);
        assertEquals(16, cfg.cornerPillarHeight, "height clamps, never disappears");
        assertEquals(Material.GLOWSTONE, cfg.cornerBlockMaterial);
        assertEquals(Material.SEA_LANTERN, cfg.cornerCapMaterial);
        // A name resolving to a NON-BLOCK material (e.g.
        // DIAMOND_SWORD) is accepted here exactly like the tool
        // materials are, and rejected at display time:
        // Visualizer.blockDataOr falls back to the shipped
        // default rather than throwing. (Material.isBlock()
        // needs the live registry, so that guard cannot live in
        // this headless layer.)
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
