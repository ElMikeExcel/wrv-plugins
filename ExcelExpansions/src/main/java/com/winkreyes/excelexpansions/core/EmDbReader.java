package com.winkreyes.excelexpansions.core;
import java.io.File; import java.sql.*; import java.util.*;
/** Reads EliteMobs' own player database - live path
 *  plugins/EliteMobs/data/player_data.db (G4 pull RECEIPTS.md, Nov 14 2026).
 *  Columns read from PlayerData, and ONLY these: PlayerUUID,
 *  HighestLevelKilled, Score. Both are cumulative EM-native signals with
 *  named provenance; neither is a "combat level" - EM's schema has no
 *  combat-level column (31 columns dumped, LOCK-PREP #16). The SkillXP_*
 *  columns are deliberately NEVER read: per-skill XP is not a band level
 *  and is never converted into one.
 *  Access is read-only (SQLite URI mode=ro) and off the calling thread's
 *  critical path (host schedules it async). If the SQLite JDBC driver is
 *  absent at runtime, driverPresent() is false and the host keeps the
 *  store's last-known values, stamping the gap in its log - a failed read
 *  never wipes stored signals. */
public final class EmDbReader {
 public record Signals(Integer highestLevelKilled, Long score){}
 public static boolean driverPresent(){
  try { Class.forName("org.sqlite.JDBC"); return true; }
  catch (Throwable t) { return false; }
 }
 public static Map<UUID,Signals> readAll(File db) throws Exception {
  Map<UUID,Signals> out = new LinkedHashMap<>();
  String url = "jdbc:sqlite:file:" + db.getPath().replace('\\','/') + "?mode=ro";
  try (Connection c = DriverManager.getConnection(url);
       Statement st = c.createStatement();
       ResultSet rs = st.executeQuery(
         "SELECT PlayerUUID, HighestLevelKilled, Score FROM PlayerData")) {
   while (rs.next()) {
    String raw = rs.getString(1);
    if (raw == null) continue;
    UUID uuid;
    try { uuid = UUID.fromString(raw.trim()); } catch (Exception e) { continue; }
    int hlk = rs.getInt(2); Integer hlkBox = rs.wasNull() ? null : hlk;
    long score = rs.getLong(3); Long scoreBox = rs.wasNull() ? null : score;
    out.put(uuid, new Signals(hlkBox, scoreBox));
   }
  }
  return out;
 }
}
