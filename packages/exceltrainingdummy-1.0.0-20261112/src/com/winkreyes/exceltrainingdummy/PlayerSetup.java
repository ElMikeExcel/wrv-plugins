package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.Hazard;
import com.winkreyes.exceltrainingdummy.core.Tier;

/**
 * A player's dialed-in spawn settings, shared by the Java menu and
 * the Bedrock form (the two doors show the same dials). In-memory
 * only — a relog starts from the defaults again.
 */
public final class PlayerSetup {
    public Tier tier = Tier.PRACTICE;
    public boolean endless = false;
    public boolean bossArmor = false;
    public Hazard hazard = Hazard.OFF;
    public boolean lethal = false;
    public boolean executeStart = false;
    public boolean checkpoints = true;
}
