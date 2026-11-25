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
  $(find src/tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes:$CP" TestMain
