package com.miry.ui.layout;

import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;

import java.util.Objects;

/**
 * Manages a {@link DockNode} tree (resize, splitter interaction, and rendering).
 */
public final class DockSpace {
    private final DockNode root;
    private com.miry.ui.Ui ui;

    private int splitterSize = 6;
    private int splitterDrawSize = 2;

    private SplitNode activeSplit;
    private SplitNode hoveredSplit;

    public DockSpace(DockNode root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public DockNode root() {
        return root;
    }

    public void setUi(com.miry.ui.Ui ui) {
        this.ui = ui;
    }

    public DockLayout layout() {
        return root.snapshotLayout();
    }

    public void resize(int width, int height) {
        root.resize(0, 0, Math.max(1, width), Math.max(1, height));
    }

    public void update(UiInput input) {
        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean mouseDown = input.mouseDown();
        boolean mousePressed = input.mousePressed();
        boolean mouseReleased = input.mouseReleased();

        hoveredSplit = findSplitHit(root, mx, my);

        if (mousePressed && hoveredSplit != null) {
            activeSplit = hoveredSplit;
        }

        if (mouseReleased) {
            activeSplit = null;
        }

        if (activeSplit != null && mouseDown) {
            updateSplitRatio(activeSplit, mx, my);
            root.resize(root.x, root.y, root.width, root.height);
        }
    }

    public void render(UiRenderer r) {
        renderNode(r, root);
        renderSplitters(r, root);
    }

    private void renderNode(UiRenderer r, DockNode node) {
        if (node instanceof SplitNode split) {
            renderNode(r, split.childA);
            renderNode(r, split.childB);
            return;
        }
        if (node instanceof LeafNode leaf) {
            leaf.render(r, ui);
            return;
        }
        node.render(r);
    }

    public int splitterSize() {
        return splitterSize;
    }

    public void setSplitterSize(int splitterSize) {
        this.splitterSize = Math.max(2, splitterSize);
    }

    public int splitterDrawSize() {
        return splitterDrawSize;
    }

    public void setSplitterDrawSize(int splitterDrawSize) {
        this.splitterDrawSize = Math.max(1, splitterDrawSize);
    }

    private void updateSplitRatio(SplitNode split, float mx, float my) {
        float ratio;
        if (split.vertical) {
            float local = my - split.y;
            ratio = split.height <= 0 ? split.splitRatio : (local / split.height);
        } else {
            float local = mx - split.x;
            ratio = split.width <= 0 ? split.splitRatio : (local / split.width);
        }
        split.splitRatio = clamp(ratio, 0.05f, 0.95f);
    }

    private void renderSplitters(UiRenderer r, DockNode node) {
        if (!(node instanceof SplitNode split)) {
            return;
        }

        int color = split == activeSplit ? 0xFF4C9AFF : (split == hoveredSplit ? 0xFF3A3A42 : 0xFF2A2A30);
        if (split.vertical) {
            int dividerY = split.childA.y + split.childA.height;
            int drawY = dividerY - splitterDrawSize / 2;
            r.drawRect(split.x, drawY, split.width, splitterDrawSize, color);
        } else {
            int dividerX = split.childA.x + split.childA.width;
            int drawX = dividerX - splitterDrawSize / 2;
            r.drawRect(drawX, split.y, splitterDrawSize, split.height, color);
        }

        renderSplitters(r, split.childA);
        renderSplitters(r, split.childB);
    }

    private SplitNode findSplitHit(DockNode node, float mx, float my) {
        if (!(node instanceof SplitNode split)) {
            return null;
        }

        SplitNode hitInChildren = findSplitHit(split.childA, mx, my);
        if (hitInChildren != null) {
            return hitInChildren;
        }
        hitInChildren = findSplitHit(split.childB, mx, my);
        if (hitInChildren != null) {
            return hitInChildren;
        }

        if (split.vertical) {
            int dividerY = split.childA.y + split.childA.height;
            int top = dividerY - splitterSize / 2;
            return contains(mx, my, split.x, top, split.width, splitterSize) ? split : null;
        } else {
            int dividerX = split.childA.x + split.childA.width;
            int left = dividerX - splitterSize / 2;
            return contains(mx, my, left, split.y, splitterSize, split.height) ? split : null;
        }
    }

    private static boolean contains(float px, float py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < (x + w) && py < (y + h);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
