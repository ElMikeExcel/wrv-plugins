package com.winkreyes.excelguildgate;

import java.util.UUID;

/**
 * The GuildGate narrator pack's JAVA delivery (0.3.2): constants and
 * the pure mappings behind it. No Bukkit dependencies, so this is
 * unit-testable on a plain JVM; the Bukkit wiring lives in
 * ExcelGuildGate.
 *
 * Why this exists: the narrator's voice line (0.3.0) reaches Bedrock
 * players through a .mcpack in Geyser's packs folder, which the live
 * Geyser config FORCE-delivers (force-resource-packs: true) - no
 * Bedrock player can miss it. Java players were served the same pack
 * only by the one-shot server.properties resource-pack prompt:
 * optional, shown once, and a decline is sticky in the client with
 * no tracking and no re-offer, ever (WRV-084 parity audit, gap 3).
 * A Java player who declined once heard silence at every seal fall
 * forever. Mike's platform parity rule (Oct 18, 2026) makes that a
 * defect, so 0.3.2 delivers the narrator pack to Java players the
 * way ExcelQuartermaster 0.2.1 already delivers its voice pack:
 * Player.addResourcePack under a fixed pack id, with per-player
 * offer state persisted in the plugin's data folder. The offer
 * discipline itself is Mike's Oct 19, 2026 rule - five chances, the
 * count shown in the prompt, and a permanent stop at an accept or
 * at the fifth offer (see NarratorPackPrompt) - which supersedes
 * the Quartermaster's 30-day / max-3 numbers for this plugin.
 *
 * The pack itself does not change: same zip, same URL, same SHA-1
 * as the copy server.properties has pointed at since 0.3.0 went live.
 * addResourcePack stacks alongside the server.properties pack rather
 * than replacing it; the server.properties prompt is left exactly as
 * it is (see BUILD.md for the reasoning).
 *
 * PACK_ID is fixed forever: status events are matched against it,
 * so the Quartermaster pack's own status events are never mistaken
 * for ours (and ours never for theirs), and a client that accepted
 * this pack once applies it on later sends without being asked
 * again.
 */
public final class NarratorPack {

    /** Fixed id of the GuildGate narrator Java pack. Never change. */
    public static final UUID PACK_ID =
            UUID.fromString("a3fd20f6-9cad-4f73-9f97-aeb77b54e6f3");

    /** Where the pack zip is hosted (the website repo's packs folder).
     *  The same URL server.properties has used since 0.3.0.
     *  Config can override. */
    public static final String DEFAULT_URL =
            "https://elmikeexcel.github.io/winkreyesvalley/packs/wrv-guildgate-narrator-pack.zip";

    /** SHA-1 of the shipped wrv-guildgate-narrator-pack.zip (the same
     *  value recorded in PACK-SHA1.txt at the 0.3.0 install and still
     *  carried by server.properties). Config can override; if the
     *  hosted zip is ever rebuilt, this default must move with it. */
    public static final String DEFAULT_SHA1 =
            "e1ef3b835d59280e613ae435f6efca760389045e";

    /** The middle of the offer prompt: what the pack is. The full
     *  prompt the player reads is composed by {@link #buildPrompt}
     *  around this body - the offer counter in front ("offer N of
     *  5") and the plain end-of-asking line behind it - because the
     *  count must be right on every single offer. Config can
     *  override the body; the counter and the closing line are not
     *  configurable, so the promise in the text always matches the
     *  discipline NarratorPackPrompt enforces. */
    public static final String DEFAULT_PROMPT_BODY =
            "The valley has a narrator — accept to hear the seal falls spoken aloud.";

    /**
     * Compose the prompt for one offer: the counter first, then the
     * body, then the plain statement of when the asking ends. The
     * offer number clamps into [1, maxOffers] so a bad caller can
     * never produce an "offer 6 of 5" text; maxOffers clamps at 1.
     */
    public static String buildPrompt(String body, int offerNumber, int maxOffers) {
        int max = Math.max(1, maxOffers);
        int n = Math.min(Math.max(1, offerNumber), max);
        String middle = body == null || body.isBlank() ? DEFAULT_PROMPT_BODY : body.trim();
        return "Narrator voice pack — offer " + n + " of " + max + ". "
                + middle
                + " After " + max + " offers, or once you download it, it will not ask again.";
    }

    /** What one Bukkit PlayerResourcePackStatusEvent status means for us. */
    public enum Status {
        /** The client downloaded and applied the pack. */
        LOADED,
        /** The player said no. Remember it; do not nag. */
        DECLINED,
        /** Download or reload failed on the client side. */
        FAILED,
        /** Progress noise (ACCEPTED, DOWNLOADED) or anything unknown:
         *  no state change. */
        IGNORED
    }

    /**
     * Map a Bukkit status name to our Status. String in, enum out, so
     * the wiring passes {@code event.getStatus().name()} and this stays
     * testable without Bukkit. Unknown and null names are IGNORED.
     */
    public static Status mapStatus(String bukkitStatusName) {
        if (bukkitStatusName == null) {
            return Status.IGNORED;
        }
        return switch (bukkitStatusName) {
            case "SUCCESSFULLY_LOADED" -> Status.LOADED;
            case "DECLINED" -> Status.DECLINED;
            case "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL", "DISCARDED" -> Status.FAILED;
            default -> Status.IGNORED;
        };
    }

    /**
     * Parse a 40-character hex SHA-1 into the 20 bytes addResourcePack
     * wants. Returns null for anything else (null, blank, wrong length,
     * non-hex) - the caller then sends without a hash rather than
     * crashing on a mistyped config value.
     */
    public static byte[] parseSha1(String hex) {
        if (hex == null) {
            return null;
        }
        String s = hex.trim();
        if (s.length() != 40) {
            return null;
        }
        byte[] out = new byte[20];
        for (int i = 0; i < 20; i++) {
            int hi = Character.digit(s.charAt(2 * i), 16);
            int lo = Character.digit(s.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private NarratorPack() {}
}
