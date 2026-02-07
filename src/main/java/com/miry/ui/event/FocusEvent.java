package com.miry.ui.event;

/**
 * Focus gained/lost event for a focusable widget id.
 */
public final class FocusEvent extends UiEvent {
    public enum Type { GAINED, LOST }

    private final int widgetId;
    private final Type type;

    public FocusEvent(int widgetId, Type type) {
        this.widgetId = widgetId;
        this.type = type;
    }

    public int widgetId() { return widgetId; }
    public Type type() { return type; }
    public boolean isGained() { return type == Type.GAINED; }
    public boolean isLost() { return type == Type.LOST; }
}
