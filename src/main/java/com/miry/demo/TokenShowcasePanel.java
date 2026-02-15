package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.DesignTokens;
import com.miry.ui.theme.ColorPalette;
import com.miry.ui.theme.Theme;
import org.joml.Vector4f;

/**
 * Panel that visualizes all design tokens from the new token system.
 * Shows spacing, typography, radii, colors, and shadows for design review.
 */
public class TokenShowcasePanel extends Panel {

    public TokenShowcasePanel() {
        super("Design Tokens");
    }

    @Override
    public void render(PanelContext ctx) {
        UiRenderer r = ctx.renderer();
        Theme theme = ctx.ui().theme();
        DesignTokens d = theme.design;
        ColorPalette p = theme.palette;

        int x = ctx.x() + d.space_md;
        int y = ctx.y() + d.space_md;
        int lineHeight = d.font_base + d.space_sm;
        int sectionGap = d.space_xl;

        // ===== SPACING SCALE =====
        y = renderSection(r, theme, "SPACING SCALE (8px base)", x, y);
        y = renderToken(r, theme, "space_xs", d.space_xs + "px", x, y, lineHeight);
        y = renderToken(r, theme, "space_sm", d.space_sm + "px", x, y, lineHeight);
        y = renderToken(r, theme, "space_md", d.space_md + "px", x, y, lineHeight);
        y = renderToken(r, theme, "space_lg", d.space_lg + "px", x, y, lineHeight);
        y = renderToken(r, theme, "space_xl", d.space_xl + "px", x, y, lineHeight);
        y = renderToken(r, theme, "space_2xl", d.space_2xl + "px", x, y, lineHeight);
        y += sectionGap;

        // ===== TYPOGRAPHY SCALE =====
        y = renderSection(r, theme, "TYPOGRAPHY SCALE", x, y);
        y = renderToken(r, theme, "font_xs", d.font_xs + "px", x, y, lineHeight);
        y = renderToken(r, theme, "font_sm", d.font_sm + "px", x, y, lineHeight);
        y = renderToken(r, theme, "font_base", d.font_base + "px", x, y, lineHeight);
        y = renderToken(r, theme, "font_md", d.font_md + "px", x, y, lineHeight);
        y = renderToken(r, theme, "font_lg", d.font_lg + "px", x, y, lineHeight);
        y = renderToken(r, theme, "font_xl", d.font_xl + "px", x, y, lineHeight);
        y += sectionGap;

        // ===== CORNER RADII =====
        y = renderSection(r, theme, "CORNER RADII", x, y);
        y = renderRadiusToken(r, theme, "radius_none", d.radius_none, x, y, lineHeight);
        y = renderRadiusToken(r, theme, "radius_sm", d.radius_sm, x, y, lineHeight);
        y = renderRadiusToken(r, theme, "radius_md", d.radius_md, x, y, lineHeight);
        y = renderRadiusToken(r, theme, "radius_lg", d.radius_lg, x, y, lineHeight);
        y = renderRadiusToken(r, theme, "radius_xl", d.radius_xl, x, y, lineHeight);
        y = renderRadiusToken(r, theme, "radius_full", 12, x, y, lineHeight); // 12 instead of 9999 for visual
        y += sectionGap;

        // ===== WIDGET HEIGHTS =====
        y = renderSection(r, theme, "WIDGET HEIGHTS", x, y);
        y = renderHeightToken(r, theme, "widget_height_sm", d.widget_height_sm, x, y, lineHeight);
        y = renderHeightToken(r, theme, "widget_height_md", d.widget_height_md, x, y, lineHeight);
        y = renderHeightToken(r, theme, "widget_height_lg", d.widget_height_lg, x, y, lineHeight);
        y = renderHeightToken(r, theme, "widget_height_xl", d.widget_height_xl, x, y, lineHeight);
        y += sectionGap;

        // ===== COLOR PALETTE (Gray Scale) =====
        y = renderSection(r, theme, "COLOR PALETTE - GRAYS", x, y);
        y = renderColorSwatch(r, theme, "gray50", p.gray50, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray100", p.gray100, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray200", p.gray200, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray300", p.gray300, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray400", p.gray400, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray500", p.gray500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray600", p.gray600, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray700", p.gray700, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray800", p.gray800, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray900", p.gray900, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "gray950", p.gray950, x, y, lineHeight);
        y += sectionGap;

        // ===== ACCENT COLORS =====
        y = renderSection(r, theme, "ACCENT COLORS", x, y);
        y = renderColorSwatch(r, theme, "blue500", p.blue500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "red500", p.red500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "green500", p.green500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "yellow500", p.yellow500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "orange500", p.orange500, x, y, lineHeight);
        y = renderColorSwatch(r, theme, "purple500", p.purple500, x, y, lineHeight);
    }

    private int renderSection(UiRenderer r, Theme theme, String title, int x, int y) {
        float baselineY = y + r.ascent();
        r.drawText(title, x, baselineY, Theme.toArgb(theme.text));
        return y + (int)r.lineHeight() + theme.design.space_md;
    }

    private int renderToken(UiRenderer r, Theme theme, String name, String value, int x, int y, int lineHeight) {
        int labelColor = Theme.toArgb(theme.text);
        int valueColor = Theme.toArgb(theme.textMuted);
        float baselineY = y + r.ascent();
        r.drawText(name, x, baselineY, labelColor);
        r.drawText(value, x + 180, baselineY, valueColor);
        return y + lineHeight;
    }

    private int renderRadiusToken(UiRenderer r, Theme theme, String name, int radius, int x, int y, int lineHeight) {
        int labelColor = Theme.toArgb(theme.text);
        int valueColor = Theme.toArgb(theme.textMuted);
        float baselineY = y + r.ascent();
        r.drawText(name, x, baselineY, labelColor);
        r.drawText(radius + "px", x + 180, baselineY, valueColor);

        // Draw visual example (rounded rect).
        int boxSize = 24;
        int boxX = x + 280;
        int boxY = y - 2;
        float rr = Math.min(radius, boxSize * 0.5f);
        int fill = Theme.toArgb(theme.widgetActive);
        int top = Theme.lightenArgb(fill, 0.10f);
        int bottom = Theme.darkenArgb(fill, 0.10f);
        r.drawRoundedRect(boxX, boxY, boxSize, boxSize, rr, top, top, bottom, bottom);

        return y + lineHeight;
    }

    private int renderHeightToken(UiRenderer r, Theme theme, String name, int height, int x, int y, int lineHeight) {
        int labelColor = Theme.toArgb(theme.text);
        int valueColor = Theme.toArgb(theme.textMuted);
        float baselineY = y + r.ascent();
        r.drawText(name, x, baselineY, labelColor);
        r.drawText(height + "px", x + 180, baselineY, valueColor);

        // Draw visual bar
        int barWidth = 200;
        int barX = x + 280;
        int barY = y - 2;
        int bg = Theme.toArgb(theme.widgetBg);
        int top = Theme.lightenArgb(bg, 0.04f);
        int bottom = Theme.darkenArgb(bg, 0.04f);
        r.drawRoundedRect(barX, barY, barWidth, height, Math.min(theme.design.radius_sm, height * 0.5f), top, top, bottom, bottom);

        return y + lineHeight + theme.design.space_xs;
    }

    private int renderColorSwatch(UiRenderer r, Theme theme, String name, Vector4f color, int x, int y, int lineHeight) {
        int labelColor = Theme.toArgb(theme.text);
        float baselineY = y + r.ascent();
        r.drawText(name, x, baselineY, labelColor);

        // Draw color swatch
        int swatchSize = 20;
        int swatchX = x + 180;
        int swatchY = y - 2;
        r.drawRect(swatchX, swatchY, swatchSize, swatchSize, Theme.toArgb(color));

        // Show RGB values
        int rr = Math.round(color.x * 255);
        int gg = Math.round(color.y * 255);
        int bb = Math.round(color.z * 255);
        String rgb = String.format("rgb(%d, %d, %d)", rr, gg, bb);
        r.drawText(rgb, x + 210, baselineY, Theme.toArgb(theme.textMuted));

        return y + lineHeight;
    }
}
