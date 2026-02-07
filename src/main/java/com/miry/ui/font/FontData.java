package com.miry.ui.font;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads font data into a direct {@link java.nio.ByteBuffer} for stb_truetype.
 */
public final class FontData {
    private FontData() {
    }

    public static ByteBuffer loadDefault() {
        String overridePath = System.getenv("MIRY_FONT_PATH");
        if (overridePath == null || overridePath.isBlank()) {
            // Back-compat for earlier builds.
            overridePath = System.getenv("MYRI_FONT_PATH");
        }
        if (overridePath == null || overridePath.isBlank()) {
            // Back-compat for earlier builds.
            overridePath = System.getenv("FLUX_FONT_PATH");
        }
        if (overridePath != null && !overridePath.isBlank()) {
            return loadFromFile(Path.of(overridePath.trim()));
        }

        ByteBuffer fromResources = tryLoadFromResource("/fonts/default.ttf");
        if (fromResources != null) {
            return fromResources;
        }

        for (Path candidate : defaultSystemFontCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return loadFromFile(candidate);
            }
        }

        throw new IllegalStateException(
            "No default font found. Set MIRY_FONT_PATH (or MYRI_FONT_PATH / FLUX_FONT_PATH) to a .ttf/.otf file path, or add a classpath resource at /fonts/default.ttf."
        );
    }

    public static ByteBuffer loadFromFile(Path path) {
        Objects.requireNonNull(path, "path");
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read font file: " + path, e);
        }
        return toDirectByteBuffer(bytes);
    }

    public static ByteBuffer loadFromResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        ByteBuffer buffer = tryLoadFromResource(resourcePath);
        if (buffer == null) {
            throw new IllegalArgumentException("Font resource not found: " + resourcePath);
        }
        return buffer;
    }

    private static ByteBuffer tryLoadFromResource(String resourcePath) {
        try (InputStream in = FontData.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return toDirectByteBuffer(in.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read font resource: " + resourcePath, e);
        }
    }

    private static ByteBuffer toDirectByteBuffer(byte[] bytes) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    private static List<Path> defaultSystemFontCandidates() {
        List<Path> candidates = new ArrayList<>();

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            candidates.add(Path.of("/usr/share/fonts/noto/NotoSans-Regular.ttf"));
            candidates.add(Path.of("/usr/share/fonts/liberation/LiberationSans-Regular.ttf"));
            candidates.add(Path.of("/usr/share/fonts/TTF/DejaVuSans.ttf"));
            candidates.add(Path.of("/usr/share/fonts/TTF/DejaVuSansCondensed.ttf"));
        } else if (os.contains("mac")) {
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Verdana.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/SFNS.ttf"));
        } else if (os.contains("win")) {
            String windir = System.getenv("WINDIR");
            if (windir != null && !windir.isBlank()) {
                candidates.add(Path.of(windir, "Fonts", "arial.ttf"));
                candidates.add(Path.of(windir, "Fonts", "segoeui.ttf"));
                candidates.add(Path.of(windir, "Fonts", "tahoma.ttf"));
            }
        }

        return candidates;
    }
}
