package com.miry.ui.theme;

/**
 * Optional texture-backed skins used by widgets.
 * <p>
 * When a skin is {@code null}, widgets fall back to flat rectangle rendering.
 */
public final class WidgetSkins {
    /** Skin for standard widgets (buttons, fields, sliders). */
    public NineSlice widget;

    /** Skin for panels and larger surfaces. */
    public NineSlice panel;

    /** Skin for elevated surfaces such as context menus and modals. */
    public NineSlice popup;
}

