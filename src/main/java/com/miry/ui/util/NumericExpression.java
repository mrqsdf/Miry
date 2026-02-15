package com.miry.ui.util;

/**
 * Small expression evaluator for numeric UI fields.
 * <p>
 * Supported:
 * <ul>
 *   <li>Operators: {@code + - * /}</li>
 *   <li>Parentheses</li>
 *   <li>Unary {@code + -}</li>
 *   <li>Constant: {@code pi}</li>
 *   <li>Optional unit suffixes on numbers (opt-in by {@link UnitKind})</li>
 * </ul>
 */
public final class NumericExpression {
    private NumericExpression() {}

    public enum UnitKind {
        NONE,
        LENGTH_METERS,
        ANGLE_RADIANS,
        TIME_SECONDS
    }

    public static double evaluate(String input, UnitKind unitKind) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        Parser p = new Parser(input, unitKind == null ? UnitKind.NONE : unitKind);
        double v = p.parseExpression();
        p.skipWs();
        if (!p.eof()) {
            throw new IllegalArgumentException("Unexpected token at " + p.pos);
        }
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Non-finite result");
        }
        return v;
    }

    private static final class Parser {
        private final String s;
        private final UnitKind unitKind;
        private int pos;

        Parser(String s, UnitKind unitKind) {
            this.s = s;
            this.unitKind = unitKind;
        }

        boolean eof() {
            return pos >= s.length();
        }

        void skipWs() {
            while (!eof()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    return;
                }
            }
        }

        double parseExpression() {
            skipWs();
            double v = parseTerm();
            while (true) {
                skipWs();
                if (match('+')) {
                    v += parseTerm();
                } else if (match('-')) {
                    v -= parseTerm();
                } else {
                    return v;
                }
            }
        }

        double parseTerm() {
            skipWs();
            double v = parseFactor();
            while (true) {
                skipWs();
                if (match('*')) {
                    v *= parseFactor();
                } else if (match('/')) {
                    v /= parseFactor();
                } else {
                    return v;
                }
            }
        }

        double parseFactor() {
            skipWs();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            return parsePrimary();
        }

        double parsePrimary() {
            skipWs();
            if (match('(')) {
                double v = parseExpression();
                skipWs();
                if (!match(')')) {
                    throw new IllegalArgumentException("Expected ')'");
                }
                return v;
            }

            if (matchIdent("pi")) {
                return Math.PI;
            }

            return parseNumberWithUnit();
        }

        double parseNumberWithUnit() {
            skipWs();
            int start = pos;
            boolean sawDigit = false;
            while (!eof()) {
                char c = s.charAt(pos);
                if (c >= '0' && c <= '9') {
                    sawDigit = true;
                    pos++;
                    continue;
                }
                if (c == '.' || c == 'e' || c == 'E' || c == '+'
                    || c == '-') {
                    // Let Double.parseDouble validate exponent sign placement; we just capture a token.
                    pos++;
                    continue;
                }
                break;
            }
            if (!sawDigit) {
                throw new IllegalArgumentException("Expected number");
            }
            String numToken = s.substring(start, pos);
            double value;
            try {
                value = Double.parseDouble(numToken);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + numToken);
            }

            skipWs();
            int unitStart = pos;
            while (!eof()) {
                char c = s.charAt(pos);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    pos++;
                } else {
                    break;
                }
            }
            String unit = s.substring(unitStart, pos);
            if (unit.isEmpty()) {
                return value;
            }

            return value * unitMultiplier(unitKind, unit);
        }

        double unitMultiplier(UnitKind kind, String unitRaw) {
            String unit = unitRaw.toLowerCase();
            return switch (kind) {
                case NONE -> throw new IllegalArgumentException("Unexpected unit: " + unitRaw);
                case LENGTH_METERS -> switch (unit) {
                    case "m" -> 1.0;
                    case "cm" -> 0.01;
                    case "mm" -> 0.001;
                    case "km" -> 1000.0;
                    default -> throw new IllegalArgumentException("Unknown length unit: " + unitRaw);
                };
                case ANGLE_RADIANS -> switch (unit) {
                    case "rad" -> 1.0;
                    case "deg" -> Math.PI / 180.0;
                    default -> throw new IllegalArgumentException("Unknown angle unit: " + unitRaw);
                };
                case TIME_SECONDS -> switch (unit) {
                    case "s" -> 1.0;
                    case "ms" -> 0.001;
                    default -> throw new IllegalArgumentException("Unknown time unit: " + unitRaw);
                };
            };
        }

        boolean match(char c) {
            if (!eof() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        boolean matchIdent(String ident) {
            skipWs();
            int n = ident.length();
            if (pos + n > s.length()) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                char a = Character.toLowerCase(s.charAt(pos + i));
                char b = Character.toLowerCase(ident.charAt(i));
                if (a != b) {
                    return false;
                }
            }
            int end = pos + n;
            if (end < s.length()) {
                char next = s.charAt(end);
                if ((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z')) {
                    return false;
                }
            }
            pos = end;
            return true;
        }
    }
}

