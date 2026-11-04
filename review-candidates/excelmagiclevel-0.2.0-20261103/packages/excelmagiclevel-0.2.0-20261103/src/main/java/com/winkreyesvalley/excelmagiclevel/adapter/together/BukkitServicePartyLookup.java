package com.winkreyesvalley.excelmagiclevel.adapter.together;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExcelMagicLevel — the REAL party source (SPEC §4.5/§0.3): reads party
 * membership from a {@link PartyLookup} implementation registered with
 * Bukkit's ServicesManager. The registration is ExcelTogether's side of
 * the contract — the ~50-line addition designed in TOGETHER-API-NOTE.md
 * (ExcelTogether 0.4.3, the live version, registers nothing: verified
 * against its jar). Until that addition ships, {@link #available()} is
 * false and the plugin selects the labelled proximity fallback instead.
 *
 * <p>Fail-soft (SPEC §9.2): a provider that throws degrades that one
 * lookup to "no party" with a single warning — it never errors into the
 * cast path and never silently becomes proximity (a broken party service
 * must not quietly change who gets copies).</p>
 */
public final class BukkitServicePartyLookup implements PartyLookup {
    private final ServicesManager services;
    private final Logger logger;
    private PartyLookup provider;
    private boolean providerResolved;
    private boolean warnedOnFailure;

    public BukkitServicePartyLookup(ServicesManager services, Logger logger) {
        this.services = services;
        this.logger = logger;
    }

    /** True when some plugin has registered a PartyLookup service. */
    public boolean available() {
        return resolve() != null;
    }

    private PartyLookup resolve() {
        if (!providerResolved) {
            RegisteredServiceProvider<PartyLookup> registration = services.getRegistration(PartyLookup.class);
            provider = registration == null ? null : registration.getProvider();
            providerResolved = true;
        }
        return provider;
    }

    @Override
    public Set<UUID> partyMembers(UUID player) {
        PartyLookup delegate = resolve();
        if (delegate == null || delegate == this) {
            return Set.of(player);
        }
        try {
            Set<UUID> members = delegate.partyMembers(player);
            return members == null || members.isEmpty() ? Set.of(player) : members;
        } catch (Throwable t) {
            if (!warnedOnFailure) {
                warnedOnFailure = true;
                logger.log(Level.WARNING,
                        "ExcelMagicLevel: the registered PartyLookup service failed — party share is off "
                                + "until it recovers (own XP unaffected)", t);
            }
            return Set.of(player);
        }
    }
}
