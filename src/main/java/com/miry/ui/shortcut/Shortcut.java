package com.miry.ui.shortcut;

/**
 * A key + modifier pair used by {@link ShortcutManager}.
 */
public record Shortcut(int key, int mods) {
    public static Shortcut of(int key) {
        return new Shortcut(key, 0);
    }

    public static Shortcut of(int key, int mods) {
        return new Shortcut(key, mods);
    }

    public boolean matches(int key, int mods) {
        return this.key == key && this.mods == mods;
    }
}
