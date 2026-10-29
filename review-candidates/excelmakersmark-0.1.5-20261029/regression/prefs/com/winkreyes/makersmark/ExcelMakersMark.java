/* Excel WRV synthetic regression fixture; original attribution retained. */ package com.winkreyes.makersmark;
// Excel test-only plugin double, never packaged. Actual Bukkit YAML used.
import java.io.File; import java.util.logging.Logger;
public final class ExcelMakersMark {
 private final File folder;
 ExcelMakersMark(File folder){this.folder=folder;}
 public File getDataFolder(){return folder;}
 public boolean isEnabled(){return false;}
 public Logger getLogger(){return Logger.getLogger("ExcelPreferenceTest");}
}
