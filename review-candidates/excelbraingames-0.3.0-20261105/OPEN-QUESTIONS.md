# ExcelBrainGames — Open Questions for Mike

Short list. Each has my recommendation; the spec (SPEC.md) is written assuming the recommendations, so a "go with yours" unblocks the build as-is.

**ALL SEVEN DECIDED (Mike, Nov 4, 2026):** "Go with your recommendations — start Phase 1." Q1–Q5 and Q7 stand as recommended below; Q6 was amended by Mike the same day (half payout for adults on kid tiers — see its entry).

1. **Word Search grid sizes.** The inventory GUI caps us at 9 columns × 6 rows, so v1 tops out at 9×6 (Kid 6×6). Bigger newspaper-style grids (12×12, 15×15) need a scrolling viewport.
   **Recommendation:** ship 9×6 in v1; add the viewport later only if 9×6 feels small in real play.

2. **Found-word highlighting.** When you find a word, its letters can stay lit green on the grid for the rest of the round, or the grid can stay clean and only the word list shows progress.
   **Recommendation:** keep the highlight on the grid — it's satisfying, and it helps Rylie see the board empty out as she finds words.

3. **Riddle answer strictness.** Exact-match only, or fuzzy (ignores "a/the", capitalization, and accepts listed alternate answers per riddle)?
   **Recommendation:** fuzzy, with an accepted-answers list per riddle in config. Exact-match on a typed riddle answer just manufactures frustration.

4. **Sudoku 9×9 display.** 81 cells don't fit one inventory screen, so the spec pages it (two views, rows 1–5 / 5–9, toggle button). Alternative: cap Sudoku at 6×6.
   **Recommendation:** keep 9×9 with paging — it's the tier the adults will actually live on — but it gets its own phone test in Phase 2, and if paging feels bad there, we cut to 6×6 then.

5. **Old saved rounds.** A round started Monday and finished Wednesday pays against Wednesday's cap in the spec. Alternative: a round only pays on the day it was started.
   **Recommendation:** pay on completion day (spec as written) — simpler, and nobody games a daily cap by stockpiling half-finished 4×4s.

6. **Kid tiers for non-kid players.** Spec: adults can play kid tiers in practice mode ($0). Alternative: kid tiers simply don't appear for non-kid accounts.
   **Recommendation:** practice mode. A parent playing the 4×4 alongside Rylie is a feature, not a leak — the anti-farm rule already removes the money.
   **DECIDED (Mike, Nov 4, 2026) — recommendation SUPERSEDED:** adults playing kid tiers earn **HALF PAYOUT**, not $0 practice mode. Kid tiers stay visible and playable for everyone; a kid-flagged player earns the full kid payout; a non-kid player earns exactly half (rounded down to the cent). The $1,500/day cap and pays-once rules apply unchanged. Built and unit/rig-proven in Phase 1 (see build-phase1/TEST-REPORT.md).

7. **Who's flagged "kid" at launch.** The anti-farm flag list starts empty in config. Rylie is the obvious first entry; anyone else (e.g., a future young guest) gets added by console command.
   **Recommendation:** flag Rylie at install; add others as they come. (Not really a decision — just confirming the starting list with you.)
