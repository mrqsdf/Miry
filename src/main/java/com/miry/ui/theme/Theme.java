package com.miry.ui.theme;

import org.joml.Vector4f;

/**
 * Editor-oriented theme containing semantic colors and shared style tokens.
 *
 * Notes:
 * - Colors are semantic (widgetBg, widgetHover, etc.) so widgets don't depend on raw palette values.
 * - Tokens are centralized in {@link StyleTokens} for consistent spacing/radius/motion.
 */
public final class Theme {
    // ===== NEW TOKEN SYSTEMS =====
    /** Comprehensive design tokens (spacing, typography, radii, shadows, timing) */
    public final DesignTokens design = new DesignTokens();
    /** Raw color palette (gray scale, accent colors) */
    public final ColorPalette palette = new ColorPalette();

    // ===== LEGACY SYSTEMS (kept for backward compatibility) =====
    /** @deprecated Use design tokens instead. Kept for backward compatibility. */
    @Deprecated
    public final StyleTokens tokens = new StyleTokens(design);
    public final IconAtlas icons = new IconAtlas();
    public final WidgetSkins skins = new WidgetSkins();

    public final Vector4f windowBg = rgba(18, 18, 22, 255);
    public final Vector4f panelBg = rgba(28, 28, 34, 255);
    public final Vector4f headerBg = rgba(20, 20, 24, 255);
    public final Vector4f headerLine = rgba(46, 46, 56, 255);

    public final Vector4f widgetBg = rgba(34, 34, 42, 255);
    public final Vector4f widgetHover = rgba(54, 54, 66, 255);
    public final Vector4f widgetActive = rgba(76, 154, 255, 255);
    public final Vector4f widgetOutline = rgba(55, 55, 66, 255);

    public final Vector4f text = rgba(230, 230, 240, 255);
    public final Vector4f textMuted = rgba(175, 175, 190, 255);

    public final Vector4f disabledFg = rgba(135, 135, 150, 255);
    public final Vector4f disabledBg = rgba(26, 26, 32, 255);

    public final Vector4f shadow = rgba(0, 0, 0, 120);
    public final Vector4f focusRing = rgba(76, 154, 255, 255);

    public final Vector4f accent = rgba(76, 154, 255, 255);
    public final Vector4f danger = rgba(220, 80, 80, 255);

    private ThemeMode mode = ThemeMode.DARK;

    public Theme() {
        this(ThemeMode.DARK);
    }

    public Theme(ThemeMode mode) {
        setMode(mode);
    }

    public static Theme dark() {
        return new Theme(ThemeMode.DARK);
    }

    public static Theme light() {
        return new Theme(ThemeMode.LIGHT);
    }

    public ThemeMode mode() {
        return mode;
    }

    public void setMode(ThemeMode mode) {
        this.mode = mode == null ? ThemeMode.DARK : mode;
        if (this.mode == ThemeMode.LIGHT) {
            applyLight();
        } else {
            applyDark();
        }
    }

    private void applyDark() {
        // Blender-inspired dark defaults: darker inputs, subtle borders, and a muted blue selection.
        windowBg.set(ColorPalette.rgb(29, 29, 29));   // #1d1d1d
        panelBg.set(ColorPalette.rgb(43, 43, 43));    // #2b2b2b
        headerBg.set(ColorPalette.rgb(29, 29, 29));   // #1d1d1d
        headerLine.set(ColorPalette.rgb(17, 17, 17)); // #111111

        widgetBg.set(ColorPalette.rgb(29, 29, 29));     // #1d1d1d (inputs)
        widgetHover.set(ColorPalette.rgb(38, 38, 38));  // #262626
        widgetActive.set(ColorPalette.rgb(71, 114, 179)); // #4772b3
        widgetOutline.set(ColorPalette.rgb(17, 17, 17));  // #111111

        text.set(ColorPalette.rgb(230, 230, 230));       // #e6e6e6
        textMuted.set(ColorPalette.rgb(141, 141, 141));  // #8d8d8d

        disabledFg.set(ColorPalette.rgba(230, 230, 230, 110));
        disabledBg.set(ColorPalette.rgb(34, 34, 34));

        shadow.set(ColorPalette.rgba(0, 0, 0, 90));
        focusRing.set(ColorPalette.rgba(71, 114, 179, 204));
        accent.set(ColorPalette.rgb(71, 114, 179));
        danger.set(ColorPalette.rgb(220, 80, 80));
    }

    private void applyLight() {
        // Simple modern light palette (not yet tuned to match Godot perfectly).
        windowBg.set(palette.gray100);
        panelBg.set(palette.white);
        headerBg.set(palette.gray50);
        headerLine.set(ColorPalette.rgba(0, 0, 0, 40));

        widgetBg.set(palette.gray50);
        widgetHover.set(palette.gray200);
        widgetActive.set(ColorPalette.rgb(46, 124, 235));
        widgetOutline.set(palette.gray300);

        text.set(palette.gray900);
        textMuted.set(palette.gray600);

        disabledFg.set(palette.gray500);
        disabledBg.set(palette.gray100);

        shadow.set(ColorPalette.rgba(0, 0, 0, 38));
        focusRing.set(ColorPalette.rgba(46, 124, 235, 204));
    }

    public static Vector4f rgba(int r, int g, int b, int a) {
        return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    public static int toArgb(Vector4f c) {
        int a = clamp255(Math.round(c.w * 255.0f));
        int r = clamp255(Math.round(c.x * 255.0f));
        int g = clamp255(Math.round(c.y * 255.0f));
        int b = clamp255(Math.round(c.z * 255.0f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerpArgb(Vector4f a, Vector4f b, float t) {
        float tt = clamp01(t);
        return toArgb(new Vector4f(a).lerp(b, tt));
    }

    public static int lerpArgbInt(int argbA, int argbB, float t) {
        float tt = clamp01(t);
        int aA = (argbA >>> 24) & 0xFF;
        int rA = (argbA >>> 16) & 0xFF;
        int gA = (argbA >>> 8) & 0xFF;
        int bA = argbA & 0xFF;

        int aB = (argbB >>> 24) & 0xFF;
        int rB = (argbB >>> 16) & 0xFF;
        int gB = (argbB >>> 8) & 0xFF;
        int bB = argbB & 0xFF;

        int a = clamp255(Math.round(aA + (aB - aA) * tt));
        int r = clamp255(Math.round(rA + (rB - rA) * tt));
        int g = clamp255(Math.round(gA + (gB - gA) * tt));
        int b = clamp255(Math.round(bA + (bB - bA) * tt));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lightenArgb(int argb, float amount) {
        return lerpArgbInt(argb, 0xFFFFFFFF, amount);
    }

    public static int darkenArgb(int argb, float amount) {
        return lerpArgbInt(argb, 0xFF000000, amount);
    }

    /**
     * Multiplies the alpha channel of an ARGB color by {@code alphaMul}.
     */
    public static int mulAlpha(int argb, float alphaMul) {
        float m = clamp01(alphaMul);
        int a = (argb >>> 24) & 0xFF;
        int na = clamp255(Math.round(a * m));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
