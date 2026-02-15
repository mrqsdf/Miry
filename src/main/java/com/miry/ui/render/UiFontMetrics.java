package com.miry.ui.render;

/**
 * Minimal font metrics abstraction for UI layout.
 * <p>
 * The draw-list renderer needs metrics but must not depend on a concrete OpenGL text renderer.
 */
public interface UiFontMetrics {
    float measureText(String text);

    float lineHeight();

    float ascent();
}

