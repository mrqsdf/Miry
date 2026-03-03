package com.miry.ui.layout;

import com.miry.ui.render.UiRenderer;
import org.joml.Vector4f;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base type for nodes in a simple editor docking tree.
 * <p>
 * Nodes store pixel bounds and can render and resize themselves.
 */
public abstract class DockNode {
    private static final AtomicInteger NEXT_LAYOUT_ID = new AtomicInteger(1);
    private final int layoutId = NEXT_LAYOUT_ID.getAndIncrement();

    protected DockNode parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    /**
     * Stable, auto-assigned ID useful for deterministic layout helpers.
     */
    public final int layoutId() {
        return layoutId;
    }

    public DockNode parent() {
        return parent;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public final void bounds(Vector4f out) {
        out.set(x, y, width, height);
    }

    public abstract void render(UiRenderer r);

    public abstract void resize(int x, int y, int w, int h);

    public abstract DockLayout snapshotLayout();
}
