package com.miry.ui.font;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple word-wrapping / line breaking helper for multi-line text.
 */
public final class TextLayout {
    public enum Align { LEFT, CENTER, RIGHT }

    private final FontAtlas atlas;

    public TextLayout(FontAtlas atlas) {
        this.atlas = atlas;
    }

    public List<Line> layoutText(String text, float maxWidth, Align align) {
        List<Line> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs) {
            lines.addAll(wrapParagraph(para, maxWidth, align));
        }
        return lines;
    }

    private List<Line> wrapParagraph(String para, float maxWidth, Align align) {
        List<Line> lines = new ArrayList<>();
        if (para.isEmpty()) {
            lines.add(new Line("", 0.0f, 0.0f));
            return lines;
        }

        String[] words = para.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float currentWidth = 0.0f;

        for (String word : words) {
            float wordWidth = measureText(word);
            float spaceWidth = measureText(" ");

            if (currentLine.length() == 0) {
                currentLine.append(word);
                currentWidth = wordWidth;
            } else if (currentWidth + spaceWidth + wordWidth <= maxWidth) {
                currentLine.append(" ").append(word);
                currentWidth += spaceWidth + wordWidth;
            } else {
                String lineText = currentLine.toString();
                float offsetX = computeOffsetX(currentWidth, maxWidth, align);
                lines.add(new Line(lineText, currentWidth, offsetX));
                currentLine = new StringBuilder(word);
                currentWidth = wordWidth;
            }
        }

        if (currentLine.length() > 0) {
            String lineText = currentLine.toString();
            float offsetX = computeOffsetX(currentWidth, maxWidth, align);
            lines.add(new Line(lineText, currentWidth, offsetX));
        }

        return lines;
    }

    private float measureText(String text) {
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)) {
                i++;
            }
            Glyph g = atlas.getGlyph(cp);
            if (g != null) {
                width += g.advanceX();
            }
        }
        return width;
    }

    private float computeOffsetX(float lineWidth, float maxWidth, Align align) {
        return switch (align) {
            case CENTER -> (maxWidth - lineWidth) * 0.5f;
            case RIGHT -> maxWidth - lineWidth;
            default -> 0.0f;
        };
    }

    public record Line(String text, float width, float offsetX) {}
}
