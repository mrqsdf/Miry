package com.miry.ui.theme;

import org.joml.Vector4f;

/**
 * Computes interpolated colors based on widget state (hover, press, focus) and variant.
 *
 * Philosophy:
 * - State transitions are smooth (exponential approach in BaseWidget)
 * - Colors blend between states using linear interpolation
 * - Variants override base colors but follow same transition logic
 *
 * Usage:
 * {@code
 *   int bgColor = StateColors.computeBackground(theme, hoverT, pressT, variant);
 * }
 */
public final class StateColors {

    /**
     * Compute background color based on state and variant.
     *
     * @param theme Theme containing color palette
     * @param hoverT Hover transition [0.0 - 1.0], 1.0 = fully hovered
     * @param pressT Press transition [0.0 - 1.0], 1.0 = fully pressed
     * @param variant Widget variant (DEFAULT, PRIMARY, etc.)
     * @return ARGB color integer
     */
    public static int computeBackground(Theme theme, float hoverT, float pressT, WidgetVariant variant) {
        ColorPalette p = theme.palette;
        Vector4f baseColor;
        Vector4f hoverColor;
        Vector4f pressColor;

        switch (variant) {
            case PRIMARY:
                baseColor = p.blue500;
                hoverColor = p.blue400;
                pressColor = p.blue600;
                break;

            case SECONDARY:
                baseColor = theme.widgetBg;
                hoverColor = ColorPalette.lighten(theme.widgetBg, 0.05f);
                pressColor = ColorPalette.darken(theme.widgetBg, 0.05f);
                break;

            case DANGER:
                baseColor = p.red500;
                hoverColor = p.red400;
                pressColor = p.red600;
                break;

            case SUCCESS:
                baseColor = p.green500;
                hoverColor = p.green400;
                pressColor = p.green600;
                break;

            case GHOST:
                baseColor = ColorPalette.rgba(0, 0, 0, 0); // Transparent
                hoverColor = ColorPalette.withAlpha(theme.widgetHover, 0.5f);
                pressColor = ColorPalette.withAlpha(theme.widgetActive, 0.2f);
                break;

            case DEFAULT:
            default:
                baseColor = theme.widgetBg;
                hoverColor = theme.widgetHover;
                pressColor = ColorPalette.darken(theme.widgetHover, 0.1f);
                break;
        }

        // Interpolate: base -> hover -> press
        Vector4f color = new Vector4f(baseColor);
        color.lerp(hoverColor, clamp01(hoverT));
        color.lerp(pressColor, clamp01(pressT));

        return Theme.toArgb(color);
    }

    /**
     * Compute border color based on state and variant.
     */
    public static int computeBorder(Theme theme, float hoverT, float pressT, float focusT, WidgetVariant variant) {
        ColorPalette p = theme.palette;

        // Focus overrides everything
        if (focusT > 0.01f) {
            Vector4f focusColor = theme.focusRing;
            Vector4f baseColor = theme.widgetOutline;
            Vector4f blended = new Vector4f(baseColor).lerp(focusColor, clamp01(focusT));
            return Theme.toArgb(blended);
        }

        // Otherwise, use variant colors
        Vector4f baseColor = theme.widgetOutline;
        Vector4f hoverColor;

        switch (variant) {
            case PRIMARY:
                hoverColor = p.blue400;
                break;
            case DANGER:
                hoverColor = p.red400;
                break;
            case SUCCESS:
                hoverColor = p.green400;
                break;
            default:
                hoverColor = ColorPalette.lighten(theme.widgetOutline, 0.2f);
                break;
        }

        Vector4f color = new Vector4f(baseColor);
        color.lerp(hoverColor, clamp01(hoverT));
        return Theme.toArgb(color);
    }

    /**
     * Compute text color based on variant (light text on dark buttons, etc.).
     */
    public static int computeText(Theme theme, WidgetVariant variant) {
        ColorPalette p = theme.palette;

        switch (variant) {
            case PRIMARY:
            case DANGER:
            case SUCCESS:
                // Light text on colored backgrounds
                return Theme.toArgb(p.white);

            case GHOST:
            case SECONDARY:
            case DEFAULT:
            default:
                // Use theme text color
                return Theme.toArgb(theme.text);
        }
    }

    /**
     * Compute focus ring color (defaults to theme focus ring, variant-aware).
     */
    public static int computeFocusRing(Theme theme, WidgetVariant variant) {
        ColorPalette p = theme.palette;

        switch (variant) {
            case DANGER:
                return Theme.toArgb(p.red500);
            case SUCCESS:
                return Theme.toArgb(p.green500);
            case PRIMARY:
            case DEFAULT:
            default:
                return Theme.toArgb(theme.focusRing);
        }
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
