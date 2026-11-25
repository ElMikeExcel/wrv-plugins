# excelemfixes-0.18.0-20261125 — RIG RESULTS

From BUILD-RECORD.md (guidance builds recovery lane, Nov 25, 2026):

- Rig proof: boot clean, census 112 quests indexed (rig content).
- ON: a bot completed The Groves Call through EliteMobs' own flow (dialog click + /em quest complete) and received "New quest unlocked: 'Population control!' — King Nemian gives it." plus a second genuine dependent, "New quest unlocked: 'Acrobatics' — Posuere Hominem at the course end gives it."
- OFF (A/B control): signposting-enabled false + reload; Feeding the Sick completed the same way — completion + rewards arrived, zero unlock lines.
- Suites: 1,167 passed / 0 failed. Deterministic double-build byte-identical.
- Live boot after install (failsafe 20261125-1134): Done (36.602s), 0.18.0 ready, Quest Chain Clarity census 111 quests indexed on live content (see SOURCE.md note), SEVERE 0, reload clean.
