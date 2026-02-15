package com.miry.ui.util;

import com.miry.ui.render.UiRenderer;

/**
 * Text processing utilities.
 */
public final class TextUtils {
    private TextUtils() {
        throw new AssertionError("No instances");
    }

    private static final String ELLIPSIS = "…";

    /**
     * Truncates text to fit within a maximum width, adding ellipsis if needed.
     * Uses binary search to find the optimal truncation point.
     *
     * @param renderer renderer to measure text width
     * @param text text to truncate
     * @param maxWidth maximum width in pixels
     * @return truncated text with ellipsis, or original text if it fits
     */
    public static String truncateToWidth(UiRenderer renderer, String text, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (maxWidth <= 0.0f) {
            return "";
        }
        if (renderer.measureText(text) <= maxWidth) {
            return text;
        }

        float ellW = renderer.measureText(ELLIPSIS);
        if (ellW >= maxWidth) {
            return "";
        }

        // Binary search for optimal truncation point
        int lo = 0;
        int hi = text.length();
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            String s = text.substring(0, mid) + ELLIPSIS;
            if (renderer.measureText(s) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return text.substring(0, lo) + ELLIPSIS;
    }

    /**
     * Escapes special characters for serialization.
     * Escapes: \ \t \n \r
     *
     * @param s string to escape
     * @return escaped string
     */
    public static String escape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Unescapes special characters from serialization.
     * Handles: \\ \t \n \r
     *
     * @param s string to unescape
     * @return unescaped string
     */
    public static String unescape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't' -> out.append('\t');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case '\\' -> out.append('\\');
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Parses an integer with a fallback default value on error.
     *
     * @param s string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed integer or default
     */
    public static int parseIntOr(String s, int defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a float with a fallback default value on error.
     *
     * @param s string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed float or default
     */
    public static float parseFloatOr(String s, float defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a double with a fallback default value on error.
     *
     * @param s string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed double or default
     */
    public static double parseDoubleOr(String s, double defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses a boolean with a fallback default value on error.
     *
     * @param s string to parse
     * @param defaultValue default value if parsing fails
     * @return parsed boolean or default
     */
    public static boolean parseBooleanOr(String s, boolean defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        String trimmed = s.trim().toLowerCase();
        if (trimmed.equals("true") || trimmed.equals("1") || trimmed.equals("yes")) {
            return true;
        }
        if (trimmed.equals("false") || trimmed.equals("0") || trimmed.equals("no")) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Checks if a string is null or empty.
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or only whitespace.
     */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Returns the string or a default value if it's null or empty.
     */
    public static String orDefault(String s, String defaultValue) {
        return isEmpty(s) ? defaultValue : s;
    }
}
