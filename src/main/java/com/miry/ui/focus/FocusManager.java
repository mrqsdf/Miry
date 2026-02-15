package com.miry.ui.focus;

import com.miry.ui.event.EventQueue;
import com.miry.ui.event.FocusEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks focused widget id and builds a per-frame tab order.
 */
public final class FocusManager {
    private int focusedId;
    private final List<Integer> focusOrder = new ArrayList<>();
    private final EventQueue eventQueue;

    public FocusManager(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    public void setFocus(int id) {
        if (focusedId == id) return;
        int prev = focusedId;
        focusedId = id;
        if (prev != 0) {
            eventQueue.push(new FocusEvent(prev, FocusEvent.Type.LOST));
        }
        if (id != 0) {
            eventQueue.push(new FocusEvent(id, FocusEvent.Type.GAINED));
        }
    }

    public void clearFocus() {
        setFocus(0);
    }

    public boolean isFocused(int id) {
        return focusedId == id;
    }

    public boolean hasAnyFocus() {
        return focusedId != 0;
    }

    public int focused() {
        return focusedId;
    }

    public void registerFocusable(int id) {
        if (!focusOrder.contains(id)) {
            focusOrder.add(id);
        }
    }

    public void unregisterFocusable(int id) {
        focusOrder.remove((Integer) id);
        if (focusedId == id) {
            focusedId = 0;
        }
    }

    public void clearFocusOrder() {
        focusOrder.clear();
    }

    /**
     * Call once per frame before widgets register themselves as focusable.
     * Keeps the focused id intact while rebuilding a correct tab order for the current frame.
     */
    public void beginFrame() {
        clearFocusOrder();
    }

    public void focusNext() {
        if (focusOrder.isEmpty()) return;
        int idx = focusOrder.indexOf(focusedId);
        int next = (idx + 1) % focusOrder.size();
        if (!focusOrder.isEmpty()) {
            setFocus(focusOrder.get(next));
        }
    }

    public void focusPrevious() {
        if (focusOrder.isEmpty()) return;
        int idx = focusOrder.indexOf(focusedId);
        int prev = (idx - 1 + focusOrder.size()) % focusOrder.size();
        if (!focusOrder.isEmpty()) {
            setFocus(focusOrder.get(prev));
        }
    }
}
