import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Proxy;

/** Scratch-boot harness: simulates an mcMMO-style /party owner and checks ExcelTogether wiring. Not shipped. */
public class Harness063 extends JavaPlugin {
    public void onEnable() {
        // pretend to be mcMMO: claim bare /party
        Command fake = new Command("party") {
            public boolean execute(CommandSender s, String l, String[] a) { s.sendMessage("FAKE-MCMMO-PARTY"); return true; }
        };
        Bukkit.getCommandMap().register("mcmmo", fake);
        getServer().getScheduler().runTaskLater(this, () -> {
            for (String n : new String[]{"excelparty", "eparty", "together", "party"}) {
                Command c = Bukkit.getCommandMap().getCommand(n);
                getLogger().info("HARNESS /" + n + " -> " + (c == null ? "none" : c.getClass().getSimpleName() + " owner=" + (c instanceof org.bukkit.command.PluginCommand pc ? pc.getPlugin().getName() : "other")));
            }
            OfflinePlayer p = (OfflinePlayer) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{OfflinePlayer.class},
                (x, m, a) -> m.getReturnType() == boolean.class ? (Object) false : m.getReturnType() == int.class ? (Object) 0 : null);
            JobsPrePaymentEvent e = new JobsPrePaymentEvent(p, null, 8.0, 4.0, 0.0, null, null, null, null);
            getServer().getPluginManager().callEvent(e);
            getLogger().info("HARNESS offline/no-partner path: money=" + e.getAmount() + " exp=" + e.getExp() + " (expect 8.0 / 4.0)");
            Bukkit.getConsoleSender().sendMessage("HARNESS console runs /excelparty:");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "excelparty invite nobody");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "party");
            getServer().shutdown();
        }, 200L);
    }
}
