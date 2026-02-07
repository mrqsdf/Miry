package com.miry.ui.layout;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Docking layout leaf node that hosts a {@link Panel} with a header.
 */
public final class LeafNode extends DockNode {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int instanceId = NEXT_ID.getAndIncrement();
    private int backgroundArgb = 0xFF1C1C1E;
    private Panel panel;
    private int headerHeight = 26;
    private int headerArgb = 0xFF17171A;
    private int headerAccentArgb = 0xFF2A2A30;
    private final PanelContext panelContext = new PanelContext();

    public LeafNode(Panel panel) {
        this.panel = panel;
    }

    public void setBackgroundArgb(int argb) {
        this.backgroundArgb = argb;
    }

    public Panel panel() {
        return panel;
    }

    public void setPanel(Panel panel) {
        this.panel = panel;
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

    @Override
    public DockLayout snapshotLayout() {
        String title = panel == null ? "" : panel.title();
        return new DockLayout.Leaf(title);
    }

    @Override
    public void render(UiRenderer r) {
        render(r, null);
    }

    public void render(UiRenderer r, Ui ui) {
        if (width <= 0 || height <= 0) {
            return;
        }

        r.drawRect(x, y, width, height, backgroundArgb);

        int hh = Math.min(headerHeight, height);
        if (hh > 0) {
            r.drawRect(x, y, width, hh, headerArgb);
            r.drawRect(x, y + hh - 1, width, 1, headerAccentArgb);

            if (panel != null && !panel.title().isEmpty()) {
                float baselineY = r.baselineForBox(y, hh);
                r.drawText(panel.title(), x + 10, baselineY, 0xFFE6E6F0);
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
                panelContext.set(r, ui, cx, cy, cw, ch);
                panel.render(panelContext);
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

}
