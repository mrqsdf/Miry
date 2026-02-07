package com.miry.ui.core;

import com.miry.ui.render.UiRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Per-frame overlay draw queue.
 * <p>
 * Widgets can enqueue popup rendering (combo drop-downs, node context menus, etc.) during their
 * normal render pass. The host renders all queued overlays at the end of the frame to guarantee
 * correct Z-ordering (above panels and other content).
 */
public final class OverlayQueue {
    /**
     * Draw command for an overlay layer.
     */
    @FunctionalInterface
    public interface OverlayDraw {
        void draw(UiRenderer r);
    }

    private final List<OverlayDraw> items = new ArrayList<>();

    /**
     * Clears the queue for the next frame.
     */
    public void beginFrame() {
        items.clear();
    }

    /**
     * Enqueues an overlay draw item.
     */
    public void add(OverlayDraw draw) {
        items.add(Objects.requireNonNull(draw, "draw"));
    }

    /**
     * Renders all queued overlays in insertion order.
     */
    public void render(UiRenderer r) {
        if (r == null || items.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            items.get(i).draw(r);
        }
    }

    public int size() {
        return items.size();
    }
}

