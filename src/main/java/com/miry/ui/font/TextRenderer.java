package com.miry.ui.font;

import com.miry.graphics.batch.BatchRenderer;

/**
 * Text renderer that draws glyph quads from a {@link FontAtlas}.
 */
public final class TextRenderer {
    private final FontAtlas atlas;

    public TextRenderer(FontAtlas atlas) {
        this.atlas = atlas;
    }

    private static float snapToPixel(float v, float pixelScale) {
        if (pixelScale <= 0.0f) {
            return v;
        }
        return Math.round(v * pixelScale) / pixelScale;
    }

    public void drawText(BatchRenderer renderer, String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) return;

        float pixelScale = atlas.pixelScale();
        float cursorX = x;
        float cursorY = y + atlas.ascent();

        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) {
                i++;
            }

            if (cp == '\n') {
                cursorX = x;
                cursorY = snapToPixel(cursorY + atlas.lineHeight(), pixelScale);
                continue;
            }

            Glyph g = atlas.getGlyph(cp);
            if (g == null) continue;

            if (g.atlasWidth() > 0 && g.atlasHeight() > 0 && g.renderWidth() > 0.0f && g.renderHeight() > 0.0f) {
                float gx = snapToPixel(cursorX + g.bearingX(), pixelScale);
                float gy = snapToPixel(cursorY + g.bearingY(), pixelScale);
                float invW = 1.0f / atlas.texture().width();
                float invH = 1.0f / atlas.texture().height();

                // Inset by half a texel to reduce glyph bleeding with linear filtering.
                float insetX = g.atlasWidth() > 1 ? 0.5f : 0.0f;
                float insetY = g.atlasHeight() > 1 ? 0.5f : 0.0f;
                float u0 = (g.atlasX() + insetX) * invW;
                float v0 = (g.atlasY() + insetY) * invH;
                float u1 = (g.atlasX() + g.atlasWidth() - insetX) * invW;
                float v1 = (g.atlasY() + g.atlasHeight() - insetY) * invH;
                if (atlas.isSdf()) {
                    renderer.drawSdfTexturedRect(atlas.texture(), gx, gy, g.renderWidth(), g.renderHeight(), u0, v0, u1, v1, argb);
                } else {
                    renderer.drawCoverageTexturedRect(atlas.texture(), gx, gy, g.renderWidth(), g.renderHeight(), u0, v0, u1, v1, argb);
                }
            }

            cursorX += g.advanceX();
        }
    }

    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        float width = 0.0f;
        float maxWidth = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) {
                i++;
            }
            if (cp == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0.0f;
                continue;
            }
            Glyph g = atlas.getGlyph(cp);
            if (g != null) {
                width += g.advanceX();
            }
        }
        return Math.max(maxWidth, width);
    }

    public FontAtlas atlas() {
        return atlas;
    }
}
