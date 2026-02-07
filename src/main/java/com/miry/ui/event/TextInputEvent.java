package com.miry.ui.event;

/**
 * Text input event (codepoint), typically emitted by the platform text callback.
 */
public final class TextInputEvent extends UiEvent {
    private final int codepoint;

    public TextInputEvent(int codepoint) {
        this.codepoint = codepoint;
    }

    public int codepoint() { return codepoint; }

    public String asString() {
        return new String(Character.toChars(codepoint));
    }
}
