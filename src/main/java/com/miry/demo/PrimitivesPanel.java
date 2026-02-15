package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.layout.constraints.ConstraintItem;
import com.miry.ui.layout.constraints.ConstraintSolver;
import com.miry.ui.panels.Panel;
import com.miry.ui.theme.Theme;
import com.miry.ui.vector.VectorIcons;


public final class PrimitivesPanel extends Panel {
    private float capsuleAnimT;
    private float dt;

    public PrimitivesPanel() {
        super("Primitives");
    }

    public void update(float dt) {
        this.dt = dt;
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        var r = ctx.renderer();
        Theme t = ui.theme();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        ui.label(r, "SDF renderer: rounded rects, gradients, circles, capsules, strokes", true);
        ui.separator(r);

        int x = ctx.x() + t.design.space_md;
        int y = ctx.y() + 64;
        int w = Math.min(520, ctx.width() - t.design.space_md * 2);

        int rowH = 34;
        int bgTop = Theme.lightenArgb(Theme.toArgb(t.widgetBg), 0.09f);
        int bgBot = Theme.darkenArgb(Theme.toArgb(t.widgetBg), 0.09f);
        int outline = Theme.toArgb(t.widgetOutline);
        r.drawLinearGradientRoundedRect(x, y, w, rowH, t.design.radius_sm, bgTop, bgBot, 0.0f, 1.0f, t.design.border_thin, outline);
        r.drawText("Linear gradient + stroke", x + 12, r.baselineForBox(y, rowH), Theme.toArgb(t.text));
        y += rowH + 10;

        int inner = Theme.toArgb(t.widgetActive);
        int outer = Theme.toArgb(t.panelBg);
        r.drawRadialGradientRoundedRect(x, y, w, rowH, t.design.radius_sm, inner, outer, 0.25f, 0.5f, 0.85f, t.design.border_thin, outline);
        r.drawText("Radial gradient + stroke", x + 12, r.baselineForBox(y, rowH), Theme.toArgb(t.text));
        y += rowH + 12;

        int circleR = 10;
        int cy = y + circleR;
        r.drawCircle(x + circleR, cy, circleR, Theme.toArgb(t.widgetActive), 2.0f, Theme.toArgb(t.widgetOutline));
        r.drawCircle(x + circleR + 34, cy, circleR, Theme.toArgb(t.palette.green500), 2.0f, Theme.toArgb(t.widgetOutline));
        r.drawCircle(x + circleR + 68, cy, circleR, Theme.toArgb(t.palette.red500), 2.0f, Theme.toArgb(t.widgetOutline));
        r.drawText("Circles (fill + stroke)", x + 120, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 34;
        capsuleAnimT += (dt * 0.9f);
        float a = (float) (Math.sin(capsuleAnimT) * 0.5 + 0.5);
        float ax = x + 20;
        float ay = y + 24;
        float bx = x + w - 20;
        float by = y + 24 + (a - 0.5f) * 28.0f;
        r.drawCapsule(ax, ay, bx, by, 6.0f, Theme.toArgb(t.widgetActive), 2.0f, Theme.toArgb(t.widgetOutline));
        r.drawText("Capsule stroke (vector paths / wires)", x + 12, r.baselineForBox(y + 36, 24), Theme.toArgb(t.textMuted));
        y += 72;

        int iconY = y;
        int[] sizes = {12, 16, 20, 28, 36};
        int ix = x;
        for (int s : sizes) {
            t.icons.draw(r, com.miry.ui.theme.Icon.CLOSE, ix, iconY, s, Theme.toArgb(t.text));
            ix += s + 10;
        }
        y = iconY + 54;

        int fill = Theme.toArgb(t.text);
        int stroke = Theme.mulAlpha(Theme.toArgb(t.textMuted), 0.8f);
        VectorIcons.PLAY_TRIANGLE.draw(r, x, y, 40, 2.0f, Theme.mulAlpha(fill, 0.90f), stroke);
        r.drawText("Filled vector path (triangulated) + stroke", x + 52, r.baselineForBox(y + 8, 24), Theme.toArgb(t.textMuted));
        y += 56;

        int rowW = Math.min(520, ctx.width() - t.design.space_md * 2);
        int labelWPref = Math.round(rowW * 0.30f);
        ConstraintItem[] items = new ConstraintItem[]{
            new ConstraintItem(80, labelWPref, 220, 0.0f),   // label
            new ConstraintItem(120, 220, 10_000, 1.0f),      // field
            new ConstraintItem(34, 34, 34, 0.0f)             // button
        };
        int[] widths = ConstraintSolver.solve(rowW, items);
        int rx = x;
        int ry = y;
        int rh = 30;
        r.drawRect(rx, ry, widths[0], rh, Theme.toArgb(t.headerBg));
        r.drawText("Label 30%", rx + 8, r.baselineForBox(ry, rh), Theme.toArgb(t.textMuted));
        rx += widths[0];
        r.drawRect(rx, ry, widths[1], rh, Theme.toArgb(t.widgetBg));
        r.drawText("Field grows/shrinks", rx + 8, r.baselineForBox(ry, rh), Theme.toArgb(t.text));
        rx += widths[1];
        r.drawRoundedRect(rx, ry, widths[2], rh, t.design.radius_sm, Theme.toArgb(t.widgetHover));
        r.drawText("+", rx + (widths[2] - r.measureText("+")) * 0.5f, r.baselineForBox(ry, rh), Theme.toArgb(t.text));
        y += 42;
        ui.endPanel();
    }
}
