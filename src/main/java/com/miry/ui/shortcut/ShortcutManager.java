package com.miry.ui.shortcut;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyboard shortcut registry with optional per-scope maps.
 */
public final class ShortcutManager {
    private final Map<Shortcut, Runnable> globalShortcuts = new HashMap<>();
    private final Map<Integer, Map<Shortcut, Runnable>> scopedShortcuts = new HashMap<>();

    public void registerGlobal(Shortcut shortcut, Runnable action) {
        globalShortcuts.put(shortcut, action);
    }

    public void registerScoped(int scope, Shortcut shortcut, Runnable action) {
        scopedShortcuts.computeIfAbsent(scope, k -> new HashMap<>()).put(shortcut, action);
    }

    public void unregisterGlobal(Shortcut shortcut) {
        globalShortcuts.remove(shortcut);
    }

    public void unregisterScoped(int scope, Shortcut shortcut) {
        Map<Shortcut, Runnable> map = scopedShortcuts.get(scope);
        if (map != null) {
            map.remove(shortcut);
        }
    }

    public void clearScope(int scope) {
        scopedShortcuts.remove(scope);
    }

    public boolean trigger(int key, int mods, int activeScope) {
        Map<Shortcut, Runnable> scoped = scopedShortcuts.get(activeScope);
        if (scoped != null) {
            for (Map.Entry<Shortcut, Runnable> e : scoped.entrySet()) {
                if (e.getKey().matches(key, mods)) {
                    e.getValue().run();
                    return true;
                }
            }
        }
        for (Map.Entry<Shortcut, Runnable> e : globalShortcuts.entrySet()) {
            if (e.getKey().matches(key, mods)) {
                e.getValue().run();
                return true;
            }
        }
        return false;
    }
}
