package com.gmail.nossr50;
import com.gmail.nossr50.config.GeneralConfig;
import com.gmail.nossr50.config.party.PartyConfig;
import com.gmail.nossr50.party.PartyManager;
/** TEST STUB shaped like mcMMO 2.3.003's API (names and signatures checked against the real jar). Not shipped. */
public class mcMMO {
    public static mcMMO p;
    public final PartyConfig partyConfig = new PartyConfig(); public final GeneralConfig general = new GeneralConfig(); public final PartyManager partyManager = new PartyManager();
    public PartyConfig getPartyConfig() { return partyConfig; }
    public GeneralConfig getGeneralConfig() { return general; }
    public PartyManager getPartyManager() { return partyManager; }
}
