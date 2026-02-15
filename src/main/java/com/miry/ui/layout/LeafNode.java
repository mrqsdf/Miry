package com.miry.ui.layout;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Docking layout leaf node that hosts a {@link Panel} with a header.
 */
public final class LeafNode extends DockNode {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    public enum HeaderButtons {
        NONE,
        CLOSE_ONLY,
        SPLIT_AND_CLOSE
    }

    private final int instanceId = NEXT_ID.getAndIncrement();
    private int backgroundArgb = 0xFF1C1C1E;
    private final ArrayList<Panel> tabs = new ArrayList<>();
    private int activeTabIndex;
    private int headerHeight = 26;
    private int headerArgb = 0xFF17171A;
    private int headerAccentArgb = 0xFF2A2A30;
    private HeaderButtons headerButtons = HeaderButtons.SPLIT_AND_CLOSE;
    private boolean headerButtonsOnlyOnHover;
    private final PanelContext panelContext = new PanelContext();

    public LeafNode(Panel panel) {
        if (panel != null) {
            tabs.add(panel);
        }
    }

    public void setBackgroundArgb(int argb) {
        this.backgroundArgb = argb;
    }

    public Panel panel() {
        if (tabs.isEmpty()) {
            return null;
        }
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            activeTabIndex = 0;
        }
        return tabs.get(activeTabIndex);
    }

    public void setPanel(Panel panel) {
        tabs.clear();
        if (panel != null) {
            tabs.add(panel);
        }
        activeTabIndex = 0;
    }

    public int tabCount() {
        return tabs.size();
    }

    public List<Panel> tabs() {
        return List.copyOf(tabs);
    }

    public int activeTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        if (tabs.isEmpty()) {
            this.activeTabIndex = 0;
            return;
        }
        this.activeTabIndex = Math.max(0, Math.min(activeTabIndex, tabs.size() - 1));
    }

    public void addTab(Panel panel) {
        Objects.requireNonNull(panel, "panel");
        tabs.add(panel);
        if (tabs.size() == 1) {
            activeTabIndex = 0;
        }
    }

    public void addTab(int index, Panel panel) {
        Objects.requireNonNull(panel, "panel");
        int i = Math.max(0, Math.min(index, tabs.size()));
        tabs.add(i, panel);
        if (tabs.size() == 1) {
            activeTabIndex = 0;
        } else if (activeTabIndex >= i) {
            activeTabIndex++;
        }
    }

    public void closeActiveTab() {
        closeTab(activeTabIndex);
    }

    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        tabs.remove(index);
        if (tabs.isEmpty()) {
            activeTabIndex = 0;
            return;
        }
        if (activeTabIndex >= tabs.size()) {
            activeTabIndex = tabs.size() - 1;
        }
    }

    public int headerHeight() {
        return headerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = Math.max(0, headerHeight);
    }

    public void setHeaderArgb(int headerArgb) {
        this.headerArgb = headerArgb;
    }

    public void setHeaderAccentArgb(int headerAccentArgb) {
        this.headerAccentArgb = headerAccentArgb;
    }

    public HeaderButtons headerButtons() {
        return headerButtons;
    }

    public void setHeaderButtons(HeaderButtons headerButtons) {
        this.headerButtons = headerButtons == null ? HeaderButtons.SPLIT_AND_CLOSE : headerButtons;
    }

    public boolean headerButtonsOnlyOnHover() {
        return headerButtonsOnlyOnHover;
    }

    public void setHeaderButtonsOnlyOnHover(boolean headerButtonsOnlyOnHover) {
        this.headerButtonsOnlyOnHover = headerButtonsOnlyOnHover;
    }

    @Override
    public DockLayout snapshotLayout() {
        if (tabs.isEmpty()) {
            return new DockLayout.Leaf("");
        }
        if (tabs.size() == 1) {
            String title = panel() == null ? "" : panel().title();
            return new DockLayout.Leaf(title);
        }
        ArrayList<String> titles = new ArrayList<>(tabs.size());
        for (Panel p : tabs) {
            titles.add(p == null ? "" : p.title());
        }
        return new DockLayout.Tabs(List.copyOf(titles), activeTabIndex());
    }

    @Override
    public void render(UiRenderer r) {
        render(r, null);
    }

    public void render(UiRenderer r, Ui ui) {
        render(r, ui, null);
    }

    public void render(UiRenderer r, Ui ui, UiContext uiContext) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Theme theme = ui != null ? ui.theme() : null;
        Panel panel = panel();

        int bg = theme != null ? Theme.toArgb(theme.panelBg) : backgroundArgb;
        int outline = theme != null ? Theme.toArgb(theme.widgetOutline) : headerAccentArgb;
        int border = theme != null ? theme.design.border_thin : 1;
        float radius = 0.0f;

        if (theme != null) {
            int shadow = Theme.toArgb(theme.shadow);
            drawDropShadow(r, x, y, width, height, shadow, 0.0f, theme.design.space_xs, theme.design.shadow_sm, radius);
        }

        r.drawRoundedRect(x, y, width, height, radius, bg, border, outline);

        int hh = Math.min(headerHeight, height);
        if (hh > 0) {
            int headerBase = theme != null ? Theme.toArgb(theme.headerBg) : headerArgb;
            int top = Theme.lightenArgb(headerBase, 0.06f);
            int bottom = Theme.darkenArgb(headerBase, 0.06f);
            r.drawLinearGradientRoundedRect(x, y, width, hh, radius, top, bottom, 0.0f, 1.0f, border, outline);
            r.drawRect(x, y + hh - 1, width, 1, outline);

            boolean headerHovered = false;
            if (ui != null) {
                float mx = ui.mouse().x;
                float my = ui.mouse().y;
                headerHovered = (mx >= x && mx < x + width && my >= y && my < y + hh);
            }

            boolean showButtons = headerButtons != HeaderButtons.NONE && (!headerButtonsOnlyOnHover || headerHovered);
            boolean reserveButtons = headerButtons != HeaderButtons.NONE;
            int reservedRight = 0;
            if (reserveButtons) {
                int btn = Math.max(12, hh - 10);
                int pad = 6;
                int count = headerButtons == HeaderButtons.CLOSE_ONLY ? 1 : 3;
                reservedRight = pad + count * btn + (count - 1) * 4;
            }

            if (tabs.size() > 1) {
                int tabInset = theme != null ? theme.design.space_sm : 8;
                int tabPadX = theme != null ? theme.design.space_sm : 8;
                int tabGap = theme != null ? theme.design.space_xs : 2;
                int tabH = Math.max(14, hh - 6);
                int tabY = y + (hh - tabH) / 2;
                int tabX = x + tabInset;
                int tabRightLimit = x + width - tabInset - reservedRight;

                float mx = ui != null ? ui.mouse().x : -1.0f;
                float my = ui != null ? ui.mouse().y : -1.0f;
                boolean pressed = ui != null && ui.input().mousePressed();

                int activeFill = theme != null ? Theme.mulAlpha(Theme.toArgb(theme.widgetBg), 0.85f) : headerAccentArgb;
                int hoverFill = theme != null ? Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f) : 0x332A2A30;
                int activeText = theme != null ? Theme.toArgb(theme.text) : 0xFFE6E6F0;
                int idleText = theme != null ? Theme.toArgb(theme.textMuted) : 0xFFB8B8C6;

                for (int i = 0; i < tabs.size(); i++) {
                    Panel p = tabs.get(i);
                    String title = p == null ? "" : p.title();
                    if (title.isEmpty()) {
                        title = "Tab " + (i + 1);
                    }
                    int tabW = Math.round(r.measureText(title)) + tabPadX * 2;
                    if (tabX + tabW > tabRightLimit) {
                        if (tabX + tabPadX * 2 + 10 <= tabRightLimit) {
                            int ellW = Math.round(r.measureText("…")) + tabPadX * 2;
                            if (tabX + ellW <= tabRightLimit) {
                                r.drawRoundedRect(tabX, tabY, ellW, tabH, theme != null ? theme.design.radius_sm : 3.0f, hoverFill);
                                r.drawText("…", tabX + tabPadX, r.baselineForBox(tabY, tabH), idleText);
                            }
                        }
                        break;
                    }

                    boolean hovered = mx >= tabX && mx < tabX + tabW && my >= tabY && my < tabY + tabH;
                    if (pressed && hovered) {
                        setActiveTabIndex(i);
                    }

                    boolean active = i == activeTabIndex;
                    int fill = active ? activeFill : (hovered ? hoverFill : 0);
                    if (fill != 0) {
                        float tabRadius = theme != null ? theme.design.radius_sm : 3.0f;
                        r.drawRoundedRect(tabX, tabY, tabW, tabH, tabRadius, fill);
                    }
                    int color = active ? activeText : idleText;
                    r.drawText(title, tabX + tabPadX, r.baselineForBox(tabY, tabH), color);
                    tabX += tabW + tabGap;
                }
            } else if (panel != null && !panel.title().isEmpty()) {
                float baselineY = r.baselineForBox(y, hh);
                int text = theme != null ? Theme.toArgb(theme.text) : 0xFFE6E6F0;
                r.drawText(panel.title(), x + 10, baselineY, text);
            }

            if (showButtons) {
                int btn = Math.max(12, hh - 10);
                int pad = 6;
                int by = y + (hh - btn) / 2;
                int bx = x + width - pad - btn;
                int btnBg = theme != null ? Theme.mulAlpha(Theme.toArgb(theme.widgetBg), 0.55f) : 0x332A2A30;
                int btnText = theme != null ? Theme.mulAlpha(Theme.toArgb(theme.textMuted), 0.95f) : 0xFFD0D0DA;
                int btnTextIdle = theme != null ? Theme.mulAlpha(Theme.toArgb(theme.textMuted), 0.70f) : 0xB0D0D0DA;
                float btnRadius = theme != null ? theme.design.radius_sm : 3.0f;

                // Right-to-left: [X][V][H] (when enabled)
                float mx = ui != null ? ui.mouse().x : -1.0f;
                float my = ui != null ? ui.mouse().y : -1.0f;
                boolean xHover = (mx >= bx && mx < bx + btn && my >= by && my < by + btn);
                if (xHover) {
                    r.drawRoundedRect(bx, by, btn, btn, btnRadius, btnBg, border, outline);
                }
                r.drawText("X", bx + (btn - r.measureText("X")) * 0.5f, r.baselineForBox(by, btn), xHover ? btnText : btnTextIdle);

                if (headerButtons == HeaderButtons.SPLIT_AND_CLOSE) {
                    bx -= (btn + 4);
                    boolean vHover = (mx >= bx && mx < bx + btn && my >= by && my < by + btn);
                    if (vHover) {
                        r.drawRoundedRect(bx, by, btn, btn, btnRadius, btnBg, border, outline);
                    }
                    r.drawText("V", bx + (btn - r.measureText("V")) * 0.5f, r.baselineForBox(by, btn), vHover ? btnText : btnTextIdle);
                    bx -= (btn + 4);
                    boolean hHover = (mx >= bx && mx < bx + btn && my >= by && my < by + btn);
                    if (hHover) {
                        r.drawRoundedRect(bx, by, btn, btn, btnRadius, btnBg, border, outline);
                    }
                    r.drawText("H", bx + (btn - r.measureText("H")) * 0.5f, r.baselineForBox(by, btn), hHover ? btnText : btnTextIdle);
                }
            }
        }

        int cx = x;
        int cy = y + hh;
        int cw = width;
        int ch = height - hh;
        if (ch > 0 && panel != null) {
            r.pushClip(cx, cy, cw, ch);
            if (ui != null) {
                ui.pushId(instanceId);
                ui.pushId(activeTabIndex);
                panelContext.set(r, ui, uiContext, cx, cy, cw, ch);
                panel.render(panelContext);
                ui.popId();
                ui.popId();
            }
            r.popClip();
        }
    }

    @Override
    public void resize(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    private static void drawDropShadow(UiRenderer r,
                                       float x,
                                       float y,
                                       float w,
                                       float h,
                                       int shadowArgb,
                                       float dx,
                                       float dy,
                                       int blurPx,
                                       float radiusPx) {
        if (r == null) {
            return;
        }
        int blur = Math.max(0, blurPx);
        if (blur == 0) {
            return;
        }

        int a0 = (shadowArgb >>> 24) & 0xFF;
        int rgb = shadowArgb & 0x00FFFFFF;
        int steps = Math.min(12, Math.max(4, blur / 2));
        for (int i = 0; i < steps; i++) {
            float t = (i + 1) / (float) steps;
            float spread = blur * t;
            int a = Math.round(a0 * (1.0f - t) * 0.65f);
            int c = (a << 24) | rgb;
            r.drawRoundedRect(x + dx - spread, y + dy - spread, w + spread * 2.0f, h + spread * 2.0f, radiusPx + spread, c);
        }
    }

}
