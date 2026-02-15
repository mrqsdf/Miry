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
    private boolean ctrlDown;
    private boolean shiftDown;
    private boolean altDown;
    private boolean superDown;

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

    public boolean ctrlDown() {
        return ctrlDown;
    }

    public boolean shiftDown() {
        return shiftDown;
    }

    public boolean altDown() {
        return altDown;
    }

    public boolean superDown() {
        return superDown;
    }

    public UiInput setModifiers(boolean ctrlDown, boolean shiftDown, boolean altDown, boolean superDown) {
        this.ctrlDown = ctrlDown;
        this.shiftDown = shiftDown;
        this.altDown = altDown;
        this.superDown = superDown;
        return this;
    }

    public double scrollY() {
        return scrollY;
    }

    public UiInput setScrollY(double scrollY) {
        this.scrollY = scrollY;
        return this;
    }

    public float mouseScrollDelta() {
        return (float)scrollY;
    }

    /**
     * Returns the current scroll delta and clears it (so other widgets don't also react this frame).
     */
    public double consumeScrollY() {
        double v = scrollY;
        scrollY = 0.0;
        return v;
    }

    public float consumeMouseScrollDelta() {
        return (float) consumeScrollY();
    }
}
