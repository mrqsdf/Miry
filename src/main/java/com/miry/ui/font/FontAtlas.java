package com.miry.ui.font;

import com.miry.graphics.Texture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Font atlas backed by stb_truetype packing.
 * <p>
 * Produces a single-channel coverage texture with glyph metrics used by {@link TextRenderer}.
 */
public final class FontAtlas implements AutoCloseable {
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Texture texture;
    private final float fontSize;
    private final float ascent;
    private final float descent;
    private final float lineGap;
    private final float pixelScale;

    public FontAtlas(ByteBuffer fontData, float fontSize, int atlasSize) {
        this(fontData, fontSize, atlasSize, 1.0f);
    }

    public FontAtlas(ByteBuffer fontData, float fontSize, int atlasSize, float pixelScale) {
        Objects.requireNonNull(fontData, "fontData");
        this.fontSize = fontSize;
        this.pixelScale = Math.max(0.1f, pixelScale);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBTTFontinfo fontInfo = STBTTFontinfo.malloc(stack);
            validateFontDataLooksReasonable(fontData);
            int offset = STBTruetype.stbtt_GetFontOffsetForIndex(fontData, 0);
            if (offset < 0 || !STBTruetype.stbtt_InitFont(fontInfo, fontData, offset)) {
                throw new IllegalArgumentException("Failed to initialize font (invalid or unsupported font data)");
            }

            float pixelFontSize = fontSize * this.pixelScale;
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixelFontSize);
            IntBuffer ascentBuf = stack.mallocInt(1);
            IntBuffer descentBuf = stack.mallocInt(1);
            IntBuffer lineGapBuf = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentBuf, descentBuf, lineGapBuf);
            this.ascent = (ascentBuf.get(0) * scale) / this.pixelScale;
            this.descent = (descentBuf.get(0) * scale) / this.pixelScale;
            this.lineGap = (lineGapBuf.get(0) * scale) / this.pixelScale;

            ByteBuffer bitmap = BufferUtils.createByteBuffer(atlasSize * atlasSize);
            MemoryUtil.memSet(bitmap, 0);
            STBTTPackContext packContext = STBTTPackContext.malloc(stack);
            if (!STBTruetype.stbtt_PackBegin(packContext, bitmap, atlasSize, atlasSize, 0, 2)) {
                throw new IllegalStateException("Failed to begin font packing");
            }
            STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);

            int charCount = 256;
            STBTTPackedchar.Buffer charData = STBTTPackedchar.malloc(charCount, stack);
            if (!STBTruetype.stbtt_PackFontRange(packContext, fontData, 0, pixelFontSize, 32, charData)) {
                throw new IllegalStateException("Failed to pack font range");
            }

            for (int i = 0; i < charCount; i++) {
                STBTTPackedchar cd = charData.get(i);
                int codepoint = 32 + i;
                int ax0 = cd.x0() & 0xFFFF;
                int ay0 = cd.y0() & 0xFFFF;
                int ax1 = cd.x1() & 0xFFFF;
                int ay1 = cd.y1() & 0xFFFF;
                Glyph g = new Glyph(
                    codepoint,
                    cd.xadvance() / this.pixelScale,
                    cd.xoff() / this.pixelScale,
                    cd.yoff() / this.pixelScale,
                    (cd.xoff2() - cd.xoff()) / this.pixelScale,
                    (cd.yoff2() - cd.yoff()) / this.pixelScale,
                    ax0,
                    ay0,
                    Math.max(0, ax1 - ax0),
                    Math.max(0, ay1 - ay0)
                );
                glyphs.put(codepoint, g);
            }

            STBTruetype.stbtt_PackEnd(packContext);
            texture = Texture.fromGrayscale(bitmap, atlasSize, atlasSize);
        }
    }

    private static void validateFontDataLooksReasonable(ByteBuffer fontData) {
        if (!fontData.isDirect()) {
            throw new IllegalArgumentException("Font data must be a direct ByteBuffer");
        }
        if (fontData.remaining() < 4) {
            throw new IllegalArgumentException("Font data is too small (" + fontData.remaining() + " bytes)");
        }

        ByteBuffer dup = fontData.duplicate().order(ByteOrder.BIG_ENDIAN);
        int signature = dup.getInt(dup.position());
        boolean ok =
            signature == 0x00010000 || // TrueType
                signature == 0x4F54544F || // "OTTO" OpenType/CFF
                signature == 0x74727565 || // "true"
                signature == 0x74746366;   // "ttcf" TrueType collection

        if (!ok) {
            throw new IllegalArgumentException("Font data does not look like TTF/OTF (signature=0x" + Integer.toHexString(signature) + ")");
        }
    }

    public Glyph getGlyph(int codepoint) {
        return glyphs.getOrDefault(codepoint, glyphs.get((int) ' '));
    }

    public Texture texture() {
        return texture;
    }

    public float fontSize() {
        return fontSize;
    }

    public float ascent() {
        return ascent;
    }

    public float descent() {
        return descent;
    }

    public float lineHeight() {
        return ascent - descent + lineGap;
    }

    public float pixelScale() {
        return pixelScale;
    }

    @Override
    public void close() {
        texture.close();
    }
}
