// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * The Server Website screen copy, ported verbatim from
 * ExcelBedrockMenu 1.5.28. Bedrock players get a form spelling the
 * addresses out (Bedrock cannot open a browser from in game) plus
 * both addresses as a plain chat copy; Java players get clickable
 * chat links (the Bukkit screen builds those from these constants).
 */
public final class WebsiteCore {

    public static final String WEBSITE_URL = "https://elmikeexcel.github.io/winkreyesvalley/";
    public static final String WEBSITE_DISPLAY = "elmikeexcel.github.io/winkreyesvalley";
    public static final String WEBSITE_SHORT_URL = "https://tinyurl.com/winkreyesvalley";
    public static final String WEBSITE_SHORT_DISPLAY = "tinyurl.com/winkreyesvalley";
    public static final String FORM_TITLE = "Server Website";
    public static final String CLOSE_LABEL = "Close";
    public static final String JAVA_FOOTER =
            "§7The guide, the command list and server news live there.";
    public static final String LINK_HOVER = "Open the server website";

    /** Bedrock website form body: the short link first (easy to
     *  remember and to type on a phone), the full address under it. */
    public static String websiteFormContent() {
        return "The server website has the guide, the full command list and "
                + "server news.\n\nType one of these into your phone or computer "
                + "browser:\n\nShort and easy to type:\n" + WEBSITE_SHORT_DISPLAY
                + "\n\nFull address:\n" + WEBSITE_DISPLAY + "\n\nBedrock cannot "
                + "open a browser from in game, so both addresses are in your "
                + "chat too.";
    }

    /** The chat copy that mirrors the form: short link, then full address. */
    public static String[] websiteChatLines() {
        return new String[]{
            "§aServer Website: §b" + WEBSITE_SHORT_URL,
            "§7Full address: §b" + WEBSITE_URL
        };
    }

    private WebsiteCore() {}
}
