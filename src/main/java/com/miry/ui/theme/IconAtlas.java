package com.miry.ui.theme;

import com.miry.graphics.Texture;
import com.miry.ui.render.UiRenderer;

import java.util.EnumMap;
import java.util.Map;

/**
 * Minimal iconography abstraction.
 *
 * For now icons are represented by simple ASCII glyphs so the UI can be consistent without
 * introducing image loading/build tooling.
 * <p>
 * This class also supports a texture-backed atlas for editor-grade iconography. When an icon sprite
 * is registered, {@link #draw(UiRenderer, Icon, float, float, float, int)} will render the sprite;
 * otherwise it falls back to the glyph mapping.
 */
public final class IconAtlas {
    private final Map<Icon, IconSprite> sprites = new EnumMap<>(Icon.class);
    private Texture texture;

    /**
     * Sets the atlas texture used by subsequently registered sprites.
     */
    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Texture texture() {
        return texture;
    }

    /**
     * Registers an icon sprite using normalized UVs (0..1) in the atlas texture.
     */
    public void registerSprite(Icon icon, float u0, float v0, float u1, float v1) {
        if (icon == null || texture == null) {
            return;
        }
        sprites.put(icon, new IconSprite(texture, u0, v0, u1, v1));
    }

    public IconSprite sprite(Icon icon) {
        return icon == null ? null : sprites.get(icon);
    }

    public String glyph(Icon icon) {
        return switch (icon) {
            case CHEVRON_DOWN -> "v";
            case CHEVRON_RIGHT -> ">";
            case CHEVRON_UP -> "^";
            case CLOSE -> "x";
            case CHECK -> "✓";
        };
    }

    /**
     * Draws an icon centered inside the provided square.
     */
    public void draw(UiRenderer r, Icon icon, float x, float y, float size, int tintArgb) {
        if (r == null || icon == null) {
            return;
        }

        IconSprite sprite = sprites.get(icon);
        if (sprite != null && sprite.texture() != null) {
            r.drawTexturedRect(sprite.texture(), x, y, size, size, sprite.u0(), sprite.v0(), sprite.u1(), sprite.v1(), tintArgb);
            return;
        }

        String g = glyph(icon);
        if (g == null || g.isEmpty()) {
            return;
        }
        float baseline = r.baselineForBox(y, size);
        float gx = x + (size - r.measureText(g)) * 0.5f;
        r.drawText(g, gx, baseline, tintArgb);
    }
}
