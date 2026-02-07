package com.miry.ui.event;

/**
 * Mouse scroll event.
 */
public final class ScrollEvent extends UiEvent {
    private final float x;
    private final float y;
    private final double dx;
    private final double dy;

    public ScrollEvent(float x, float y, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public float x() { return x; }
    public float y() { return y; }
    public double dx() { return dx; }
    public double dy() { return dy; }
}
