// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The form layer's session tokens — DESIGN-2.0 §2.5's standing
 * invariants, implemented once for every registry form:
 *
 * <ol>
 *   <li><b>Closed = cancelled.</b> The bridge reports a close
 *       separately; a close consumes nothing and runs nothing (the
 *       Bukkit layer simply drops the session).</li>
 *   <li><b>Stale answers are refused.</b> A token binds the player,
 *       the menu (+ group step), the world the form was opened in,
 *       and an expiry. A response arriving after a world change,
 *       after the TTL, or after a newer form replaced this one is
 *       dropped, never applied.</li>
 *   <li><b>Single-use answers.</b> The first valid response consumes
 *       the token; a duplicate or replayed response finds nothing
 *       pending.</li>
 *   <li><b>One live form per player.</b> Issuing a new token retires
 *       the player's previous one. Quitting invalidates it.</li>
 * </ol>
 *
 * Pure and Bukkit-free: the Bukkit form service supplies the clock
 * and the player's current world at answer time.
 */
public final class FormSession {

    public enum Verdict { OK, NO_SESSION, SUPERSEDED, CONSUMED, EXPIRED, WORLD_CHANGED }

    /** One issued form token. {@code group} is null for flat forms
     *  and picker forms; set for a grouped form's group step. */
    public record Token(long id, UUID player, String menu, String group,
                        String world, long issuedAtMillis, long ttlMillis) {}

    public static final class Store {
        private final Map<UUID, Token> live = new HashMap<>();
        private final Map<UUID, Long> consumed = new HashMap<>();
        private long nextId = 1L;

        /** Issue a token; any previous live token for the player is
         *  superseded (it will answer SUPERSEDED, not OK). */
        public synchronized Token issue(UUID player, String menu, String group,
                                        String world, long nowMillis, long ttlMillis) {
            Token t = new Token(nextId++, player, menu, group, world,
                    nowMillis, ttlMillis);
            live.put(player, t);
            return t;
        }

        /**
         * Validate AND consume an answer. Callers pass the token id
         * the form was issued under and the player's world NOW; only
         * an OK verdict may dispatch, and it consumes the token.
         */
        public synchronized Verdict answer(UUID player, long tokenId, String worldNow,
                                           long nowMillis) {
            Token t = live.get(player);
            if (t == null) {
                Long c = consumed.get(player);
                return c != null && c == tokenId ? Verdict.CONSUMED : Verdict.NO_SESSION;
            }
            if (t.id() != tokenId) return Verdict.SUPERSEDED;
            if (nowMillis - t.issuedAtMillis() > t.ttlMillis()) {
                live.remove(player);
                return Verdict.EXPIRED;
            }
            if (worldNow != null && t.world() != null && !worldNow.equals(t.world())) {
                live.remove(player);
                return Verdict.WORLD_CHANGED;
            }
            live.remove(player);
            consumed.put(player, tokenId);
            return Verdict.OK;
        }

        /** Drop the player's live token without answering (form
         *  closed, player quit, a chest menu opened instead). */
        public synchronized void invalidate(UUID player) {
            live.remove(player);
        }

        public synchronized void clear() {
            live.clear();
            consumed.clear();
        }

        public synchronized boolean hasLive(UUID player) {
            return live.containsKey(player);
        }
    }

    private FormSession() {}
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
