import com.winkreyesvalley.excelmenujoin.PouchCompassCheck;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

/**
 * ExcelMenuJoin 1.4.0 pouch-consultation tests (SPEC Part I section 9):
 * the give decision matrix and the reflective service lookup,
 * including the FAIL-OPEN cases — ExcelPouch absent (no plugin, no
 * service) and ExcelPouch broken (provider throws) both read as
 * "no answer" and the give proceeds.
 */
public class PouchCompassCheckTest {
    private static final UUID PLAYER = UUID.randomUUID();

    // -- provider doubles (public class + public method: the reflective contract)
    public static class HoldingProvider { public boolean holdsKind(UUID u, String k) { return true; } }
    public static class EmptyProvider { public boolean holdsKind(UUID u, String k) { return false; } }
    public static class ThrowingProvider { public boolean holdsKind(UUID u, String k) { throw new RuntimeException("boom"); } }
    public static class UnrelatedProvider { public String somethingElse() { return "x"; } }

    static void run() {
        System.out.println("== ShouldGive matrix ==");
        Check.ok(!PouchCompassCheck.shouldGive(true, null), "already has compass -> never give");
        Check.ok(!PouchCompassCheck.shouldGive(true, Boolean.FALSE), "already has compass (pouch empty) -> never give");
        Check.ok(!PouchCompassCheck.shouldGive(true, Boolean.TRUE), "already has compass (pouch holds) -> never give");
        Check.ok(PouchCompassCheck.shouldGive(false, null), "no compass + NO ANSWER (pouch absent) -> GIVE (fail-open)");
        Check.ok(PouchCompassCheck.shouldGive(false, Boolean.FALSE), "no compass + pouch empty -> give");
        Check.ok(!PouchCompassCheck.shouldGive(false, Boolean.TRUE), "no compass + POUCH HOLDS ONE -> SKIP (the 1.4.0 fix)");

        System.out.println("== QueryPouch lookup ==");
        Plugin pouchPlugin = pluginProxy();
        Check.eq(null, PouchCompassCheck.pouchHoldsMenuCompass(null, pouchPlugin, PLAYER), "no ServicesManager -> null");
        Check.eq(null, PouchCompassCheck.pouchHoldsMenuCompass(servicesWith(pouchPlugin), null, PLAYER),
                "ExcelPouch plugin ABSENT -> null (fail-open, spec section 9)");
        Check.eq(null, PouchCompassCheck.pouchHoldsMenuCompass(servicesWith(pouchPlugin), pouchPlugin, PLAYER),
                "plugin present but no registrations -> null");
        Check.eq(null, PouchCompassCheck.pouchHoldsMenuCompass(
                servicesWith(pouchPlugin, new UnrelatedProvider()), pouchPlugin, PLAYER),
                "unrelated service only -> null");
        Check.eq(Boolean.TRUE, PouchCompassCheck.pouchHoldsMenuCompass(
                servicesWith(pouchPlugin, new HoldingProvider()), pouchPlugin, PLAYER),
                "pouch holds a compass -> TRUE (give will be skipped)");
        Check.eq(Boolean.FALSE, PouchCompassCheck.pouchHoldsMenuCompass(
                servicesWith(pouchPlugin, new EmptyProvider()), pouchPlugin, PLAYER),
                "pouch holds no compass -> FALSE (give proceeds)");
        Check.eq(null, PouchCompassCheck.pouchHoldsMenuCompass(
                servicesWith(pouchPlugin, new ThrowingProvider()), pouchPlugin, PLAYER),
                "provider throws -> null (fail-open, never breaks the give path)");
        Check.eq(Boolean.TRUE, PouchCompassCheck.pouchHoldsMenuCompass(
                servicesWith(pouchPlugin, new UnrelatedProvider(), new HoldingProvider()), pouchPlugin, PLAYER),
                "unrelated registration first, real service second -> still finds it");
    }

    private static Plugin pluginProxy() {
        return (Plugin) Proxy.newProxyInstance(PouchCompassCheckTest.class.getClassLoader(),
                new Class<?>[] { Plugin.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "ExcelPouch";
                    if (method.getName().equals("isEnabled")) return true;
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ServicesManager servicesWith(Plugin plugin, Object... providers) {
        java.util.Collection<RegisteredServiceProvider<?>> regs = new java.util.ArrayList<>();
        for (Object provider : providers) {
            regs.add(new RegisteredServiceProvider<>(Object.class, provider, ServicePriority.Normal, plugin));
        }
        final java.util.Collection<RegisteredServiceProvider<?>> snapshot = List.copyOf(regs);
        return (ServicesManager) Proxy.newProxyInstance(PouchCompassCheckTest.class.getClassLoader(),
                new Class<?>[] { ServicesManager.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getRegistrations")) return new java.util.ArrayList<>(snapshot);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ServicesManager servicesWith(Plugin plugin) {
        return servicesWith(plugin, new Object[0]);
    }
}
