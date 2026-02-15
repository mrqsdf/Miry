package com.miry.ui.theme;

import org.joml.Vector4f;

/**
 * Raw color palette with gray scale and accent colors.
 *
 * Naming:
 * - Gray 50-950: lightest to darkest
 * - Accent colors: blue, red, green, yellow (variants: 400, 500, 600)
 */
public final class ColorPalette {
    /** Nearly white - backgrounds in light mode */
    public final Vector4f gray50 = rgb(250, 250, 252);
    /** Very light gray - hover states in light mode */
    public final Vector4f gray100 = rgb(245, 245, 248);
    /** Light gray - borders in light mode */
    public final Vector4f gray200 = rgb(232, 232, 240);
    /** Medium-light gray */
    public final Vector4f gray300 = rgb(210, 210, 222);
    /** Medium gray - text muted */
    public final Vector4f gray400 = rgb(175, 175, 190);
    /** True middle gray */
    public final Vector4f gray500 = rgb(135, 135, 150);
    /** Medium-dark gray */
    public final Vector4f gray600 = rgb(90, 90, 102);
    /** Dark gray - text in dark mode */
    public final Vector4f gray700 = rgb(55, 55, 66);
    /** Very dark gray - widgets in dark mode */
    public final Vector4f gray800 = rgb(34, 34, 42);
    /** Nearly black - panels in dark mode */
    public final Vector4f gray900 = rgb(22, 22, 26);
    /** Darkest - window background in dark mode */
    public final Vector4f gray950 = rgb(18, 18, 22);
    /** Light blue - hover state */
    public final Vector4f blue400 = rgb(100, 170, 255);
    /** Medium blue - default accent */
    public final Vector4f blue500 = rgb(76, 154, 255);
    /** Dark blue - pressed state */
    public final Vector4f blue600 = rgb(50, 120, 220);
    /** Light red */
    public final Vector4f red400 = rgb(255, 110, 110);
    /** Medium red - default danger */
    public final Vector4f red500 = rgb(240, 80, 80);
    /** Dark red - pressed danger */
    public final Vector4f red600 = rgb(200, 50, 50);
    /** Light green */
    public final Vector4f green400 = rgb(100, 230, 130);
    /** Medium green - default success */
    public final Vector4f green500 = rgb(70, 200, 100);
    /** Dark green - pressed success */
    public final Vector4f green600 = rgb(50, 170, 80);
    /** Light yellow */
    public final Vector4f yellow400 = rgb(255, 220, 100);
    /** Medium yellow - default warning */
    public final Vector4f yellow500 = rgb(240, 200, 60);
    /** Dark yellow - pressed warning */
    public final Vector4f yellow600 = rgb(200, 160, 30);
    /** Light orange */
    public final Vector4f orange400 = rgb(255, 170, 100);
    /** Medium orange */
    public final Vector4f orange500 = rgb(240, 140, 60);
    /** Dark orange */
    public final Vector4f orange600 = rgb(200, 110, 30);
    /** Light purple */
    public final Vector4f purple400 = rgb(180, 140, 255);
    /** Medium purple */
    public final Vector4f purple500 = rgb(150, 100, 240);
    /** Dark purple */
    public final Vector4f purple600 = rgb(120, 70, 200);

    /** Pure black with alpha for shadows */
    public final Vector4f black = rgb(0, 0, 0);
    /** Pure white for highlights */
    public final Vector4f white = rgb(255, 255, 255);
    /** Transparent */
    public final Vector4f transparent = rgba(0, 0, 0, 0);

    /** Create RGB color (alpha = 1.0) */
    public static Vector4f rgb(int r, int g, int b) {
        return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
    }

    /** Create RGBA color */
    public static Vector4f rgba(int r, int g, int b, int a) {
        return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    /** Create color with alpha override */
    public static Vector4f withAlpha(Vector4f color, float alpha) {
        return new Vector4f(color.x, color.y, color.z, alpha);
    }

    /** Darken color by percentage (0.0 - 1.0) */
    public static Vector4f darken(Vector4f color, float amount) {
        float factor = 1.0f - Math.max(0.0f, Math.min(1.0f, amount));
        return new Vector4f(color.x * factor, color.y * factor, color.z * factor, color.w);
    }

    /** Lighten color by percentage (0.0 - 1.0) */
    public static Vector4f lighten(Vector4f color, float amount) {
        float factor = Math.max(0.0f, Math.min(1.0f, amount));
        return new Vector4f(
            color.x + (1.0f - color.x) * factor,
            color.y + (1.0f - color.y) * factor,
            color.z + (1.0f - color.z) * factor,
            color.w
        );
    }
}
