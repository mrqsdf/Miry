package com.miry.ui.event;

/**
 * Keyboard key event (press/release/repeat) with modifier flags.
 */
public final class KeyEvent extends UiEvent {
    public enum Action { PRESS, RELEASE, REPEAT }

    private final int key;
    private final int scancode;
    private final Action action;
    private final int mods;

    public KeyEvent(int key, int scancode, Action action, int mods) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.mods = mods;
    }

    public int key() { return key; }
    public int scancode() { return scancode; }
    public Action action() { return action; }
    public int mods() { return mods; }

    public boolean isPress() { return action == Action.PRESS; }
    public boolean isRelease() { return action == Action.RELEASE; }
    public boolean isRepeat() { return action == Action.REPEAT; }
    public boolean isPressOrRepeat() { return action == Action.PRESS || action == Action.REPEAT; }

    public boolean hasCtrl() { return (mods & 0x0002) != 0; }
    public boolean hasShift() { return (mods & 0x0001) != 0; }
    public boolean hasAlt() { return (mods & 0x0004) != 0; }
    public boolean hasSuper() { return (mods & 0x0008) != 0; }
}
