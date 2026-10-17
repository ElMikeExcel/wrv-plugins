package com.gmail.nossr50.util.player;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import org.bukkit.entity.Player;
import java.util.*;
public final class UserManager {
    public static final Map<UUID, McMMOPlayer> players = new HashMap<>();
    public static McMMOPlayer getPlayer(Player p) { return players.get(p.getUniqueId()); }
}
