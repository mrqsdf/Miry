package com.miry.ui.theme;

/**
 * Centralized styling constants used across widgets.
 * Keep these as "design tokens" (spacing/radius/elevation/motion), not per-widget tweaks.
 *
 * @deprecated Use {@link DesignTokens} via theme.design instead.
 * This class is kept for backward compatibility and delegates to DesignTokens.
 */
@Deprecated
public final class StyleTokens {
    private final DesignTokens design;

    /** @deprecated Use theme.design.space_md */
    @Deprecated
    public int padding;

    /** @deprecated Use theme.design.widget_height_md */
    @Deprecated
    public int itemHeight;

    /** @deprecated Use theme.design.space_sm */
    @Deprecated
    public int itemSpacing;

    /** @deprecated Use theme.design.radius_md */
    @Deprecated
    public int cornerRadius;

    /** Primary UI animation speed used for hover/press transitions.
     * @deprecated Use theme.design.animSpeed_base */
    @Deprecated
    public float animSpeed;

    /** Focus ring thickness in pixels.
     * @deprecated Use theme.design.border_medium */
    @Deprecated
    public int focusRingThickness;

    /** Default shadow size in pixels for elevated surfaces.
     * @deprecated Use theme.design.shadow_md */
    @Deprecated
    public int shadowSize;

    public StyleTokens() {
        this(new DesignTokens());
    }

    public StyleTokens(DesignTokens design) {
        this.design = design == null ? new DesignTokens() : design;
        padding = this.design.space_md;
        itemHeight = this.design.widget_height_md;
        itemSpacing = this.design.space_sm;
        cornerRadius = this.design.radius_md;
        animSpeed = this.design.animSpeed_base;
        focusRingThickness = this.design.border_medium;
        shadowSize = this.design.shadow_md;
    }
}
