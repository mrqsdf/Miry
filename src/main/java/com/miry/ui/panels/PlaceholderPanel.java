package com.miry.ui.panels;

import com.miry.ui.PanelContext;
import com.miry.ui.theme.Theme;

public final class PlaceholderPanel extends Panel {
    private final String message;

    public PlaceholderPanel(String title, String message) {
        super(title);
        this.message = message == null ? "" : message;
    }

    @Override
    public void render(PanelContext ctx) {
        var r = ctx.renderer();
        var ui = ctx.ui();
        var theme = ui != null ? ui.theme() : new Theme();
        int x = ctx.x();
        int y = ctx.y();
        int w = ctx.width();
        int h = ctx.height();

        r.drawRect(x, y, w, h, Theme.toArgb(theme.panelBg));
        r.drawText(message, x + theme.design.space_md, r.baselineForBox(y + theme.design.space_md, 24), Theme.toArgb(theme.textMuted));
    }
}

