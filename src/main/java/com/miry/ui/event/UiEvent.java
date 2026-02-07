package com.miry.ui.event;

/**
 * Base UI event type consumed from {@link EventQueue}.
 */
public abstract class UiEvent {
    private boolean consumed;

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        this.consumed = true;
    }
}
