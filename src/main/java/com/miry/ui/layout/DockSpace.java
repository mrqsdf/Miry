package com.miry.ui.layout;

import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;

import java.util.Objects;

/**
 * Manages a {@link DockNode} tree (resize, splitter interaction, and rendering).
 */
public final class DockSpace {
    private DockNode root;
    private Ui ui;
    private UiContext uiContext;

    private int splitterSize = 6;
    private int splitterDrawSize = 2;

    private SplitNode activeSplit;
    private SplitNode hoveredSplit;
    private LeafNode hoveredLeaf;
    private int nextPlaceholder = 1;
    private int lastWidth = 1;
    private int lastHeight = 1;

    public DockSpace(DockNode root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public DockNode root() {
        return root;
    }

    public void setUi(Ui ui) {
        this.ui = ui;
    }

    public void setUiContext(UiContext uiContext) {
        this.uiContext = uiContext;
    }

    public DockLayout layout() {
        return root.snapshotLayout();
    }

    public void resize(int width, int height) {
        lastWidth = Math.max(1, width);
        lastHeight = Math.max(1, height);
        root.resize(0, 0, lastWidth, lastHeight);
    }

    public void update(UiInput input) {
        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean mouseDown = input.mouseDown();
        boolean mousePressed = input.mousePressed();
        boolean mouseReleased = input.mouseReleased();

        hoveredSplit = findSplitHit(root, mx, my);
        hoveredLeaf = findLeafHit(root, mx, my);

        if (mousePressed && hoveredSplit != null) {
            activeSplit = hoveredSplit;
        }

        if (mouseReleased) {
            activeSplit = null;
        }

        if (activeSplit != null && mouseDown) {
            updateSplitRatio(activeSplit, mx, my);
            root.resize(0, 0, lastWidth, lastHeight);
        }

        if (mousePressed && hoveredLeaf != null && activeSplit == null) {
            HeaderAction action = hitHeaderAction(hoveredLeaf, mx, my);
            if (action == HeaderAction.SPLIT_H) {
                splitLeaf(hoveredLeaf, false);
                root.resize(0, 0, lastWidth, lastHeight);
            } else if (action == HeaderAction.SPLIT_V) {
                splitLeaf(hoveredLeaf, true);
                root.resize(0, 0, lastWidth, lastHeight);
            } else if (action == HeaderAction.CLOSE) {
                closeLeaf(hoveredLeaf);
                root.resize(0, 0, lastWidth, lastHeight);
            }
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
            leaf.render(r, ui, uiContext);
            return;
        }
        node.render(r);
    }

    private enum HeaderAction {
        NONE,
        SPLIT_H,
        SPLIT_V,
        CLOSE
    }

    private HeaderAction hitHeaderAction(LeafNode leaf, float mx, float my) {
        int hh = Math.min(leaf.headerHeight(), leaf.height());
        if (hh <= 0) return HeaderAction.NONE;
        if (mx < leaf.x() || mx >= leaf.x() + leaf.width()) return HeaderAction.NONE;
        if (my < leaf.y() || my >= leaf.y() + hh) return HeaderAction.NONE;

        LeafNode.HeaderButtons buttons = leaf.headerButtons();
        if (buttons == LeafNode.HeaderButtons.NONE) {
            return HeaderAction.NONE;
        }

        // Keep geometry aligned with LeafNode header rendering.
        int btn = Math.max(12, hh - 10);
        int pad = 6;
        int x0 = leaf.x() + leaf.width() - pad - btn;
        int y0 = leaf.y() + (hh - btn) / 2;
        // Right-to-left: [X][V][H]
        if (mx >= x0 && my >= y0 && mx < x0 + btn && my < y0 + btn) return HeaderAction.CLOSE;
        if (buttons == LeafNode.HeaderButtons.CLOSE_ONLY) {
            return HeaderAction.NONE;
        }
        x0 -= (btn + 4);
        if (mx >= x0 && my >= y0 && mx < x0 + btn && my < y0 + btn) return HeaderAction.SPLIT_V;
        x0 -= (btn + 4);
        if (mx >= x0 && my >= y0 && mx < x0 + btn && my < y0 + btn) return HeaderAction.SPLIT_H;
        return HeaderAction.NONE;
    }

    private void splitLeaf(LeafNode leaf, boolean vertical) {
        if (leaf == null) return;
        var panel = new com.miry.ui.panels.PlaceholderPanel(
            "Panel " + nextPlaceholder,
            "Placeholder panel (created by split)."
        );
        nextPlaceholder++;
        LeafNode other = new LeafNode(panel);
        other.setBackgroundArgb(0xFF1C1C1E);

        SplitNode split = new SplitNode(leaf, other, vertical, 0.5f);
        DockNode parent = leaf.parent();
        if (parent instanceof SplitNode sp) {
            split.parent = sp;
            sp.replaceChild(leaf, split);
        } else {
            split.parent = null;
            root = split;
        }
    }

    private void closeLeaf(LeafNode leaf) {
        if (leaf == null) return;
        if (leaf.tabCount() > 1) {
            leaf.closeActiveTab();
            return;
        }
        DockNode parent = leaf.parent();
        if (!(parent instanceof SplitNode sp)) {
            return;
        }
        DockNode sibling = sp.childA == leaf ? sp.childB : sp.childA;
        DockNode grand = sp.parent();
        if (grand instanceof SplitNode gp) {
            gp.replaceChild(sp, sibling);
            sibling.parent = gp;
        } else {
            sibling.parent = null;
            root = sibling;
        }
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

    private LeafNode findLeafHit(DockNode node, float mx, float my) {
        if (node instanceof SplitNode split) {
            LeafNode a = findLeafHit(split.childA, mx, my);
            if (a != null) return a;
            return findLeafHit(split.childB, mx, my);
        }
        if (node instanceof LeafNode leaf) {
            return contains(mx, my, leaf.x(), leaf.y(), leaf.width(), leaf.height()) ? leaf : null;
        }
        return null;
    }

    private static boolean contains(float px, float py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < (x + w) && py < (y + h);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
