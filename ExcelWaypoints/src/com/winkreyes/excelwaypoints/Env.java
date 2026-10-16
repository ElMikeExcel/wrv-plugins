package com.winkreyes.excelwaypoints;

import java.util.UUID;

/** Server facts the menu logic needs. */
public interface Env {
    PlayerView online(UUID id);
    /** "NORMAL", "NETHER", "THE_END" or null when the world is not loaded or does not exist. */
    String worldEnvironment(String worldName);
    boolean tpaAvailable();
    long now();
}
