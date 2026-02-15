package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click style context menu widget.
 */
public final class ContextMenu extends BaseWidget {
    private final List<MenuItem> items = new ArrayList<>();
    private boolean open;
    private int x, y;
    private int lastWidth;
    private int lastHeight;
    private int hoverIdx = -1;

    public ContextMenu() {
        setFocusable(false);
    }

    public void addItem(String label, Runnable action) {
        items.add(new MenuItem(label, action, false, null));
    }

    public void addSubmenu(String label, ContextMenu submenu) {
        items.add(new MenuItem(label, null, false, submenu));
    }

    public void addSeparator() {
        items.add(new MenuItem(null, null, true, null));
    }

    public void clear() {
        items.clear();
    }

    public void open(int x, int y) {
        if (!enabled()) {
            this.open = false;
            return;
        }
        this.x = x;
        this.y = y;
        this.open = true;
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void render(UiRenderer r, int itemHeight, int bgColor, int hoverColor, int textColor, int hoverIndex) {
        render(r, null, itemHeight, bgColor, hoverColor, textColor, hoverIndex);
    }

    public void render(UiRenderer r, Theme theme, int itemHeight, int bgColor, int hoverColor, int textColor, int hoverIndex) {
        if (!open) return;

        int padX = theme != null ? theme.design.space_md : 10;
        int iconSlot = theme != null ? (theme.design.space_lg + theme.design.space_xs) : 20;

        int minWidth = theme != null ? (theme.design.space_2xl * 6 + theme.design.space_sm) : 200;
        float maxTextW = 0.0f;
        for (MenuItem item : items) {
            if (item != null && !item.separator && item.label != null) {
                maxTextW = Math.max(maxTextW, r.measureText(item.label));
            }
        }
        int width = Math.max(minWidth, Math.round(maxTextW + padX * 2.0f + iconSlot));
        int height = items.size() * itemHeight;
        lastWidth = width;
        lastHeight = height;

        int outline = theme != null ? Theme.toArgb(theme.widgetOutline) : 0xFF3A3A42;
        if (theme != null) {
            int shadow = Theme.toArgb(theme.shadow);
            drawDropShadow(r, x, y, width, height, shadow, 0.0f, theme.design.space_xs, theme.design.shadow_md, theme.design.radius_sm, 1.0f);
        }
        if (theme != null && theme.skins.popup != null) {
            theme.skins.popup.drawWithOutline(r, x, y, width, height, bgColor, outline, theme.design.border_thin);
        } else {
            int t = theme != null ? theme.design.border_thin : 1;
            float radius = theme != null ? theme.design.radius_sm : 3.0f;
            int top = Theme.lightenArgb(bgColor, 0.02f);
            int bottom = Theme.darkenArgb(bgColor, 0.02f);
            r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
        }

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            int itemY = y + i * itemHeight;

            if (item.separator) {
                int sepInset = theme != null ? theme.design.space_md : 10;
                r.drawRect(x + sepInset, itemY + itemHeight / 2, width - sepInset * 2, 1, outline);
            } else {
                if (i == hoverIndex) {
                    r.drawRect(x, itemY, width, itemHeight, hoverColor);
                }
                float baselineY = r.baselineForBox(itemY, itemHeight);
                r.drawText(item.label, x + padX, baselineY, textColor);
                if (item.submenu != null) {
                    if (theme != null) {
                        float iconSize = Math.min(theme.design.icon_sm, itemHeight - theme.design.space_sm);
                        theme.icons.draw(r, Icon.CHEVRON_RIGHT, x + width - iconSize - theme.design.space_sm, itemY + (itemHeight - iconSize) * 0.5f, iconSize, textColor);
                    } else {
                        r.drawText(">", x + width - 20, baselineY, textColor);
                    }
                }
            }
        }
    }

    public boolean handleClick(int mx, int my, int itemHeight) {
        if (!enabled() || !open) return false;
        int width = lastWidth > 0 ? lastWidth : 200;
        int height = lastHeight > 0 ? lastHeight : items.size() * itemHeight;

        if (mx < x || my < y || mx >= x + width || my >= y + height) {
            close();
            return false;
        }

        int idx = (my - y) / itemHeight;
        if (idx >= 0 && idx < items.size()) {
            MenuItem item = items.get(idx);
            if (!item.separator && item.action != null) {
                item.action.run();
                close();
                return true;
            }
        }
        return true;
    }

    public List<MenuItem> items() {
        return items;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int lastWidth() { return lastWidth; }
    public int lastHeight() { return lastHeight; }

    public void updateFromInput(com.miry.ui.input.UiInput input, Theme theme, int itemHeight) {
        if (!open || input == null) {
            hoverIdx = -1;
            return;
        }
        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        int width = lastWidth > 0 ? lastWidth : 200;
        int height = lastHeight > 0 ? lastHeight : items.size() * itemHeight;

        if (mx >= x && my >= y && mx < x + width && my < y + height) {
            hoverIdx = (int)((my - y) / itemHeight);
            if (hoverIdx < 0 || hoverIdx >= items.size()) hoverIdx = -1;
        } else {
            hoverIdx = -1;
        }
    }

    public int hoverIndex() {
        return hoverIdx;
    }

    public record MenuItem(String label, Runnable action, boolean separator, ContextMenu submenu) {}
}
