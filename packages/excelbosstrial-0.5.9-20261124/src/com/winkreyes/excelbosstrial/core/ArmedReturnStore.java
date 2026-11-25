/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 0.5.9: the armed-returns store - the pure half of join-armed
 * returns (see ArmReturnPlan for the command words and the
 * ArmedReturns adapter for the join listener). An ARM is data
 * only: "when this player next joins, build this EliteMobs
 * item with them present and file it into their boss rewards
 * claims." Arming an offline player is the primary use, so the
 * store is just (player, loot file, who armed it, when) - the
 * item itself is never built or held here.
 *
 * The on-disk shape follows the claims ledger's integrity
 * posture: a plain-text body (one arm per line, tab-separated)
 * with a SHA-256 trailer over every byte before it. A file
 * whose checksum does not verify, or whose lines do not parse,
 * is refused whole (decode throws) - a half-read arm store
 * could drop a player's owed return or, worse, double it.
 * Persistence itself is the adapter's job (AtomicStateFile);
 * this class only encodes, decodes and keeps the rules:
 * one arm per (player, loot file) pair, and every mutation is
 * in-memory until the adapter writes the new encoding.
 */
public final class ArmedReturnStore {

    /** One armed return. Fields are structurally clean (no
     *  tabs or line breaks - they are the file's separators);
     *  the compact constructor enforces that. */
    public record Arm(UUID player, String playerName, String lootFile,
                      String armedBy, long armedAtMs) {
        public Arm {
            if (player == null) {
                throw new IllegalArgumentException("arm needs a player");
            }
            playerName = clean(playerName, "player name");
            lootFile = clean(lootFile, "loot file");
            armedBy = clean(armedBy, "armed by");
            if (lootFile.indexOf('/') >= 0 || lootFile.indexOf('\\') >= 0) {
                throw new IllegalArgumentException("loot file must be a bare file name");
            }
            if (armedAtMs < 0) {
                throw new IllegalArgumentException("armed-at must be >= 0");
            }
        }

        private static String clean(String field, String what) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("arm needs a " + what);
            }
            String trimmed = field.strip();
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '\t' || c == '\n' || c == '\r') {
                    throw new IllegalArgumentException("arm " + what + " carries a separator");
                }
            }
            return trimmed;
        }
    }

    private static final String HEADER =
            "# ExcelBossTrial armed returns - armed by /bosstrial armreturn; do not edit by hand.\n"
            + "# player-uuid\tplayer-name\tloot-file\tarmed-by\tarmed-at-epoch-ms\n";
    private static final String CHECKSUM_PREFIX = "sha256:";

    private static final Comparator<Arm> ORDER = Comparator
            .comparingLong(Arm::armedAtMs)
            .thenComparing(a -> a.player().toString())
            .thenComparing(Arm::lootFile);

    private final List<Arm> arms = new ArrayList<>();

    private ArmedReturnStore() {
    }

    public static ArmedReturnStore empty() {
        return new ArmedReturnStore();
    }

    public int size() {
        return arms.size();
    }

    public boolean isEmpty() {
        return arms.isEmpty();
    }

    /** Every arm, oldest first (the order listreturns shows). */
    public List<Arm> all() {
        List<Arm> out = new ArrayList<>(arms);
        out.sort(ORDER);
        return List.copyOf(out);
    }

    /** Every arm waiting on one player, oldest first. */
    public List<Arm> forPlayer(UUID player) {
        List<Arm> out = new ArrayList<>();
        for (Arm arm : arms) {
            if (arm.player().equals(player)) {
                out.add(arm);
            }
        }
        out.sort(ORDER);
        return List.copyOf(out);
    }

    public Optional<Arm> find(UUID player, String lootFile) {
        for (Arm arm : arms) {
            if (arm.player().equals(player) && arm.lootFile().equals(lootFile)) {
                return Optional.of(arm);
            }
        }
        return Optional.empty();
    }

    /**
     * Add an arm. False (nothing changed) when the (player,
     * loot file) pair is already armed - re-arming the same
     * pair is a refusal, not a replace, so an admin who means
     * to change an arm disarms it first and the audit trail
     * stays honest.
     */
    public boolean add(Arm arm) {
        if (find(arm.player(), arm.lootFile()).isPresent()) {
            return false;
        }
        arms.add(arm);
        return true;
    }

    /** Remove one (player, loot file) arm. True when one went. */
    public boolean remove(UUID player, String lootFile) {
        return arms.removeIf(a -> a.player().equals(player)
                && a.lootFile().equals(lootFile));
    }

    /** Remove every arm waiting on one player; returns the count. */
    public int removeAll(UUID player) {
        int before = arms.size();
        arms.removeIf(a -> a.player().equals(player));
        return before - arms.size();
    }

    /** The full file text: header, one line per arm, checksum. */
    public String encode() {
        StringBuilder body = new StringBuilder(HEADER);
        for (Arm arm : all()) {
            body.append(arm.player()).append('\t')
                    .append(arm.playerName()).append('\t')
                    .append(arm.lootFile()).append('\t')
                    .append(arm.armedBy()).append('\t')
                    .append(arm.armedAtMs()).append('\n');
        }
        return body + CHECKSUM_PREFIX + digestHex(body.toString()) + "\n";
    }

    /**
     * Parse a store file. Any defect - a missing or wrong
     * checksum, a malformed line, a duplicate pair - refuses
     * the whole file with an IOException; the adapter decides
     * what an unreadable store means at boot (it logs loudly
     * and starts empty, keeping the bad file aside).
     */
    public static ArmedReturnStore decode(String text) throws IOException {
        if (text == null) {
            throw new IOException("armed-returns store is missing");
        }
        int marker = text.lastIndexOf(CHECKSUM_PREFIX);
        if (marker < 0) {
            throw new IOException("armed-returns store carries no checksum");
        }
        String body = text.substring(0, marker);
        String claimed = text.substring(marker + CHECKSUM_PREFIX.length()).strip();
        if (!digestHex(body).equals(claimed)) {
            throw new IOException("armed-returns store checksum mismatch");
        }
        ArmedReturnStore store = new ArmedReturnStore();
        for (String line : body.split("\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 5) {
                throw new IOException("armed-returns line is malformed: " + line);
            }
            try {
                Arm arm = new Arm(UUID.fromString(fields[0]), fields[1], fields[2],
                        fields[3], Long.parseLong(fields[4]));
                if (!store.add(arm)) {
                    throw new IOException("armed-returns store arms one pair twice: "
                            + fields[0] + " / " + fields[2]);
                }
            } catch (IllegalArgumentException bad) {
                throw new IOException("armed-returns line is malformed: " + line, bad);
            }
        }
        return store;
    }

    private static String digestHex(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
