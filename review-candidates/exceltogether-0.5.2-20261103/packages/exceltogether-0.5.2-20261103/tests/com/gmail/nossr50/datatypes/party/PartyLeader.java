package com.gmail.nossr50.datatypes.party;
import java.util.UUID;
public class PartyLeader { private final UUID id; private final String name; public PartyLeader(UUID id, String name) { this.id = id; this.name = name; } public UUID getUniqueId() { return id; } public String getName() { return name; } }
