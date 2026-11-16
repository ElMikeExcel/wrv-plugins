package com.winkreyes.exceltogether;

import java.util.UUID;

/** What the party menu logic needs from a player. The plugin wraps a Bukkit Player; tests use a fake. */
public interface PlayerView {
    UUID id();
    /** Real login name (Floodgate names start with "."). */
    String realName();
    /** Nickname or display name as other players see it (may equal realName). */
    String displayName();
    boolean bedrock();
    void say(String line);
    void say(Panel panel);
    void actionBar(String text);
    /** Dimmer action bar line (WRV-068: in a party but nobody in range). Default: the normal bar. */
    default void actionBarDim(String text) { actionBar(text); }
    /** Small title flash when the party bonus flips on or off (WRV-068). Default: nothing. */
    default void titleFlash(String text, boolean on) { }
    /** Soft sound for the bonus turning on (WRV-068). Default: nothing. */
    default void cueSound() { }
    /**
     * 0.5.4 (BB-17): open the ExcelMenu Server Menu - the FIX-2 return target for the
     * party hub - through the same "menu" command the player would type (the shared
     * opener every WRV plugin's Server Menu return uses). Default: cannot open it
     * (tests / chat-only views); the Bukkit view runs the command fail-soft.
     * @return true when the menu command ran.
     */
    default boolean openServerMenu() { return false; }
}
