package com.winkreyes.excelcosmetics.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * One player's cosmetic state: their grants and what they have
 * equipped. The rental rules live here and nowhere else:
 *
 *  - Renewal STACKS from the current expiry, never from now, so
 *    renewing early is never a trap (and never shortens a grant).
 *  - Renewing after a grant already lapsed starts fresh from now.
 *  - The own-it tier is permanent; a later rental NEVER shortens
 *    a permanent grant, and renting while owned is a no-op.
 *  - A set, while equipped and active, shadows the individually
 *    equipped color/effect for the whole chat line. When the set
 *    lapses, the individual choices resurface untouched.
 *  - Equipping only fills the pointer; appearance resolution
 *    ignores pointers whose grant is gone or expired, so expiry
 *    needs no announcement and no cleanup pass to take effect.
 *
 * All methods take an explicit {@code now} so the whole state
 * machine is unit-testable without a clock.
 */
public final class PlayerState {

    private final List<Grant> grants = new ArrayList<>();
    private String equippedColorId;
    private String equippedEffectId;
    private String equippedSetId;

    public List<Grant> grants() { return List.copyOf(grants); }
    public String equippedColorId() { return equippedColorId; }
    public String equippedEffectId() { return equippedEffectId; }
    public String equippedSetId() { return equippedSetId; }

    public void setEquipped(Kind kind, String id) {
        switch (kind) {
            case COLOR -> equippedColorId = id;
            case EFFECT -> equippedEffectId = id;
            case SET -> equippedSetId = id;
            case EMOTES -> { /* the pack is held, never "equipped" */ }
        }
    }

    /** The active grant for (kind,id) at {@code now}, or null. */
    public Grant activeGrant(Kind kind, String id, long now) {
        for (Grant g : grants) {
            if (g.kind() == kind && g.id().equals(id) && g.isActiveAt(now)) return g;
        }
        return null;
    }

    public boolean hasActive(Kind kind, String id, long now) {
        return activeGrant(kind, id, now) != null;
    }

    /** Any active grant of a kind (used for the emote pack check). */
    public boolean hasAnyActive(Kind kind, long now) {
        for (Grant g : grants) {
            if (g.kind() == kind && g.isActiveAt(now)) return true;
        }
        return false;
    }

    /**
     * Apply a grant of {@code durationMillis} (or permanent when
     * {@code permanent}). Returns the resulting Grant. Auto-equips
     * into an EMPTY slot only — a player's existing choice is never
     * overridden by a purchase.
     */
    public Grant applyGrant(Kind kind, String id, long now, long durationMillis, boolean permanent) {
        Grant result = null;
        for (int i = 0; i < grants.size(); i++) {
            Grant g = grants.get(i);
            if (g.kind() != kind || !g.id().equals(id)) continue;
            if (g.isPermanent()) {
                result = g; // owned already: a rental changes nothing
            } else if (permanent) {
                result = new Grant(kind, id, g.grantedAtMillis(), Grant.PERMANENT);
                grants.set(i, result);
            } else if (g.isActiveAt(now)) {
                result = new Grant(kind, id, g.grantedAtMillis(), g.expiresAtMillis() + durationMillis);
                grants.set(i, result);
            } else {
                result = new Grant(kind, id, now, now + durationMillis);
                grants.set(i, result);
            }
            break;
        }
        if (result == null) {
            result = new Grant(kind, id, now, permanent ? Grant.PERMANENT : now + durationMillis);
            grants.add(result);
        }
        autoEquip(kind, id);
        return result;
    }

    private void autoEquip(Kind kind, String id) {
        switch (kind) {
            case COLOR -> { if (equippedColorId == null) equippedColorId = id; }
            case EFFECT -> { if (equippedEffectId == null) equippedEffectId = id; }
            case SET -> { if (equippedSetId == null) equippedSetId = id; }
            case EMOTES -> { }
        }
    }

    /** Remove a grant outright (admin revoke / support). Clears a
     *  dead equip pointer for the same SKU. Returns true if removed. */
    public boolean revoke(Kind kind, String id) {
        boolean removed = grants.removeIf(g -> g.kind() == kind && g.id().equals(id));
        if (removed) {
            if (kind == Kind.COLOR && id.equals(equippedColorId)) equippedColorId = null;
            if (kind == Kind.EFFECT && id.equals(equippedEffectId)) equippedEffectId = null;
            if (kind == Kind.SET && id.equals(equippedSetId)) equippedSetId = null;
        }
        return removed;
    }

    /** Drop expired grants and dead equip pointers. Returns how
     *  many grants were dropped. Silent by design: nothing here
     *  produces player-facing output, ever. */
    public int normalize(long now) {
        int removed = 0;
        Iterator<Grant> it = grants.iterator();
        while (it.hasNext()) {
            Grant g = it.next();
            if (!g.isPermanent() && g.expiresAtMillis() <= now) { it.remove(); removed++; }
        }
        if (equippedColorId != null && !hasActive(Kind.COLOR, equippedColorId, now)) equippedColorId = null;
        if (equippedEffectId != null && !hasActive(Kind.EFFECT, equippedEffectId, now)) equippedEffectId = null;
        if (equippedSetId != null && !hasActive(Kind.SET, equippedSetId, now)) equippedSetId = null;
        return removed;
    }

    /** What the chat line should look like at {@code now}. */
    public record Appearance(Integer rgb, String effectId, String setId, String colorId) {
        public static final Appearance PLAIN = new Appearance(null, null, null, null);
        public boolean isPlain() { return rgb == null && effectId == null; }
    }

    public Appearance appearance(Catalog catalog, long now) {
        if (equippedSetId != null && hasActive(Kind.SET, equippedSetId, now)) {
            Catalog.SetItem set = catalog.sets().get(equippedSetId);
            if (set != null) {
                return new Appearance(set.rgb(), set.effectId(), set.id(), null);
            }
        }
        Integer rgb = null;
        String colorId = null;
        if (equippedColorId != null && hasActive(Kind.COLOR, equippedColorId, now)) {
            Catalog.ColorItem c = catalog.colors().get(equippedColorId);
            if (c != null) { rgb = c.rgb(); colorId = c.id(); }
        }
        String effectId = null;
        if (equippedEffectId != null && hasActive(Kind.EFFECT, equippedEffectId, now)) {
            if (catalog.effects().containsKey(equippedEffectId)) effectId = equippedEffectId;
        }
        if (rgb == null && effectId == null) return Appearance.PLAIN;
        return new Appearance(rgb, effectId, null, colorId);
    }

    /** The stored grant for (kind,id), active or not, or null. */
    public Grant findGrant(Kind kind, String id) {
        for (Grant g : grants) {
            if (g.kind() == kind && g.id().equals(id)) return g;
        }
        return null;
    }

    /** Admin/support: move a rental's expiry (rig tests + Mike
     *  fixing a grant by hand). Refuses permanent grants — owned
     *  is owned. Returns true when an expiry was actually set. */
    public boolean setExpiry(Kind kind, String id, long newExpiresAtMillis) {
        for (int i = 0; i < grants.size(); i++) {
            Grant g = grants.get(i);
            if (g.kind() == kind && g.id().equals(id)) {
                if (g.isPermanent()) return false;
                grants.set(i, new Grant(kind, id, g.grantedAtMillis(), newExpiresAtMillis));
                return true;
            }
        }
        return false;
    }

    /** Directly seed a grant (persistence load path — no stacking,
     *  no auto-equip; the file already holds the merged truth). */
    public void loadGrant(Grant g) { grants.add(g); }
    public void loadEquipped(String colorId, String effectId, String setId) {
        this.equippedColorId = colorId;
        this.equippedEffectId = effectId;
        this.equippedSetId = setId;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
