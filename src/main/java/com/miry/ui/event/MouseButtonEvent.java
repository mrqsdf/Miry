package com.miry.ui.event;

/**
 * Mouse button press/release event.
 */
public final class MouseButtonEvent extends UiEvent {
    public enum Action { PRESS, RELEASE }

    private final int button;
    private final Action action;
    private final int mods;
    private final float x;
    private final float y;

    public MouseButtonEvent(int button, Action action, int mods, float x, float y) {
        this.button = button;
        this.action = action;
        this.mods = mods;
        this.x = x;
        this.y = y;
    }

    public int button() { return button; }
    public Action action() { return action; }
    public int mods() { return mods; }
    public float x() { return x; }
    public float y() { return y; }

    public boolean isPress() { return action == Action.PRESS; }
    public boolean isRelease() { return action == Action.RELEASE; }
}
