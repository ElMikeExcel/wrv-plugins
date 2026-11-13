package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Genuine repeated item-use input, not a timer after one click. Thread safe for Geyser loops. */
public final class CrouchHoldGate {
    private static final long HOLD_MS = 400, MAX_GAP_MS = 450, EXPIRE_MS = 5000;
    private static final int MAX_PLAYERS = 2048;
    private static final class State {
        final int slot; final long start; long last; boolean fired, consumed;
        State(int slot, long now) { this.slot=slot; this.start=now; this.last=now; }
    }
    private final Map<UUID, State> states = new HashMap<>();
    /** initial=true is PLAYER_INPUT; held=true is protocol SIMULATION_TICK. */
    public synchronized boolean accept(UUID id, int slot, boolean crouched, boolean initial, boolean held, long now) {
        if (id == null) return false;
        if (!crouched || slot < 0 || slot > 8 || (!initial && !held)) { states.remove(id); return false; }
        State s=states.get(id);
        if (initial) {
            // A fresh click always restarts the requirement; rapid taps cannot accumulate a hold.
            if (states.size() >= MAX_PLAYERS && s == null) {
                states.entrySet().removeIf(e -> now - e.getValue().last > EXPIRE_MS);
                if (states.size() >= MAX_PLAYERS) return false;
            }
            states.put(id,new State(slot,now)); return false;
        }
        if (s == null || s.slot != slot || now < s.last || now-s.last > MAX_GAP_MS) {
            states.remove(id); return false;
        }
        s.last=now;
        if (s.fired || now-s.start < HOLD_MS) return false;
        s.fired=true; return true;
    }
    /** Main-thread dispatch must consume the still-current hold once, after lifecycle cancellation checks. */
    public synchronized boolean consume(UUID id, int slot, long now) {
        State s=states.get(id);
        if(s==null || s.slot!=slot || !s.fired || s.consumed || now<s.last || now-s.last>MAX_GAP_MS)return false;
        s.consumed=true;return true;
    }
    public synchronized void reset(UUID id) { states.remove(id); }
    public synchronized void clear() { states.clear(); }
}
