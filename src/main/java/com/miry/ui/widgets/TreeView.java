package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Virtualized tree view widget.
 */
public final class TreeView<T> extends BaseWidget {
    private final TreeNode<T> root;
    private final int itemHeight;
    private final Set<TreeNode<T>> selectedNodes = new HashSet<>();
    private Function<T, String> labelFunc = Object::toString;

    public TreeView(TreeNode<T> root, int itemHeight) {
        this.root = root;
        this.itemHeight = Math.max(1, itemHeight);
    }

    public void setLabelFunction(Function<T, String> func) {
        this.labelFunc = func != null ? func : Object::toString;
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int width,
                       int height,
                       int scrollOffset,
                       boolean interactive) {
        if (r == null || theme == null) {
            return;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hovered = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        int bg = Theme.toArgb(theme.panelBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        if (theme.skins.panel != null) {
            theme.skins.panel.drawWithOutline(r, x, y, width, height, bg, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bg);
            drawOutline(r, x, y, width, height, 1, outline);
        }
        drawFocusRing(r, theme, x, y, width, height);

        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            handleClick((int) mx, (int) my, x, y, width, height, scrollOffset);
        }

        render(r, theme, x, y, width, height, scrollOffset);
    }

    public void render(UiRenderer r, int x, int y, int width, int height, int scrollOffset) {
        render(r, null, x, y, width, height, scrollOffset);
    }

    private void render(UiRenderer r, Theme theme, int x, int y, int width, int height, int scrollOffset) {
        List<VisibleNode<T>> visible = collectVisible(root, 0);
        int startIdx = scrollOffset / itemHeight;
        int endIdx = Math.min(visible.size(), startIdx + (height / itemHeight) + 1);

        for (int i = startIdx; i < endIdx; i++) {
            VisibleNode<T> vn = visible.get(i);
            int itemY = y + (i * itemHeight) - scrollOffset;
            renderNode(r, theme, vn, x, itemY, width);
        }
    }

    public boolean handleClick(int mx, int my, int x, int y, int width, int height, int scrollOffset) {
        if (mx < x || my < y || mx >= x + width || my >= y + height) {
            return false;
        }

        List<VisibleNode<T>> visible = collectVisible(root, 0);
        int idx = (my - y + scrollOffset) / itemHeight;
        if (idx < 0 || idx >= visible.size()) {
            return false;
        }

        VisibleNode<T> vn = visible.get(idx);
        int indent = vn.depth * 20;

        boolean hasChildren = !vn.node.children().isEmpty();
        int iconX0 = x + indent;
        int iconX1 = iconX0 + 20;
        if (hasChildren && mx >= iconX0 && mx < iconX1) {
            vn.node.toggleExpanded();
            return true;
        }

        clearSelection();
        vn.node.setSelected(true);
        selectedNodes.add(vn.node);
        return true;
    }

    public boolean handleKey(UiContext ctx, KeyEvent event) {
        if (!enabled() || ctx == null || event == null || !event.isPressOrRepeat() || !isFocused(ctx)) {
            return false;
        }

        List<VisibleNode<T>> visible = collectVisible(root, 0);
        if (visible.isEmpty()) {
            return false;
        }

        int selectedIdx = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).node.selected()) {
                selectedIdx = i;
                break;
            }
        }
        if (selectedIdx < 0) {
            selectedIdx = 0;
        }

        int key = event.key();
        if (key == InputConstants.KEY_DOWN) {
            selectedIdx = Math.min(visible.size() - 1, selectedIdx + 1);
            selectVisible(visible, selectedIdx);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            selectedIdx = Math.max(0, selectedIdx - 1);
            selectVisible(visible, selectedIdx);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_ENTER) {
            TreeNode<T> n = visible.get(selectedIdx).node;
            if (!n.children().isEmpty()) {
                n.setExpanded(true);
                return true;
            }
        }
        if (key == InputConstants.KEY_LEFT) {
            TreeNode<T> n = visible.get(selectedIdx).node;
            if (!n.children().isEmpty() && n.expanded()) {
                n.setExpanded(false);
                return true;
            }
        }

        return false;
    }

    private void selectVisible(List<VisibleNode<T>> visible, int idx) {
        clearSelection();
        idx = Math.max(0, Math.min(idx, visible.size() - 1));
        TreeNode<T> n = visible.get(idx).node;
        n.setSelected(true);
        selectedNodes.add(n);
    }

    private void clearSelection() {
        clearSelectionRecursive(root);
        selectedNodes.clear();
    }

    private void clearSelectionRecursive(TreeNode<T> node) {
        node.setSelected(false);
        for (TreeNode<T> child : node.children()) {
            clearSelectionRecursive(child);
        }
    }

    private void renderNode(UiRenderer r, Theme theme, VisibleNode<T> vn, int x, int y, int width) {
        int indent = vn.depth * 20;
        int bgColor;
        int textColor;
        int mutedColor;
        if (theme != null) {
            bgColor = vn.node.selected()
                ? Theme.lerpArgb(theme.widgetHover, theme.widgetActive, 0.14f)
                : Theme.toArgb(theme.panelBg);
            textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);
            mutedColor = Theme.toArgb(theme.textMuted);
        } else {
            bgColor = vn.node.selected() ? 0xFF3A3A42 : 0xFF1E1E24;
            textColor = 0xFFE6E6F0;
            mutedColor = 0xFFFFFFFF;
        }
        r.drawRect(x, y, width, itemHeight, bgColor);
        float baselineY = r.baselineForBox(y, itemHeight);

        if (!vn.node.children().isEmpty()) {
            if (theme != null) {
                float iconSize = Math.min(16.0f, itemHeight - 6.0f);
                theme.icons.draw(r, vn.node.expanded() ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, x + indent + 2.0f, y + (itemHeight - iconSize) * 0.5f, iconSize, mutedColor);
            } else {
                String icon = vn.node.expanded() ? "v" : ">";
                r.drawText(icon, x + indent, baselineY, mutedColor);
            }
        }

        String label = labelFunc.apply(vn.node.data());
        r.drawText(label, x + indent + 20, baselineY, textColor);
    }

    private List<VisibleNode<T>> collectVisible(TreeNode<T> node, int depth) {
        List<VisibleNode<T>> result = new ArrayList<>();
        result.add(new VisibleNode<>(node, depth));
        if (node.expanded()) {
            for (TreeNode<T> child : node.children()) {
                result.addAll(collectVisible(child, depth + 1));
            }
        }
        return result;
    }

    public int computeContentHeight() {
        return root.visibleCount() * itemHeight;
    }

    public TreeNode<T> root() { return root; }
    public Set<TreeNode<T>> selectedNodes() { return selectedNodes; }

    private record VisibleNode<T>(TreeNode<T> node, int depth) {}
}
