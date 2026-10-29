/* Excel WRV synthetic regression fixture; original attribution retained. */ package org.bukkit;
// Excel test-only scheduler trap. Disabled plugin path must not schedule.
public final class Bukkit {
 public static Scheduler getScheduler(){throw new AssertionError("disabled plugin scheduled work");}
 public static final class Scheduler { public void runTaskLater(Object plugin,Runnable task,long ticks){throw new AssertionError("unexpected schedule");} }
}
