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
}
