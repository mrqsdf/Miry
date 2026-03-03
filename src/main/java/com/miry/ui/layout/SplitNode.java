package com.miry.ui.layout;

import com.miry.ui.render.UiRenderer;

import java.util.Objects;

/**
 * A docking layout node that splits its region between two children.
 */
public final class SplitNode extends DockNode {
    public DockNode childA;
    public DockNode childB;
    public final boolean vertical;
    public float splitRatio;

    /**
     * Creates a split node.
     * <p>
     * Note: order matters. {@code childA} is laid out on the left/top, and {@code childB} on the right/bottom.
     * If you want a deterministic ordering independent of argument order, use {@link #stable(DockNode, DockNode, boolean, float)}.
     * </p>
     */
    public SplitNode(DockNode childA, DockNode childB, boolean vertical, float splitRatio) {
        this.childA = Objects.requireNonNull(childA, "childA");
        this.childB = Objects.requireNonNull(childB, "childB");
        this.vertical = vertical;
        this.splitRatio = clamp01(splitRatio);
        this.childA.parent = this;
        this.childB.parent = this;
    }

    /**
     * Builds a {@link SplitNode} with a stable, deterministic child ordering.
     * <p>
     * The two children are ordered by {@link DockNode#layoutId()} so swapping arguments produces the same layout.
     * The lower {@code layoutId} is placed on the left/top.
     * </p>
     * <p>
     * This is useful when you don't want accidental argument swapping to flip the layout.
     * </p>
     */
    public static SplitNode stable(DockNode a, DockNode b, boolean vertical, float splitRatio) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a.layoutId() <= b.layoutId()) {
            return new SplitNode(a, b, vertical, splitRatio);
        }
        return new SplitNode(b, a, vertical, splitRatio);
    }

    /**
     * Builds a {@link SplitNode} where the provided {@code leading} child is placed on the left/top,
     * regardless of argument order.
     *
     * @param a            First node (any order).
     * @param b            Second node (any order).
     * @param vertical     {@code true} for top/bottom split, {@code false} for left/right split.
     * @param leading      The node that should be placed on the left/top (must be {@code a} or {@code b}).
     * @param leadingRatio Fraction of space assigned to the leading node.
     */
    public static SplitNode leading(DockNode a, DockNode b, boolean vertical, DockNode leading, float leadingRatio) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(leading, "leading");
        if (leading == a) {
            return new SplitNode(a, b, vertical, leadingRatio);
        }
        if (leading == b) {
            return new SplitNode(b, a, vertical, leadingRatio);
        }
        throw new IllegalArgumentException("leading must be either a or b");
    }

    public void replaceChild(DockNode oldNode, DockNode newNode) {
        if (newNode == null || oldNode == null) return;
        if (childA == oldNode) {
            childA = newNode;
            newNode.parent = this;
            oldNode.parent = null;
        } else if (childB == oldNode) {
            childB = newNode;
            newNode.parent = this;
            oldNode.parent = null;
        }
    }

    @Override
    public void render(UiRenderer r) {
        childA.render(r);
        childB.render(r);
    }

    @Override
    public void resize(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;

        if (w <= 0 || h <= 0) {
            childA.resize(x, y, 0, 0);
            childB.resize(x, y, 0, 0);
            return;
        }

        float ratio = clamp01(splitRatio);
        if (vertical) {
            int splitPx = Math.round(h * ratio);
            splitPx = clamp(splitPx, 0, h);
            childA.resize(x, y, w, splitPx);
            childB.resize(x, y + splitPx, w, h - splitPx);
        } else {
            int splitPx = Math.round(w * ratio);
            splitPx = clamp(splitPx, 0, w);
            childA.resize(x, y, splitPx, h);
            childB.resize(x + splitPx, y, w - splitPx, h);
        }
    }

    @Override
    public DockLayout snapshotLayout() {
        return new DockLayout.Split(vertical, splitRatio, childA.snapshotLayout(), childB.snapshotLayout());
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
