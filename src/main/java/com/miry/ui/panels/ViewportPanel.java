package com.miry.ui.panels;

import com.miry.ui.PanelContext;

/**
 * Minimal demo viewport panel (draws a marker at the mouse position).
 */
public final class ViewportPanel extends Panel {
    public ViewportPanel(String title) {
        super(title);
    }

    @Override
    public void render(PanelContext ctx) {
        var r = ctx.renderer();
        var mouse = ctx.ui().mouse();
        r.drawRect(mouse.x - 4, mouse.y - 4, 8, 8, 0xFFFFAA00);
    }
}
