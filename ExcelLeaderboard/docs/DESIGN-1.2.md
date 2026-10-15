# ExcelLeaderboard 1.2 design note: shop-style Bedrock forms

Written Oct 15, 2026 (research only; no code changed). Question from Mike:
the /leaderboard chest menu "kind of sucks at least on mobile" — can it be
"like the shop one"? Answer: yes. ExcelShops 1.1's mobile flow is a pattern
we already own, and Leaderboard can copy it directly. This note records how
the shop actually does it (with evidence), how 1.2 would mirror it, what it
needs, and the open risks.

## (a) How ExcelShops 1.1 actually renders Bedrock menus

ExcelShops builds native Bedrock forms ITSELF, inside its own jar, using the
Geyser Cumulus form API — reached entirely by reflection, with zero
compile-time dependency on Geyser, Cumulus, or Floodgate. It does NOT use
the BedrockMenu plugin at all (zero references in its sources), and
DeluxeMenus is not the Bedrock renderer either (see the bridge note below).

Evidence (source: `ExcelShops-1.1-DRAFT-source-guide.zip` in
`~/workspace/wrv-handoff-2026-10-14/`, folder `source-publish/WRVShops/`;
the same classes are compiled into the installed jar
`~/workspace/excelbuildmode-pilot/install-staging/upload/shop/ExcelShops-1.1.jar`,
which contains `BedrockForms.class`, `MobileMenuBridge.class`,
`MobileCatalog.class`, `ShopUi.class`):

- `src/com/winkreyes/shops/BedrockForms.java` — the whole bridge:
  - Bedrock detection, `isBedrock(Player)`: reflectively loads
    `org.geysermc.geyser.api.GeyserApi` from the Geyser plugin's own
    classloader and checks `connectionByUuid(UUID) != null`; falls back to
    `org.geysermc.floodgate.api.FloodgateApi.isFloodgatePlayer(UUID)`.
    Only public API interfaces are accepted; any error means "not proven
    Bedrock", never a guess.
  - Choice menus, `choices(...)`: builds `org.geysermc.cumulus.form.SimpleForm`
    via its builder (`title`, `content`, one `button` per label), reads the
    answer from `SimpleFormResponse.clickedButtonId()`.
  - Text/number entry, `number(...)`: builds
    `org.geysermc.cumulus.form.CustomForm` with one `input`, reads
    `CustomFormResponse.asInput(0)`. (Leaderboard needs only SimpleForm.)
  - Both form types REQUIRE `validResultHandler` and
    `closedOrInvalidResultHandler` to exist before use, and every form is
    one-shot through a `ResponseGate`: a late, duplicate, or post-close
    response can never act twice.
  - Sending: `GeyserApi.sendForm(UUID, Form)`. If there is no local Geyser
    connection the send returns false and the caller falls back safely.
  - `pom.xml` confirms the design: dependencies are VaultAPI, ChestShop and
    a test-only logger — no Geyser/Cumulus artifact at all. The reflection
    IS the dependency strategy.
- `src/com/winkreyes/shops/ShopUi.java` — the flow built on those forms:
  - `showMobileRoot`: one SimpleForm, a button per category, plus
    "Back to Menu" and Cancel.
  - `showMobileCategory`: one button per product, details (bundle, price)
    written into the button label text.
  - `showMobileHub` / `mobileExactAmount`: details as form content text,
    actions as buttons, exact amounts via the CustomForm input. Back always
    returns to the exact originating category.
  - A mobile-origin form failure CANCELS the flow ("shop is closed; no
    purchase started") rather than falling through to a chest inventory.
    That fail-closed choice is purchase safety, not a forms limitation.
- `src/com/winkreyes/shops/MobileMenuBridge.java` — DeluxeMenus' only role:
  the shop's Java UI lives in DeluxeMenus menus. When a positively-Bedrock
  player opens one of the six shop menus, the bridge cancels the
  `InventoryOpenEvent` at HIGHEST (DeluxeMenus' click listener ignores
  cancellation, so the open itself must be stopped), removes DeluxeMenus'
  registered holder via its public `Menu.getMenuHolder` / `closeMenu`
  (reflection again), then hands off to `ShopUi.showMobileMenu`, which shows
  the Cumulus form instead. Java players are never touched.
- Wiring: installed `plugin.yml` softdepends on
  `[Vault, GriefPrevention, ChestShop, Geyser-Spigot, floodgate, DeluxeMenus]`;
  installed `config.yml` carries `bedrock-forms: true` (the kill switch).
- Prior art on this same server: the older BedrockMenu plugin
  (`~/workspace/bedrockmenu/src/com/winkreyes/bedrockmenu/GeyserForms.java`)
  uses the identical reflection approach. The pattern is proven house style,
  not an experiment.

## (b) How ExcelLeaderboard 1.2 mirrors it

New code stays inside the existing 1.1 structure
(`~/workspace/excelleaderboard/work/ExcelLeaderboard-1.1/src/main/java/com/winkreyes/leaderboard/`):

1. New class `paper/BedrockBoardForms.java`, adapted from the shop's
   `BedrockForms` pattern (same reflection discipline, same ClassLookup
   test seam, SimpleForm only):
   - `isBedrock(Player)` — Geyser `connectionByUuid`, Floodgate fallback.
   - `choices(Player, title, content, labels, IntConsumer, Runnable)` with
     the one-shot ResponseGate copied in spirit: one answer per form,
     close/invalid routes to the close action, send failure returns false.
2. Entry point in `ExcelLeaderboard.onCommand` (unchanged permission flow):
   - Bedrock player AND `bedrock-forms: true` (new config key, default true,
     mirroring the shop) AND form send succeeds → form flow.
   - Everyone else → the existing `BoardMenu` chest UI, untouched.
   - If a Bedrock player's form send fails, fall back to the chest menu.
     Deliberate difference from the shop: a leaderboard is read-only, so a
     chest fallback is harmless convenience, not a safety hole.
3. Main form (mirrors `showMobileRoot`):
   - SimpleForm, title "Leaderboards", content "Pick a board."
   - One button per available board, using the exact availability filter
     `BoardMenu` already applies to the snapshot. Button label carries the
     leader line because Bedrock buttons are text-only, e.g.
     "Top Miner\nLeader: Name — 12,345 blocks mined" in the shop's label
     style. Closing the form just ends (no close action needed).
4. Board form (mirrors the shop's hub: details in content, actions as
   buttons):
   - SimpleForm, title = board title (e.g. "Top Miner").
   - Content = top 10 as plain text lines ("#1 Name — value", one per line,
     values via the existing `Category.formatValue`), then a blank line and
     the viewer's own line: "Your rank: #N — value", or
     "Your rank: not on this board yet" — the same two cases `openBoard`
     already computes (`BoardMath.rankOf`, top via `BoardMath.top`).
   - Buttons: "← All boards" (opens the main form again) and "Close".
   - Data is read from `plugin.snapshot()` at open time, same as the chest
     UI, so both UIs always agree.
5. `plugin.yml`: bump to 1.2, add `Geyser-Spigot` and `floodgate` to
   softdepend, and per Mike's dependency-disclosure rule the description
   gains a plain line: Bedrock players get native form menus via Geyser
   (Cumulus) when Geyser is installed; without it everyone gets the chest
   menu. README updated the same way.
6. Tests (workflow rule: suites before "ready"):
   - Form-content tests: top-10 text building, own-rank present/absent,
     empty board, more than 10 entries, button-to-board mapping, leader
     line formatting — against fixture API interfaces through the
     ClassLookup seam, exactly how the shop's `BedrockFormsTest` works.
   - Extend `LiveApiSignatureTests` to assert the Cumulus/Geyser targets
     (SimpleForm builder methods, `clickedButtonId`, `GeyserApi.sendForm`,
     `connectionByUuid`, Floodgate `isFloodgatePlayer`) against the real
     Geyser/Floodgate jars, the same way 1.1 pinned the mcMMO/Jobs targets.
   - Local boot test on the local runtime, which already carries real
     Geyser + Floodgate jars (`~/workspace/minecraft-server/plugins/`).

## (c) Dependencies: build and runtime

- Compile time: NOTHING new. Reflection only — JDK 25 + Paper API, exactly
  the current 1.1 build. No Cumulus/Geyser jar needs to enter the build.
  (If typed compilation were ever wanted, official `Geyser-Spigot.jar` and
  `floodgate-spigot.jar` sit in the handoff review zip's
  `publisher-dependencies/`, and full jars are in
  `~/workspace/minecraft-server/plugins/` — the Floodgate there is
  2.2.5-b141, the live build. `~/workspace/economy-jars/` and the local
  Maven repo hold no Geyser/Cumulus artifacts, and none are needed.)
- Runtime: Geyser-Spigot (live: 2.11.3-b1251) supplies Cumulus and
  `sendForm`; Floodgate (live: 2.2.5-b141) supplies the backup detection.
  BOTH ARE ALREADY ON THE LIVE SERVER — ExcelShops 1.1 runs on exactly
  these today, and Mike's phone check proved the path end to end. No new
  plugin installs, no server changes beyond the Leaderboard jar swap and
  its config key.

## (d) Open questions and risks

1. Prerequisite, already decided: the third-party Leaderboard add-on must
   actually be removed (Mike chose removal, Oct 15) and the server
   restarted, or /leaderboard still never reaches our plugin on ANY
   device and the new forms are moot in practice.
2. Cumulus form callbacks arrive off the server thread; the shop
   re-enters safely via its gate + scheduler hop. 1.2 only opens another
   form from a callback (no world/economy state), but it must copy the
   same discipline anyway — no Bukkit calls straight from a callback.
3. Bedrock forms are text-only: no player heads, icons, or item art like
   the chest board view. Formatting follows the shop's proven plain style
   (line breaks inside labels, "•" separators). Whether "§" colour codes
   render acceptably in Cumulus content is a phone-check question, not an
   assumption.
4. A form shows the snapshot as of the tap; the chest UI has the same
   property (60s cache), so no new staleness — but worth stating in the
   completion report, not discovering later.
5. Detection edge: Floodgate can identify a Bedrock player even if Geyser
   ran on a proxy, but `sendForm` lives in the local Geyser API. On WRV
   Geyser is local, so this cannot bite here; the code still treats
   "detected but unsendable" as chest fallback.
6. Questions for Mike when 1.2 is briefed: (i) board form shows top 10
   exactly like the chest view (this design assumes yes); (ii) "Close"
   button vs relying on the form's own close gesture (the shop uses
   explicit Cancel buttons — this design matches the shop); (iii) Java
   players stay chest-only (assumed yes; the ask was about mobile).
7. As with the shop, automated tests cannot prove a delivered form reads
   well on a real phone. Mike's in-game check on Bedrock is the real gate,
   recorded separately from build/test status as always.

## Layout decisions from Mike's review (Oct 15, 2026, ~12:36-12:38 AM)

- Mike screenshot the 1.1 chest menu on mobile: icons bunched mid-grid, no visible labels on Bedrock, easy to fat-finger. His asks: organize the items instead of "thrown all over the box", and "leave a space between each so ppl don't fat finger".
- Java chest layout (1.2): fixed spaced grid, one empty slot on both sides of every board icon (slots 10, 12, 14, 16 / 19, 21, 23, 25 / center for the 9th), replacing the current contiguous run.
- Mike then asked for the phone forms to be SMALLER so more fit on screen. Agreed shape: compact one-line buttons (board name left, leader + value right, same line), slim padding, all 9 boards visible on one screen with no scrolling; board view shows the full top 10 in tighter lines with the viewer's rank pinned. Buttons stay full width, which is what keeps taps fat-finger safe; only the height shrank. Diagram v2 shown to Mike Oct 15.

## Java screen question (Mike, Oct 15, ~12:39 AM)

Mike: "Can the man's players not have a better screen? The whole chest design is a bit lame and not modern" (read as: the Java players). Facts: the Java client has NO native form UI; the chest inventory is its only built-in window, so a Bedrock-style form cannot be sent to a Java client. Two real upgrades exist:
- (A) Clickable chat menu: /leaderboard prints a compact panel in chat (one line per board, leader shown, every line a real click target); a board view prints the ranked top 10 as clean text with the viewer's rank pinned and a clickable back line. Same design language as the Bedrock form, no downloads, works on the vanilla client today. Recommended.
- (B) Resource-pack custom GUI: chest window skinned with custom artwork via a server resource pack. Prettiest, but players must accept the pack download, it is a much bigger build, and pack art breaks/needs upkeep across version bumps.
- (C) Keep the chest for Java with the spaced grid from Mike's earlier feedback.
Awaiting Mike's pick; the 1.2 build go covers whichever he chooses. Diagram of (A) shown to Mike Oct 15.
