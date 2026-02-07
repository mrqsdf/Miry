package com.miry.ui.event;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple FIFO queue for UI events.
 */
public final class EventQueue {
    private final Deque<UiEvent> events = new ArrayDeque<>();

    public void push(UiEvent event) {
        if (event != null) {
            events.addLast(event);
        }
    }

    public UiEvent poll() {
        return events.pollFirst();
    }

    public void clear() {
        events.clear();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public int size() {
        return events.size();
    }
}
