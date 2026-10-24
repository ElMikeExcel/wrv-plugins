package com.winkreyes.excelmenu.core;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Java chest-GUI purchase confirm (0.1.4). Through 0.1.3 a Java
 * click on a spending button ran its actions on the same click:
 * one left/right/shift-left click on a cosmetics item charged
 * $200 to $15,000+ with no second screen, while Bedrock players
 * always got the item-detail form first (BedrockMenus.sendItemDetail).
 * This is the pure decision the chest listener asks before it runs
 * a click: does this click need a Confirm/Cancel screen, and what
 * should the green button say?
 *
 * A click needs the confirm when it has actions AND either
 *  - the item carries a "bedrock: mode: modal" block (the same
 *    signal that gives Bedrock its detail screen), or
 *  - one of its lines runs a command that charges on the spot
 *    (CHARGES_NOW, today the ExcelCosmetics buy command).
 * Commands that already open their own review (excelshop browse,
 * mobveil buy, deathprotection buy) are deliberately NOT here:
 * those plugins show their own Java chest Confirm screen.
 */
public final class JavaConfirm {

    /** Console/player lines that take money immediately. */
    static final Pattern CHARGES_NOW = Pattern.compile(
            "^\\[(console|player)\\]\\s*/?excelcosmetics\\s+buy\\b.*", Pattern.CASE_INSENSITIVE);

    public static final String CANCEL_LABEL = "Cancel";

    private JavaConfirm() {}

    public static boolean required(MenuDef.ItemDef item, List<String> lines) {
        if (item == null || lines == null || lines.isEmpty()) return false;
        if (item.bedrock() != null && "modal".equals(item.bedrock().mode())) return true;
        for (String line : lines) if (line != null && CHARGES_NOW.matcher(line.trim()).matches()) return true;
        return false;
    }

    /** The authored action label for a click kind ("Own forever - $15,000"), or null. */
    public static String actionLabel(MenuDef.ItemDef item, String kind) {
        if (item == null || item.bedrock() == null || kind == null) return null;
        List<String> kinds = BedrockDetail.actionKinds(item.bedrock());
        List<String> labels = BedrockDetail.actionLabels(item.bedrock());
        int i = kinds.indexOf(kind);
        if (i < 0) return null;
        String label = labels.get(i);
        return label == null || label.isBlank() ? null : label;
    }

    /** Green button text: "Confirm: Own forever - $15,000", or plain "Confirm". */
    public static String confirmLabel(MenuDef.ItemDef item, String kind) {
        String a = actionLabel(item, kind);
        return a == null ? "Confirm" : "Confirm: " + a;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
