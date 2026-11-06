package com.winkreyes.excelbraingames.games.text;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Riddle of the Day rotation (SPEC §8.1): ONE riddle per server day, the
 * same for everyone, drawn without repeat from the config list until
 * the list cycles. The state is global (not per-player) and lives in
 * plugins/ExcelBrainGames/riddle-state.yml so a restart can never
 * change today's riddle. The day's pick is deterministic for the date
 * and is saved the moment it is made. On weekends the pick prefers
 * kid-suitable riddles when config says so.
 */
public final class RiddleService {

    private final File file;
    private final Logger log;
    private String date = "";
    private int index = -1;
    private final List<Integer> used = new ArrayList<>();

    public RiddleService(File dataRoot, Logger log) {
        this.file = new File(dataRoot, "riddle-state.yml");
        this.log = log;
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            date = y.getString("date", "");
            index = y.getInt("index", -1);
            used.addAll(y.getIntegerList("used"));
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("date", date);
        y.set("index", index);
        y.set("used", new ArrayList<>(used));
        try { y.save(file); }
        catch (IOException e) { log.warning("ExcelBrainGames: could not save riddle-state.yml: " + e.getMessage()); }
    }

    public static String today(ZoneId zone) { return LocalDate.now(zone).toString(); }

    /**
     * Today's riddle index into the list. Rolls the day over on first
     * touch: picks the next unused riddle (kid-preferred on weekends
     * when configured), starting a fresh cycle when the list runs out.
     * Returns -1 when the list is empty.
     */
    public synchronized int todayIndex(int listSize, ZoneId zone, boolean preferKidWeekends,
                                       java.util.function.IntPredicate isKid) {
        if (listSize <= 0) return -1;
        String today = today(zone);
        if (today.equals(date) && index >= 0 && index < listSize) return index;
        List<Integer> fresh = new ArrayList<>();
        for (int i = 0; i < listSize; i++) if (!used.contains(i)) fresh.add(i);
        if (fresh.isEmpty()) { used.clear(); for (int i = 0; i < listSize; i++) fresh.add(i); }
        List<Integer> pool = fresh;
        DayOfWeek dow = LocalDate.now(zone).getDayOfWeek();
        if (preferKidWeekends && (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)) {
            List<Integer> kidPool = new ArrayList<>();
            for (int i : fresh) if (isKid.test(i)) kidPool.add(i);
            if (!kidPool.isEmpty()) pool = kidPool;
        }
        int pick = pool.get(Math.floorMod(today.hashCode(), pool.size()));
        date = today;
        index = pick;
        if (!used.contains(pick)) used.add(pick);
        save();
        return index;
    }

    /** Admin: force today's riddle to a list index (SPEC §10 setriddle). */
    public synchronized boolean forceIndex(int newIndex, int listSize, ZoneId zone) {
        if (newIndex < 0 || newIndex >= listSize) return false;
        date = today(zone);
        index = newIndex;
        if (!used.contains(newIndex)) used.add(newIndex);
        save();
        return true;
    }

    public synchronized int currentIndex() { return index; }
    public synchronized String currentDate() { return date; }
}
