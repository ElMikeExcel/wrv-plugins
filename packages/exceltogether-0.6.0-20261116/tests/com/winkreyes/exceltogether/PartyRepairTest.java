package com.winkreyes.exceltogether;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * ExcelTogether 0.5.1 unit tests for the party-repair cores: EquipmentRepair
 * (slot scope, wand / undamaged / non-damageable exclusion, and above all the
 * get -> zero -> SET BACK ordering Paper 26.3 demands) and PartyRoster
 * (solo, offline, cross-world, range-cap selection).
 *
 * Headless by construction: EquipmentRepair's damage access runs through its
 * DamageModel seam, so a fake model stands in for the Damageable-meta
 * production model (which only a running server can serve; it is proven on
 * the rig by durability deltas). EntityEquipment / Player / World are
 * interfaces and are faked with proxies. The fake equipment records every
 * setItem with the stack's damage AT THE MOMENT OF THE CALL, which is what
 * proves zeroing happened before the write-back, not after or never.
 * Run: java -cp <classes> com.winkreyes.exceltogether.PartyRepairTest
 */
public class PartyRepairTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    // ------------------------------------------------------------ fakes

    /** Damage state kept beside the stacks, keyed by identity. */
    static final class FakeModel implements EquipmentRepair.DamageModel {
        final Map<ItemStack, int[]> state = new IdentityHashMap<>();   // {damage, max}
        void register(ItemStack s, int damage, int max) { state.put(s, new int[]{damage, max}); }
        @Override public boolean isAir(ItemStack s) { return false; }
        @Override public int maxDamage(ItemStack s) { int[] v = state.get(s); return v == null ? 0 : v[1]; }
        @Override public int damage(ItemStack s) { int[] v = state.get(s); return v == null ? 0 : v[0]; }
        @Override public void zeroDamage(ItemStack s) { int[] v = state.get(s); if (v != null) v[0] = 0; }
    }

    /** EntityEquipment proxy: a slot map plus a write-back log. */
    static final class FakeEquipment {
        final Map<EquipmentSlot, ItemStack> worn = new EnumMap<>(EquipmentSlot.class);
        final List<String> setLog = new ArrayList<>();
        final EntityEquipment proxy;
        FakeEquipment(FakeModel model) {
            proxy = (EntityEquipment) Proxy.newProxyInstance(PartyRepairTest.class.getClassLoader(),
                    new Class<?>[]{EntityEquipment.class}, (p, m, a) -> {
                        switch (m.getName()) {
                            case "getItem": return worn.get((EquipmentSlot) a[0]);
                            case "setItem": {
                                EquipmentSlot slot = (EquipmentSlot) a[0];
                                ItemStack stack = (ItemStack) a[1];
                                worn.put(slot, stack);
                                setLog.add(slot + "@damageAtSet=" + (stack == null ? -1 : model.damage(stack)));
                                return null;
                            }
                            case "toString": return "FakeEquipment";
                            case "hashCode": return System.identityHashCode(p);
                            case "equals": return p == a[0];
                            default: throw new UnsupportedOperationException(m.getName());
                        }
                    });
        }
    }

    static World fakeWorld(String name) {
        return (World) Proxy.newProxyInstance(PartyRepairTest.class.getClassLoader(),
                new Class<?>[]{World.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getName": return name;
                        case "toString": return "World[" + name + "]";
                        case "hashCode": return System.identityHashCode(p);
                        case "equals": return p == a[0];
                        default: throw new UnsupportedOperationException(m.getName());
                    }
                });
    }

    static Player fakePlayer(UUID id, World world, double x, double y, double z, boolean online) {
        Location loc = new Location(world, x, y, z);
        return (Player) Proxy.newProxyInstance(PartyRepairTest.class.getClassLoader(),
                new Class<?>[]{Player.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "getUniqueId": return id;
                        case "getWorld": return world;
                        case "getLocation": return loc;
                        case "isOnline": return online;
                        case "toString": return "Player[" + id + "]";
                        case "hashCode": return System.identityHashCode(p);
                        case "equals": return p == a[0];
                        default: throw new UnsupportedOperationException(m.getName());
                    }
                });
    }

    static ItemStack stack(String label) {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (ItemStack) ((sun.misc.Unsafe) f.get(null)).allocateInstance(ItemStack.class);
        } catch (Exception e) { throw new RuntimeException("cannot allocate a headless ItemStack (" + label + ")", e); }
    }

    // ------------------------------------------------------------ tests

    public static void main(String[] args) {
        // ---- slot scope: stock semantics, offhand never ----
        check("slots armor+held = 4 armor then HAND, no OFFHAND",
                EquipmentRepair.slots(true, true).equals(List.of(
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET, EquipmentSlot.HAND)));
        check("slots armor only has no HAND",
                EquipmentRepair.slots(true, false).equals(List.of(
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)));
        check("slots held only = HAND", EquipmentRepair.slots(false, true).equals(List.of(EquipmentSlot.HAND)));
        check("slots neither = empty", EquipmentRepair.slots(false, false).isEmpty());

        // ---- the repair itself, with the write-back ordering watched ----
        FakeModel model = new FakeModel();
        ItemStack helm = stack("diamond helmet");    model.register(helm, 100, 363);
        ItemStack chest = stack("diamond chestplate"); model.register(chest, 0, 592);
        ItemStack legs = stack("diamond leggings");   model.register(legs, 50, 555);
        ItemStack sword = stack("diamond sword");     model.register(sword, 200, 1561);
        ItemStack shield = stack("shield");           model.register(shield, 300, 336);
        FakeEquipment eq = new FakeEquipment(model);
        eq.worn.put(EquipmentSlot.HEAD, helm);
        eq.worn.put(EquipmentSlot.CHEST, chest);
        eq.worn.put(EquipmentSlot.LEGS, legs);
        // FEET deliberately empty
        eq.worn.put(EquipmentSlot.HAND, sword);
        eq.worn.put(EquipmentSlot.OFF_HAND, shield);

        int n = EquipmentRepair.repair(eq.proxy, true, true, s -> false, model);
        check("repair counts only damaged covered items (helm+legs+sword)", n == 3);
        check("helmet damage zeroed", model.damage(helm) == 0);
        check("leggings damage zeroed", model.damage(legs) == 0);
        check("sword damage zeroed", model.damage(sword) == 0);
        check("offhand damage untouched", model.damage(shield) == 300);
        check("write-back happened per repaired slot, after zeroing",
                eq.setLog.equals(List.of("HEAD@damageAtSet=0", "LEGS@damageAtSet=0", "HAND@damageAtSet=0")));
        check("no write-back for undamaged / empty / offhand slots",
                eq.setLog.stream().noneMatch(s -> s.startsWith("CHEST") || s.startsWith("FEET") || s.startsWith("OFF_HAND")));

        // ---- flag scope ----
        FakeModel m2 = new FakeModel();
        ItemStack h2 = stack("iron helmet"); m2.register(h2, 40, 165);
        ItemStack s2 = stack("iron sword");  m2.register(s2, 60, 250);
        FakeEquipment eq2 = new FakeEquipment(m2);
        eq2.worn.put(EquipmentSlot.HEAD, h2); eq2.worn.put(EquipmentSlot.HAND, s2);
        check("held-only repair skips armor", EquipmentRepair.repair(eq2.proxy, false, true, s -> false, m2) == 1
                && m2.damage(h2) == 40 && m2.damage(s2) == 0);
        check("armor-only repair skips the hand", EquipmentRepair.repair(eq2.proxy, true, false, s -> false, m2) == 1
                && m2.damage(h2) == 0);

        // ---- wand exclusion ----
        FakeModel m3 = new FakeModel();
        ItemStack wand = stack("wand stand-in"); m3.register(wand, 10, 100);
        FakeEquipment eq3 = new FakeEquipment(m3);
        eq3.worn.put(EquipmentSlot.HAND, wand);
        check("wand in hand is never repaired",
                EquipmentRepair.repair(eq3.proxy, true, true, s -> s == wand, m3) == 0
                        && m3.damage(wand) == 10 && eq3.setLog.isEmpty());

        // ---- non-damageable + whiff + null ----
        FakeModel m4 = new FakeModel();
        ItemStack pumpkin = stack("carved pumpkin"); m4.register(pumpkin, 10, 0);
        FakeEquipment eq4 = new FakeEquipment(m4);
        eq4.worn.put(EquipmentSlot.HEAD, pumpkin);
        check("non-damageable worn item is skipped", EquipmentRepair.repair(eq4.proxy, true, true, s -> false, m4) == 0);
        FakeModel m5 = new FakeModel();
        ItemStack fresh = stack("netherite boots"); m5.register(fresh, 0, 481);
        FakeEquipment eq5 = new FakeEquipment(m5);
        eq5.worn.put(EquipmentSlot.FEET, fresh);
        check("nothing damaged = nothing repaired (the whiff)", EquipmentRepair.repair(eq5.proxy, true, true, s -> false, m5) == 0);
        check("null equipment repairs nothing", EquipmentRepair.repair(null, true, true, s -> false, m5) == 0);

        // ---- roster selection ----
        World w1 = fakeWorld("world"), w2 = fakeWorld("world_nether");
        UUID casterId = UUID.randomUUID(), allyId = UUID.randomUUID(), farId = UUID.randomUUID(),
                otherWorldId = UUID.randomUUID(), offlineId = UUID.randomUUID(), graceId = UUID.randomUUID();
        Player caster = fakePlayer(casterId, w1, 0, 64, 0, true);
        Player ally = fakePlayer(allyId, w1, 3, 64, 0, true);
        Player far = fakePlayer(farId, w1, 100, 64, 0, true);
        Player otherWorld = fakePlayer(otherWorldId, w2, 1, 64, 0, true);
        Player grace = fakePlayer(graceId, w1, 2, 64, 0, false);
        Map<UUID, Player> online = new HashMap<>();
        online.put(allyId, ally); online.put(farId, far); online.put(otherWorldId, otherWorld); online.put(graceId, grace);
        Function<UUID, Player> resolver = online::get;

        check("solo caster rosters himself alone",
                PartyRoster.select(caster, null, 0, resolver).equals(List.of(caster)));
        check("party = caster first, then members in roster order",
                PartyRoster.select(caster, List.of(casterId, allyId), 0, resolver).equals(List.of(caster, ally)));
        check("duplicated roster ids do not duplicate the selection",
                PartyRoster.select(caster, List.of(allyId, allyId, casterId), 0, resolver).equals(List.of(caster, ally)));
        check("cross-world member is excluded",
                PartyRoster.select(caster, List.of(otherWorldId), 0, resolver).equals(List.of(caster)));
        check("offline member (unresolvable) is excluded",
                PartyRoster.select(caster, List.of(offlineId), 0, resolver).equals(List.of(caster)));
        check("grace member (resolves but not online) is excluded",
                PartyRoster.select(caster, List.of(graceId), 0, resolver).equals(List.of(caster)));
        check("no range cap reaches the far member",
                PartyRoster.select(caster, List.of(farId), 0, resolver).equals(List.of(caster, far)));
        check("range cap drops the far member, keeps the near one",
                PartyRoster.select(caster, List.of(allyId, farId), 10, resolver).equals(List.of(caster, ally)));
        check("full house selection",
                PartyRoster.select(caster, List.of(allyId, farId, otherWorldId, offlineId, graceId), 0, resolver)
                        .equals(List.of(caster, ally, far)));
        check("partyMembersOf degrades to self while unbound",
                PartyRepairBridge.partyMembersOf(casterId).equals(List.of(casterId)));

        System.out.println("PartyRepairTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
