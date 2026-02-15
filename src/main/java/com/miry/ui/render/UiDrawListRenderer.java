package com.miry.ui.render;

import com.miry.graphics.Texture;

import java.util.Objects;

/**
 * Records {@link UiRenderer} calls into a {@link UiDrawList}.
 * <p>
 * This renderer does not perform any graphics API calls.
 */
public final class UiDrawListRenderer implements UiRenderer {
    private final UiDrawList drawList;
    private final UiFontMetrics metrics;

    public UiDrawListRenderer(UiDrawList drawList, UiFontMetrics metrics) {
        this.drawList = Objects.requireNonNull(drawList, "drawList");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    public UiDrawList drawList() {
        return drawList;
    }

    @Override
    public void flush() {
        // No-op: backends consume the list.
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        drawList.add(new UiDrawList.PushClip(x, y, width, height));
    }

    @Override
    public void popClip() {
        drawList.add(new UiDrawList.PopClip());
    }

    @Override
    public void drawRect(float x, float y, float w, float h, int argb) {
        drawList.add(new UiDrawList.Rect(x, y, w, h, argb));
    }

    @Override
    public void drawRoundedRect(float x, float y, float w, float h, float radiusPx, int argb) {
        drawRoundedRect(x, y, w, h, radiusPx, argb, argb, argb, argb, 0.0f, 0);
    }

    @Override
    public void drawRoundedRect(float x,
                                float y,
                                float w,
                                float h,
                                float radiusPx,
                                int argbTL,
                                int argbTR,
                                int argbBR,
                                int argbBL) {
        drawRoundedRect(x, y, w, h, radiusPx, argbTL, argbTR, argbBR, argbBL, 0.0f, 0);
    }

    @Override
    public void drawRoundedRect(float x,
                                float y,
                                float w,
                                float h,
                                float radiusPx,
                                int fillTL,
                                int fillTR,
                                int fillBR,
                                int fillBL,
                                float borderPx,
                                int strokeArgb) {
        drawList.add(new UiDrawList.RoundedRect(x, y, w, h, radiusPx, fillTL, fillTR, fillBR, fillBL, borderPx, strokeArgb));
    }

    @Override
    public void drawTexturedRect(Texture texture, float x, float y, float w, float h, int argb) {
        drawTexturedRect(texture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f, argb);
    }

    @Override
    public void drawTexturedRect(Texture texture,
                                 float x,
                                 float y,
                                 float w,
                                 float h,
                                 float u0,
                                 float v0,
                                 float u1,
                                 float v1,
                                 int argb) {
        if (texture == null) {
            return;
        }
        drawList.add(new UiDrawList.TexturedRect(texture, x, y, w, h, u0, v0, u1, v1, argb));
    }

    @Override
    public void drawText(String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        drawList.add(new UiDrawList.Text(text, x, y, argb));
    }

    @Override
    public float measureText(String text) {
        return metrics.measureText(text);
    }

    @Override
    public float lineHeight() {
        return metrics.lineHeight();
    }

    @Override
    public float ascent() {
        return metrics.ascent();
    }
}
