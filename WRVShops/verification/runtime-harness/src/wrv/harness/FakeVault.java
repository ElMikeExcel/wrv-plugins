package wrv.harness;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

/** Disposable in-memory provider. No real economy or player database is accessed. */
public final class FakeVault extends JavaPlugin {
    public final Map<UUID, Double> balances = new HashMap<>();
    public final List<String> calls = new ArrayList<>();
    public UUID failDeposit, failWithdraw;
    public boolean throwDeposit, throwWithdraw;
    public Consumer<UUID> afterWithdraw;
    public Economy economy;

    @Override public void onEnable() {
        if (Boolean.getBoolean("wrv.test.no-economy")) { getLogger().info("Intentionally omitting Economy service for disposable protection-only startup probe"); return; }
        economy = (Economy) Proxy.newProxyInstance(Economy.class.getClassLoader(), new Class<?>[]{Economy.class}, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("getName")) return "WRV disposable fake economy";
            if (name.equals("isEnabled") || name.equals("hasAccount") || name.equals("createPlayerAccount")) return true;
            if (name.equals("hasBankSupport")) return false;
            if (name.equals("fractionalDigits")) return 2;
            if (name.equals("currencyNameSingular")) return "test dollar";
            if (name.equals("currencyNamePlural")) return "test dollars";
            if (name.equals("format")) return String.format(Locale.ROOT, "$%.2f", (double) args[0]);
            if (name.equals("getBanks")) return List.of();
            if (name.equals("toString")) return "DisposableFakeVault";
            if (name.equals("hashCode")) return System.identityHashCode(proxy);
            if (name.equals("equals")) return proxy == args[0];
            UUID id = null;
            if (args != null && args.length > 0) {
                Object account = args[0];
                id = account instanceof OfflinePlayer player ? player.getUniqueId() : UUID.nameUUIDFromBytes(account.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            double balance = balances.getOrDefault(id, 0.0);
            if (name.equals("getBalance")) return balance;
            double amount = args != null && args.length > 1 && args[args.length - 1] instanceof Number number ? number.doubleValue() : 0.0;
            if (name.equals("has")) return balance >= amount;
            if (name.equals("withdrawPlayer") || name.equals("depositPlayer")) {
                calls.add(name + ":" + id + ":" + amount);
                if ((name.equals("withdrawPlayer") && id.equals(failWithdraw)) || (name.equals("depositPlayer") && id.equals(failDeposit))) {
                    if ((name.equals("withdrawPlayer") && throwWithdraw) || (name.equals("depositPlayer") && throwDeposit)) throw new IllegalStateException("Injected provider failure");
                    return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Injected provider failure");
                }
                if (!Double.isFinite(amount) || amount < 0 || (name.equals("withdrawPlayer") && balance < amount)) {
                    return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient test balance or invalid amount");
                }
                balance += name.equals("depositPlayer") ? amount : -amount;
                balances.put(id, balance);
                if (name.equals("withdrawPlayer") && afterWithdraw != null) afterWithdraw.accept(id);
                return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
            }
            if (method.getReturnType() == EconomyResponse.class) return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No banks in test provider");
            throw new UnsupportedOperationException("Untested Economy API method " + method);
        });
        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
    }
    public void reset() { balances.clear(); calls.clear(); failDeposit = null; failWithdraw = null; throwDeposit = false; throwWithdraw = false; afterWithdraw = null; }
}
