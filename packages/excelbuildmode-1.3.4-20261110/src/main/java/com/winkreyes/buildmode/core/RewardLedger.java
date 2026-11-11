package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.util.*;

/** Pure-Java, synchronized admission/origin ledger. No player objects and no deferred award replay. */
public final class RewardLedger {
    public static final int API_VERSION = 1;
    public static final int MAX_OPEN_TOKENS = 100_000, MAX_SESSION_IDENTITIES = 100_000;
    public static final class CapacityExceeded extends IllegalStateException {
        public CapacityExceeded(String message) { super(message); }
    }
    public static final Map<String, Set<String>> COVERAGE = Map.of(
        "Jobs", Set.of("jobs.action.v1", "jobs.perform.v1", "jobs.deferred-origin.v1", "jobs.pending-drain.v1"),
        "mcMMO", Set.of("mcmmo.root-origin.v1", "mcmmo.personal.v1", "mcmmo.party-share.v1", "mcmmo.party-xp.v1", "mcmmo.deferred-origin.v1", "mcmmo.pending-drain.v1"),
        "EliteMobs", Set.of("elitemobs.contributor-rewards.v1", "elitemobs.deferred-origin.v1", "elitemobs.pending-drain.v1"));
    public static final class AdmissionRejected extends IOException {
        public AdmissionRejected(String message) { super(message); }
    }
    @FunctionalInterface public interface DurablePreparation { void run() throws Exception; }
    private record Registration(String revision, Set<String> coverage) { }
    private static final class Token {
        final UUID actor, receiver, target;
        final String provider, source;
        final boolean allowed;
        final long generation;
        int references = 1;
        Token(UUID actor, UUID receiver, UUID target, String provider, String source, boolean allowed, long generation) {
            this.actor = actor; this.receiver = receiver; this.target = target; this.provider = provider;
            this.source = source; this.allowed = allowed; this.generation = generation;
        }
    }
    private final UUID epoch = UUID.randomUUID();
    private final Set<String> required;
    private final Map<String, Registration> providers = new HashMap<>();
    private final Set<String> unavailable = new HashSet<>();
    private final Set<UUID> guarded = new HashSet<>();
    private final Map<UUID, Long> generations = new HashMap<>();
    private final Map<UUID, Integer> pending = new HashMap<>();
    private final Map<UUID, Token> tokens = new HashMap<>();
    private boolean closed;
    private String capacityProblem;
    private String operationalProblem;
    private final int maxTokens, maxIdentities;
    public RewardLedger(Set<String> required) { this(required, MAX_OPEN_TOKENS, MAX_SESSION_IDENTITIES); }
    /** Explicit lower limits are for isolated tests; production uses the fixed reviewed defaults. */
    public RewardLedger(Set<String> required, int maxTokens, int maxIdentities) {
        if (maxTokens < 1 || maxIdentities < 1) throw new IllegalArgumentException("Positive ledger limits required");
        this.maxTokens = maxTokens; this.maxIdentities = maxIdentities;
        if (!COVERAGE.keySet().containsAll(required)) throw new IllegalArgumentException("Unknown required provider");
        this.required = new HashSet<>(required);
    }
    public UUID epoch() { return epoch; }
    public synchronized boolean registerProvider(String provider, String revision, String[] coverage) {
        Set<String> expected = provider == null ? null : COVERAGE.get(provider);
        if (closed || expected == null || revision == null || revision.isBlank() || coverage == null || unavailable.contains(provider)) return false;
        Set<String> offered = new HashSet<>(Arrays.asList(coverage));
        if (offered.size() != coverage.length || !expected.equals(offered)) return false;
        Registration next = new Registration(revision, Set.copyOf(offered));
        Registration previous = providers.get(provider);
        if (previous != null && !previous.equals(next)) { unavailable.add(provider); return false; }
        providers.put(provider, next); return true;
    }
    public synchronized String readinessProblem() {
        if (closed) return "Reward bridge is stopping; a full server restart is required.";
        if (operationalProblem != null) return operationalProblem;
        if (capacityProblem != null) return capacityProblem;
        for (String name : new TreeSet<>(required)) {
            if (unavailable.contains(name)) return name + " changed or stopped; a full server restart is required.";
            if (!providers.containsKey(name)) return name + " has not registered complete Build Mode reward coverage.";
        }
        return null;
    }
    /** Entry-only operational failure; known recovery actors are guarded separately. Never suppress unrelated Survival. */
    public synchronized void blockAdmission(String problem) {
        if (problem == null || problem.isBlank()) throw new IllegalArgumentException("Operational problem required");
        if (operationalProblem == null) operationalProblem = problem;
    }
    public synchronized void providerUnavailable(String provider) {
        if (provider != null && COVERAGE.containsKey(provider)) { required.add(provider); unavailable.add(provider); }
    }
    public synchronized boolean isRequired(String provider) { return required.contains(provider); }
    /** Initial loaded providers are fixed at cold start; a newly loaded provider makes hot loading unsupported. */
    public synchronized void providerLoaded(String provider) {
        if (provider != null && COVERAGE.containsKey(provider) && required.add(provider)) unavailable.add(provider);
    }
    /** Snapshot access/file writes only inside preparation. NEVER game-mode, permissions, saves or event callbacks. */
    public synchronized void admit(UUID player, DurablePreparation preparation) throws Exception {
        Objects.requireNonNull(player); Objects.requireNonNull(preparation);
        String problem = readinessProblem();
        if (problem != null) throw new AdmissionRejected(problem);
        if (guarded.contains(player)) throw new AdmissionRejected("Recovery is already pending.");
        if (pending.getOrDefault(player, 0) != 0) throw new AdmissionRejected("Your earned Survival rewards are still settling. Try Build Mode again after they finish.");
        if (!generations.containsKey(player) && generations.size() >= maxIdentities) {
            capacityProblem = "Reward session capacity reached; new Build entry is blocked until administrator review and a full restart.";
            throw new AdmissionRejected(capacityProblem);
        }
        preparation.run();
        // Reentrant preparation is read-only; defend the boundary if an implementation violates that rule.
        if (pending.getOrDefault(player, 0) != 0) {
            guardRecovery(player);
            throw new IllegalStateException("Reward producer ran during durable preparation");
        }
        guardRecovery(player);
    }
    public synchronized void guardRecovery(UUID player) {
        Objects.requireNonNull(player);
        if (guarded.add(player)) {
            // Never discard a durable recovery guard to satisfy a memory cap. Startup recovery wins.
            if (!generations.containsKey(player) && generations.size() >= maxIdentities)
                capacityProblem = "Recovery identity capacity reached; administrator review and a full restart are required.";
            generations.merge(player, 1L, Math::addExact);
        }
    }
    public synchronized void releaseGuard(UUID player) { guarded.remove(player); }
    public synchronized boolean isGuarded(UUID player) { return guarded.contains(player); }
    /** State-only cold-incomplete-provider query; null/stopped state is explicitly unavailable and denied. */
    public synchronized boolean isActorGuarded(UUID player) { return player == null || closed || guarded.contains(player); }
    public synchronized int pending(UUID player) { return pending.getOrDefault(player, 0); }
    public synchronized int openTokens() { return tokens.size(); }
    public synchronized void closeOrigins() { closed = true; }
    public synchronized UUID begin(UUID actor, String provider, String source, UUID target, boolean sourceAllowed) {
        if (actor == null || source == null || source.isBlank() || !providers.containsKey(provider) || unavailable.contains(provider) || closed) return null;
        boolean allowed = sourceAllowed && !guarded.contains(actor);
        return add(new Token(actor, actor, target, provider, source, allowed, generations.getOrDefault(actor, 0L)));
    }
    public synchronized boolean isAllowed(UUID handle) { Token token = tokens.get(handle); return token != null && token.allowed; }
    public synchronized long generation(UUID handle) { Token token = tokens.get(handle); return token == null ? -1 : token.generation; }
    /** Preview only. Fork recipients before computing a split; this method does not reserve pending work. */
    public synchronized boolean canReceive(UUID handle, UUID recipient) {
        Token token = tokens.get(handle); return recipient != null && token != null && token.allowed && !guarded.contains(recipient);
    }
    public synchronized UUID fork(UUID handle, UUID recipient) {
        Token parent = tokens.get(handle);
        if (parent == null || recipient == null) return null;
        return add(new Token(parent.actor, recipient, parent.target, parent.provider, parent.source,
            parent.allowed && !guarded.contains(recipient), parent.generation));
    }
    public synchronized boolean retain(UUID handle) {
        Token token = tokens.get(handle);
        if (token == null || token.references == Integer.MAX_VALUE) return false;
        token.references++; return true;
    }
    /** Release one owned reference. Callers need per-owner once flags; duplicate live releases are NOT idempotent. */
    public synchronized void complete(UUID handle) {
        Token token = tokens.get(handle);
        if (token == null) return;
        if (--token.references != 0) return;
        tokens.remove(handle);
        if (token.allowed) {
            int count = pending.getOrDefault(token.receiver, 0);
            if (count <= 0) throw new IllegalStateException("Pending reward accounting underflow");
            if (count == 1) pending.remove(token.receiver); else pending.put(token.receiver, count - 1);
        }
    }
    private UUID add(Token token) {
        if (tokens.size() >= maxTokens) {
            capacityProblem = "Reward token capacity reached; new Build entry is blocked until administrator review and a full restart.";
            throw new CapacityExceeded(capacityProblem);
        }
        UUID handle;
        do { handle = UUID.randomUUID(); } while (tokens.containsKey(handle));
        // Compute before publishing the handle so arithmetic failure cannot leave an unaccounted live token.
        int count = token.allowed ? Math.addExact(pending.getOrDefault(token.receiver, 0), 1) : 0;
        tokens.put(handle, token);
        if (token.allowed) pending.put(token.receiver, count);
        return handle;
    }
}
