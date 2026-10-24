package com.winkreyes.excelmenu.core;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Evaluates menu requirements (view requirements decide whether an
 * item appears in a slot; open requirements gate the whole menu).
 * The engine is platform-free: the Bukkit layer supplies a Context
 * that answers placeholders, permissions, balance and items.
 *
 * Supported types (format 1): has-permission, has-money, has-item,
 * string-equals, string-equals-ignorecase, string-contains,
 * regex-matches, gt, gte, eq, lte, lt. Anything else fails
 * validation at load time, so an unrecognized type can never reach
 * this engine from a loaded menu.
 */
public final class RequirementEngine {

    public interface Context {
        /** Resolve %placeholder% tokens in a string for this viewer. */
        String resolve(String text);
        boolean hasPermission(String permission);
        double balance();
        boolean hasItem(String material, int amount);
    }

    public static final Set<String> TYPES = Set.of(
            "has-permission", "has-money", "has-item",
            "string-equals", "string-equals-ignorecase", "string-contains",
            "regex-matches", "gt", "gte", "eq", "lte", "lt");

    private RequirementEngine() {}

    /** ALL requirements must pass; an empty list passes. */
    public static boolean allPass(List<MenuDef.Requirement> reqs, Context ctx) {
        if (reqs == null) return true;
        for (MenuDef.Requirement r : reqs) {
            if (!passes(r, ctx)) return false;
        }
        return true;
    }

    public static boolean passes(MenuDef.Requirement r, Context ctx) {
        try {
            switch (r.type()) {
                case "has-permission":
                    return ctx.hasPermission(str(r, "permission"));
                case "has-money":
                    return ctx.balance() >= num(r, "amount");
                case "has-item":
                    return ctx.hasItem(str(r, "material"),
                            (int) num(r, "amount", 1));
                case "string-equals":
                    return ctx.resolve(str(r, "input")).equals(ctx.resolve(str(r, "output")));
                case "string-equals-ignorecase":
                    return ctx.resolve(str(r, "input")).equalsIgnoreCase(ctx.resolve(str(r, "output")));
                case "string-contains":
                    return ctx.resolve(str(r, "input")).contains(ctx.resolve(str(r, "output")));
                case "regex-matches":
                    return Pattern.compile(str(r, "regex"))
                            .matcher(ctx.resolve(str(r, "input"))).find();
                case "gt":  return numIn(r, ctx) > numOut(r, ctx);
                case "gte": return numIn(r, ctx) >= numOut(r, ctx);
                case "eq":  return numIn(r, ctx) == numOut(r, ctx);
                case "lte": return numIn(r, ctx) <= numOut(r, ctx);
                case "lt":  return numIn(r, ctx) < numOut(r, ctx);
                default:    return false;
            }
        } catch (RuntimeException e) {
            // A requirement that cannot be evaluated (bad number,
            // bad regex, missing option) means "not met" — the item
            // stays hidden / the menu stays closed, never an error
            // sprayed at a player mid-click.
            return false;
        }
    }

    private static double numIn(MenuDef.Requirement r, Context ctx) {
        return Double.parseDouble(ctx.resolve(str(r, "input")).trim());
    }

    private static double numOut(MenuDef.Requirement r, Context ctx) {
        return Double.parseDouble(ctx.resolve(str(r, "output")).trim());
    }

    private static String str(MenuDef.Requirement r, String key) {
        Object v = r.options().get(key);
        if (v == null) throw new IllegalArgumentException("missing option " + key);
        return String.valueOf(v);
    }

    private static double num(MenuDef.Requirement r, String key) {
        return num(r, key, 0);
    }

    private static double num(MenuDef.Requirement r, String key, double dflt) {
        Object v = r.options().get(key);
        if (v == null) return dflt;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
