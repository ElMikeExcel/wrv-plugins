package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.Hazard;
import com.winkreyes.exceltrainingdummy.core.TestSession;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * One live dummy: its entity, its owner, its current test session,
 * and the per-dummy settings the tap-the-dummy menu edits (hazard,
 * lethal, boss-armor, checkpoint prints). Settings live here, not in
 * the session, so a tier change / execute reset can rebuild the
 * session without losing them.
 */
public final class Dummy {
    public final UUID entityId;
    public final UUID ownerId;
    public final String ownerName;
    public TestSession session;
    public Hazard hazard;
    public boolean lethal;
    public boolean bossArmor;
    public boolean endless;
    public long lastActivityMillis;

    public Dummy(UUID entityId, UUID ownerId, String ownerName, TestSession session,
                 Hazard hazard, boolean lethal, boolean bossArmor, boolean endless, long nowMillis) {
        this.entityId = entityId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.session = session;
        this.hazard = hazard;
        this.lethal = lethal;
        this.bossArmor = bossArmor;
        this.endless = endless;
        this.lastActivityMillis = nowMillis;
    }

    public LivingEntity entity() {
        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(entityId);
        return e instanceof LivingEntity ? (LivingEntity) e : null;
    }
}
