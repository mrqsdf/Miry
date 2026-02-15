package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.panels.Panel;
import com.miry.ui.widgets.Viewport3D;

/**
 * Panel that displays the 3D viewport.
 */
public final class ViewportPanel extends Panel {
    private Viewport3D viewport;
    int vx, vy, vw, vh;

    public ViewportPanel() {
        super("Viewport");
    }

    public void setViewport(Viewport3D viewport) {
        this.viewport = viewport;
    }

    @Override
    public void render(PanelContext ctx) {
        vx = ctx.x();
        vy = ctx.y();
        vw = ctx.width();
        vh = ctx.height();

        var r = ctx.renderer();
        r.drawRect(vx, vy, vw, vh, 0xFF121216);
        if (viewport != null) {
            r.drawTexturedRect(viewport.texture(), vx, vy, vw, vh, 0.0f, 1.0f, 1.0f, 0.0f, 0xFFFFFFFF);
        }
    }
}
