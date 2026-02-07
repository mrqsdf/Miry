package com.miry.ui.event;

/**
 * Mouse move event with absolute coordinates.
 */
public final class MouseMoveEvent extends UiEvent {
    private final float x;
    private final float y;
    private final float dx;
    private final float dy;

    public MouseMoveEvent(float x, float y, float dx, float dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public float x() { return x; }
    public float y() { return y; }
    public float dx() { return dx; }
    public float dy() { return dy; }
}
