package com.miry.ui.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates monotonically increasing widget ids.
 * <p>
 * Ids are used to link widget state (focus, pointer capture, etc.) across frames.
 */
final class WidgetIds {
    private static final AtomicInteger NEXT = new AtomicInteger(1);

    private WidgetIds() {
    }

    static int next() {
        return NEXT.getAndIncrement();
    }
}
