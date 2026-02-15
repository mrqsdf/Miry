package com.miry.ui.theme;

import com.miry.graphics.Texture;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.vector.VectorIcon;
import com.miry.ui.vector.VectorIcons;

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
            case CHEVRON_LEFT -> "<";
            case CLOSE -> "x";
            case CHECK -> "✓";
            case EYEDROPPER -> "⊙";
            case SELECT -> "S";
            case MOVE -> "M";
            case ROTATE -> "R";
            case SCALE -> "E";
            case LOCK -> "🔒";
            case UNLOCK -> "🔓";
            case VISIBLE -> "👁";
            case INVISIBLE -> "⊘";
            case GRID -> "#";
            case SNAP -> "⊞";
            case SEARCH -> "🔍";
            case ADD -> "+";
            case FOLDER -> "📁";
            case FILE -> "📄";
            case SETTINGS -> "⚙";
            case PLAY -> "▶";
            case PAUSE -> "⏸";
            case STOP -> "⏹";
            case EYE -> "👁";
            case EYE_OFF -> "⊘";
            case CODE -> "{ }";
            case TEXT -> "T";
            case IMAGE -> "🖼";
            case SHADER -> "◆";
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

        VectorIcon vec = vector(icon);
        if (vec != null) {
            float stroke = Math.max(1.25f, size * 0.12f);
            vec.drawStroke(r, x, y, size, stroke, tintArgb);
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

    private static VectorIcon vector(Icon icon) {
        return switch (icon) {
            case CHEVRON_DOWN -> VectorIcons.CHEVRON_DOWN;
            case CHEVRON_RIGHT -> VectorIcons.CHEVRON_RIGHT;
            case CHEVRON_UP -> VectorIcons.CHEVRON_UP;
            case CHEVRON_LEFT -> VectorIcons.CHEVRON_LEFT;
            case CLOSE -> VectorIcons.CLOSE;
            case CHECK -> VectorIcons.CHECK;
            case EYEDROPPER -> VectorIcons.EYEDROPPER;
            case SELECT -> VectorIcons.SELECT;
            case MOVE -> VectorIcons.MOVE;
            case ROTATE -> VectorIcons.ROTATE;
            case SCALE -> VectorIcons.SCALE;
            case LOCK -> VectorIcons.LOCK;
            case UNLOCK -> VectorIcons.UNLOCK;
            case VISIBLE -> VectorIcons.VISIBLE;
            case INVISIBLE -> VectorIcons.INVISIBLE;
            case GRID -> VectorIcons.GRID;
            case SNAP -> VectorIcons.SNAP;
            case SEARCH -> VectorIcons.SEARCH;
            case ADD -> VectorIcons.ADD;
            case FOLDER -> VectorIcons.FOLDER;
            case FILE -> VectorIcons.FILE;
            case SETTINGS -> VectorIcons.SETTINGS;
            case PLAY -> null; // PLAY_TRIANGLE is FilledVectorIcon, handled separately
            case PAUSE -> VectorIcons.PAUSE;
            case STOP -> VectorIcons.STOP;
            case EYE -> VectorIcons.VISIBLE;
            case EYE_OFF -> VectorIcons.INVISIBLE;
            case CODE -> null;
            case TEXT -> null;
            case IMAGE -> null;
            case SHADER -> null;
        };
    }
}
