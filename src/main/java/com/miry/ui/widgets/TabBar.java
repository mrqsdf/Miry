package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Document tab bar with close buttons, reordering, and unsaved indicators.
 */
public final class TabBar extends BaseWidget {
    public static final class Tab {
        public String title;
        public boolean unsaved;
        public boolean closable = true;
        public Object userData;

        public Tab(String title) {
            this.title = title != null ? title : "Untitled";
        }
    }

    private final List<Tab> tabs = new ArrayList<>();
    private int activeIndex = -1;
    private int hoverIndex = -1;
    private int hoverCloseIndex = -1;
    private int dragIndex = -1;
    private float dragOffsetX = 0;
    private Runnable onTabChanged;
    private TabCloseListener onTabClose;
    private float[] hoverT = new float[0];
    private float[] closeHoverT = new float[0];

    @FunctionalInterface
    public interface TabCloseListener {
        void onClose(int index, Tab tab);
    }

    public void addTab(Tab tab) {
        tabs.add(tab);
        if (activeIndex < 0) activeIndex = 0;
    }

    public void removeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.remove(index);
            if (activeIndex >= tabs.size()) activeIndex = tabs.size() - 1;
        }
    }

    public void setActiveIndex(int index) {
        if (index >= -1 && index < tabs.size()) {
            activeIndex = index;
            if (onTabChanged != null) onTabChanged.run();
        }
    }

    public int activeIndex() { return activeIndex; }
    public Tab activeTab() { return activeIndex >= 0 && activeIndex < tabs.size() ? tabs.get(activeIndex) : null; }
    public List<Tab> tabs() { return tabs; }
    public void setOnTabChanged(Runnable callback) { this.onTabChanged = callback; }
    public void setOnTabClose(TabCloseListener listener) { this.onTabClose = listener; }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        render(r, null, input, theme, x, y, width, height, true);
    }

    public void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        if (tabs.isEmpty()) return;

        int pad = theme.design.space_sm;
        int minTabWidth = 80;
        int maxTabWidth = 200;
        int closeSize = 16;

        int bg = Theme.toArgb(theme.headerBg);
        r.drawRect(x, y, width, height, bg);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;
        boolean mousePressed = canInteract && input.mousePressed();
        boolean mouseDown = canInteract && input.mouseDown();
        boolean mouseReleased = canInteract && input.mouseReleased();

        float dt = ctx != null ? ctx.lastDt() : 0.0f;
        float speed = theme.design.animSpeed_fast;

        if (hoverT.length != tabs.size()) hoverT = new float[tabs.size()];
        if (closeHoverT.length != tabs.size()) closeHoverT = new float[tabs.size()];

        hoverIndex = -1;
        hoverCloseIndex = -1;

        int tabWidth = Math.min(maxTabWidth, Math.max(minTabWidth, (width - pad) / tabs.size()));
        int cursorX = x + pad;

        // Drag reordering
        if (canInteract && dragIndex >= 0 && mouseDown) {
            int newIndex = (int) ((mx - x - pad) / tabWidth);
            newIndex = Math.max(0, Math.min(tabs.size() - 1, newIndex));
            if (newIndex != dragIndex) {
                moveTab(dragIndex, newIndex);
                dragIndex = newIndex;
            }
        }

        for (int i = 0; i < tabs.size(); i++) {
            if (i == dragIndex && mouseDown) continue;

            Tab tab = tabs.get(i);
            int tabX = cursorX;
            int tabY = y;
            int tabW = tabWidth - pad;
            int tabH = height;

            boolean active = i == activeIndex;
            boolean hovered = canInteract && mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + tabH;
            if (hovered) hoverIndex = i;
            hoverT[i] = approachExp(hoverT[i], hovered ? 1.0f : 0.0f, speed, dt);

            int baseBg = Theme.darkenArgb(bg, 0.05f);
            int hoverBg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.6f);
            int tabBg = active ? Theme.toArgb(theme.widgetBg) : Theme.lerpArgbInt(baseBg, hoverBg, hoverT[i]);
            r.drawRoundedRect(tabX, tabY, tabW, tabH, theme.design.radius_sm, tabBg);

            if (active) {
                r.drawRect(tabX, tabY + tabH - 2, tabW, 2, Theme.toArgb(theme.accent));
            }

            // Close button
            int closeX = tabX + tabW - closeSize - pad * 2;
            int closeY = tabY + (tabH - closeSize) / 2;
            boolean closeHovered = tab.closable && hovered && mx >= closeX && mx < closeX + closeSize && my >= closeY && my < closeY + closeSize;
            if (closeHovered) hoverCloseIndex = i;
            closeHoverT[i] = approachExp(closeHoverT[i], closeHovered ? 1.0f : 0.0f, speed, dt);

            if (tab.closable) {
                int closeBg = Theme.mulAlpha(Theme.toArgb(theme.danger), 0.85f * closeHoverT[i]);
                if (closeBg != 0) {
                    r.drawRoundedRect(closeX - 2, closeY - 2, closeSize + 4, closeSize + 4, 2, closeBg);
                }
                int closeColor = Theme.lerpArgbInt(Theme.toArgb(theme.textMuted), 0xFFFFFFFF, closeHoverT[i]);
                int cx = closeX + closeSize / 2;
                int cy = closeY + closeSize / 2;
                int s = closeSize / 3;
                r.drawLine(cx - s, cy - s, cx + s, cy + s, 2, closeColor);
                r.drawLine(cx + s, cy - s, cx - s, cy + s, 2, closeColor);
            }

            // Title
            String title = tab.unsaved ? "• " + tab.title : tab.title;
            int textX = tabX + pad;
            int textW = closeX - textX - pad;
            String clipped = r.clipText(title, textW);
            float baseline = r.baselineForBox(tabY, tabH);
            int textColor = active ? Theme.toArgb(theme.text) : Theme.toArgb(theme.textMuted);
            r.drawText(clipped, textX, baseline, textColor);

            cursorX += tabWidth;
        }

        // Draw dragged tab on top
        if (canInteract && dragIndex >= 0 && mouseDown && dragIndex < tabs.size()) {
            Tab tab = tabs.get(dragIndex);
            int tabY = y;
            int tabW = tabWidth - pad;
            int tabH = height;
            int tabX = Math.max(x + pad, Math.min(x + width - tabW - pad, (int) (mx - dragOffsetX)));

            int tabBg = Theme.mulAlpha(Theme.toArgb(theme.widgetBg), 0.95f);
            r.drawRoundedRect(tabX, tabY, tabW, tabH, theme.design.radius_sm, tabBg);
            r.drawRect(tabX, tabY + tabH - 2, tabW, 2, Theme.toArgb(theme.accent));

            int closeX = tabX + tabW - closeSize - pad * 2;
            int textX = tabX + pad;
            int textW = closeX - textX - pad;
            String title = tab.unsaved ? "• " + tab.title : tab.title;
            String clipped = r.clipText(title, textW);
            float baseline = r.baselineForBox(tabY, tabH);
            r.drawText(clipped, textX, baseline, Theme.toArgb(theme.text));
        }

        // Handle clicks
        if (mousePressed && hoverIndex >= 0) {
            if (hoverCloseIndex >= 0) {
                if (onTabClose != null) {
                    onTabClose.onClose(hoverCloseIndex, tabs.get(hoverCloseIndex));
                } else {
                    removeTab(hoverCloseIndex);
                }
            } else {
                setActiveIndex(hoverIndex);
                dragIndex = hoverIndex;
                int tabX = x + pad + hoverIndex * tabWidth;
                dragOffsetX = mx - tabX;
                if (ctx != null) {
                    ctx.pointer().capture(id());
                }
            }
        }

        if (dragIndex >= 0 && !mouseDown) {
            dragIndex = -1;
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        if (mouseReleased && ctx != null && ctx.pointer().isCaptured(id())) {
            ctx.pointer().release();
        }
    }

    private void moveTab(int from, int to) {
        if (from == to) return;
        if (from < 0 || from >= tabs.size()) return;
        if (to < 0 || to >= tabs.size()) return;
        Tab t = tabs.remove(from);
        tabs.add(to, t);
        if (activeIndex == from) {
            activeIndex = to;
        } else if (activeIndex >= 0) {
            if (from < activeIndex && to >= activeIndex) activeIndex -= 1;
            else if (from > activeIndex && to <= activeIndex) activeIndex += 1;
        }
        if (onTabChanged != null) onTabChanged.run();
    }
}
