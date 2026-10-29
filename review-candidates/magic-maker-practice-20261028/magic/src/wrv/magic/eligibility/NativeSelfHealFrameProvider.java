package wrv.magic.eligibility;
import java.util.*;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
import wrv.magic.evidence.*;
/** Reads only the exact native observation belonging to this receipt during synchronous delivery. */
public final class NativeSelfHealFrameProvider implements SourceAwareReceiptSink.ExactHealthFrame {
 private final BukkitCombatHealthTracker tracker;
 public NativeSelfHealFrameProvider(BukkitCombatHealthTracker tracker){this.tracker=Objects.requireNonNull(tracker);}
 @Override public CombatDeficitLedger.Frame resolve(EffectObserver.Receipt receipt){
  if(!Bukkit.isPrimaryThread()||receipt==null)return null;
  var observation=HealthTransitionBridge.current(receipt).orElse(null);
  if(observation==null||!Objects.equals(observation.recipient(),receipt.recipient()))return null;
  Player target=Bukkit.getPlayer(observation.recipient());
  if(target==null||!target.isOnline()||target.isDead())return null;
  return tracker.nativeHeal(target,observation.before(),observation.after(),observation.maximum());
 }
}
