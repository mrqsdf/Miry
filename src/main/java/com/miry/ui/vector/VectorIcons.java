package com.miry.ui.vector;

/**
 * A tiny set of built-in stroke icons (viewBox 16x16).
 */
public final class VectorIcons {
    private VectorIcons() {}

    // Navigation
    public static final VectorIcon CLOSE = icon16("M3 3 L13 13 M13 3 L3 13");
    public static final VectorIcon CHEVRON_DOWN = icon16("M4 6 L8 10 L12 6");
    public static final VectorIcon CHEVRON_RIGHT = icon16("M6 4 L10 8 L6 12");
    public static final VectorIcon CHEVRON_UP = icon16("M4 10 L8 6 L12 10");
    public static final VectorIcon CHEVRON_LEFT = icon16("M10 4 L6 8 L10 12");
    public static final VectorIcon CHECK = icon16("M3 8 L7 12 L13 4");

    // Tools
    public static final VectorIcon SELECT = icon16("M2 2 L6 2 M2 2 L2 6 M14 2 L10 2 M14 2 L14 6 M2 14 L2 10 M2 14 L6 14 M14 14 L14 10 M14 14 L10 14");
    public static final VectorIcon MOVE = icon16("M8 2 L8 14 M8 2 L6 4 M8 2 L10 4 M8 14 L6 12 M8 14 L10 12 M2 8 L14 8 M2 8 L4 6 M2 8 L4 10 M14 8 L12 6 M14 8 L12 10");
    public static final VectorIcon ROTATE = icon16("M8 3 A5 5 0 1 1 8 13 M8 3 L6 1 M8 3 L10 1");
    public static final VectorIcon SCALE = icon16("M2 2 L6 2 L6 6 L2 6 Z M10 10 L14 10 L14 14 L10 14 Z M6 6 L10 10");

    // Visibility
    public static final VectorIcon LOCK = icon16("M5 7 L5 5 A3 3 0 0 1 11 5 L11 7 M4 7 L12 7 L12 13 L4 13 Z");
    public static final VectorIcon UNLOCK = icon16("M5 7 L5 5 A3 3 0 0 1 11 5 M4 7 L12 7 L12 13 L4 13 Z");
    public static final VectorIcon VISIBLE = icon16("M8 5 A6 3 0 0 1 14 8 A6 3 0 0 1 8 11 A6 3 0 0 1 2 8 A6 3 0 0 1 8 5 M8 6.5 A1.5 1.5 0 1 1 8 9.5 A1.5 1.5 0 1 1 8 6.5");
    public static final VectorIcon INVISIBLE = icon16("M8 5 A6 3 0 0 1 14 8 M8 11 A6 3 0 0 1 2 8 M3 3 L13 13");

    // Grid/Snap
    public static final VectorIcon GRID = icon16("M2 2 L14 2 L14 14 L2 14 Z M2 8 L14 8 M8 2 L8 14");
    public static final VectorIcon SNAP = icon16("M8 3 L10 5 L13 2 M3 8 L5 10 L2 13 M13 8 L11 10 L14 13 M8 13 L6 11 L3 14");

    // Common
    public static final VectorIcon SEARCH = icon16("M7 3 A4 4 0 1 1 7 11 A4 4 0 1 1 7 3 M10 10 L14 14");
    public static final VectorIcon ADD = icon16("M8 3 L8 13 M3 8 L13 8");
    public static final VectorIcon FOLDER = icon16("M2 4 L2 13 L14 13 L14 6 L7 6 L6 4 Z");
    public static final VectorIcon FILE = icon16("M4 2 L10 2 L13 5 L13 14 L4 14 Z M10 2 L10 5 L13 5");
    public static final VectorIcon SETTINGS = icon16("M8 5 A3 3 0 1 1 8 11 A3 3 0 1 1 8 5 M8 2 L8 4 M8 12 L8 14 M2 8 L4 8 M12 8 L14 8 M4.5 4.5 L5.5 5.5 M10.5 10.5 L11.5 11.5 M4.5 11.5 L5.5 10.5 M10.5 5.5 L11.5 4.5");
    public static final VectorIcon EYEDROPPER = icon16("M3 13 L9 7 L13 11 M9 7 L7 5 L11 1");

    // Playback
    public static final FilledVectorIcon PLAY_TRIANGLE = filled16("M5 3 L13 8 L5 13 Z");
    public static final VectorIcon PAUSE = icon16("M5 3 L5 13 M11 3 L11 13");
    public static final VectorIcon STOP = icon16("M4 4 L12 4 L12 12 L4 12 Z");

    private static VectorIcon icon16(String d) {
        return new VectorIcon(16.0f, 16.0f, SvgPath.parseAndFlatten(d, 0.35f));
    }

    private static FilledVectorIcon filled16(String d) {
        return new FilledVectorIcon(16.0f, 16.0f, SvgPath.parseAndFlatten(d, 0.35f));
    }
}
