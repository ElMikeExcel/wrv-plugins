package wrv.harness;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import net.kyori.adventure.text.Component;

/** Synthetic player backed by Paper's real CraftInventory and real ItemStacks. No network client. */
public final class FakePlayer {
    public final UUID uuid;
    public final String name;
    public World world;
    public double x = 0.5, y = 5, z = 0.5;
    public boolean online = true, allowed = true, op;
    public int selected;
    public final Set<String> denied = new HashSet<>();
    public final List<String> messages = new ArrayList<>();
    public final Inventory storage;
    public final PlayerInventory inventory;
    public final Player player;
    public InventoryView view;
    public ItemStack cursor = new ItemStack(Material.AIR), offhand = new ItemStack(Material.AIR);
    public boolean failSetStorage;
    public int opens, closes;
    public final List<Object> shownDialogs = new ArrayList<>();
    public Object dialog;
    public boolean failDialog;
    public int dialogCloses;
    public FakePlayer(UUID uuid, String name, World world) {
        this.uuid = uuid; this.name = name; this.world = world;
        storage = Bukkit.createInventory(null, 36, Component.text("Disposable player storage"));
        inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getHolder": return playerRef();
                case "getType": return InventoryType.PLAYER;
                case "getSize": return 41;
                case "getLocation": return new Location(this.world, x, y, z);
                case "getStorageContents": return cloneItems(storage.getContents());
                case "setStorageContents": if (failSetStorage) throw new IllegalStateException("Injected inventory write failure"); storage.setContents(cloneItems((ItemStack[]) args[0])); return null;
                case "getContents": { ItemStack[] all = Arrays.copyOf(storage.getContents(), 41); all[40] = offhand; return all; }
                case "setContents": storage.setContents(Arrays.copyOf((ItemStack[]) args[0], 36)); return null;
                case "getItemInMainHand": return Optional.ofNullable(storage.getItem(selected)).orElse(new ItemStack(Material.AIR));
                case "setItemInMainHand": storage.setItem(selected, (ItemStack) args[0]); return null;
                case "getItemInOffHand": return offhand;
                case "setItemInOffHand": offhand = (ItemStack) args[0]; return null;
                case "getHeldItemSlot": return selected;
                case "setHeldItemSlot": selected = (int) args[0]; return null;
                case "getArmorContents": return new ItemStack[4];
                case "getExtraContents": return new ItemStack[]{offhand};
                case "equals": return proxy == args[0];
                case "hashCode": return uuid.hashCode();
                case "toString": return "TestInventory:" + name;
                default:
                    try { return storage.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(storage, args); }
                    catch (NoSuchMethodException ignored) { return defaultValue(method.getReturnType()); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
            }
        });
        player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return uuid;
                case "getName": return name;
                case "getWorld": return this.world;
                case "getLocation": return new Location(this.world, x, y, z);
                case "getEyeLocation": return new Location(this.world, x, y + 1.62, z);
                case "getInventory": return inventory;
                case "getItemOnCursor": return cursor;
                case "setItemOnCursor": cursor = (ItemStack) args[0]; return null;
                case "isOnline": case "isValid": return online;
                case "isDead": return false;
                case "isOp": return op;
                case "isSneaking": return false;
                case "hasPermission": case "isPermissionSet": return allowed && !denied.contains(args[0].toString());
                case "canSee": return true;
                case "getOpenInventory": return view;
                case "showDialog":
                    if (failDialog) throw new IllegalStateException("Injected native dialog send failure");
                    dialog = args[0]; shownDialogs.add(dialog); return null;
                case "closeDialog": dialog = null; dialogCloses++; return null;
                case "openInventory":
                    if (args[0] instanceof Inventory inv) view = view(inv);
                    else view = (InventoryView) args[0];
                    opens++; return view;
                case "closeInventory":
                    InventoryView old = view; view = null; closes++;
                    if (old != null) Bukkit.getPluginManager().callEvent(new InventoryCloseEvent(old));
                    return null;
                case "getGameMode": return GameMode.SURVIVAL;
                case "equals": return proxy == args[0];
                case "hashCode": return uuid.hashCode();
                case "toString": return name;
                case "sendMessage": case "sendActionBar": if (args != null) messages.add(Arrays.deepToString(args)); return null;
                default: return defaultValue(method.getReturnType());
            }
        });
    }
    private Player playerRef() { return player; }
    public InventoryView view(Inventory top) {
        return (InventoryView) Proxy.newProxyInstance(InventoryView.class.getClassLoader(), new Class<?>[]{InventoryView.class}, (proxy, method, args) -> {
            int slot;
            switch (method.getName()) {
                case "getTopInventory": return top;
                case "getBottomInventory": return inventory;
                case "getPlayer": return player;
                case "getType": return InventoryType.CHEST;
                case "countSlots": return top.getSize() + 36;
                case "getCursor": return cursor;
                case "setCursor": cursor = (ItemStack) args[0]; return null;
                case "getInventory": slot = (int) args[0]; return slot < 0 ? null : slot < top.getSize() ? top : inventory;
                case "convertSlot": slot = (int) args[0]; return slot < top.getSize() ? slot : slot - top.getSize();
                case "getItem": slot = (int) args[0]; return slot < 0 ? null : slot < top.getSize() ? top.getItem(slot) : inventory.getItem(slot - top.getSize());
                case "setItem": slot = (int) args[0]; if (slot >= 0) { if (slot < top.getSize()) top.setItem(slot, (ItemStack) args[1]); else inventory.setItem(slot - top.getSize(), (ItemStack) args[1]); } return null;
                case "getSlotType": return InventoryType.SlotType.CONTAINER;
                case "getTitle": case "getOriginalTitle": return "Disposable test view";
                case "title": return Component.text("Disposable test view");
                case "close": player.closeInventory(); return null;
                case "equals": return proxy == args[0];
                case "hashCode": return System.identityHashCode(proxy);
                default: return defaultValue(method.getReturnType());
            }
        });
    }
    public int count(ItemStack sample) { int total = 0; for (ItemStack stack : storage.getContents()) if (stack != null && stack.isSimilar(sample)) total += stack.getAmount(); return total; }
    public void clear() { storage.clear(); cursor = new ItemStack(Material.AIR); }
    private static ItemStack[] cloneItems(ItemStack[] source) { ItemStack[] result = source.clone(); for (int i = 0; i < result.length; i++) if (result[i] != null) result[i] = result[i].clone(); return result; }
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return (char) 0;
    }
}
