package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical tab strip with icons (Blender-like properties tabs).
 */
public final class VerticalIconTabs extends BaseWidget {
    public record Tab(Icon icon, String tooltip) {}

    private final List<Tab> tabs = new ArrayList<>();
    private int selected;

    public void addTab(Icon icon, String tooltip) {
        tabs.add(new Tab(icon, tooltip));
        selected = Math.min(selected, Math.max(0, tabs.size() - 1));
    }

    public int selected() {
        return selected;
    }

    public void setSelected(int index) {
        selected = Math.max(0, Math.min(index, tabs.size() - 1));
    }

    public int render(UiRenderer r,
                      UiContext ctx,
                      UiInput input,
                      Theme theme,
                      int x,
                      int y,
                      int w,
                      int h,
                      int itemSize,
                      boolean interactive) {
        if (r == null || theme == null) return selected;
        registerFocusable(ctx);

        int count = tabs.size();
        if (count == 0) {
            return selected;
        }

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        int bg = Theme.toArgb(theme.panelBg);
        r.drawRect(x, y, w, h, bg);

        int size = Math.max(18, itemSize);
        int pad = Math.max(4, (w - size) / 2);
        int startY = y + theme.design.space_sm;

        for (int i = 0; i < count; i++) {
            int iy = startY + i * (size + 4);
            if (iy + size > y + h - theme.design.space_sm) break;

            boolean hovered = canInteract && mx >= x && my >= iy && mx < x + w && my < iy + size;
            boolean active = i == selected;
            int itemBg = active ? Theme.toArgb(theme.widgetActive) : (hovered ? Theme.toArgb(theme.widgetHover) : 0);
            if (itemBg != 0) {
                r.drawRoundedRect(x + 3, iy, w - 6, size, theme.design.radius_sm, Theme.mulAlpha(itemBg, active ? 0.35f : 0.25f));
            }

            Icon icon = tabs.get(i).icon();
            int iconColor = active ? Theme.toArgb(theme.text) : Theme.toArgb(theme.textMuted);
            float is = Math.min(theme.design.icon_sm, size - 6);
            theme.icons.draw(r, icon, x + pad + (size - is) * 0.5f, iy + (size - is) * 0.5f, is, iconColor);

            if (hovered && canInteract && input.mousePressed()) {
                focus(ctx);
                selected = i;
            }
        }

        drawFocusRing(r, theme, x, y, w, h);
        return selected;
    }
}

