package excelemfixes.questchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure chain math for quest-chain-clarity: given the
 * quest/NPC data EliteMobs has loaded (as plain records — the
 * EmQuestChainData bridge fills them in) and the set of quest
 * permissions a player actually holds, work out what to tell a
 * player who just clicked a quest NPC and got EliteMobs'
 * generic "finish the quests that come before them first"
 * refusal.
 *
 * The model mirrors EliteMobs' own accept rule, read from its
 * bytecode (CustomQuest.hasPermissionForQuest, EliteMobs
 * 10.9.14): a quest is acceptable when the player carries a
 * completion marker (Bukkit metadata, set by EliteMobs'
 * PlayerQuestCooldowns when a quest completes) for every one
 * of the quest's accept permissions — the singular
 * questAcceptPermission plus every entry of
 * questAcceptPermissions. Completing a quest grants the marker
 * named by that quest's questLockoutPermission, so a chain is
 * a linked list: quest B's accept permission is quest A's
 * lockout permission.
 *
 * For a locked quest, the walk follows unmet accept
 * permissions from quest to granting quest until it reaches a
 * quest whose own accept permissions are ALL met (or which has
 * none): that quest is the next thing the player can actually
 * go and do, and it is the one the message names. Nothing here
 * knows any quest, NPC or place by name — the index is built
 * entirely from whatever content is loaded.
 */
public final class QuestChainCore {

    /** One quest, as plain data. */
    public record QuestInfo(
            String file,
            String name,
            List<String> acceptPerms,
            String lockoutPerm,
            String turnInNpcFile,
            List<String> objectiveNpcFiles,
            String objectiveNpcName,
            String objectiveLocation) {
        public QuestInfo {
            acceptPerms = acceptPerms == null
                    ? List.of() : List.copyOf(acceptPerms);
            objectiveNpcFiles = objectiveNpcFiles == null
                    ? List.of() : List.copyOf(objectiveNpcFiles);
            lockoutPerm = lockoutPerm == null ? "" : lockoutPerm;
        }
    }

    /** The next actionable step of one locked chain. */
    public record NextStep(
            String questFile,
            String questName,
            String giverName,
            String location) {}

    private final Map<String, QuestInfo> byFile;
    private final Map<String, QuestInfo> byLockoutPerm;
    private final Map<String, List<QuestInfo>> questsByNpc;
    private final Map<String, String> giverByQuest;
    private final Map<String, String> npcNames;
    private final Set<String> allAcceptPerms;

    /**
     * @param quests         every enabled quest
     * @param npcQuestFiles  NPC file -> the quests that NPC
     *                       hands out (its questFileName list)
     * @param npcNames       NPC file -> display name (cleaned)
     */
    public QuestChainCore(List<QuestInfo> quests,
                          Map<String, List<String>> npcQuestFiles,
                          Map<String, String> npcNames) {
        byFile = new LinkedHashMap<>();
        byLockoutPerm = new HashMap<>();
        allAcceptPerms = new HashSet<>();
        for (QuestInfo q : quests) {
            if (q == null || q.file() == null || q.file().isBlank()) {
                continue;
            }
            byFile.put(q.file(), q);
            if (!q.lockoutPerm().isBlank()) {
                byLockoutPerm.putIfAbsent(q.lockoutPerm(), q);
            }
            allAcceptPerms.addAll(q.acceptPerms());
        }

        // Who hands each quest out: the NPC whose quest list
        // names it. Several NPCs can list the same quest; the
        // first by NPC filename wins, so the answer is stable.
        giverByQuest = new HashMap<>();
        List<String> npcFiles = new ArrayList<>(npcQuestFiles.keySet());
        npcFiles.sort(String::compareTo);
        for (String npcFile : npcFiles) {
            List<String> given = npcQuestFiles.get(npcFile);
            if (given == null) continue;
            for (String questFile : given) {
                if (questFile != null && byFile.containsKey(questFile)) {
                    giverByQuest.putIfAbsent(questFile, npcFile);
                }
            }
        }

        // Every quest an NPC is involved in: it hands the quest
        // out, or the quest turns in there, or one of the
        // quest's objectives talks to it. Clicking any of those
        // NPCs is a click EliteMobs answers with the generic
        // refusal when the quest is chain-locked.
        questsByNpc = new HashMap<>();
        for (QuestInfo q : byFile.values()) {
            Set<String> npcs = new HashSet<>();
            String giver = giverByQuest.get(q.file());
            if (giver != null) npcs.add(giver);
            if (q.turnInNpcFile() != null && !q.turnInNpcFile().isBlank()) {
                npcs.add(q.turnInNpcFile());
            }
            npcs.addAll(q.objectiveNpcFiles());
            for (String npc : npcs) {
                questsByNpc.computeIfAbsent(npc, k -> new ArrayList<>())
                        .add(q);
            }
        }
        for (List<QuestInfo> list : questsByNpc.values()) {
            list.sort((a, b) -> a.file().compareTo(b.file()));
        }
        this.npcNames = Map.copyOf(npcNames);
    }

    /** Every accept permission any quest in the index names —
     *  the bridge checks exactly these against the player. */
    public Set<String> allAcceptPerms() {
        return Set.copyOf(allAcceptPerms);
    }

    /** The quests a click on this NPC can be about. */
    public List<QuestInfo> questsForNpc(String npcFile) {
        return questsByNpc.getOrDefault(npcFile, List.of());
    }

    public int questCount() {
        return byFile.size();
    }

    /**
     * The next steps to name for a click on {@code npcFile}:
     * one per distinct locked chain, in quest-filename order,
     * capped at {@code maxLines}.
     *
     * @param heldPerms  the quest permissions the player holds
     *                   (EliteMobs completion markers)
     * @param activeFiles quests the player is already carrying
     *                   (never reported — EliteMobs is already
     *                   talking to them about those)
     */
    public List<NextStep> nextSteps(String npcFile,
                                    Set<String> heldPerms,
                                    Set<String> activeFiles,
                                    int maxLines) {
        List<NextStep> out = new ArrayList<>();
        Set<String> named = new HashSet<>();
        for (QuestInfo quest : questsForNpc(npcFile)) {
            if (out.size() >= maxLines) break;
            if (activeFiles.contains(quest.file())) continue;
            List<String> unmet = unmetPerms(quest, heldPerms);
            if (unmet.isEmpty()) continue;
            QuestInfo terminal = walk(unmet.get(0), quest, heldPerms);
            if (terminal == null) continue;
            if (!named.add(terminal.file())) continue;
            out.add(new NextStep(
                    terminal.file(),
                    terminal.name(),
                    giverName(terminal),
                    terminal.objectiveLocation()));
        }
        return out;
    }

    /**
     * Follows the chain from one unmet permission to the first
     * quest the player could accept right now (all of its own
     * accept permissions held, or none). Returns null when the
     * chain cannot be named from the data: the permission is
     * granted by no loaded quest, or the chain loops.
     */
    private QuestInfo walk(String unmetPerm, QuestInfo from,
                           Set<String> heldPerms) {
        Set<String> visited = new HashSet<>();
        visited.add(from.file());
        QuestInfo current = byLockoutPerm.get(unmetPerm);
        while (current != null) {
            if (!visited.add(current.file())) return null;
            List<String> unmet = unmetPerms(current, heldPerms);
            if (unmet.isEmpty()) return current;
            current = byLockoutPerm.get(unmet.get(0));
        }
        return null;
    }

    private static List<String> unmetPerms(QuestInfo quest,
                                           Set<String> heldPerms) {
        List<String> out = new ArrayList<>();
        for (String perm : quest.acceptPerms()) {
            if (perm != null && !perm.isBlank()
                    && !heldPerms.contains(perm)) {
                out.add(perm);
            }
        }
        return out;
    }

    /** Who to send the player to for this quest: the NPC that
     *  hands it out, else its turn-in NPC, else the NPC its
     *  first objective talks to. */
    private String giverName(QuestInfo quest) {
        String giver = giverByQuest.get(quest.file());
        if (giver != null) {
            String name = npcNames.get(giver);
            if (name != null && !name.isBlank()) return name;
        }
        if (quest.turnInNpcFile() != null) {
            String name = npcNames.get(quest.turnInNpcFile());
            if (name != null && !name.isBlank()) return name;
        }
        return quest.objectiveNpcName();
    }

    /**
     * The player-facing line for one next step, in the pack's
     * legacy-color message style. The location text is the
     * quest's own objective location ("in The Groves", "at the
     * Lighthouse") when the content provides one.
     */
    public static String formatLine(NextStep step) {
        StringBuilder sb = new StringBuilder();
        sb.append("&eLocked: &ffinish &a'")
                .append(step.questName()).append("'&f first");
        if (step.giverName() != null && !step.giverName().isBlank()) {
            sb.append(" — &a").append(step.giverName()).append("&f");
            if (step.location() != null && !step.location().isBlank()) {
                sb.append(' ').append(step.location());
            }
            sb.append(" gives it.");
        } else {
            sb.append('.');
        }
        return sb.toString();
    }
}
