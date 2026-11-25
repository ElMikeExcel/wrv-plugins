# ExcelDowned 1.0.5 — wand-loss hardening (death deferral + mark hygiene)

Reason (two real losses): MikeExcel's Renewal (Wand) was silently deleted
twice — Nov 23, 2026 (logout while downed, return after bleed-out) and
Nov 24, 2026 (unclean disconnect while downed, rejoin after the session
expired). Both times the wand was restored by operator playerdata
surgery. The mirror investigation (wand-hardening lane, Nov 24, 2026)
isolated the killing step by elimination, and it is NOT the name mark:

- Plain joins, rejoins into a still-live downed session, and ONLINE
  bleed-out deaths are all safe (wand verified present after each).
- The one losing shape is the bleed-out death finalized DURING the
  join dispatch, when a rejoin finds the session already expired.
  Magic parks a keep-wand in its respawn inventory at any death and
  hands it back at respawn; that cycle works for normal deaths but
  breaks when the death fires inside the login sequence — the parked
  wand never lands back: not dropped, not carried, not in the death
  record. Both live losses were this shape (deaths ~1–2 s after the
  return join). Reproduced on the mirror on the unpatched stack AND
  with the mark-hygiene changes alone.

Headline change (new config join-expired-death-delay-ticks, default
100 ≈ 5 s; 0 = legacy immediate behavior):

0. **The expired-join death is deferred past the join.** The join
   handler schedules the finalize a few seconds out instead of
   firing it mid-dispatch; the state tick skips deferred players.
   The death lands as an ordinary online death, whose respawn
   cycle returns the wand (mirror-proven). The session is not
   consumed until the death actually fires, so relogging can delay
   the death but never dodge it.

Mark hygiene (the incident-1 aggravator — the Nov 23 mage save DID
absorb the marked name into the wand's owner string; these changes
keep the mark from ever serving as identity; behind the existing
wand-owner-guard.unmark-on-quit toggle, off = pre-1.0.5 join behavior):

1. **Mark last at join.** The join path applies the downed state WITHOUT
   the name mark at NORMAL; a new MONITOR-priority join handler
   (DownedListener.onJoinMarkLast → ExcelDowned.applyJoinMark) applies
   the mark at the end of the join dispatch, and only if the session is
   still live at that point. During the entire join, the player's name
   is their real name. Visible behavior unchanged — the mark lands in
   the same event dispatch.
2. **Bookkeeping-free unmark fallback.** unmarkForHandoff no longer
   depends on the in-memory savedNames map: when no saved original
   exists, a current display/list name that IS this plugin's mark for
   this player (new DownedMark.isMarkedName) is reset to the raw
   profile name. Covers server restarts while a session persisted and
   any future path that loses the map.

Companion piece: ExcelMagicGate 0.3.5 adds the resurrection net
(Mage.restoreWand on the bound-but-not-carried detection) and escalates
that detection to a WARNING — see the wand-hardening lane record
(~/workspace/wand-hardening-20261124/RECORD.md) for the full seam
analysis and the mirror repro evidence.
