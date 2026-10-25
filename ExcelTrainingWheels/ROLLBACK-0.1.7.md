# Rollback ExcelTrainingWheels 0.1.7 -> 0.1.5 (the live version before this install)

0.1.7 replaces 0.1.6, so the rollback target is the 0.1.5 jar backup made by INSTALL step 2.

1. Quick off, no restart: night-hearing.enabled: false, then "tw reload". Every zombie TW pulled
   is released and every steered zombie stops; beds go back to the vanilla rule. Bedrock grace can
   be turned off the same way (bedrock-grace.enabled: false). Use this first if only one feature misbehaves.
2. Full rollback (server empty, one-minute warning + countdown rule if anyone is on):
   a. Panel Shut Down.
   b. Rename plugins/ExcelTrainingWheels-0.1.7.jar -> plugins/ExcelTrainingWheels-0.1.7.jar.bak-20261025-rollback
      (never delete; never overwrite an existing .bak).
   c. Rename plugins/ExcelTrainingWheels-0.1.5.jar.bak-20261025 back to plugins/ExcelTrainingWheels-0.1.5.jar
      and confirm sha256 fbe4b1e826ff87ac0294aabc51074720471aeee7c7f9c80a137e8d01a4e89afa.
      Exactly one loadable TW jar in plugins/.
   d. Panel Start.
3. Config: the install did not edit the live config.yml. 0.1.5 ignores shield.compensation.source,
   bedrock-grace.*, night-hearing.* and doors.wood-fallback-* (unknown keys are harmless). Restore the
   downloaded config.yml copy only if something else changed it.
4. Tip: the night-hearing entry needs ExcelTrainingWheels>=0.1.7 (needs-plugin), so it goes quiet by
   itself on 0.1.5. To remove it, restore plugins/ExcelTips/tips.yml.bak-20261025-nighthearing (the
   ClaimFly file, sha256 f69e7283...) and "tips reload". Never roll tips.yml back further: the port
   warning and ClaimFly entries live in it.
5. Verify: boot log "Enabling ExcelTrainingWheels v0.1.5", no "night hearing" and no "bedrock
   loading-screen grace" line, zero SEVERE, guildgate status unchanged. Record in Big Brain, Route
   Sheet, daily log, version map and website.
