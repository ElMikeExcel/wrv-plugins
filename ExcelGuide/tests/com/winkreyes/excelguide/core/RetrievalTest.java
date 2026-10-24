package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Retrieval over the REAL knowledge.yml (SPEC 10.1), expectations
 * measured on the shipped corpus (Oct 18, 2026 tuning):
 *  - questions where one chunk genuinely dominates must CLEAR the
 *    shipped bar (score >= 8, margin 1.5x);
 *  - questions whose best chunk is right but mid-pack must still pick
 *    the right winner (the engine then serves the curated intent line
 *    - that fallback is the designed path, not a failure);
 *  - an adversarial set that must produce non-answers;
 *  - the margin rule itself on a synthetic base with controlled math.
 */
public final class RetrievalTest {

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        KnowledgeBase kb = TestSupport.realKnowledge();
        GuideConfig cfg = TestSupport.realConfig();

        // Dominating questions: must clear the shipped bar unscoped.
        Map<String, Set<String>> clearing = Map.of(
            "how does builder mode work with worldedit",
                Set.of("player-map-help", "builder-mode-use", "builder-mode-worldedit", "builder-mode-rules"),
            "how do i join the server on playstation", Set.of("top"));
        for (Map.Entry<String, Set<String>> e : clearing.entrySet()) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(e.getKey()), null);
            t.check("clears [" + e.getKey() + "]", Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin));
            t.check("clears [" + e.getKey() + "] winner in " + e.getValue()
                            + " (got " + (r.best() == null ? "none" : r.best().id()) + ")",
                    r.best() != null && e.getValue().contains(r.best().id()));
            t.check("clears [" + e.getKey() + "] answer trimmed",
                    r.answerText() != null && !r.answerText().isBlank() && r.answerText().length() <= 380);
        }

        // Right winner, mid-pack score: winner correctness only.
        Map<String, Set<String>> winners = Map.of(
            "how do mcMMO parties work", Set.of("mcmmo-party"),
            "how do shops work", Set.of("shops-preview", "mcmmo-xp-preview"),
            "what commands can i use in the server menu", Set.of("shops-preview", "guide", "top", "all-plugins"));
        for (Map.Entry<String, Set<String>> e : winners.entrySet()) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(e.getKey()), null);
            t.check("winner [" + e.getKey() + "] in " + e.getValue()
                            + " (got " + (r.best() == null ? "none" : r.best().id()) + ")",
                    r.best() != null && e.getValue().contains(r.best().id()));
        }
        // Vanilla Basics pack (Oct 18, 2026): canonical questions whose
        // chunk dominates must clear the shipped bar unscoped.
        Map<String, Set<String>> vanillaClearing = Map.ofEntries(
            Map.entry("where do I find an allay", Set.of("vanilla-allays")),
            Map.entry("where do I find ancient debris", Set.of("vanilla-ancient-debris")),
            Map.entry("where do axolotls live", Set.of("vanilla-axolotls")),
            Map.entry("how do I make a beacon work", Set.of("vanilla-beacon")),
            Map.entry("how do I breed animals", Set.of("vanilla-breeding")),
            Map.entry("how do I brew potions", Set.of("vanilla-brewing")),
            Map.entry("how do I make a boat with a chest", Set.of("vanilla-chest-boat")),
            Map.entry("how do I make a copper golem", Set.of("vanilla-copper-golem")),
            Map.entry("how do I make a crossbow", Set.of("vanilla-crossbow")),
            Map.entry("how do I cure a zombie villager", Set.of("vanilla-curing")),
            Map.entry("where do I find elytra wings", Set.of("vanilla-elytra")),
            Map.entry("how does an ender chest work", Set.of("vanilla-ender-chest")),
            Map.entry("how does sugar cane grow", Set.of("vanilla-farm-crops")),
            Map.entry("how do I get a goat horn", Set.of("vanilla-goats")),
            Map.entry("how do I make an iron golem", Set.of("vanilla-iron-golem")),
            Map.entry("how does kelp grow", Set.of("vanilla-kelp")),
            Map.entry("how do I make a lead", Set.of("vanilla-leads")),
            Map.entry("how do I make a compass point to my base", Set.of("vanilla-lodestone")),
            Map.entry("how do maps and the cartography table work", Set.of("vanilla-maps")),
            Map.entry("how do I name a mob", Set.of("vanilla-name-tags")),
            Map.entry("how do I build a nether portal", Set.of("vanilla-nether-portal")),
            Map.entry("how do I upgrade to netherite gear", Set.of("vanilla-netherite")),
            Map.entry("where do I find the netherite upgrade template", Set.of("vanilla-netherite-template")),
            Map.entry("how do I trade with piglins", Set.of("vanilla-piglin-bartering")),
            Map.entry("how does a raid start", Set.of("vanilla-raids")),
            Map.entry("how do I find my stuff after I die", Set.of("vanilla-recovery-compass")),
            Map.entry("how do I set my spawn in the nether", Set.of("vanilla-respawn-anchor")),
            Map.entry("what is a sculk shrieker", Set.of("vanilla-shriekers")),
            Map.entry("how do I get a shulker box", Set.of("vanilla-shulker-box")),
            Map.entry("silk touch or fortune which is better", Set.of("vanilla-silk-fortune")),
            Map.entry("how do I make a snow golem", Set.of("vanilla-snow-golem")),
            Map.entry("how does a totem of undying work", Set.of("vanilla-totem")),
            Map.entry("how do villager trades work", Set.of("vanilla-villager-trades"))
        );
        for (Map.Entry<String, Set<String>> e : vanillaClearing.entrySet()) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(e.getKey()), null);
            t.check("vanilla clears [" + e.getKey() + "]", Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin));
            t.check("vanilla clears [" + e.getKey() + "] winner in " + e.getValue()
                            + " (got " + (r.best() == null ? "none" : r.best().id()) + ")",
                    r.best() != null && e.getValue().contains(r.best().id()));
            t.check("vanilla clears [" + e.getKey() + "] answer trimmed",
                    r.answerText() != null && !r.answerText().isBlank() && r.answerText().length() <= 380);
        }

        // Vanilla Basics: single-term canonicals. By design a one-term
        // question tops out at 6 (title 3 + body cap 3), below the min
        // score of 8, so these assert the right WINNER only; in game the
        // intent/AI path serves them (Retrieval class doc).
        Map<String, Set<String>> vanillaWinners = Map.ofEntries(
            Map.entry("what does an anvil do", Set.of("vanilla-anvil")),
            Map.entry("how do bees work", Set.of("vanilla-bees")),
            Map.entry("what does a composter do", Set.of("vanilla-composter")),
            Map.entry("how does a conduit work", Set.of("vanilla-conduit")),
            Map.entry("how does enchanting work", Set.of("vanilla-enchanting")),
            Map.entry("how does fishing work", Set.of("vanilla-fishing")),
            Map.entry("how do frogs work", Set.of("vanilla-frogs")),
            Map.entry("what does a grindstone do", Set.of("vanilla-grindstone")),
            Map.entry("how do I get a trident", Set.of("vanilla-trident"))
        );
        for (Map.Entry<String, Set<String>> e : vanillaWinners.entrySet()) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(e.getKey()), null);
            t.check("vanilla winner [" + e.getKey() + "] in " + e.getValue()
                            + " (got " + (r.best() == null ? "none" : r.best().id()) + ")",
                    r.best() != null && e.getValue().contains(r.best().id()));
        }

        // Phantom Debt event (Oct 18, 2026): the canonical question
        // dominates its chunk and must clear the shipped bar unscoped.
        Map<String, Set<String>> phantomClearing = Map.of(
            "what is the phantom debt", Set.of("phantom-debt"));
        for (Map.Entry<String, Set<String>> e : phantomClearing.entrySet()) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(e.getKey()), null);
            t.check("phantom clears [" + e.getKey() + "]", Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin));
            t.check("phantom clears [" + e.getKey() + "] winner in " + e.getValue()
                            + " (got " + (r.best() == null ? "none" : r.best().id()) + ")",
                    r.best() != null && e.getValue().contains(r.best().id()));
            t.check("phantom clears [" + e.getKey() + "] answer trimmed",
                    r.answerText() != null && !r.answerText().isBlank() && r.answerText().length() <= 380);
        }

        Retrieval.Result claim = Retrieval.search(kb,
                TextNorm.normalize("how do i claim land with a golden shovel"), null);
        t.check("claims winner is the GriefPrevention text",
                claim.best() != null && (claim.best().title().contains("GriefPrevention")
                        || claim.best().id().equals("top")));

        // Scoped (hinted) search is the engine's path for topic intents.
        Retrieval.Result scopedTrees = Retrieval.search(kb,
                TextNorm.normalize("what do skill trees show for mining"), List.of("skill-trees"));
        t.check("scoped skill-trees clears", Retrieval.clearsBar(scopedTrees,
                cfg.retrievalMinScore, cfg.retrievalMargin));
        t.check("scoped skill-trees winner", scopedTrees.best() != null
                && scopedTrees.best().id().startsWith("skill-trees"));
        Retrieval.Result scopedParty = Retrieval.search(kb,
                TextNorm.normalize("how do mcMMO parties work"), List.of("mcmmo-party"));
        t.check("scoped party winner", scopedParty.best() != null
                && scopedParty.best().id().equals("mcmmo-party"));

        // Adversarial set: none of these may clear the bar.
        List<String> adversarial = List.of(
            "what is the capital of france",
            "how do i do my math homework",
            "asdfgh qwerty zzz nonsense",
            "who won the football game last night",
            "how do i install mods on my xbox",
            "what is the weather tomorrow");
        for (String q : adversarial) {
            Retrieval.Result r = Retrieval.search(kb, TextNorm.normalize(q), null);
            t.check("adversarial [" + q + "] stays a non-answer",
                    !Retrieval.clearsBar(r, cfg.retrievalMinScore, cfg.retrievalMargin));
        }

        // Margin rule, controlled arithmetic on a synthetic base.
        KnowledgeBase tiny = new KnowledgeBase(List.of(
                new KnowledgeBase.Chunk("a", "a", "Mining guide", "mining mining mining ore"),
                new KnowledgeBase.Chunk("b", "b", "Fishing guide", "fishing fishing rod")),
                "deadbeefcafe");
        Retrieval.Result clear = Retrieval.search(tiny, "mining ore", null);
        t.check("synthetic clear winner", clear.best() != null && clear.best().key().equals("a")
                && Retrieval.clearsBar(clear, 4, 1.5));
        Retrieval.Result nothing = Retrieval.search(tiny, "cooking recipes", null);
        t.check("synthetic no-topic fails", !Retrieval.clearsBar(nothing, 1, 1.5));
        Retrieval.Result weak = Retrieval.search(tiny, "mining", null);
        t.check("below min score fails", !Retrieval.clearsBar(weak, 100, 1.5));
        t.eq("version stamp is hash prefix", "deadbeef", tiny.version());

        // Answer text prefers sentences that carry query terms.
        KnowledgeBase.Chunk chunk = new KnowledgeBase.Chunk("k", "k", "T",
                "Welcome to the valley. Mining gives ore from stone. The sky is blue. Mining levels come from mining ore.");
        String ans = Retrieval.answerText(chunk, Set.of("mining"));
        t.check("answer text picks term sentences", ans.contains("Mining gives ore"));

        t.finish("RetrievalTest");
    }
}
