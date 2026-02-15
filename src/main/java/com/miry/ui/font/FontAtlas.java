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
 * Manages font texture generation using stb_truetype.
 * <p>
 * This class handles the creation of a texture atlas for a specific font size.
 * It supports standard bitmap coverage (for crisp text) and Signed Distance Fields
 * (SDF) for scalable text.
 */
public final class FontAtlas implements AutoCloseable {

    public enum Mode {
        COVERAGE,
        SDF
    }

    private static final int CHAR_START = 32;
    private static final int CHAR_COUNT = 96; // ASCII 32 to 126
    private static final int SDF_PADDING = 12;
    private static final int COVERAGE_PADDING = 2;

    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Texture texture;
    private final ByteBuffer nativeFontBuffer;

    private final float fontSize;
    private final float pixelScale;
    private final Mode mode;
    private float ascent;
    private float descent;
    private float lineGap;

    public FontAtlas(ByteBuffer fontData, float fontSize, int atlasSize) {
        this(fontData, fontSize, atlasSize, 1.0f, Mode.COVERAGE);
    }

    public FontAtlas(ByteBuffer fontData, float fontSize, int atlasSize, float pixelScale) {
        this(fontData, fontSize, atlasSize, pixelScale, Mode.COVERAGE);
    }

    public FontAtlas(ByteBuffer rawData, float fontSize, int atlasSize, float pixelScale, Mode mode) {
        Objects.requireNonNull(rawData, "Font data cannot be null");

        this.fontSize = fontSize;
        this.pixelScale = Math.max(0.1f, pixelScale);
        this.mode = mode == null ? Mode.COVERAGE : mode;
        validateFontSignature(rawData);
        this.nativeFontBuffer = MemoryUtil.memAlloc(rawData.remaining());
        this.nativeFontBuffer.put(rawData.slice()).flip();

        STBTTFontinfo fontInfo = STBTTFontinfo.malloc();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (!initFontInfo(fontInfo)) {
                throw new IllegalArgumentException("Failed to initialize font info. Data may be corrupt.");
            }

            loadFontMetrics(fontInfo, stack);

            ByteBuffer bitmapData = BufferUtils.createByteBuffer(atlasSize * atlasSize);
            STBTTPackedchar.Buffer packedCharData = STBTTPackedchar.malloc(CHAR_COUNT, stack);

            packFontBitmap(bitmapData, atlasSize, packedCharData, stack);

            extractGlyphs(packedCharData);

            this.texture = createTexture(bitmapData, atlasSize);

        } catch (Exception e) {
            MemoryUtil.memFree(nativeFontBuffer);
            throw new RuntimeException("Failed to create FontAtlas", e);
        } finally {
            fontInfo.free();
        }
    }

    private boolean initFontInfo(STBTTFontinfo fontInfo) {
        int offset = STBTruetype.stbtt_GetFontOffsetForIndex(nativeFontBuffer, 0);
        return offset >= 0 && STBTruetype.stbtt_InitFont(fontInfo, nativeFontBuffer, offset);
    }

    private void loadFontMetrics(STBTTFontinfo fontInfo, MemoryStack stack) {
        float renderSize = fontSize * pixelScale;
        float scaleFactor = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, renderSize);

        IntBuffer ascentBuf = stack.mallocInt(1);
        IntBuffer descentBuf = stack.mallocInt(1);
        IntBuffer lineGapBuf = stack.mallocInt(1);

        STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentBuf, descentBuf, lineGapBuf);

        this.ascent = (ascentBuf.get(0) * scaleFactor) / pixelScale;
        this.descent = (descentBuf.get(0) * scaleFactor) / pixelScale;
        this.lineGap = (lineGapBuf.get(0) * scaleFactor) / pixelScale;
    }

    private void packFontBitmap(ByteBuffer bitmapBuffer, int atlasSize, STBTTPackedchar.Buffer charData, MemoryStack stack) {
        STBTTPackContext packContext = STBTTPackContext.malloc(stack);

        int padding = (mode == Mode.SDF) ? SDF_PADDING : COVERAGE_PADDING;
        float renderSize = fontSize * pixelScale;

        if (!STBTruetype.stbtt_PackBegin(packContext, bitmapBuffer, atlasSize, atlasSize, 0, padding)) {
            throw new IllegalStateException("STB Failed to begin packing context");
        }

        int oversample = getOversampleLevel(renderSize);
        STBTruetype.stbtt_PackSetOversampling(packContext, oversample, oversample);

        if (!STBTruetype.stbtt_PackFontRange(packContext, nativeFontBuffer, 0, renderSize, CHAR_START, charData)) {
            throw new IllegalStateException("Failed to pack font characters into atlas. Atlas size might be too small.");
        }

        STBTruetype.stbtt_PackEnd(packContext);
    }

    private int getOversampleLevel(float pixelFontSize) {
        if (pixelFontSize <= 18.0f) return 4;
        if (pixelFontSize <= 24.0f) return 3;
        return 2;
    }

    private void extractGlyphs(STBTTPackedchar.Buffer packedData) {
        for (int i = 0; i < CHAR_COUNT; i++) {
            STBTTPackedchar info = packedData.get(i);
            int codePoint = CHAR_START + i;

            int atlasX = info.x0();
            int atlasY = info.y0();
            int width = info.x1() - info.x0();
            int height = info.y1() - info.y0();

            Glyph glyph = new Glyph(
                    codePoint,
                    info.xadvance() / pixelScale,
                    info.xoff() / pixelScale,
                    info.yoff() / pixelScale,
                    (info.xoff2() - info.xoff()) / pixelScale,
                    (info.yoff2() - info.yoff()) / pixelScale,
                    atlasX,
                    atlasY,
                    Math.max(0, width),
                    Math.max(0, height)
            );

            glyphs.put(codePoint, glyph);
        }
    }

    private Texture createTexture(ByteBuffer bitmap, int size) {
        if (mode == Mode.SDF) {
            float spread = Math.max(6.0f, Math.min(12.0f, SDF_PADDING - 2.0f));
            ByteBuffer sdfData = DistanceFieldProcessor.generateSdf(bitmap, size, size, spread);
            return Texture.fromGrayscale(sdfData, size, size);
        } else {
            return Texture.fromGrayscale(bitmap, size, size);
        }
    }

    private void validateFontSignature(ByteBuffer data) {
        if (!data.isDirect()) {
            throw new IllegalArgumentException("Font buffer must be direct.");
        }
        if (data.remaining() < 1024) {
            throw new IllegalArgumentException("Font data too small to be valid.");
        }

        int pos = data.position();
        int signature = data.getInt(pos);
        if (data.order() == ByteOrder.LITTLE_ENDIAN) {
            signature = Integer.reverseBytes(signature);
        }

        boolean isValid =
                signature == 0x00010000 || // TTF
                        signature == 0x4F54544F || // OTTO
                        signature == 0x74727565 || // true
                        signature == 0x74746366;   // ttcf

        if (!isValid) {
            throw new IllegalArgumentException(String.format("Invalid font signature: 0x%08X", signature));
        }
    }

    public Glyph getGlyph(int codepoint) {
        return glyphs.getOrDefault(codepoint, glyphs.get((int) '?'));
    }

    public Texture texture() { return texture; }
    public float fontSize() { return fontSize; }
    public float pixelScale() { return pixelScale; }
    public float ascent() { return ascent; }
    public float descent() { return descent; }
    public float lineHeight() { return ascent - descent + lineGap; }
    public boolean isSdf() { return mode == Mode.SDF; }

    @Override
    public void close() {
        freeNativeBuffer(nativeFontBuffer);
        if (texture != null) {
            texture.close();
        }
    }

    private static void freeNativeBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        // Minecraft ships its own LWJGL; avoid linking against a memFree overload that may not exist at runtime.
        try {
            long address = MemoryUtil.memAddress0(buffer);
            if (address != 0L) {
                MemoryUtil.nmemFree(address);
                return;
            }
        } catch (Throwable ignored) {
            // Fall back below.
        }
        try {
            MemoryUtil.memFree(buffer);
        } catch (Throwable ignored) {
            // Give up; leaking is better than crashing the client on shutdown.
        }
    }

    private static class DistanceFieldProcessor {

        static ByteBuffer generateSdf(ByteBuffer coverage, int width, int height, float spread) {
            int len = width * height;
            boolean[] inside = new boolean[len];
            boolean[] outside = new boolean[len];
            ByteBuffer src = coverage.duplicate();
            src.rewind();

            for (int i = 0; i < len; i++) {
                boolean isInside = (Byte.toUnsignedInt(src.get()) >= 128);
                inside[i] = isInside;
                outside[i] = !isInside;
            }

            float[] distInside = computeEdt(inside, width, height);
            float[] distOutside = computeEdt(outside, width, height);

            ByteBuffer out = BufferUtils.createByteBuffer(len);
            for (int i = 0; i < len; i++) {
                float signedDist = distOutside[i] - distInside[i];

                float alpha = 0.5f + (signedDist / (2.0f * spread));

                int byteVal = (int) (Math.min(1.0f, Math.max(0.0f, alpha)) * 255.0f);
                out.put((byte) byteVal);
            }
            out.flip();
            return out;
        }


        private static float[] computeEdt(boolean[] grid, int width, int height) {
            float[] distances = new float[width * height];
            float[] f = new float[Math.max(width, height)]; // Buffer for 1D pass
            final float INF = 1e20f;

            for (int i = 0; i < distances.length; i++) {
                distances[i] = grid[i] ? 0.0f : INF;
            }

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    f[y] = distances[y * width + x];
                }
                float[] d = calculate1D_EDT(f, height);
                for (int y = 0; y < height; y++) {
                    distances[y * width + x] = d[y];
                }
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    f[x] = distances[y * width + x];
                }
                float[] d = calculate1D_EDT(f, width);
                for (int x = 0; x < width; x++) {
                    distances[y * width + x] = (float) Math.sqrt(d[x]);
                }
            }

            return distances;
        }

        /**
         * 1D squared distance transform.
         */
        private static float[] calculate1D_EDT(float[] gridValues, int size) {
            float[] finalDistances = new float[size];
            int[] parabolas = new int[size];
            float[] boundaries = new float[size + 1];

            int count = 0;
            parabolas[0] = 0;
            boundaries[0] = -Float.MAX_VALUE;
            boundaries[1] = Float.MAX_VALUE;
            for (int i = 1; i < size; i++) {
                float intersection = calculateIntersection(gridValues, parabolas[count], i);

                while (intersection <= boundaries[count]) {
                    count--;
                    intersection = calculateIntersection(gridValues, parabolas[count], i);
                }

                // Add the new parabola
                count++;
                parabolas[count] = i;
                boundaries[count] = intersection;
                boundaries[count + 1] = Float.MAX_VALUE;
            }

            int currentParabola = 0;
            for (int i = 0; i < size; i++) {
                while (boundaries[currentParabola + 1] < i) {
                    currentParabola++;
                }

                int sourceIndex = parabolas[currentParabola];
                float dx = i - sourceIndex;
                finalDistances[i] = (dx * dx) + gridValues[sourceIndex];
            }

            return finalDistances;
        }
        private static float calculateIntersection(float[] values, int p, int q) {
            float valueP = values[p] + (p * p);
            float valueQ = values[q] + (q * q);
            return (valueQ - valueP) / (2.0f * (q - p));
        }
    }
}
