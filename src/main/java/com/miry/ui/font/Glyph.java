package com.miry.ui.font;

/**
 * Cached glyph metrics and atlas location for a single codepoint.
 */
public record Glyph(
    int codepoint,
    float advanceX,
    float bearingX,
    float bearingY,
    float renderWidth,
    float renderHeight,
    int atlasX,
    int atlasY,
    int atlasWidth,
    int atlasHeight
) {}
