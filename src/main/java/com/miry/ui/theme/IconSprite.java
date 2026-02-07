package com.miry.ui.theme;

import com.miry.graphics.Texture;

/**
 * A single icon sprite stored in an atlas texture.
 *
 * @param texture atlas texture
 * @param u0 left UV (0..1)
 * @param v0 top UV (0..1)
 * @param u1 right UV (0..1)
 * @param v1 bottom UV (0..1)
 */
public record IconSprite(Texture texture, float u0, float v0, float u1, float v1) {}

