package com.uddernetworks.reddicord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Cooldown<T> {

    private final long mills;
    private final Map<T, Long> cooldown = new ConcurrentHashMap<>();

    public Cooldown(long duration) {
        this(duration, TimeUnit.MILLISECONDS);
    }

    public Cooldown(long duration, TimeUnit unit) {
        this.mills = unit.toMillis(duration);
    }

    /**
     * Returns if the object has been cooled down, AND updates the time from the constructor's cooldown time.
     *
     * @param t The key
     * @return If the object has been cooled down
     */
    public boolean isDone(T t) {
        if (!cooldown.containsKey(t)) return true;
        if (cooldown.get(t) <= System.currentTimeMillis()) {
            cooldown.put(t, System.currentTimeMillis() + mills);
            return true;
        }
        return false;
    }

    /**
     * Checks if the object has been cooled down, without modifying its presence in the map.
     *
     * @param t The key
     * @return If the object has been cooled down
     */
    public boolean checkDone(T t) {
        if (!cooldown.containsKey(t)) return true;
        return cooldown.get(t) <= System.currentTimeMillis();
    }

    /**
     * Gets the remaining cooldown in milliseconds
     *
     * @param t The key
     * @return The remaining time in milliseconds, minimum value being 0
     */
    public long getRemaining(T t) {
        return Math.max(cooldown.getOrDefault(t, 0L) - System.currentTimeMillis(), 0);
    }

    /**
     * Resets the cooldown for the given object.
     *
     * @param t The object to reset
     */
    public void reset(T t) {
        cooldown.remove(t);
    }

    public void ifDone(T t, Consumer<T> done) {
        if (isDone(t)) done.accept(t);
    }

    public void ifDone(T t, Consumer<T> done, Consumer<T> waiting) {
        (isDone(t) ? done : waiting).accept(t);
    }

    public void ifDone(T t, Runnable done) {
        if (isDone(t)) done.run();
    }

    public void ifDone(T t, Runnable done, Runnable waiting) {
        (isDone(t) ? done : waiting).run();
    }

    public <V> V ifDone(T t, Supplier<V> done, V def) {
        if (isDone(t)) return done.get();
        return def;
    }

    public <V> V ifDone(T t, Supplier<V> done, Supplier<V> waiting) {
        return (isDone(t) ? done : waiting).get();
    }

}
