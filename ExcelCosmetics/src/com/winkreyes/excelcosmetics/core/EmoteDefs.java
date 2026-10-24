package com.winkreyes.excelcosmetics.core;

import java.util.Map;

/**
 * The emote COMMAND pack (feasibility §1.5b): ordinary server chat
 * lines in plain text, parity-safe by construction — there is no
 * client feature to mismatch. {actor} and {target} are substituted
 * at send time. RequiresTarget commands take a player argument.
 */
public final class EmoteDefs {

    /** The SKU id of the command pack as a grant. */
    public static final String PACK_ID = "command-pack";

    public record Emote(String command, boolean requiresTarget, String withTarget, String solo) {}

    public static final Map<String, Emote> COMMANDS = Map.of(
            "shrug",    new Emote("shrug", false, null, "{actor} shrugs ¯\\_(ツ)_/¯"),
            "tableflip", new Emote("tableflip", false, null, "{actor} flips a table (╯°□°)╯︵ ┻━┻"),
            "unflip",   new Emote("unflip", false, null, "{actor} puts the table back ┬─┬ノ( º _ ºノ)"),
            "wave",     new Emote("wave", true, "{actor} waves at {target} (^-^)/", "{actor} waves (^-^)/"),
            "hug",      new Emote("hug", true, "{actor} hugs {target} (づ｡◕‿‿◕｡)づ", "{actor} hugs the air (づ｡◕‿‿◕｡)づ"),
            "highfive", new Emote("highfive", true, "{actor} high-fives {target} o(^-^)o", "{actor} high-fives the air o(^-^)o")
    );

    private EmoteDefs() {}

    public static String render(Emote emote, String actor, String target) {
        String template = (target != null && emote.withTarget() != null) ? emote.withTarget() : emote.solo();
        String out = template.replace("{actor}", actor);
        if (target != null) out = out.replace("{target}", target);
        return out;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
