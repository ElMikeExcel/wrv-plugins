package com.winkreyes.excelwaypoints;

import java.util.UUID;

/** What the menu logic needs from a player. The plugin wraps a Bukkit Player; tests use a fake. */
public interface PlayerView {
    UUID id();
    /** Real login name (Floodgate names start with "."). Used for /tpa. */
    String realName();
    boolean bedrock();
    String worldName();
    double x();
    double y();
    double z();
    void say(String line);
    void say(Panel panel);
    void actionBar(String text);
    /** Points the player's compass at the place. Returns false if that world is not the player's current world. */
    boolean compassTo(Place p);
    /** Runs a command as this player (used only for the existing /tpa request). */
    void runCommand(String cmd);
}
