package com.gmail.nossr50.datatypes.player;
import com.gmail.nossr50.datatypes.party.Party;
import org.bukkit.entity.Player;
public class McMMOPlayer {
    private final Player player; private Party party, invite;
    public McMMOPlayer(Player p) { player = p; }
    public Player getPlayer() { return player; }
    public Party getParty() { return party; } public void setParty(Party p) { party = p; }
    public void setPartyInvite(Party p) { invite = p; } public Party getPartyInvite() { return invite; } public boolean hasPartyInvite() { return invite != null; } public void removePartyInvite() { invite = null; }
}
