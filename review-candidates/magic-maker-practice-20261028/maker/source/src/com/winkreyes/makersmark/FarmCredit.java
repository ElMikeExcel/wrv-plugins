package com.winkreyes.makersmark;
import com.winkreyes.makersmark.core.KillCredit;
import java.lang.reflect.Method;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
/** Read-only narrow classifier. Captured before RewardGuard MONITOR cleanup. */
final class FarmCredit {
    private final ExcelMakersMark plugin;
    private Plugin rewardGuard;
    private Method fallMethod;
    private boolean warned;
    private static final NamespacedKey ORIGIN = new NamespacedKey("excelrewardguard", "origin_v1");
    private static final NamespacedKey REASON = new NamespacedKey("excelrewardguard", "reason_v1");
    FarmCredit(ExcelMakersMark plugin) {
        this.plugin = plugin;
        rewardGuard = plugin.getServer().getPluginManager().getPlugin("ExcelRewardGuard");
        try {
            if (rewardGuard == null || !rewardGuard.isEnabled()) throw new IllegalStateException("ExcelRewardGuard is not enabled");
            fallMethod = rewardGuard.getClass().getMethod("isFallSoftened", LivingEntity.class);
            if (fallMethod.getReturnType() != boolean.class) throw new IllegalStateException("Unexpected fall API type");
            Object threshold = rewardGuard.getClass().getMethod("farmFallDamagePercent").invoke(rewardGuard);
            plugin.getLogger().info("Milestone credit uses ExcelRewardGuard fall threshold " + threshold + "% and ordinary SPAWNER origin; actual kills unchanged.");
        } catch (ReflectiveOperationException | RuntimeException ex) { fallMethod = null; unavailable(ex); }
    }
    boolean reduced(LivingEntity victim) {
        boolean softened = false;
        try {
            if (fallMethod == null || rewardGuard == null || !rewardGuard.isEnabled()) throw new IllegalStateException("Fall classifier unavailable");
            softened = (boolean) fallMethod.invoke(rewardGuard, victim);
        } catch (ReflectiveOperationException | RuntimeException ex) { unavailable(ex); }
        var data = victim.getPersistentDataContainer();
        return KillCredit.reduced(victim.getEntitySpawnReason().name(), data.get(ORIGIN, PersistentDataType.STRING), data.get(REASON, PersistentDataType.STRING), softened);
    }
    private void unavailable(Exception ex) {
        if (warned) return; warned = true;
        plugin.getLogger().severe("Milestone fall classification UNAVAILABLE: " + ex.getClass().getSimpleName()
            + ". Unknown fall history receives full credit; ordinary-spawner detection remains. Actual kills are preserved. Do not accept this deployment until the dependency is restored.");
    }
}
