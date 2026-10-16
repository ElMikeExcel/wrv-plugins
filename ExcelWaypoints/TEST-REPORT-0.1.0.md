# ExcelWaypoints 0.1.0 test report, Oct 16, 2026
Build: src/ExcelWaypoints/build.sh (JDK 25, javac --release 21). Runner is dependency-free (WaypointsTest). Result: 225 passed, 0 failed (test-output.log).
Coverage against the design edge list:
1 limit 5, 6th refused: pass (per player, frees on delete, simultaneous saves never pass the limit)
2 duplicate name: refused, case-insensitive, rename too
3 name cleanup: color codes (section and ampersand), empty, spaces only, symbols, newline/tab, 24 char cap, emoji, surrogate cut, command characters
4 Bedrock (.Name) and Java save, rename, delete: pass (fake players through the real Controller)
5 Java views Bedrock shared place and reverse: pass, dot prefix hidden in display, real name used for /tpa
6 Nether, End allowed; guild world blocked with plain message: pass (guild rule is a build default, UNCONFIRMED with Mike)
7 world removed: list, view, compass explain "world missing", delete allowed, no error: pass
8 compass in another world: no arrow, names the world: pass
9 ask to visit: owner offline, visits off, not shared, own place, no Essentials, cooldown, other world, owner hid place: pass. Owner declines, request timeout, requester moves or dies: handled by Essentials (not our code), UNCONFIRMED live
10 never teleports: source scan finds no teleport call; controller has exactly one runCommand and it is "tpa <owner>" as the visitor; compass only uses setCompassTarget: pass
11 persistence across restart, missing file, corrupt file (backup copy kept, boots empty), partly bad entries skipped, no temp file left: pass. Also real scratch boot with corrupt places.yml: warned, enabled, copy kept
12 simultaneous saves, double tap Yes on delete (chat and Bedrock), delete token expiry, wrong token: pass
13 menu while dead: /places refuses until respawn (code, not unit tested). Vehicle or sleeping: no special handling needed, UNCONFIRMED live
14 phone button length: longest Bedrock list button 42 chars (24 char name, far place), longest detail button 30. Test caps at 42. UNCONFIRMED on a real phone
15 permissions: use default true, admin default op, admin list/delete tested at console on scratch boot; service adminDelete tested
16 boot with ExcelTogether, ExcelBedrockMenu 1.5.6, Jobs, Essentials, Geyser, floodgate, ViaVersion, ChestShop, DeluxeMenus, others: enabled with no errors from ExcelWaypoints
17 without Essentials and without Geyser: boots clean, chat menu only (scratch boot 2); forms null and form-refused fallbacks tested
18 performance: 100 places save 10 ms, load 30 ms, 1000 family lookups 22 ms, 22 KB file; no repeating tasks, no chunk loading (source scan)
Real Cumulus check: forms built with the real floodgate Cumulus classes, replies replayed through Cumulus's own parser into our handlers (button tap, close, modal yes/no, custom form values, invalid reply). Found and fixed: wrong modal reply method name, invalid replies not reaching the handler.
Bugs found and fixed during build: place id generator overflow (crashed first save), newline/tab joined words.
Not covered: a real Java client, a real Bedrock phone, a real two-player /tpa accept, a real death while a menu is open.
