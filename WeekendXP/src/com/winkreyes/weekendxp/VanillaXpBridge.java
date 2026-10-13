package com.winkreyes.weekendxp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Optional runtime API on the custom mcMMO plugin, without a compile-time dependency. */
final class VanillaXpBridge {
    private final Supplier<?> pluginSupplier;
    private final Consumer<String> warning;
    private Object plugin;
    private Method supported;
    private Method setter;
    private Method getter;
    private String lastWarning;

    VanillaXpBridge(Supplier<?> pluginSupplier, Consumer<String> warning) {
        this.pluginSupplier = Objects.requireNonNull(pluginSupplier);
        this.warning = Objects.requireNonNull(warning);
    }

    /** Probe immediately at startup, even if command dispatch will subsequently fail. */
    boolean isSupported() {
        try {
            resolveMethods();
            if (!Boolean.TRUE.equals(supported.invoke(plugin))) {
                return fail("the installed mcMMO reports that vanilla XP bonuses are unsupported");
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return fail(describe(e));
        }
    }

    boolean applyMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0) {
            throw new IllegalArgumentException("Vanilla XP multiplier must be finite and at least 1.0");
        }
        if (!isSupported()) {
            return false;
        }
        try {
            setter.invoke(plugin, multiplier);
            double actual = (double) getter.invoke(plugin);
            if (Double.compare(actual, multiplier) != 0) {
                return fail("multiplier readback was " + actual + ", expected " + multiplier);
            }
            lastWarning = null;
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return fail(describe(e));
        }
    }

    private void resolveMethods() throws ReflectiveOperationException {
        Object current = pluginSupplier.get();
        if (current == null) {
            throw new IllegalStateException("the enabled mcMMO plugin could not be found");
        }
        if (current == plugin && setter != null) {
            return;
        }
        // Resolve against the actual plugin classloader. Cache only a complete API,
        // and invalidate it if a replacement plugin instance is supplied later.
        Class<?> type = current.getClass();
        Method newSupported = instanceMethod(type, "isVanillaXpBonusSupported", boolean.class);
        Method newSetter = instanceMethod(type, "setVanillaXpEventMultiplier", void.class, double.class);
        Method newGetter = instanceMethod(type, "getVanillaXpEventMultiplier", double.class);
        plugin = current;
        supported = newSupported;
        setter = newSetter;
        getter = newGetter;
    }

    private static Method instanceMethod(Class<?> type, String name, Class<?> returnType,
                                         Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = type.getMethod(name, parameterTypes);
        if (method.getReturnType() != returnType || Modifier.isStatic(method.getModifiers())) {
            throw new NoSuchMethodException(type.getName() + "." + name + " has an incompatible signature");
        }
        return method;
    }

    private boolean fail(String detail) {
        String message = "Minecraft greenbar XP schedule is not confirmed: " + detail
                + ". WeekendXP 1.0.2 requires the matching custom mcMMO vanilla XP bridge;"
                + " check the installed mcMMO build. The schedule will retry at the next check.";
        if (!message.equals(lastWarning)) {
            warning.accept(message);
            lastWarning = message;
        }
        return false;
    }

    private static String describe(Throwable failure) {
        Throwable cause = failure instanceof InvocationTargetException invocation
                && invocation.getCause() != null ? invocation.getCause() : failure;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
