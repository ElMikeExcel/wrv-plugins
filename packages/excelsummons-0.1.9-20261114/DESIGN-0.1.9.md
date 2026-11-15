# ExcelSummons 0.1.9 — pet names in the menu + in-menu rename

Commission: parent-backlog "ExcelSummons — 0.1.8 proof PASSED +
pet names + Back audit" (Mike, Nov 13, 2026, ~12:53 PM CST:
"Can we already rename the wolfs? So we know which one is
which?"). Base: 0.1.8 exactly as installed (base gate: the
0.1.8 tree rebuilt byte-identical, 835419aa…, before any
edit). Scope is the PET NAMES item only — the Back-button
audit is a separate lane. Everything else is unchanged: the
mana model (adopted = 0%), the caps, the dismiss scopes, the
confirm-gated adopted dismiss as the ONLY adopted-dismiss
door, and config.yml.

## §1 The defect

Verified against the 0.1.8 source (MenuModel.build): every
entry's displayName was the record's spell TYPE
(Nameplate.spellDisplayName over the base key) — a pet's real
name was never read. With 8 adopted wolves, all 8 list rows
and all 8 "Dismiss Wolf" buttons were identical, and the
dismiss confirm text could not name the actual animal either.
That is also a safety gap: per-pet dismiss permanently
removes a real tamed animal, and the player could not tell
which one a button removes. Meanwhile the in-world NAMEPLATE
already adopts a name-tag name as its base (Nameplate /
SummonsListener composition rule, never clobber) — renaming
already worked over the animal's head; only the menu ignored
it.

## §2 Display names (the fix)

MenuModel.build gains one more pre-computed input, exactly
the shape hpByEntity established in 0.1.7: a per-entity BASE
NAME map (entity id -> the name Nameplate.baseName computes
from the entity's live custom name, HP suffix stripped).
MenuModel stays pure; the plugin (ExcelSummons.menuPageFor)
reads the entities and fills the map with the SAME expression
refreshNameplate composes from, so menu and nameplate can
never disagree about what a pet is called.

An entry's displayName is the map's base name when present
and non-blank, else the spell type display name as before.
The name flows everywhere displayName already flowed:

- Java adopted-page row titles AND cast-page row titles
  (cast summons carry names the same way when Magic ever
  sets one — the map covers every record, not just adopted);
- the Bedrock per-pet buttons and the dismiss confirm title,
  body, and confirm button (all built from displayName);
- the adopted Bedrock body line now LEADS with the display
  name ("- Buddy (pack, persistent, HP 20/20) — ADOPTED,
  holds no mana") so Bedrock rows are distinguishable in the
  body text too, not only on buttons. Entry.listLine itself
  is UNCHANGED — it stays byte-for-byte the /summons list
  line, as the 0.1.7 invariant requires.

## §3 In-menu rename (adopted pets)

House buttons rule: no typed-command-only flow. Each adopted
pet gets a per-pet page (Java inventory view / Bedrock pet
form) carrying its facts and two actions: Rename and Dismiss
(dismiss still routes to the existing confirm step,
unchanged). The adopted page's row tap / pet button now
opens the pet's page instead of going straight to dismiss.

- Bedrock: the pet form's Rename button opens a Cumulus
  CustomForm with one text input (the new name, prefilled
  with the current name), built and answered through the
  SAME proven 0.1.8 resultHandler shape as the simple forms
  (BedrockForms.buildCustomForm — Geyser loader only, new
  Cumulus, nothing swallowed; a closed form submits null
  and changes nothing).
- Java: the pet page's Rename button closes the view and
  opens a CHAT PROMPT — the next chat message from that
  player (within 30 seconds, or the word "cancel") is taken
  as the new name. The message is cancelled as chat (the
  name never broadcasts); the prompt is one-shot and is
  dropped on expiry, quit, or use. (AsyncPlayerChatEvent is
  the seam — deprecated on Paper but still fired; the rig
  pass must prove the prompt live, see BUILD-RECORD.)

The rename itself (SummonsListener.renameAdopted) is gated
by the new pure RenamePolicy, the AdoptedDismissPolicy
shape: the record must exist, belong to the player, and be
ADOPTED origin. Cast summons are refused — their names are
theirs/Magic's; rename never clobbers another name source.
The name is SANITIZED by policy (legacy colour codes and
control characters stripped, whitespace collapsed, max 32
characters, and a name ending in the nameplate's own health
suffix is refused — the plate composition would eat it).
The listener then sets the entity's custom name to the
cleaned name and re-runs refreshNameplate, so the rename
composes through the exact nameplate rule (base + HP
suffix, visible) and shows over the animal's head; the
menu, the /summons list HP, and the plate all read the
same base from then on. A successful rename writes an INFO
audit line, like the adopted dismiss does. Renaming a pet
whose entity is not loaded is refused with a "stand near
it" message — the menu never names an animal it cannot see.

Acceptance (player proof, owed post-install): Mike's pack
shows distinct names in the list, each Dismiss button and
confirm names its own wolf, and a rename done in the menu
shows over the wolf's head.
