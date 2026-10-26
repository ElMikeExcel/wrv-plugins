package com.winkreyes.excelguide.core;

/** 0.3.3 platform control notes (parity rule, Oct 26, 2026). */
public final class PlatformTextTest {

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        String mcmmo = "Use/right-click the tool to ready it, then break a block within 4 seconds.";
        String vault = "Sneak + right-click a locked vault to reopen it early for $250.";
        String party = "Click the [Accept] button in chat to join the party.";
        String claim = "Hold a golden shovel. Right-click (or tap) one corner, then the opposite corner.";
        String wand = "Right-click on Java, press and hold on touch, or LT on a controller.";
        String touchOnly = "Tap Wand controls on the compass to pick Classic or Sneak bars.";
        String plain = "Type /vaults to see your vaults.";

        t.eq("bedrock gets touch note for mcMMO ready", PlatformText.BEDROCK_NOTE, PlatformText.noteFor(mcmmo, true));
        t.eq("bedrock gets touch note for sneak + right-click", PlatformText.BEDROCK_NOTE, PlatformText.noteFor(vault, true));
        t.eq("bedrock gets touch note for a chat button", PlatformText.BEDROCK_NOTE, PlatformText.noteFor(party, true));
        t.check("java gets nothing extra for mouse wording", PlatformText.noteFor(mcmmo, false) == null);
        t.check("both ways already given: bedrock nothing", PlatformText.noteFor(claim, true) == null);
        t.check("both ways already given: java nothing", PlatformText.noteFor(claim, false) == null);
        t.check("wand both ways: bedrock nothing", PlatformText.noteFor(wand, true) == null);
        t.eq("java gets mouse note for touch-only wording", PlatformText.JAVA_NOTE, PlatformText.noteFor(touchOnly, false));
        t.check("bedrock gets nothing for touch-only wording", PlatformText.noteFor(touchOnly, true) == null);
        t.check("no controls: bedrock nothing", PlatformText.noteFor(plain, true) == null);
        t.check("no controls: java nothing", PlatformText.noteFor(plain, false) == null);
        t.check("null safe", PlatformText.noteFor(null, true) == null);
        t.check("blank safe", PlatformText.noteFor("  ", false) == null);
        t.check("RightClick spelled together", PlatformText.mentionsMouse("rightclick the block"));
        t.check("left-clicking counts", PlatformText.mentionsMouse("left-clicking a wolf shows its stats"));
        t.check("lt lowercase is not a pad key", !PlatformText.mentionsTouch("salt and halt"));
        t.check("LT is a pad key", PlatformText.mentionsTouch("press LT"));
        t.check("tapestry is not tap", !PlatformText.mentionsTouch("a tapestry"));
        t.check("no em dash in notes", !PlatformText.BEDROCK_NOTE.contains("\u2014") && !PlatformText.JAVA_NOTE.contains("\u2014")
                && !PlatformText.AI_RULE.contains("\u2014"));
        t.check("system prompt carries the both-ways rule",
                AiPrompts.systemPrompt("Guide", "x").contains(PlatformText.AI_RULE));
        t.check("public prompt keeps the both-ways rule",
                AiPrompts.publicSystemPrompt("Guide", "x").contains(PlatformText.AI_RULE));
        t.check("public prompt still swaps rule 2",
                AiPrompts.publicSystemPrompt("Guide", "x").contains("shown to EVERYONE in public chat"));
        t.check("config default on", GuideConfig.fromYaml(null).platformNotes);
        t.check("config key missing stays on", GuideConfig.fromYaml("enabled: true\n").platformNotes);
        t.check("config can turn it off", !GuideConfig.fromYaml("platform-notes: false\n").platformNotes);
        t.finish("PlatformTextTest");
    }
}
