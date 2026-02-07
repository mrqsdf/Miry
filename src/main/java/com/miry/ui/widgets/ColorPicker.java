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
            theme.skins.panel.drawWithOutline(r, x, y, width, height, bg, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bg);
            drawOutline(r, x, y, width, height, 1, outline);
        }

        boolean canInteract = enabled() && interactive;
        boolean changed = render(r, canInteract ? input : null, x, y, width, height, textColor);

        if (!canInteract) {
            r.drawRect(x, y, width, height, 0x55000000);
        }

        return changed;
    }

    public boolean render(UiRenderer r, UiInput input, int x, int y, int width, int height, int textColor) {
        if (width <= 0 || height <= 0) {
            return false;
        }

        int pad = 12;
        int rightColWidth = Math.min(190, Math.max(120, width / 3));
        int wheelSize = Math.min(height - pad * 2, width - rightColWidth - pad * 3);

        boolean stacked = wheelSize < 160;
        if (stacked) {
            rightColWidth = width - pad * 2;
            wheelSize = Math.min(width - pad * 2, height - pad * 2 - 84);
        }

        wheelSize = Math.max(64, wheelSize);
        wheelSize = wheelSize & ~1; // keep even so radius is integer
        int radius = wheelSize / 2;

        int wheelX = x + pad;
        int wheelY = y + pad;
        int wheelCx = wheelX + radius;
        int wheelCy = wheelY + radius;

        int colX = stacked ? x + pad : wheelX + wheelSize + pad;
        int colY = wheelY;

        float baseline = r.baselineForBox(colY, 18);
        r.drawText("Preview", colX, baseline, textColor);

        int previewY = colY + 22;
        int previewW = rightColWidth;
        int previewH = 40;

        int valueSliderY = previewY + previewH + 18;
        int sliderW = rightColWidth;
        int sliderH = 16;
        int alphaSliderY = valueSliderY + sliderH + 16;

        if (stacked) {
            colY = wheelY + wheelSize + 14;
            baseline = r.baselineForBox(colY, 18);
            r.drawText("Preview", colX, baseline, textColor);
            previewY = colY + 22;
            valueSliderY = previewY + previewH + 18;
            alphaSliderY = valueSliderY + sliderH + 16;
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
        r.drawRect(ix - 5, iy - 5, 10, 10, 0xFF0D0D10);
        r.drawRect(ix - 4, iy - 4, 8, 8, 0xFFFFFFFF);
        r.drawRect(ix - 3, iy - 3, 6, 6, 0xFF0D0D10);

        // Preview + hex
        int argb = toArgb();
        r.drawRect(colX, previewY, previewW, previewH, argb);
        String hex = String.format("#%08X", argb);
        float hexBaseline = r.baselineForBox(previewY + previewH + 2, 16);
        r.drawText(hex, colX, hexBaseline, textColor);

        // Value slider: black -> full color for current hue/sat (v=1)
        float[] fullRgb = hsvToRgb(hue, saturation, 1.0f);
        for (int i = 0; i < sliderW; i++) {
            float t = sliderW <= 1 ? 0.0f : (i / (float) (sliderW - 1));
            int rr = Math.round(fullRgb[0] * 255.0f * t);
            int gg = Math.round(fullRgb[1] * 255.0f * t);
            int bb = Math.round(fullRgb[2] * 255.0f * t);
            int c = (0xFF << 24) | (rr << 16) | (gg << 8) | bb;
            r.drawRect(colX + i, valueSliderY, 1, sliderH, c);
        }
        r.drawRect(colX, valueSliderY, sliderW, 1, 0xFF3A3A42);
        r.drawRect(colX, valueSliderY + sliderH - 1, sliderW, 1, 0xFF3A3A42);
        int handleX = colX + Math.round(value * (sliderW - 1));
        r.drawRect(handleX - 2, valueSliderY - 2, 4, sliderH + 4, 0xFF0D0D10);
        r.drawRect(handleX - 1, valueSliderY - 1, 2, sliderH + 2, 0xFFFFFFFF);

        float vLabelBaseline = r.baselineForBox(valueSliderY - 16, 14);
        r.drawText("Value", colX, vLabelBaseline, textColor);

        // Alpha slider with checkerboard background
        int checkSize = 8;
        int light = 0xFF3A3A42;
        int dark = 0xFF24242A;
        for (int yy = 0; yy < sliderH; yy += checkSize) {
            for (int xx = 0; xx < sliderW; xx += checkSize) {
                boolean a = ((xx / checkSize) + (yy / checkSize)) % 2 == 0;
                r.drawRect(colX + xx, alphaSliderY + yy,
                    Math.min(checkSize, sliderW - xx), Math.min(checkSize, sliderH - yy),
                    a ? light : dark);
            }
        }
        int rgbNoAlpha = argb & 0x00FFFFFF;
        for (int i = 0; i < sliderW; i++) {
            float t = sliderW <= 1 ? 0.0f : (i / (float) (sliderW - 1));
            int aa = Math.round(t * 255.0f);
            r.drawRect(colX + i, alphaSliderY, 1, sliderH, (aa << 24) | rgbNoAlpha);
        }
        r.drawRect(colX, alphaSliderY, sliderW, 1, 0xFF3A3A42);
        r.drawRect(colX, alphaSliderY + sliderH - 1, sliderW, 1, 0xFF3A3A42);

        int aHandleX = colX + Math.round(alpha * (sliderW - 1));
        r.drawRect(aHandleX - 2, alphaSliderY - 2, 4, sliderH + 4, 0xFF0D0D10);
        r.drawRect(aHandleX - 1, alphaSliderY - 1, 2, sliderH + 2, 0xFFFFFFFF);

        float aLabelBaseline = r.baselineForBox(alphaSliderY - 16, 14);
        r.drawText("Alpha", colX, aLabelBaseline, textColor);

        return changed;
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
