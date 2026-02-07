package com.miry.ui.font;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal rich text container with per-span styling.
 */
public final class RichText {
    public enum Style { NORMAL, BOLD, ITALIC, BOLD_ITALIC }

    private final List<Span> spans = new ArrayList<>();

    public RichText append(String text, int color, Style style) {
        spans.add(new Span(text, color, style));
        return this;
    }

    public RichText append(String text, int color) {
        return append(text, color, Style.NORMAL);
    }

    public List<Span> spans() {
        return spans;
    }

    public void clear() {
        spans.clear();
    }

    public record Span(String text, int color, Style style) {}
}
