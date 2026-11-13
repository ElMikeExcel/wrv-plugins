# ExcelSummons 0.1.8 — dead Bedrock buttons fixed + out-now counts

Commission: `../COMMISSION-018.md` (Mike's live findings, Nov 12,
2026, 6:51–6:52 PM CST). Base: 0.1.7 exactly as installed. Two
changes only; everything else is byte-identical to 0.1.7 — the
mana model (adopted = 0%), the caps, the dismiss scopes, the
confirm-gated adopted dismiss as the ONLY adopted-dismiss door,
and config.yml.

## §1 The dead-button defect and the fix

Mike's screenshot proved the 0.1.7 Bedrock main form RENDERS
("Your Summons", the registered line, all four buttons) but
tapping "Adopted pets (8)" did nothing — no sub-form, no chat
message, no log line, no exception. The server log confirmed:
ExcelSummons logged nothing at tap time.

Root cause (parent diagnosis, verified against the sources):
0.1.7's BedrockForms tries Floodgate FIRST using the OLD
Cumulus (`org.geysermc.cumulus.SimpleForm`) and attaches its
response handler by hunting for a 1-arg `validResultHandler`,
reading `clickedButtonId()` inside a proxy whose failures are
swallowed by `catch (Exception ignored)`. The old-Cumulus
response shape does not match (its accessor is
`getClickedButtonId`; the handler contract differs), so taps
were silently swallowed — and because the SEND succeeded, the
Geyser path never ran. The Java GUI path was unaffected.

The fix replaces BedrockForms wholesale with the live-proven
ExcelWandBars implementation (WandBars 0.3.15+ sources, taps
proven working on this server by Mike's Nov 12 proof):

- Geyser class loader ONLY (`GeyserApi.connectionByUuid`); the
  Floodgate attempt is removed.
- NEW Cumulus: `org.geysermc.cumulus.form.SimpleForm`.
- Response handling via `FormBuilder.resultHandler(BiConsumer)`:
  the handler receives (form, FormResponseResult); a valid
  result unwraps `ValidFormResponseResult.response()` →
  `SimpleFormResponse.clickedButtonId()`. A closed form
  reports −1 to the callback.
- The send hunts the connection's assignable 1-arg `sendForm`
  overload.
- The Summons API is unchanged:
  `BedrockForms.sendSimpleForm(player, title, body, buttons,
  onButton, log) → boolean`; false still falls back to chat.
- NO silent swallowing: a failed handler attach fails the
  build loudly; an unparseable response, a missing connection,
  a missing sendForm, or a failed send logs at WARNING.
- NOT imported from WandBars: FormContrast (its legacy-color
  label normalization). Summons labels are plain already; they
  stay plain.

`buildForm(loader, title, body, buttons, onButton, log)` is
exposed (the WandBars buildSimple pattern) so the harness and
the rig build forms through the EXACT production method and
drive responses through the attached handler.

Proof of the API shape was established against the real jar
before the rewrite (Geyser-Spigot 2.12.1-b1292, the staged
2.12.1 line that live b1298 also runs): SimpleFormResponse
declares clickedButtonId(); ValidFormResponseResult.of is a
public factory; FormImpl.callResultHandler is the dispatch
Geyser's own FormCache uses; SimpleFormDefinition
.handleFormResponse(form, raw) is the entry the client's raw
payload takes ("null" = closed).

## §2 Out-now counts on the main page

Mike: "I was hoping for some number of mounts or number of
cast pets would show so people know how many they have out."

MenuModel derives an `OutCounts` from the page's CAST entries
via `SummonRecord.baseKey` (tiered keys fold into their base):

- `mount` → mounts out
- `wolf` / `cat` / `familiar` → cast pets out
- every other cast base key (archer / bruiser / guardian /
  revenant / ghast) → combat summons out
- total cast out = the cast list size — the same rows the page
  renders, so the numbers can never disagree with the list.

Adopted pets are never counted: they are permanent animals,
not casts on the field.

Rendering (both renderers, one derivation — `Page.outNowLine()`):

- Bedrock main body, right after the registered line:
  `Out now: 1 mount · 2 cast pets · 1 combat summon (4 total)`,
  with the existing held-% line after it. All three groups
  ALWAYS print (a zero is information, not absence); if nothing
  is cast out the line is `Nothing out right now.` (the 0.1.7
  "No cast summons out right now." closer still renders).
- Java info book: the name stays `Mana held: H% of your pool`;
  the lore leads with the same out-now line, then the adopted
  count, then the adopted-hold-no-mana note.

## §3 Proof bar (per the commission)

- All 0.1.7 harness tests carried (only the two re-pins touch
  version strings) + new tests: counts derivation (incl.
  tiered folding, adopted exclusion, zero cases), the line's
  format and placement, the BedrockForms handler SHAPE
  (structural pin: new Cumulus in, old Cumulus / Floodgate /
  validResultHandler out), and a BEHAVIORAL handler test that
  builds all three form shapes against the real Geyser jar's
  Cumulus classes and drives a synthetic response through the
  attached handler for EVERY button position — via
  callResultHandler and via the raw-payload entry — plus the
  closed case.
- Rig on real Paper 26.3 + Magic 11.2.5: phase A (no Geyser)
  carries all 22 of 0.1.7's checks + the info-book counts lore;
  phase B adds the LIVE Geyser + Floodgate jars and drives
  synthetic responses through the attached handler against the
  real Cumulus classes for every button of all three forms.
  Send-only proof is the 0.1.7 failure and is not repeated.
- Triple build byte-identical, including a clean-room rebuild
  from the shipped source zip.
- A real Bedrock client's tap remains Mike's live proof; the
  packet states that plainly.

Excel — Wink Reyes Valley (Iris / Mike).
