package com.winkreyes.excellandmarks.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The ONE menu model behind both renderers (SPEC §6): Bedrock gets a
 * native Floodgate form, Java a chest inventory, both drawn from this
 * exact structure — title, content lines, ordered buttons — so the two
 * platforms can never drift apart. Pure Java; renderers live in the
 * Bukkit layer and contain no decisions.
 */
public final class SpotMenuModel {

    public enum Btn { RENT, BUY, RENEW, RELEASE, CONFIRM, BACK, CLOSE, CLAIM }

    public record Button(Btn id, String label) { }

    public record Screen(String title, List<String> lines, List<Button> buttons) {
        public Screen {
            lines = List.copyOf(lines);
            buttons = List.copyOf(buttons);
        }
        public boolean has(Btn b) {
            for (Button x : buttons) if (x.id() == b) return true;
            return false;
        }
    }

    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US).withZone(ZONE);

    private SpotMenuModel() { }

    public static String date(long ms) {
        return DATE.format(Instant.ofEpochMilli(ms));
    }

    private static String typeTier(Spot s) {
        String type = s.type == Spot.Type.PLOT ? "Plot" : "Landmark";
        String tier = switch (s.tier) {
            case SPOT -> "Spot";
            case LANDMARK -> "Landmark";
            case HEART -> "Heart piece";
        };
        return type + " · " + tier;
    }

    private static List<String> header(Spot s, String notice) {
        List<String> lines = new ArrayList<>();
        if (notice != null && !notice.isEmpty()) lines.add(notice);
        lines.add(typeTier(s));
        return lines;
    }

    /** The spot's main screen for one viewer (SPEC §6 content rules). */
    public static Screen mainScreen(Spot s, UUID viewer, long now, int graceDays, String notice) {
        List<String> lines = header(s, notice);
        List<Button> buttons = new ArrayList<>();
        RentClock.State state = RentClock.stateAt(s, now, graceDays);
        boolean mine = viewer != null && viewer.equals(s.ownerId);

        if (s.isAvailable()) {
            lines.add("Available");
            lines.add("Rent " + Money.fmt(s.rentCents) + "/mo · Buy " + Money.fmt(s.buyCents));
            if (s.isPlot()) lines.add("Plot: " + (s.size * 2 + 1) + "x" + (s.size * 2 + 1) + " build area");
            buttons.add(new Button(Btn.RENT, "Rent — " + Money.fmt(s.rentCents) + "/mo"));
            buttons.add(new Button(Btn.BUY, "Buy — " + Money.fmt(s.buyCents)));
        } else if (mine && s.mode == Spot.Mode.RENTED) {
            if (state == RentClock.State.GRACE) {
                lines.add("Rented by you — in grace");
                lines.add("Renew by " + date(RentClock.graceEnd(s.paidThroughMs, graceDays))
                        + " or the spot is released");
                buttons.add(new Button(Btn.RENEW,
                        "Renew by " + date(RentClock.graceEnd(s.paidThroughMs, graceDays))
                                + " — " + Money.fmt(s.rentCents)));
            } else {
                lines.add("Rented by you until " + date(s.paidThroughMs));
                buttons.add(new Button(Btn.RENEW, "Renew — " + Money.fmt(s.rentCents) + "/mo"));
            }
            lines.add("Rent " + Money.fmt(s.rentCents) + "/mo · Buy out " + Money.fmt(s.buyCents));
            buttons.add(new Button(Btn.BUY, "Buy Outright — " + Money.fmt(s.buyCents)));
            buttons.add(new Button(Btn.RELEASE, "Release this spot"));
        } else if (mine && s.mode == Spot.Mode.OWNED) {
            lines.add("Owned by you");
            buttons.add(new Button(Btn.RELEASE, "Release this spot"));
        } else {
            // Someone else's spot: view-only. No action buttons, ever.
            if (s.mode == Spot.Mode.RENTED) {
                lines.add("Rented by " + s.ownerName + " until " + date(s.paidThroughMs));
            } else {
                lines.add("Owned by " + s.ownerName);
            }
            lines.add("Rent " + Money.fmt(s.rentCents) + "/mo · Buy " + Money.fmt(s.buyCents));
        }
        buttons.add(new Button(Btn.CLOSE, "Close"));
        return new Screen(s.name, lines, buttons);
    }

    /** Confirm screen shown before ANY Vault charge or release (SPEC §6). */
    public static Screen confirmScreen(Spot s, Btn action, long now, int periodDays) {
        List<String> lines = new ArrayList<>();
        String title;
        switch (action) {
            case RENT -> {
                title = "Rent this spot?";
                lines.add(s.name + " — " + Money.fmt(s.rentCents) + "/mo");
                lines.add("Paid through " + date(RentClock.rentStart(now, periodDays)));
            }
            case BUY -> {
                title = "Buy this spot?";
                lines.add(s.name + " — " + Money.fmt(s.buyCents) + ", yours for good");
                if (!s.isAvailable()) lines.add("Buying out a rental: the current period is not refunded");
            }
            case RENEW -> {
                title = "Renew this spot?";
                lines.add(s.name + " — " + Money.fmt(s.rentCents) + "/mo");
                lines.add("New paid-through: " + date(RentClock.renew(now, s.paidThroughMs, periodDays)));
            }
            case RELEASE -> {
                title = "Release this spot?";
                lines.add(s.name + " goes back up for anyone to rent or buy");
                if (s.isPlot()) lines.add("Anything left inside is packed into your redemption box");
            }
            default -> throw new IllegalArgumentException("no confirm screen for " + action);
        }
        List<Button> buttons = List.of(
                new Button(Btn.CONFIRM, "Confirm"),
                new Button(Btn.BACK, "Back"));
        return new Screen(title, lines, buttons);
    }

    /** Sign face lines 1–4, text only — the Bukkit layer adds colour (SPEC §3). */
    public static String[] signLines(Spot s, long now, int graceDays) {
        String l3, l4;
        if (s.isAvailable()) {
            l3 = "Available";
            l4 = "Rent " + Money.fmtShort(s.rentCents) + "/mo · Buy " + Money.fmtShort(s.buyCents);
        } else if (s.mode == Spot.Mode.OWNED) {
            l3 = s.ownerName;
            l4 = "Owned";
        } else if (RentClock.stateAt(s, now, graceDays) == RentClock.State.GRACE) {
            l3 = s.ownerName;
            l4 = "Grace til " + DateTimeFormatter.ofPattern("MMM d", Locale.US)
                    .withZone(ZONE).format(Instant.ofEpochMilli(RentClock.graceEnd(s.paidThroughMs, graceDays)));
        } else {
            l3 = s.ownerName;
            l4 = "Rented til " + DateTimeFormatter.ofPattern("MMM d", Locale.US)
                    .withZone(ZONE).format(Instant.ofEpochMilli(s.paidThroughMs));
        }
        return new String[] {"[Landmark]", s.name, l3, l4};
    }
}
