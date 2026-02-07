package com.miry.ui.panels;

import com.miry.graphics.Texture;
import com.miry.ui.PanelContext;
import com.miry.ui.Ui;

/**
 * Demo inspector panel that showcases common widgets and texture preview.
 */
public final class InspectorPanel extends Panel {
    private boolean enabled = true;
    private float strength = 0.35f;
    private Texture preview;

    public InspectorPanel(String title) {
        super(title);
    }

    public void setPreview(Texture preview) {
        this.preview = preview;
    }

    @Override
    public void render(PanelContext ctx) {
        var r = ctx.renderer();
        var ui = ctx.ui();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        var theme = ui.theme();
        int ix = ctx.x() + theme.tokens.padding;
        int iy = ctx.y() + theme.tokens.padding;
        int iw = Math.max(1, ctx.width() - theme.tokens.padding * 2);
        int ih = Math.max(1, ctx.height() - theme.tokens.padding * 2);

        int estimatedContentHeight = 420;
        Ui.ScrollArea scroll = ui.beginScrollArea(r, "scroll", ix, iy, iw, ih, estimatedContentHeight);
        ui.label(r, "Widgets", true);
        ui.separator(r);

        if (ui.button(r, "Primary Button")) {
            enabled = !enabled;
        }
        enabled = ui.toggle(r, "Enabled", enabled);
        strength = ui.sliderFloat(r, "Strength", strength, 0.0f, 1.0f);

        ui.separator(r);
        ui.label(r, "Texture", true);
        if (preview != null) {
            ui.image(r, preview, 128, 128, 0xFFFFFFFF);
        } else {
            ui.label(r, "(no texture)", true);
        }

        ui.endScrollArea(scroll);
        ui.endPanel();
    }
}
