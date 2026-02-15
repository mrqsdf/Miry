package com.miry.integration;

import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;

/**
 * Helpers for embedding Miry in hosts that already own the OS/GLFW input callbacks.
 * <p>
 * Use {@link UiContext.Config#MANUAL_INPUT} when constructing the {@link UiContext}, then forward key/char events
 * from your host into {@link UiContext#keyboard()} manually.
 */
public final class MiryManualInput {
    private MiryManualInput() {}

    public static void pushKey(UiContext ctx, int key, int scancode, KeyEvent.Action action, int mods) {
        if (ctx == null) return;
        ctx.keyboard().pushKeyEvent(key, scancode, action, mods);
    }

    public static void pushChar(UiContext ctx, int codepoint) {
        if (ctx == null) return;
        ctx.keyboard().pushCharEvent(codepoint);
    }
}

