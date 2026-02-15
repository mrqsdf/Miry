package com.miry.ui.theme;

/**
 * Visual variants for widgets.
 *
 * Each variant has distinct colors and visual treatment:
 * - DEFAULT: Standard widget appearance (gray)
 * - PRIMARY: Emphasized action (blue accent)
 * - SECONDARY: De-emphasized variant (lighter gray)
 * - DANGER: Destructive action (red)
 * - SUCCESS: Positive confirmation (green)
 * - GHOST: Borderless, transparent until hover
 */
public enum WidgetVariant {
    /** Standard widget appearance - uses theme default colors */
    DEFAULT,

    /** Primary action - blue accent colors */
    PRIMARY,

    /** Secondary action - muted appearance */
    SECONDARY,

    /** Dangerous/destructive action - red accent */
    DANGER,

    /** Success/confirmation action - green accent */
    SUCCESS,

    /** Minimal appearance - transparent background until hover */
    GHOST
}
