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
    public static final class Style {
        public boolean drawContainer = true;
        public boolean drawFocusRing = true;
        public boolean stripedRows = true;

        public int containerBg = 0;
        public int containerOutline = 0;
        public int containerBorderThickness = -1;
        public float containerRadius = -1.0f;

        public int rowBgEven = 0;
        public int rowBgOdd = 0;
        public int rowBgHover = 0;
        public int rowBgSelected = 0;

        public int textColor = 0;
        public int mutedColor = 0;

        public int rowInsetPx = -1;
    }

    private final TreeNode<T> root;
    private final int itemHeight;
    private final Set<TreeNode<T>> selectedNodes = new HashSet<>();
    private Function<T, String> labelFunc = Object::toString;
    private Function<T, Icon> iconFunc = null;
    private int indentStepPx = 20;
    private boolean multiSelect = false;
    private boolean showVisibilityToggles = false;
    private int selectionAnchorIndex = -1;
    private int lastIndentStep = 20;
    private Style style;

    public TreeView(TreeNode<T> root, int itemHeight) {
        this.root = root;
        this.itemHeight = Math.max(1, itemHeight);
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Style style() {
        return style;
    }

    public void setLabelFunction(Function<T, String> func) {
        this.labelFunc = func != null ? func : Object::toString;
    }

    public void setIconFunction(Function<T, Icon> func) {
        this.iconFunc = func;
    }

    public void setIndentStepPx(int px) {
        this.indentStepPx = Math.max(0, px);
    }

    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        if (!multiSelect) {
            selectionAnchorIndex = -1;
        }
    }

    public void setShowVisibilityToggles(boolean show) {
        this.showVisibilityToggles = show;
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

        lastIndentStep = theme.design.space_lg + theme.design.space_xs;

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hovered = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        Style s = style;
        if (s == null) {
            int bg = Theme.toArgb(theme.panelBg);
            int outline = Theme.toArgb(theme.widgetOutline);
            if (theme.skins.panel != null) {
                theme.skins.panel.drawWithOutline(r, x, y, width, height, bg, outline, theme.design.border_thin);
            } else {
                int t = theme.design.border_thin;
                float radius = theme.design.radius_sm;
                int top = Theme.lightenArgb(bg, 0.02f);
                int bottom = Theme.darkenArgb(bg, 0.02f);
                r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
            }
            drawFocusRing(r, theme, x, y, width, height);
        } else {
            if (s.drawContainer) {
                int bg = s.containerBg != 0 ? s.containerBg : Theme.toArgb(theme.panelBg);
                int outline = s.containerOutline != 0 ? s.containerOutline : Theme.toArgb(theme.widgetOutline);
                int t = s.containerBorderThickness >= 0 ? s.containerBorderThickness : theme.design.border_thin;
                float radius = s.containerRadius >= 0.0f ? s.containerRadius : theme.design.radius_sm;
                if (radius > 0.0f || t > 0) {
                    r.drawRoundedRect(x, y, width, height, radius, bg, t, outline);
                } else {
                    r.drawRect(x, y, width, height, bg);
                }
            }
            if (s.drawFocusRing) {
                drawFocusRing(r, theme, x, y, width, height);
            }
        }

        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            handleClick(input, (int) mx, (int) my, x, y, width, height, scrollOffset);
        }

        int hoveredRowIdx = -1;
        if (hovered && canInteract) {
            hoveredRowIdx = (int) ((my - y + scrollOffset) / itemHeight);
        }
        render(r, theme, x, y, width, height, scrollOffset, hoveredRowIdx);
    }

    public void render(UiRenderer r, int x, int y, int width, int height, int scrollOffset) {
        render(r, null, x, y, width, height, scrollOffset, -1);
    }

    private void render(UiRenderer r, Theme theme, int x, int y, int width, int height, int scrollOffset, int hoveredRowIdx) {
        List<VisibleNode<T>> visible = collectVisible(root, 0);
        int startIdx = scrollOffset / itemHeight;
        int endIdx = Math.min(visible.size(), startIdx + (height / itemHeight) + 1);

        for (int i = startIdx; i < endIdx; i++) {
            VisibleNode<T> vn = visible.get(i);
            int itemY = y + (i * itemHeight) - scrollOffset;
            renderNode(r, theme, vn, x, itemY, width, i, i == hoveredRowIdx);
        }
    }

    public boolean handleClick(int mx, int my, int x, int y, int width, int height, int scrollOffset) {
        return handleClick(null, mx, my, x, y, width, height, scrollOffset);
    }

    public boolean handleClick(UiInput input, int mx, int my, int x, int y, int width, int height, int scrollOffset) {
        if (mx < x || my < y || mx >= x + width || my >= y + height) {
            return false;
        }

        List<VisibleNode<T>> visible = collectVisible(root, 0);
        int idx = (my - y + scrollOffset) / itemHeight;
        if (idx < 0 || idx >= visible.size()) {
            return false;
        }

        VisibleNode<T> vn = visible.get(idx);
        int indent = vn.depth * lastIndentStep;

        if (showVisibilityToggles) {
            int visToggleX = x + width - 20;
            if (mx >= visToggleX && mx < visToggleX + 16) {
                vn.node.toggleVisible();
                return true;
            }
        }

        boolean hasChildren = !vn.node.children().isEmpty();
        int iconX0 = x + indent;
        int iconX1 = iconX0 + lastIndentStep;
        if (hasChildren && mx >= iconX0 && mx < iconX1) {
            vn.node.toggleExpanded();
            return true;
        }

        boolean ctrl = input != null && input.ctrlDown();
        boolean shift = input != null && input.shiftDown();

        if (!multiSelect || (!ctrl && !shift)) {
            clearSelection();
            vn.node.setSelected(true);
            selectedNodes.add(vn.node);
            selectionAnchorIndex = idx;
            return true;
        }

        if (shift && selectionAnchorIndex >= 0) {
            int a = Math.max(0, Math.min(selectionAnchorIndex, visible.size() - 1));
            int b = idx;
            int lo = Math.min(a, b);
            int hi = Math.max(a, b);
            clearSelection();
            for (int i = lo; i <= hi; i++) {
                TreeNode<T> n = visible.get(i).node;
                n.setSelected(true);
                selectedNodes.add(n);
            }
            return true;
        }

        // Ctrl toggles selection.
        if (!vn.node.selected()) {
            vn.node.setSelected(true);
            selectedNodes.add(vn.node);
            selectionAnchorIndex = idx;
        } else {
            vn.node.setSelected(false);
            selectedNodes.remove(vn.node);
            if (selectionAnchorIndex == idx) {
                selectionAnchorIndex = -1;
            }
        }
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

    private void renderNode(UiRenderer r, Theme theme, VisibleNode<T> vn, int x, int y, int width, int rowIndex, boolean rowHovered) {
        int indentStep = theme != null ? theme.design.space_lg + theme.design.space_xs : indentStepPx;
        int indent = vn.depth * indentStep;
        Style s = style;
        int rowInset = 8;
        int bgColor;
        int textColor;
        int mutedColor;
        if (theme != null) {
            rowInset = (s != null && s.rowInsetPx >= 0) ? s.rowInsetPx : theme.design.space_sm;
            int baseBg = Theme.toArgb(theme.panelBg);
            int stripe = Theme.lerpArgb(theme.panelBg, theme.widgetBg, 0.35f);
            int hoverBg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f);
            int selBg = Theme.lerpArgb(theme.widgetHover, theme.widgetActive, 0.14f);

            if (s != null) {
                baseBg = (s.rowBgEven != 0) ? s.rowBgEven : baseBg;
                stripe = (s.rowBgOdd != 0) ? s.rowBgOdd : stripe;
                hoverBg = (s.rowBgHover != 0) ? s.rowBgHover : hoverBg;
                selBg = (s.rowBgSelected != 0) ? s.rowBgSelected : selBg;
            }

            if (vn.node.selected()) {
                bgColor = selBg;
            } else if (rowHovered) {
                bgColor = hoverBg;
            } else if (s != null && !s.stripedRows) {
                bgColor = baseBg;
            } else {
                bgColor = ((rowIndex & 1) == 0) ? baseBg : stripe;
            }

            textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);
            mutedColor = Theme.toArgb(theme.textMuted);
            if (s != null) {
                if (s.textColor != 0) {
                    textColor = s.textColor;
                }
                if (s.mutedColor != 0) {
                    mutedColor = s.mutedColor;
                }
            }
        } else {
            bgColor = vn.node.selected() ? 0xFF3A3A42 : 0xFF1E1E24;
            if (rowHovered && !vn.node.selected()) {
                bgColor = 0xFF2A2E36;
            }
            textColor = 0xFFE6E6F0;
            mutedColor = 0xFFFFFFFF;
        }
        r.drawRect(x, y, width, itemHeight, bgColor);
        float baselineY = r.baselineForBox(y, itemHeight);

        int cursorX = x + indent;

        if (!vn.node.children().isEmpty()) {
            if (theme != null) {
                float iconSize = Math.min(theme.design.icon_sm, itemHeight - theme.design.space_sm);
                float iconX = cursorX + theme.design.space_xs;
                theme.icons.draw(r, vn.node.expanded() ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, iconX, y + (itemHeight - iconSize) * 0.5f, iconSize, mutedColor);
            } else {
                String icon = vn.node.expanded() ? "v" : ">";
                r.drawText(icon, cursorX, baselineY, mutedColor);
            }
        }

        cursorX += indentStep;

        // Node icon (optional)
        Icon nodeIcon = vn.node.icon() != null ? vn.node.icon() : (iconFunc != null ? iconFunc.apply(vn.node.data()) : null);
        if (nodeIcon != null && theme != null && theme.icons != null) {
            float iconSize = Math.min(theme.design.icon_sm, itemHeight - theme.design.space_xs);
            theme.icons.draw(r, nodeIcon, cursorX, y + (itemHeight - iconSize) * 0.5f, iconSize, textColor);
            cursorX += (int) iconSize + rowInset;
        }

        String label = labelFunc.apply(vn.node.data());
        r.drawText(label, cursorX + rowInset, baselineY, textColor);

        // Visibility toggle (optional)
        if (showVisibilityToggles && theme != null && theme.icons != null) {
            int visToggleX = x + width - 20;
            int visToggleY = y + (itemHeight - 16) / 2;
            Icon eyeIcon = vn.node.visible() ? Icon.EYE : Icon.EYE_OFF;
            int eyeColor = vn.node.visible() ? textColor : mutedColor;
            theme.icons.draw(r, eyeIcon, visToggleX, visToggleY, 16, eyeColor);
        }
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
