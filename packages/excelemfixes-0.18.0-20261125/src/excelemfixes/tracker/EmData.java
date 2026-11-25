package excelemfixes.tracker;

import com.magmaguy.elitemobs.advancedcombat.AdvancedCombatModule;
import com.magmaguy.elitemobs.advancedcombat.progression.FormProgressSnapshot;
import com.magmaguy.elitemobs.advancedcombat.progression.ProfileSnapshot;
import com.magmaguy.elitemobs.dungeons.DungeonBossLockout;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.Quest;
import com.magmaguy.elitemobs.quests.playercooldowns.PlayerQuestCooldowns;
import com.magmaguy.elitemobs.quests.playercooldowns.QuestCooldown;
import com.magmaguy.elitemobs.skills.SkillType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reads a TrackerSnapshot out of EliteMobs' public API. This is the
 * only tracker class that touches EM types; the module checks that
 * EliteMobs is installed before calling it, and every read is
 * guarded so an EM hiccup degrades to "unavailable" instead of an
 * error in a player's face.
 */
public final class EmData {

    private EmData() {}

    public static TrackerSnapshot read(UUID uuid, Logger log) {
        try {
            PlayerData data = PlayerData.getPlayerData(uuid);
            if (data == null) return TrackerSnapshot.unavailable();

            Set<String> distinct = new HashSet<>();
            PlayerQuestCooldowns cooldowns =
                    PlayerData.getPlayerQuestCooldowns(uuid);
            if (cooldowns != null) {
                for (QuestCooldown cd : cooldowns.getQuestCooldowns()) {
                    String perm = cd.getPermission();
                    if (perm != null && perm.startsWith("elitequest.")) {
                        distinct.add(perm.substring("elitequest.".length()));
                    }
                }
            }

            List<String> active = new ArrayList<>();
            List<Quest> quests = PlayerData.getQuests(uuid);
            if (quests != null) {
                for (Quest q : quests) {
                    if (q instanceof com.magmaguy.elitemobs.quests.CustomQuest
                            custom) {
                        String file = custom.getConfigurationFilename();
                        if (file != null && !file.isBlank()) {
                            active.add(file);
                        }
                    }
                }
            }

            Set<String> lockouts = new HashSet<>();
            DungeonBossLockout lockout =
                    PlayerData.getDungeonBossLockout(uuid);
            if (lockout != null) {
                for (String key : lockout.getLockouts().keySet()) {
                    int cut = key.indexOf(':');
                    lockouts.add(cut > 0 ? key.substring(0, cut) : key);
                }
            }

            List<TrackerSnapshot.SkillProgress> skills = new ArrayList<>();
            for (SkillType type : SkillType.values()) {
                long xp = PlayerData.getSkillXP(uuid, type);
                if (xp > 0) {
                    skills.add(new TrackerSnapshot.SkillProgress(
                            type.name(), xp,
                            PlayerData.getSkillLevel(uuid, type)));
                }
            }
            skills.sort((a, b) -> Long.compare(b.xp(), a.xp()));

            List<TrackerSnapshot.FormProgress> forms = new ArrayList<>();
            try {
                Optional<ProfileSnapshot> profile =
                        AdvancedCombatModule.get().profile(uuid);
                if (profile.isPresent()) {
                    ProfileSnapshot snap = profile.get();
                    String selected = snap.selectedFormId();
                    for (FormProgressSnapshot form : snap.forms().values()) {
                        forms.add(new TrackerSnapshot.FormProgress(
                                form.formId(), form.xp(),
                                form.effectiveLevel(),
                                form.formId() != null
                                        && form.formId().equals(selected)));
                    }
                }
            } catch (Throwable t) {
                log.fine("ExcelEMFixes tracker: ACS profile read failed: " + t);
            }
            forms.sort((a, b) -> {
                if (a.selected() != b.selected()) return a.selected() ? -1 : 1;
                return Long.compare(b.xp(), a.xp());
            });

            return new TrackerSnapshot(true,
                    PlayerData.getQuestsCompleted(uuid),
                    distinct, active,
                    PlayerData.getDungeonsCompleted(uuid),
                    lockouts,
                    PlayerData.getKills(uuid),
                    PlayerData.getHighestLevelKilled(uuid),
                    PlayerData.getDeaths(uuid),
                    PlayerData.getPlayerLevel(uuid),
                    forms, skills);
        } catch (Throwable t) {
            log.warning("ExcelEMFixes tracker: EM data read failed for "
                    + uuid + ": " + t);
            return TrackerSnapshot.unavailable();
        }
    }
}
