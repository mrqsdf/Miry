package com.miry.ui.core;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;

/**
 * Small helper for global keyboard navigation (Tab focus cycling).
 *
 * Usage: call from your event processing loop before forwarding key events to widgets.
 */
public final class KeyboardNavigation {
    private KeyboardNavigation() {
    }

    public static boolean handleKey(UiContext ctx, KeyEvent event) {
        if (ctx == null || event == null || !event.isPress()) {
            return false;
        }

        if (event.key() == InputConstants.KEY_TAB) {
            if (event.hasShift()) {
                ctx.focus().focusPrevious();
            } else {
                ctx.focus().focusNext();
            }
            return true;
        }

        return false;
    }
}

