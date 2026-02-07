package com.miry.ui.input;

import org.joml.Vector2f;

/**
 * Small per-frame snapshot of mouse input consumed by widgets.
 */
public final class UiInput {
    private final Vector2f mousePos = new Vector2f();
    private boolean mouseDown;
    private boolean mousePressed;
    private boolean mouseReleased;
    private double scrollY;

    public Vector2f mousePos() {
        return mousePos;
    }

    public UiInput setMousePos(float x, float y) {
        mousePos.set(x, y);
        return this;
    }

    public boolean mouseDown() {
        return mouseDown;
    }

    public boolean mousePressed() {
        return mousePressed;
    }

    public boolean mouseReleased() {
        return mouseReleased;
    }

    public UiInput setMouseButtons(boolean down, boolean pressed, boolean released) {
        this.mouseDown = down;
        this.mousePressed = pressed;
        this.mouseReleased = released;
        return this;
    }

    public double scrollY() {
        return scrollY;
    }

    public UiInput setScrollY(double scrollY) {
        this.scrollY = scrollY;
        return this;
    }
}
