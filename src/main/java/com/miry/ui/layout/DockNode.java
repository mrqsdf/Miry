package com.miry.ui.layout;

import com.miry.ui.render.UiRenderer;
import org.joml.Vector4f;

/**
 * Base type for nodes in a simple editor docking tree.
 * <p>
 * Nodes store pixel bounds and can render and resize themselves.
 */
public abstract class DockNode {
    protected DockNode parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

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
