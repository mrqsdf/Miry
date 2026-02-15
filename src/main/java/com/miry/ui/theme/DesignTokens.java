package com.miry.ui.theme;

/**
 * Comprehensive design token system for UI styling.
 */
public final class DesignTokens {

    /** 4px - compact spacing, tight gaps */
    public int space_xs = 4;
    /** 8px - default small spacing */
    public int space_sm = 8;
    /** 12px - medium spacing */
    public int space_md = 12;
    /** 16px - large spacing */
    public int space_lg = 16;
    /** 24px - extra large spacing */
    public int space_xl = 24;
    /** 32px - maximum spacing for major sections */
    public int space_2xl = 32;

    /** 11px - caption, labels */
    public int font_xs = 11;
    /** 12px - small text, code */
    public int font_sm = 12;
    /** 14px - base UI text (default) */
    public int font_base = 14;
    /** 16px - headings, emphasized text */
    public int font_md = 16;
    /** 20px - large headings */
    public int font_lg = 20;
    /** 24px - titles */
    public int font_xl = 24;

    /** 0px - sharp corners */
    public int radius_none = 0;
    /** 3px - small radius */
    public int radius_sm = 3;
    /** 4px - default radius */
    public int radius_md = 4;
    /** 6px - large radius */
    public int radius_lg = 6;
    /** 8px - extra large radius */
    public int radius_xl = 8;
    /** 9999px - pill shape (fully rounded) */
    public int radius_full = 9999;

    /** 1px - default border */
    public int border_thin = 1;
    /** 2px - emphasis (focus rings) */
    public int border_medium = 2;
    /** 3px - strong emphasis */
    public int border_thick = 3;

    /** 4px - subtle shadow (tooltips) */
    public int shadow_sm = 4;
    /** 8px - default shadow (dropdowns) */
    public int shadow_md = 8;
    /** 16px - elevated shadow (modals) */
    public int shadow_lg = 16;
    /** 24px - dramatic shadow (major overlays) */
    public int shadow_xl = 24;

    /** 24px - compact widget */
    public int widget_height_sm = 24;
    /** 28px - default widget height */
    public int widget_height_md = 28;
    /** 32px - comfortable widget */
    public int widget_height_lg = 32;
    /** 40px - large clickable target */
    public int widget_height_xl = 40;

    /** 0.08s - instant feedback (button press) */
    public float anim_instant = 0.08f;
    /** 0.12s - fast transition (hover states) */
    public float anim_fast = 0.12f;
    /** 0.20s - base transition (most UI) */
    public float anim_base = 0.20f;
    /** 0.35s - slow transition (modals, drawers) */
    public float anim_slow = 0.35f;

    /** Speed multiplier for instant feedback - maps to anim_instant */
    public float animSpeed_instant = 25.0f;  // ~0.08s to 95%
    /** Speed multiplier for fast transitions - maps to anim_fast */
    public float animSpeed_fast = 16.0f;     // ~0.12s to 95%
    /** Speed multiplier for base transitions - maps to anim_base */
    public float animSpeed_base = 10.0f;     // ~0.20s to 95%
    /** Speed multiplier for slow transitions - maps to anim_slow */
    public float animSpeed_slow = 6.0f;      // ~0.35s to 95%

    /** Base layer - normal widgets */
    public int z_base = 0;
    /** Elevated layer - dropdowns */
    public int z_dropdown = 100;
    /** Modal layer - dialogs */
    public int z_modal = 200;
    /** Tooltip layer - always on top */
    public int z_tooltip = 300;

    /** 12px - small icons in text */
    public int icon_xs = 12;
    /** 16px - default icon size */
    public int icon_sm = 16;
    /** 20px - medium icons */
    public int icon_md = 20;
    /** 24px - large icons */
    public int icon_lg = 24;
    /** 32px - extra large icons */
    public int icon_xl = 32;
}
