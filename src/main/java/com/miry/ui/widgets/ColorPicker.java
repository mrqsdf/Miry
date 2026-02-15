package com.miry.ui.widgets;

import com.miry.graphics.Texture;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * HSV color picker widget (wheel + value/alpha sliders).
 */
public final class ColorPicker extends BaseWidget {
    private float hue;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private float alpha = 1.0f;

    private DragMode dragMode = DragMode.NONE;

    private Texture wheelTexture;
    private int wheelTextureSize;

    private static final float TAU = (float) (Math.PI * 2.0);

    public ColorPicker() {
        setFocusable(false);
    }

    public ColorPicker(int argb) {
        setFocusable(false);
        fromArgb(argb);
    }

    public void setHsva(float h, float s, float v, float a) {
        this.hue = clamp01(h);
        this.saturation = clamp01(s);
        this.value = clamp01(v);
        this.alpha = clamp01(a);
    }

    public void fromArgb(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float[] hsv = rgbToHsv(r / 255.0f, g / 255.0f, b / 255.0f);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.alpha = a / 255.0f;
    }

    public int toArgb() {
        float[] rgb = hsvToRgb(hue, saturation, value);
        int a = (int) (alpha * 255);
        int r = (int) (rgb[0] * 255);
        int g = (int) (rgb[1] * 255);
        int b = (int) (rgb[2] * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Themed render wrapper with consistent background/outline and disabled handling.
     */
    public boolean render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        if (r == null || theme == null) {
            return false;
        }

        int bg = enabled() ? Theme.toArgb(theme.panelBg) : Theme.toArgb(theme.disabledBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        int textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);

        if (theme.skins.panel != null) {
            theme.skins.panel.drawWithOutline(r, x, y, width, height, bg, outline, theme.design.border_thin);
        } else {
            int t = theme.design.border_thin;
            float radius = theme.design.radius_sm;
            int top = Theme.lightenArgb(bg, 0.02f);
            int bottom = Theme.darkenArgb(bg, 0.02f);
            r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
        }

        boolean canInteract = enabled() && interactive;
        boolean changed = renderInternal(r, canInteract ? input : null, theme, x, y, width, height, textColor);

        if (!canInteract) {
            r.drawRect(x, y, width, height, 0x55000000);
        }

        return changed;
    }

    public boolean render(UiRenderer r, UiInput input, int x, int y, int width, int height, int textColor) {
        return renderInternal(r, input, null, x, y, width, height, textColor);
    }

    private boolean renderInternal(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height, int textColor) {
        if (width <= 0 || height <= 0) {
            return false;
        }

        int pad = theme != null ? theme.design.space_md : 12;
        int spaceXs = theme != null ? theme.design.space_xs : 4;
        int spaceSm = theme != null ? theme.design.space_sm : 8;
        int spaceMd = theme != null ? theme.design.space_md : 12;
        int spaceLg = theme != null ? theme.design.space_lg : 16;
        int space2xl = theme != null ? theme.design.space_2xl : 32;
        int previewH = theme != null ? theme.design.widget_height_xl : 40;
        int sliderH = theme != null ? theme.design.icon_sm : 16;

        int outline = theme != null ? Theme.toArgb(theme.widgetOutline) : 0xFF3A3A42;
        int knobShadow = theme != null ? Theme.toArgb(theme.headerBg) : 0xFF0D0D10;
        float sliderRadius = theme != null ? theme.design.radius_sm : 3.0f;

        int rightColMinW = theme != null ? (space2xl * 3 + spaceLg + spaceSm) : 120;
        int rightColMaxW = theme != null ? (space2xl * 5 + spaceLg + spaceSm + spaceXs + theme.design.border_medium) : 190;
        int rightColWidth = Math.min(rightColMaxW, Math.max(rightColMinW, width / 3));
        int wheelSize = Math.min(height - pad * 2, width - rightColWidth - pad * 3);

        boolean stacked = wheelSize < (space2xl * 5);
        if (stacked) {
            rightColWidth = width - pad * 2;
            int extra = previewH + sliderH * 2 + spaceMd;
            wheelSize = Math.min(width - pad * 2, height - pad * 2 - extra);
        }

        int minWheel = theme != null ? (theme.design.icon_xl * 2) : 64;
        wheelSize = Math.max(minWheel, wheelSize);
        wheelSize = wheelSize & ~1; // keep even so radius is integer
        int radius = wheelSize / 2;

        int wheelX = x + pad;
        int wheelY = y + pad;
        int wheelCx = wheelX + radius;
        int wheelCy = wheelY + radius;

        int colX = stacked ? x + pad : wheelX + wheelSize + pad;
        int colY = wheelY;

        int labelBoxH = theme != null ? (spaceLg + spaceXs) : 18;
        float baseline = r.baselineForBox(colY, labelBoxH);
        r.drawText("Preview", colX, baseline, textColor);

        int previewY = colY + labelBoxH + spaceXs;
        int previewW = rightColWidth;

        int valueSliderY = previewY + previewH + spaceMd;
        int sliderW = rightColWidth;
        int alphaSliderY = valueSliderY + sliderH + spaceMd;

        if (stacked) {
            colY = wheelY + wheelSize + spaceMd;
            baseline = r.baselineForBox(colY, labelBoxH);
            r.drawText("Preview", colX, baseline, textColor);
            previewY = colY + labelBoxH + spaceXs;
            valueSliderY = previewY + previewH + spaceMd;
            alphaSliderY = valueSliderY + sliderH + spaceMd;
        }

        boolean changed = handleInput(input, wheelCx, wheelCy, radius,
            colX, valueSliderY, sliderW, sliderH,
            colX, alphaSliderY, sliderW, sliderH);

        ensureWheelTexture(wheelSize);

        int v = Math.max(0, Math.min(255, Math.round(value * 255.0f)));
        int tint = (0xFF << 24) | (v << 16) | (v << 8) | v;
        r.drawTexturedRect(wheelTexture, wheelX, wheelY, wheelSize, wheelSize, tint);

        // Wheel indicator
        float angle = hue * TAU;
        float ix = wheelCx + (float) Math.cos(angle) * saturation * radius;
        float iy = wheelCy + (float) Math.sin(angle) * saturation * radius;
        int ringSize = theme != null ? (spaceSm + theme.design.border_medium) : 10;
        float ringOuter = ringSize * 0.5f;
        float ringInner = Math.max(2.0f, ringOuter - (theme != null ? theme.design.border_medium : 2));
        r.drawCircle(ix, iy, ringOuter, 0xFFFFFFFF, 2.0f, knobShadow);
        r.drawCircle(ix, iy, ringInner, knobShadow);

        // Preview + hex
        int argb = toArgb();
        r.drawRect(colX, previewY, previewW, previewH, argb);
        String hex = String.format("#%08X", argb);
        int hexBoxH = sliderH;
        float hexBaseline = r.baselineForBox(previewY + previewH + borderThin(theme) * 2, hexBoxH);
        r.drawText(hex, colX, hexBaseline, textColor);

        // Value slider: black -> full color for current hue/sat (v=1)
        float[] fullRgb = hsvToRgb(hue, saturation, 1.0f);
        int full = (0xFF << 24)
            | (Math.round(fullRgb[0] * 255.0f) << 16)
            | (Math.round(fullRgb[1] * 255.0f) << 8)
            | Math.round(fullRgb[2] * 255.0f);
        r.drawRoundedRect(colX, valueSliderY, sliderW, sliderH, sliderRadius,
            0xFF000000, full, full, 0xFF000000,
            borderThin(theme), outline);
        int handleX = colX + Math.round(value * (sliderW - 1));
        int handleW = theme != null ? theme.design.border_medium : 2;
        int handlePad = theme != null ? theme.design.border_medium : 2;
        float handleR = Math.min(sliderRadius, sliderH * 0.5f);
        r.drawRoundedRect(handleX - handleW - handlePad, valueSliderY - handlePad, handleW * 2 + handlePad * 2, sliderH + handlePad * 2, handleR, knobShadow);
        r.drawRoundedRect(handleX - handleW, valueSliderY, handleW * 2, sliderH, handleR, 0xFFFFFFFF, 1.0f, knobShadow);

        float vLabelBaseline = r.baselineForBox(valueSliderY - labelBoxH, labelBoxH);
        r.drawText("Value", colX, vLabelBaseline, textColor);

        // Alpha slider with checkerboard background
        int checkSize = spaceSm;
        int light = outline;
        int dark = theme != null ? Theme.lerpArgb(theme.widgetOutline, theme.panelBg, 0.45f) : 0xFF24242A;
        int inset = Math.max(0, Math.round(sliderRadius));
        int cbX = colX + inset;
        int cbY = alphaSliderY + inset;
        int cbW = Math.max(0, sliderW - inset * 2);
        int cbH = Math.max(0, sliderH - inset * 2);
        for (int yy = 0; yy < cbH; yy += checkSize) {
            for (int xx = 0; xx < cbW; xx += checkSize) {
                boolean a = ((xx / checkSize) + (yy / checkSize)) % 2 == 0;
                r.drawRect(cbX + xx, cbY + yy,
                    Math.min(checkSize, cbW - xx), Math.min(checkSize, cbH - yy),
                    a ? light : dark);
            }
        }
        int rgbNoAlpha = argb & 0x00FFFFFF;
        int left = rgbNoAlpha; // alpha 0
        int right = (0xFF << 24) | rgbNoAlpha;
        r.drawRoundedRect(colX, alphaSliderY, sliderW, sliderH, sliderRadius,
            left, right, right, left,
            borderThin(theme), outline);

        int aHandleX = colX + Math.round(alpha * (sliderW - 1));
        r.drawRoundedRect(aHandleX - handleW - handlePad, alphaSliderY - handlePad, handleW * 2 + handlePad * 2, sliderH + handlePad * 2, handleR, knobShadow);
        r.drawRoundedRect(aHandleX - handleW, alphaSliderY, handleW * 2, sliderH, handleR, 0xFFFFFFFF, 1.0f, knobShadow);

        float aLabelBaseline = r.baselineForBox(alphaSliderY - labelBoxH, labelBoxH);
        r.drawText("Alpha", colX, aLabelBaseline, textColor);

        return changed;
    }

    private static int borderThin(Theme theme) {
        return theme != null ? theme.design.border_thin : 1;
    }

    public void close() {
        if (wheelTexture != null) {
            wheelTexture.close();
            wheelTexture = null;
        }
        wheelTextureSize = 0;
    }

    private boolean handleInput(UiInput input,
                                int wheelCx, int wheelCy, int radius,
                                int valueX, int valueY, int valueW, int valueH,
                                int alphaX, int alphaY, int alphaW, int alphaH) {
        if (!enabled() || input == null) {
            return false;
        }

        boolean changed = false;
        float mx = input.mousePos().x;
        float my = input.mousePos().y;

        boolean pressed = input.mousePressed();
        boolean down = input.mouseDown();
        boolean released = input.mouseReleased();

        if (released) {
            dragMode = DragMode.NONE;
        }

        if (pressed) {
            if (hitCircle(mx, my, wheelCx, wheelCy, radius)) {
                dragMode = DragMode.WHEEL;
            } else if (hitRect(mx, my, valueX, valueY, valueW, valueH)) {
                dragMode = DragMode.VALUE;
            } else if (hitRect(mx, my, alphaX, alphaY, alphaW, alphaH)) {
                dragMode = DragMode.ALPHA;
            }
        }

        if (!down && !pressed) {
            return false;
        }

        if (dragMode == DragMode.WHEEL) {
            float dx = (mx - wheelCx) / (float) radius;
            float dy = (my - wheelCy) / (float) radius;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float sat = clamp01(dist);
            float angle = (float) Math.atan2(dy, dx);
            float h = angle / TAU;
            if (h < 0.0f) h += 1.0f;
            hue = h;
            saturation = sat;
            changed = true;
        } else if (dragMode == DragMode.VALUE) {
            float t = (mx - valueX) / Math.max(1.0f, (float) valueW);
            value = clamp01(t);
            changed = true;
        } else if (dragMode == DragMode.ALPHA) {
            float t = (mx - alphaX) / Math.max(1.0f, (float) alphaW);
            alpha = clamp01(t);
            changed = true;
        }

        return changed;
    }

    private void ensureWheelTexture(int size) {
        if (wheelTexture != null && wheelTextureSize == size) {
            return;
        }
        close();
        wheelTextureSize = size;
        wheelTexture = new Texture();
        wheelTexture.setFilteringLinear();

        int radius = size / 2;
        int cx = radius;
        int cy = radius;
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - cx) / (float) radius;
                float dy = (y - cy) / (float) radius;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= 1.0f) {
                    float angle = (float) Math.atan2(dy, dx);
                    float h = angle / TAU;
                    if (h < 0.0f) h += 1.0f;
                    float[] rgb = hsvToRgb(h, clamp01(dist), 1.0f);

                    float edge = 1.0f - dist;
                    float aa = clamp01(edge * 18.0f);
                    int a = Math.round(aa * 255.0f);

                    pixels.put((byte) Math.round(rgb[0] * 255.0f));
                    pixels.put((byte) Math.round(rgb[1] * 255.0f));
                    pixels.put((byte) Math.round(rgb[2] * 255.0f));
                    pixels.put((byte) a);
                } else {
                    pixels.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
        }
        pixels.flip();
        wheelTexture.uploadRgba(size, size, pixels);
    }

    private static boolean hitRect(float mx, float my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static boolean hitCircle(float mx, float my, int cx, int cy, int radius) {
        float dx = mx - cx;
        float dy = my - cy;
        float rr = radius * (float) radius;
        return dx * dx + dy * dy <= rr;
    }

    private enum DragMode {
        NONE,
        WHEEL,
        VALUE,
        ALPHA
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0.0f;
        if (delta > 0.0f) {
            if (max == r) {
                h = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                h = ((b - r) / delta) + 2.0f;
            } else {
                h = ((r - g) / delta) + 4.0f;
            }
            h /= 6.0f;
            if (h < 0.0f) h += 1.0f;
        }

        float s = max > 0.0f ? delta / max : 0.0f;
        float v = max;
        return new float[]{h, s, v};
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0f - Math.abs((h * 6.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r, g, b;
        int segment = (int) (h * 6.0f);
        switch (segment) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return new float[]{r + m, g + m, b + m};
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public float hue() { return hue; }
    public float saturation() { return saturation; }
    public float value() { return value; }
    public float alpha() { return alpha; }
}
