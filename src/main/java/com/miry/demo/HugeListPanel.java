package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.panels.Panel;
import com.miry.ui.theme.Theme;

public final class HugeListPanel extends Panel {
    private static final int COUNT = 50_000;

    public HugeListPanel() {
        super("Huge List");
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        var r = ctx.renderer();
        Theme t = ui.theme();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        ui.label(r, "Virtualized list (50k rows)", true);
        ui.separator(r);

        int rowH = ui.theme().tokens.itemHeight + ui.theme().tokens.itemSpacing;
        int contentH = COUNT * rowH;
        int x = ctx.x() + t.design.space_md;
        int y = ctx.y() + 64;
        int w = ctx.width() - t.design.space_md * 2;
        int h = ctx.height() - 74;

        Ui.ScrollArea scroll = ui.beginScrollArea(r, "huge", x, y, w, h, contentH);
        int viewH = Math.max(1, scroll.height());
        int start = Math.max(0, (int) (scroll.scrollY() / Math.max(1.0f, rowH)) - 2);
        int end = Math.min(COUNT, start + (viewH / Math.max(1, rowH)) + 5);
        ui.spacer(start * rowH);
        for (int i = start; i < end; i++) {
            ui.button(r, "Item " + i);
        }
        ui.spacer((COUNT - end) * rowH);
        ui.endScrollArea(scroll);
        ui.endPanel();
    }
}
