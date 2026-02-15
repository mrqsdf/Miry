package com.miry.ui.util;

/**
 * Color manipulation utilities for ARGB integer colors.
 */
public final class ColorUtils {
    private ColorUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Extracts the alpha component from an ARGB color.
     */
    public static int getAlpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /**
     * Extracts the red component from an ARGB color.
     */
    public static int getRed(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    /**
     * Extracts the green component from an ARGB color.
     */
    public static int getGreen(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    /**
     * Extracts the blue component from an ARGB color.
     */
    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    /**
     * Packs ARGB components into a 32-bit integer.
     */
    public static int argb(int a, int r, int g, int b) {
        return (MathUtils.clamp255(a) << 24) |
               (MathUtils.clamp255(r) << 16) |
               (MathUtils.clamp255(g) << 8) |
               MathUtils.clamp255(b);
    }

    /**
     * Packs RGB components into a 32-bit integer with full opacity.
     */
    public static int rgb(int r, int g, int b) {
        return argb(255, r, g, b);
    }

    /**
     * Linear interpolation between two ARGB colors.
     *
     * @param argbA first color
     * @param argbB second color
     * @param t interpolation factor (0.0 to 1.0)
     * @return interpolated color
     */
    public static int lerpArgb(int argbA, int argbB, float t) {
        float tt = MathUtils.clamp01(t);

        int aA = getAlpha(argbA);
        int rA = getRed(argbA);
        int gA = getGreen(argbA);
        int bA = getBlue(argbA);

        int aB = getAlpha(argbB);
        int rB = getRed(argbB);
        int gB = getGreen(argbB);
        int bB = getBlue(argbB);

        int a = MathUtils.clamp255(Math.round(aA + (aB - aA) * tt));
        int r = MathUtils.clamp255(Math.round(rA + (rB - rA) * tt));
        int g = MathUtils.clamp255(Math.round(gA + (gB - gA) * tt));
        int b = MathUtils.clamp255(Math.round(bA + (bB - bA) * tt));

        return argb(a, r, g, b);
    }

    /**
     * Lightens an ARGB color by lerping toward white.
     *
     * @param argb color to lighten
     * @param amount amount to lighten (0.0 to 1.0)
     * @return lightened color
     */
    public static int lighten(int argb, float amount) {
        return lerpArgb(argb, 0xFFFFFFFF, amount);
    }

    /**
     * Darkens an ARGB color by lerping toward black.
     *
     * @param argb color to darken
     * @param amount amount to darken (0.0 to 1.0)
     * @return darkened color
     */
    public static int darken(int argb, float amount) {
        return lerpArgb(argb, 0xFF000000, amount);
    }

    /**
     * Multiplies the alpha channel by a factor.
     *
     * @param argb color to modify
     * @param alphaMul alpha multiplier (0.0 to 1.0)
     * @return color with modified alpha
     */
    public static int mulAlpha(int argb, float alphaMul) {
        float m = MathUtils.clamp01(alphaMul);
        int a = getAlpha(argb);
        int outA = MathUtils.clamp255(Math.round(a * m));
        return (outA << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Sets the alpha channel to a specific value.
     *
     * @param argb color to modify
     * @param alpha new alpha value (0 to 255)
     * @return color with new alpha
     */
    public static int withAlpha(int argb, int alpha) {
        int a = MathUtils.clamp255(alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Reduces the alpha channel by a factor (for disabled/dimmed elements).
     *
     * @param argb color to dim
     * @param factor dimming factor (0.0 to 1.0)
     * @return dimmed color
     */
    public static int dimAlpha(int argb, float factor) {
        int a = getAlpha(argb);
        int na = MathUtils.clamp255(Math.round(a * MathUtils.clamp01(factor)));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Converts RGB color to HSV color space.
     *
     * @param r red component (0.0 to 1.0)
     * @param g green component (0.0 to 1.0)
     * @param b blue component (0.0 to 1.0)
     * @return array [h, s, v] where h is 0-1, s is 0-1, v is 0-1
     */
    public static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0.0f;
        if (delta > 1e-6f) {
            if (max == r) {
                h = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                h = (b - r) / delta + 2.0f;
            } else {
                h = (r - g) / delta + 4.0f;
            }
            h /= 6.0f;
            if (h < 0.0f) {
                h += 1.0f;
            }
        }

        float s = (max > 1e-6f) ? (delta / max) : 0.0f;
        float v = max;

        return new float[]{h, s, v};
    }

    /**
     * Converts HSV color to RGB color space.
     *
     * @param h hue (0.0 to 1.0)
     * @param s saturation (0.0 to 1.0)
     * @param v value (0.0 to 1.0)
     * @return array [r, g, b] where each component is 0-1
     */
    public static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0f - Math.abs((h * 6.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r, g, b;
        float hh = h * 6.0f;
        if (hh < 1.0f) {
            r = c; g = x; b = 0.0f;
        } else if (hh < 2.0f) {
            r = x; g = c; b = 0.0f;
        } else if (hh < 3.0f) {
            r = 0.0f; g = c; b = x;
        } else if (hh < 4.0f) {
            r = 0.0f; g = x; b = c;
        } else if (hh < 5.0f) {
            r = x; g = 0.0f; b = c;
        } else {
            r = c; g = 0.0f; b = x;
        }

        return new float[]{r + m, g + m, b + m};
    }

    /**
     * Converts ARGB integer to HSV color space.
     *
     * @param argb ARGB color
     * @return array [h, s, v, a] where h,s,v are 0-1 and a is 0-255
     */
    public static float[] argbToHsv(int argb) {
        float r = getRed(argb) / 255.0f;
        float g = getGreen(argb) / 255.0f;
        float b = getBlue(argb) / 255.0f;
        float a = getAlpha(argb);

        float[] hsv = rgbToHsv(r, g, b);
        return new float[]{hsv[0], hsv[1], hsv[2], a};
    }

    /**
     * Converts HSV color space to ARGB integer.
     *
     * @param h hue (0.0 to 1.0)
     * @param s saturation (0.0 to 1.0)
     * @param v value (0.0 to 1.0)
     * @param a alpha (0 to 255)
     * @return ARGB color
     */
    public static int hsvToArgb(float h, float s, float v, int a) {
        float[] rgb = hsvToRgb(h, s, v);
        return argb(a,
                   Math.round(rgb[0] * 255.0f),
                   Math.round(rgb[1] * 255.0f),
                   Math.round(rgb[2] * 255.0f));
    }
}
