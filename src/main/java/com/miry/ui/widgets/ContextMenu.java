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

        int width = 200;
        int height = items.size() * itemHeight;

        int outline = theme != null ? Theme.toArgb(theme.widgetOutline) : 0xFF3A3A42;
        if (theme != null && theme.skins.popup != null) {
            theme.skins.popup.drawWithOutline(r, x, y, width, height, bgColor, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bgColor);
            r.drawRect(x, y, width, 1, outline);
        }

        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            int itemY = y + i * itemHeight;

            if (item.separator) {
                r.drawRect(x + 10, itemY + itemHeight / 2, width - 20, 1, outline);
            } else {
                if (i == hoverIndex) {
                    r.drawRect(x, itemY, width, itemHeight, hoverColor);
                }
                float baselineY = r.baselineForBox(itemY, itemHeight);
                r.drawText(item.label, x + 10, baselineY, textColor);
                if (item.submenu != null) {
                    if (theme != null) {
                        float iconSize = Math.min(14.0f, itemHeight - 8.0f);
                        theme.icons.draw(r, Icon.CHEVRON_RIGHT, x + width - iconSize - 6.0f, itemY + (itemHeight - iconSize) * 0.5f, iconSize, textColor);
                    } else {
                        r.drawText(">", x + width - 20, baselineY, textColor);
                    }
                }
            }
        }
    }

    public boolean handleClick(int mx, int my, int itemHeight) {
        if (!enabled() || !open) return false;
        int width = 200;
        int height = items.size() * itemHeight;

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

    public record MenuItem(String label, Runnable action, boolean separator, ContextMenu submenu) {}
}
