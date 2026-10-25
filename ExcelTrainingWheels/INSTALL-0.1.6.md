# Install ExcelTrainingWheels 0.1.6 (parent does this; nothing here was applied)

Planned together with the S1 MobCombatSettings step, at a verified-empty moment.

1. Panel: confirm plugins/ExcelTrainingWheels-0.1.5.jar sha256 = fbe4b1e826ff87ac0294aabc51074720471aeee7c7f9c80a137e8d01a4e89afa.
2. Move the 0.1.5 jar OUT of plugins/ (keep it, e.g. plugins/_disabled/), upload
   ExcelTrainingWheels-0.1.6.jar (sha256 in SHA256SUMS.txt). Only one TW jar in plugins/.
3. A plugin jar swap needs a server RESTART (no /reload). The live config.yml is kept
   (saveDefaultConfig never overwrites it), so debug stays as live has it: debug: true.
4. Boot log must show:
   - "Enabling ExcelTrainingWheels v0.1.6"
   - "bedrock loading-screen grace: ON (4s ..., detection Floodgate API)"
   - "shield compensation divisor source: LIVE EliteMobs" (values may read "?" at enable,
     EliteMobs loads its configs after TW enables; they bind by themselves)
   Then console "tw reload" must show "divisor LIVE scaled x1.75 / normalized x1.00 / other x1.00"
   after S1 (x1.60 before S1).

## Live config keys
No live config edit is REQUIRED: every new key falls back to the defaults below when missing.
Recommended additions (append, never replace the protected.players section):

    shield:
      compensation:
        source: live            # new; missing = live
        dealt-multipliers:      # update to the new ladder (fallback + sync check)
          "0": 1.60
          "1": 1.75
          "2": 1.90
          "3": 2.05
          "4": 2.20
          "5": 2.20
    bedrock-grace:              # new section; missing = these defaults
      enabled: true
      seconds: 4
      allow-lava: true
      show-actionbar: true
      actionbar-message: "Loading in... protected"

Last recorded live table (Oct 25): 0:1.60, 1:1.75, 2:1.90, 3:2.05, 4:2.20, 5:2.35. Only row 5 differs
(2.35 -> 2.20). That matters only at Seal 5, and with source: live the shield follows EliteMobs anyway.
If the live table rows differ from live EliteMobs, the console prints a WARNING once per
change ("table row seal N = x.. but EliteMobs ... is x.."); the shield already follows live.
Check live excluded-worlds: with source: live an EliteMobs world does not need to be listed.
