#!/bin/bash
# Compile and run the ExcelEMFixes plain-JVM suites.
# Run from the project root: the suites read src/quest-destinations.yml.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
M2=/home/hatch/workspace/m2-repo
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$M2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
CP="$CP:libs/ExcelItems-0.1.0.jar"
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/test-classes \
  src/src/excelemfixes/shared/TextUtil.java \
  src/src/excelemfixes/shared/Views.java \
  src/src/excelemfixes/questdest/QuestDestinations.java \
  src/src/excelemfixes/questdest/DialogDerivation.java \
  src/src/excelemfixes/questdest/FetchDerivation.java \
  src/src/excelemfixes/questdest/TurnInDerivation.java \
  src/src/excelemfixes/questdest/QuestWhereMenus.java \
  src/src/excelemfixes/questdest/TrackerBlankScan.java \
  src/src/excelemfixes/arcane/CompensationMath.java \
  src/src/excelemfixes/arcane/AbilityHitPairing.java \
  src/src/excelemfixes/dropguide/DropIndex.java \
  src/src/excelemfixes/dropguide/DropGuideMenus.java \
  src/src/excelemfixes/tracker/Expertise.java \
  src/src/excelemfixes/tracker/TrackerCatalog.java \
  src/src/excelemfixes/tracker/TrackerSnapshot.java \
  src/src/excelemfixes/tracker/ProtectKills.java \
  src/src/excelemfixes/tracker/TrackerMenus.java \
  src/src/excelemfixes/foodheal/FoodTable.java \
  src/src/excelemfixes/foodheal/HealEngine.java \
  src/src/excelemfixes/foodheal/WorldGate.java \
  src/src/excelemfixes/foodheal/BonusPolicy.java \
  src/src/excelemfixes/leash/LeashRemovalPolicy.java \
  src/src/excelemfixes/leash/ReturnPolicy.java \
  src/src/excelemfixes/seal/SealRestoreData.java \
  src/src/excelemfixes/jobsdummy/DummyBossList.java \
  src/src/excelemfixes/enderman/ResistanceMath.java \
  src/src/excelemfixes/enderman/EndermanScope.java \
  src/src/excelemfixes/enderman/CastTargeting.java \
  src/src/excelemfixes/enderman/LearnedAmounts.java \
  src/src/excelemfixes/fieldmedic/FieldMedicEngine.java \
  src/src/excelemfixes/fieldmedic/BandageItem.java \
  src/src/excelemfixes/fieldmedic/PotionItem.java \
  src/src/excelemfixes/fieldmedic/VendorGoods.java \
  src/src/excelemfixes/fieldmedic/VendorArms.java \
  src/src/excelemfixes/meleepremium/MeleePremiumMath.java \
  src/src/excelemfixes/meleepremium/MeleeClassifier.java \
  src/src/excelemfixes/meleepremium/MeleeScope.java \
  src/src/excelemfixes/meleemitigation/MitigationMath.java \
  src/src/excelemfixes/meleemitigation/MeleeWeaponClassifier.java \
  src/src/excelemfixes/meleemitigation/OffHandClassifier.java \
  src/src/excelemfixes/meleemitigation/IndicatorText.java \
  src/src/excelemfixes/skillnudge/SkillNudgeMath.java \
  src/src/excelemfixes/skillnudge/BonusCatalog.java \
  src/src/excelemfixes/respawngrace/GraceLedger.java \
  src/src/excelemfixes/respawngrace/RespawnGracePolicy.java \
  src/src/excelemfixes/mcmmo/AwardRates.java \
  src/src/excelemfixes/mcmmo/CeilingLedger.java \
  src/src/excelemfixes/mcmmo/HitTokens.java \
  src/src/excelemfixes/mcmmo/WeaponSkills.java \
  src/src/excelemfixes/mcmmo/WatchdogLedger.java \
  src/src/excelemfixes/mcmmo/EmWorlds.java \
  src/src/excelemfixes/mcmmo/DummyFiles.java \
  src/src/excelemfixes/questbeacon/BeamGeometry.java \
  src/src/excelemfixes/questbeacon/BeamPolicy.java \
  src/src/excelemfixes/questglow/GlowState.java \
  src/src/excelemfixes/questglow/QuestGlowCore.java \
  src/src/excelemfixes/clarity/ClarityText.java \
  src/src/excelemfixes/clarity/ClarityMenus.java \
  src/src/excelemfixes/foodstack/StampView.java \
  src/src/excelemfixes/foodstack/RestorePlanner.java \
  src/src/excelemfixes/foodstack/StackMerger.java \
  src/src/excelemfixes/skillsmenu/SkillsMenuModel.java \
  src/src/excelemfixes/questchain/QuestChainCore.java \
  src/src/excelemfixes/casterxp/CasterXpMath.java \
  src/src/excelemfixes/casterxp/ClassFormTable.java \
  src/src/excelemfixes/casterxp/CasterXpState.java \
  src/src/excelemfixes/casterxp/CasterXpView.java \
  src/src/excelemfixes/questxp/QuestXpTable.java \
  src/src/excelemfixes/questxp/GrantPolicy.java \
  src/src/excelemfixes/questxp/BackpayTable.java \
  src/src/excelemfixes/questxp/BackpayLedger.java \
  src/src/excelemfixes/questxp/TargetPolicy.java \
  src/src/excelemfixes/questxp/TargetStore.java \
  src/src/excelemfixes/questxp/QuestXpView.java \
  src/src/excelemfixes/witherskulls/SkullMath.java \
  src/src/excelemfixes/witherskulls/SkullSettings.java \
  src/src/excelemfixes/witherskulls/TopUpDecider.java \
  src/src/excelemfixes/partydrops/MultiplierMath.java \
  src/src/excelemfixes/partydrops/PartyRange.java \
  src/src/excelemfixes/partydrops/ParticipantCounter.java \
  src/src/excelemfixes/partydrops/OriginGate.java \
  src/src/excelemfixes/partydrops/HintGate.java \
  src/src/excelemfixes/partydrops/PartyDropsSettings.java \
  src/src/excelemfixes/bedrockabilities/AcsGateway.java \
  src/src/excelemfixes/bedrockabilities/BridgeFailure.java \
  src/src/excelemfixes/bedrockabilities/BridgeSlot.java \
  src/src/excelemfixes/bedrockabilities/ClassView.java \
  src/src/excelemfixes/bedrockabilities/FailureMessages.java \
  src/src/excelemfixes/bedrockabilities/FireOutcome.java \
  src/src/excelemfixes/bedrockabilities/FormModel.java \
  src/src/excelemfixes/bedrockabilities/FormModelBuilder.java \
  src/src/excelemfixes/bedrockabilities/Labels.java \
  src/src/excelemfixes/bedrockabilities/SlotView.java \
  src/src/excelemfixes/bedrockabilities/StatusCommand.java \
  src/src/excelemfixes/timedevents/TimedEventSettings.java \
  src/src/excelemfixes/timedevents/EventNames.java \
  src/src/excelemfixes/timedevents/CompassText.java \
  src/src/excelemfixes/timedevents/RelocationPolicy.java \
  src/src/excelemfixes/timedevents/TrackedEvent.java \
  $(find src/tests -name '*.java' -not -path '*bedrockabilities*')
"$JDK/bin/java" -cp "build/test-classes:$CP" TestMain

# bedrock-abilities suites (ported from the standalone bridge
# 0.2.0): compiled against the pack's compile-time-only
# EliteMobs jar, run twice - without it (probe must report
# UNAVAILABLE and never throw) and with it (enum mapping pins
# + probe honesty). No MD5 pin: the pack posture is guarded
# seams + the boot probe (programme Wave 4 decision).
EMJAR="libs/EliteMobs-10.9.10.jar"
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "build/test-classes:$CP:$EMJAR" -d build/test-classes \
  src/src/excelemfixes/bedrockabilities/AcsProbe.java \
  src/src/excelemfixes/bedrockabilities/EmAcsGateway.java \
  src/tests/com/winkreyes/excelemfixes/bedrockabilities/BridgeStubGateway.java \
  src/tests/com/winkreyes/excelemfixes/bedrockabilities/BridgeTestMain.java \
  src/tests/com/winkreyes/excelemfixes/bedrockabilities/BridgeEmMappingTestMain.java \
  src/tests/com/winkreyes/excelemfixes/bedrockabilities/BridgeStatusTestMain.java
echo "== bedrock-abilities Run A: no EliteMobs jar at runtime =="
"$JDK/bin/java" -DprobeExpect=UNAVAILABLE -cp "build/test-classes:$CP" excelemfixes.bedrockabilities.BridgeTestMain
"$JDK/bin/java" -cp "build/test-classes:$CP" excelemfixes.bedrockabilities.BridgeStatusTestMain
echo "== bedrock-abilities Run B: EliteMobs jar present =="
"$JDK/bin/java" -DprobeExpect=NOT_READY -cp "build/test-classes:$CP:$EMJAR" excelemfixes.bedrockabilities.BridgeTestMain
"$JDK/bin/java" -cp "build/test-classes:$CP:$EMJAR" excelemfixes.bedrockabilities.BridgeEmMappingTestMain
"$JDK/bin/java" -cp "build/test-classes:$CP:$EMJAR" excelemfixes.bedrockabilities.BridgeStatusTestMain
