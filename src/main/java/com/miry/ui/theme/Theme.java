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
    public final StyleTokens tokens = new StyleTokens();
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
        windowBg.set(rgba(18, 18, 22, 255));
        panelBg.set(rgba(28, 28, 34, 255));
        headerBg.set(rgba(20, 20, 24, 255));
        headerLine.set(rgba(46, 46, 56, 255));

        widgetBg.set(rgba(34, 34, 42, 255));
        widgetHover.set(rgba(54, 54, 66, 255));
        widgetActive.set(rgba(76, 154, 255, 255));
        widgetOutline.set(rgba(55, 55, 66, 255));

        text.set(rgba(230, 230, 240, 255));
        textMuted.set(rgba(175, 175, 190, 255));

        disabledFg.set(rgba(135, 135, 150, 255));
        disabledBg.set(rgba(26, 26, 32, 255));

        shadow.set(rgba(0, 0, 0, 120));
        focusRing.set(rgba(76, 154, 255, 255));
    }

    private void applyLight() {
        windowBg.set(rgba(245, 245, 248, 255));
        panelBg.set(rgba(255, 255, 255, 255));
        headerBg.set(rgba(242, 242, 246, 255));
        headerLine.set(rgba(220, 220, 228, 255));

        widgetBg.set(rgba(244, 244, 248, 255));
        widgetHover.set(rgba(232, 232, 240, 255));
        widgetActive.set(rgba(50, 120, 255, 255));
        widgetOutline.set(rgba(210, 210, 222, 255));

        text.set(rgba(22, 22, 26, 255));
        textMuted.set(rgba(90, 90, 102, 255));

        disabledFg.set(rgba(130, 130, 140, 255));
        disabledBg.set(rgba(236, 236, 242, 255));

        shadow.set(rgba(0, 0, 0, 55));
        focusRing.set(rgba(50, 120, 255, 255));
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

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
