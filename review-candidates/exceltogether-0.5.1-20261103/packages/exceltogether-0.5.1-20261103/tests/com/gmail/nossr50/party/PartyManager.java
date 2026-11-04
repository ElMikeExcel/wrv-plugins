package com.gmail.nossr50.party;
import com.gmail.nossr50.datatypes.party.*;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.util.*;
/** Behaves like mcMMO's PartyManager for the calls ExcelTogether makes. */
public final class PartyManager {
    public final List<Party> parties = new ArrayList<>();
    public final List<String> messages = new ArrayList<>();
    public boolean isPartyFull(Player p, Party t) { int m = mcMMO.p.getGeneralConfig().getPartyMaxSize(); return !p.hasPermission("mcmmo.bypass.partylimit") && m >= 1 && t.getMembers().size() >= m; }
    public Party getParty(String n) { for (Party p : parties) if (p.getName().equalsIgnoreCase(n)) return p; return null; }
    public Party getParty(Player pl) { McMMOPlayer m = UserManager.getPlayer(pl); return m == null ? null : m.getParty(); }
    public Party getPlayerParty(String name, UUID id) { for (Party p : parties) if (p.getMembers().containsKey(id)) return p; return null; }
    public boolean canInvite(McMMOPlayer m) { Party p = m.getParty(); return !p.isLocked() || p.getLeader().getUniqueId().equals(m.getPlayer().getUniqueId()); }
    public void createParty(McMMOPlayer m, String name, String pw) {
        Party p = new Party(new PartyLeader(m.getPlayer().getUniqueId(), m.getPlayer().getName()), name.replace(".", ""));
        parties.add(p); messages.add("created " + p.getName()); add(m, p);
    }
    private void add(McMMOPlayer m, Party p) { m.setParty(p); p.getMembers().put(m.getPlayer().getUniqueId(), m.getPlayer().getName()); }
    public void joinInvitedParty(McMMOPlayer m) {
        Party inv = m.getPartyInvite();
        if (!parties.contains(inv)) { messages.add("disbanded"); return; }
        int max = mcMMO.p.getGeneralConfig().getPartyMaxSize();
        if (max > 0 && inv.getMembers().size() >= max) { messages.add("full"); return; }
        m.removePartyInvite(); add(m, inv); messages.add("joined " + inv.getName());
    }
    public void removeFromParty(OfflinePlayer pl, Party party) {
        UUID id = pl.getUniqueId(); party.getMembers().remove(id);
        if (party.getLeader().getUniqueId().equals(id) && !party.getMembers().isEmpty()) { Map.Entry<UUID, String> e = party.getMembers().entrySet().iterator().next(); party.setLeader(new PartyLeader(e.getKey(), e.getValue())); }
        if (party.getMembers().isEmpty()) parties.remove(party);
    }
    public void removeFromParty(McMMOPlayer m) { if (m.getParty() == null) return; removeFromParty(m.getPlayer(), m.getParty()); m.setParty(null); }
}
