package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.UUID;

/**
 * The fact-injection source for the AI fallback (0.2.0): the PLUGIN
 * fetches live numbers; the model only ever reads them. Two kinds,
 * with a hard privacy line between them:
 *
 * - {@link #askerFacts}: the ASKER's OWN numbers only (claim blocks
 *   accrued/bonus/used/remaining, mcMMO power + top skills, Jobs
 *   levels, Vault balance, seal progress, Magic standing when it
 *   exists). The engine calls this with the asker's UUID and no
 *   other - there is deliberately no method that takes a second
 *   player's id for personal data.
 * - {@link #publicFacts}: facts that are already public in the
 *   world (guild seal progress, the Land Claimed board's top
 *   entries) - the same numbers anyone can read off the boards.
 *
 * Each fact is one short "Label: value" line. A missing plugin or
 * unreadable number simply omits its line (an empty list is fine);
 * the prompt assembler fences whatever it gets and the model is
 * told to treat anything absent as unknown.
 */
public interface FactsView {

    /** The asker's own live facts, as labeled lines. Never another player's. */
    List<String> askerFacts(UUID asker);

    /** Public world facts (seal progress, public board leaders). */
    List<String> publicFacts();
}
