package com.miry.ui.theme;

/**
 * Centralized styling constants used across widgets.
 * Keep these as "design tokens" (spacing/radius/elevation/motion), not per-widget tweaks.
 */
public final class StyleTokens {
    public int padding = 10;
    public int itemHeight = 26;
    public int itemSpacing = 8;
    public int cornerRadius = 6;

    /** Primary UI animation speed used for hover/press transitions. */
    public float animSpeed = 14.0f;

    /** Focus ring thickness in pixels. */
    public int focusRingThickness = 2;

    /** Default shadow size in pixels for elevated surfaces. */
    public int shadowSize = 10;
}

