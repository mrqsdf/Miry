package com.miry.ui.theme;

import org.joml.Vector4f;

/**
 * Custom style overrides for individual widgets.
 *
 * Allows per-widget customization while falling back to theme/variant defaults.
 * All fields are nullable - null means "use default from theme/variant".
 *
 * Example usage:
 * {@code
 *   WidgetStyle style = new WidgetStyle()
 *       .paddingX(20)
 *       .backgroundColor(theme.palette.purple500);
 *   button.setCustomStyle(style);
 * }
 */
public final class WidgetStyle {
    private Integer paddingX;
    private Integer paddingY;
    private Integer cornerRadius;
    private Integer borderWidth;
    private Vector4f backgroundColor;
    private Vector4f borderColor;
    private Vector4f textColor;
    private Integer fontSize;
    private Integer minWidth;
    private Integer minHeight;

    // ===== BUILDER-STYLE SETTERS =====

    public WidgetStyle paddingX(int value) {
        this.paddingX = value;
        return this;
    }

    public WidgetStyle paddingY(int value) {
        this.paddingY = value;
        return this;
    }

    public WidgetStyle cornerRadius(int value) {
        this.cornerRadius = value;
        return this;
    }

    public WidgetStyle borderWidth(int value) {
        this.borderWidth = value;
        return this;
    }

    public WidgetStyle backgroundColor(Vector4f color) {
        this.backgroundColor = color;
        return this;
    }

    public WidgetStyle borderColor(Vector4f color) {
        this.borderColor = color;
        return this;
    }

    public WidgetStyle textColor(Vector4f color) {
        this.textColor = color;
        return this;
    }

    public WidgetStyle fontSize(int value) {
        this.fontSize = value;
        return this;
    }

    public WidgetStyle minWidth(int value) {
        this.minWidth = value;
        return this;
    }

    public WidgetStyle minHeight(int value) {
        this.minHeight = value;
        return this;
    }

    // ===== GETTERS WITH FALLBACK CHAIN =====

    /**
     * Get paddingX with fallback: custom -> theme default.
     */
    public int getPaddingX(Theme theme) {
        return paddingX != null ? paddingX : theme.design.space_md;
    }

    /**
     * Get paddingY with fallback: custom -> theme default.
     */
    public int getPaddingY(Theme theme) {
        return paddingY != null ? paddingY : theme.design.space_sm;
    }

    /**
     * Get corner radius with fallback: custom -> theme default.
     */
    public int getCornerRadius(Theme theme) {
        return cornerRadius != null ? cornerRadius : theme.design.radius_md;
    }

    /**
     * Get border width with fallback: custom -> theme default.
     */
    public int getBorderWidth(Theme theme) {
        return borderWidth != null ? borderWidth : theme.design.border_thin;
    }

    /**
     * Get background color (nullable - null means use computed state color).
     */
    public Vector4f getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Get border color (nullable - null means use computed state color).
     */
    public Vector4f getBorderColor() {
        return borderColor;
    }

    /**
     * Get text color (nullable - null means use computed variant color).
     */
    public Vector4f getTextColor() {
        return textColor;
    }

    /**
     * Get font size with fallback: custom -> theme default.
     */
    public int getFontSize(Theme theme) {
        return fontSize != null ? fontSize : theme.design.font_base;
    }

    /**
     * Get min width with fallback: custom -> 0 (no minimum).
     */
    public int getMinWidth() {
        return minWidth != null ? minWidth : 0;
    }

    /**
     * Get min height with fallback: custom -> theme default.
     */
    public int getMinHeight(Theme theme) {
        return minHeight != null ? minHeight : theme.design.widget_height_md;
    }

    // ===== CHECK IF OVERRIDE EXISTS =====

    public boolean hasBackgroundColor() {
        return backgroundColor != null;
    }

    public boolean hasBorderColor() {
        return borderColor != null;
    }

    public boolean hasTextColor() {
        return textColor != null;
    }
}
