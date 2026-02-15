package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple editor menu bar (File/Edit/Help...) that opens {@link ContextMenu} dropdowns.
 * <p>
 * Intended to mimic typical engine editors (e.g. Godot): click a menu title to open its dropdown,
 * then click other titles to switch menus.
 * </p>
 */
public final class MenuBar extends BaseWidget {
    public record Menu(String title, ContextMenu menu) {
        public Menu {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(menu, "menu");
        }
    }

    private final List<Menu> menus = new ArrayList<>();
    private int openIndex = -1;
    private int barX, barY, barW, barH;

    public MenuBar() {
        setFocusable(false);
    }

    public void clear() {
        menus.clear();
        closeAll();
    }

    public void addMenu(String title, ContextMenu menu) {
        menus.add(new Menu(title == null ? "" : title, Objects.requireNonNull(menu, "menu")));
    }

    public void closeAll() {
        openIndex = -1;
        for (Menu m : menus) {
            m.menu.close();
        }
    }

    public boolean hasOpenMenu() {
        return openIndex >= 0 && openIndex < menus.size() && menus.get(openIndex).menu.isOpen();
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int width,
                       int height,
                       boolean interactive) {
        if (r == null || theme == null) {
            return;
        }

        barX = x;
        barY = y;
        barW = width;
        barH = height;

        boolean canInteract = enabled() && interactive && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        int bg = Theme.toArgb(theme.headerBg);
        int outline = Theme.toArgb(theme.headerLine);
        int top = Theme.lightenArgb(bg, 0.05f);
        int bottom = Theme.darkenArgb(bg, 0.07f);
        r.drawLinearGradientRoundedRect(x, y, width, height, 0.0f, top, bottom, 0.0f, 1.0f, theme.design.border_thin, outline);

        int pad = theme.design.space_sm;
        int cursorX = x + pad;
        int cursorY = y;

        // Handle switching menus + rendering titles.
        for (int i = 0; i < menus.size(); i++) {
            Menu m = menus.get(i);
            String title = m.title();
            int itemW = Math.round(r.measureText(title)) + pad * 2;
            int itemX = cursorX;
            int itemY = cursorY + 1;
            int itemH = height - 2;

            boolean hovered = canInteract && mx >= itemX && my >= itemY && mx < itemX + itemW && my < itemY + itemH;
            boolean open = i == openIndex && m.menu.isOpen();
            if (hovered && canInteract && input.mousePressed()) {
                if (open) {
                    closeAll();
                } else {
                    openMenu(i, itemX, itemY + itemH);
                }
            } else if (hovered && canInteract && hasOpenMenu()) {
                // Hover-switch: when a menu is open, hovering other titles switches the open dropdown.
                if (openIndex != i) {
                    openMenu(i, itemX, itemY + itemH);
                }
            }

            int fill = 0;
            if (open) {
                fill = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.85f);
            } else if (hovered) {
                fill = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f);
            }
            if (fill != 0) {
                r.drawRoundedRect(itemX, itemY, itemW, itemH, theme.design.radius_sm, fill);
            }
            float baseline = r.baselineForBox(itemY, itemH);
            r.drawText(title, itemX + pad, baseline, Theme.toArgb(theme.text));

            cursorX += itemW;
        }

        // Close menus on outside click.
        if (canInteract && hasOpenMenu() && input.mousePressed()) {
            boolean insideBar = mx >= barX && my >= barY && mx < barX + barW && my < barY + barH;
            ContextMenu openMenu = menus.get(openIndex).menu;
            boolean insideMenu = mx >= openMenu.x()
                    && my >= openMenu.y()
                    && mx < openMenu.x() + Math.max(1, openMenu.lastWidth())
                    && my < openMenu.y() + Math.max(1, openMenu.lastHeight());
            if (!insideBar && !insideMenu) {
                closeAll();
            }
        }

        // Render dropdown (only one open at a time).
        if (openIndex >= 0 && openIndex < menus.size()) {
            ContextMenu menu = menus.get(openIndex).menu;
            menu.updateFromInput(input, theme, theme.tokens.itemHeight);
            menu.render(r, theme, theme.tokens.itemHeight, Theme.toArgb(theme.panelBg), Theme.toArgb(theme.widgetHover), Theme.toArgb(theme.text), menu.hoverIndex());
            if (canInteract && input != null && input.mousePressed()) {
                menu.handleClick((int) mx, (int) my, theme.tokens.itemHeight);
            }
        }
    }

    private void openMenu(int index, int x, int y) {
        closeAll();
        if (index < 0 || index >= menus.size()) {
            return;
        }
        openIndex = index;
        menus.get(index).menu.open(x, y);
    }
}
