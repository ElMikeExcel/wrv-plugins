package com.gmail.nossr50.datatypes.party;
import java.util.*;
public class Party {
    private final LinkedHashMap<UUID, String> members = new LinkedHashMap<>(); private PartyLeader leader; private final String name; private boolean locked;
    public Party(PartyLeader leader, String name) { this.leader = leader; this.name = name; }
    public LinkedHashMap<UUID, String> getMembers() { return members; }
    public String getName() { return name; }
    public PartyLeader getLeader() { return leader; }
    public void setLeader(PartyLeader l) { leader = l; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean b) { locked = b; }
}
