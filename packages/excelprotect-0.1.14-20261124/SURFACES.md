# ExcelProtect 0.1.14 — SURFACES (same-pass check)

Player-facing behavior change: a dropped stackable item no longer
comes back permanently split from its identical copies. No text,
menu, price, command, or Guide content changes anywhere.

- Guide knowledge / intents: NO CHANGE (Protect is a staff-side
  records plugin; nothing player-taught references the rid).
- Website pair (index.html + ai-knowledge.json): NO content
  change. At install time the supervisor chain bumps the
  ExcelProtect plugin-row version 0.1.13 -> 0.1.14 in the same
  pass as the records close-out (with the G2 duplicate check).
- What's New (website + in-game changelog): NO ENTRY — this is an
  invisible plumbing fix; players were never taught the rid
  exists. (If Mike wants a "your dropped items stack again" note,
  it is a one-line TEXT live-edit the parent can file separately.)
- Broadcaster tips: NO CHANGE (checked — no tip mentions item
  stacking or Protect internals).
- In-game menus: NO CHANGE (no menu renders Protect data).
- Player note owed (parent follow-up, not this packet): Jester
  reported the bug (Nov 24, 2026); when 0.1.14 is live, tell him
  the Protect half is fixed and ask for the drop/re-pick proof
  named in the packet. The EM food-variant half
  (P-EMFOOD-STACKSPLIT-20261124) is still open separately.
- Records owed at install (supervisor chain): wrv-plugins OPEN
  DRAFT record PR + Drive deposit (jar + source zip) for
  ExcelProtect 0.1.14 (G2 duplicate check before any push).
