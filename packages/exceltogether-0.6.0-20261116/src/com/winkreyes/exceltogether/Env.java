package com.winkreyes.exceltogether;

import java.util.List;
import java.util.UUID;

/** Server facts the party logic needs. */
public interface Env {
    PlayerView online(UUID id);
    /** Online players the viewer can see (vanished players and the viewer are left out). */
    List<PlayerView> visibleOthers(PlayerView viewer);
    long now();
}
