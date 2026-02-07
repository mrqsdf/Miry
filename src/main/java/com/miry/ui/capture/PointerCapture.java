package com.miry.ui.capture;

/**
 * Pointer capture state used to prevent mouse interactions from "punching through" to other widgets.
 */
public final class PointerCapture {
    private int capturedId;

    public void capture(int id) {
        this.capturedId = id;
    }

    public void release() {
        this.capturedId = 0;
    }

    public boolean isCaptured(int id) {
        return capturedId != 0 && capturedId == id;
    }

    public boolean hasCaptured() {
        return capturedId != 0;
    }

    public int captured() {
        return capturedId;
    }
}
