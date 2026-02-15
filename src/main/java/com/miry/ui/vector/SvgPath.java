package com.miry.ui.vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Robust SVG path parser.
 * <p>
 * This implementation is a port of the C-based (https://github.com/nilostolte/SVGPathParser/blob/main/src/SVGparser.c)
 * It parses the "d" attribute, handles all standard commands (M, L, H, V, C, S, Q, T, A, Z),
 * converts relative coordinates to absolute, and flattens curves into line segments.
 */
public final class SvgPath {
    private SvgPath() {}

    public static VectorPath parseAndFlatten(String d, float flatness) {
        if (d == null || d.isBlank()) {
            return new VectorPath(List.of());
        }
        float tol = Math.max(0.05f, flatness);
        Parser p = new Parser(d);
        return p.parse(tol);
    }

    private static final class Parser {
        private final String s;
        private final int len;
        private int idx;

        private float cpx;
        private float cpy;

        private float cpx2;
        private float cpy2;

        private float startX;
        private float startY;
        private boolean isSubpathStarted = false;

        private final List<VectorPath.Contour> out = new ArrayList<>();
        private final FloatList cur = new FloatList();
        private boolean curClosed;

        Parser(String s) {
            this.s = s;
            this.len = s.length();
            this.idx = 0;
        }

        VectorPath parse(float tol) {
            char cmd = 0;
            // Arguments buffer (max arguments for an SVG command is 7 for Arc)
            float[] args = new float[10];
            int nargs = 0;
            int rargs = 0; // required args for current cmd

            while (idx < len) {
                skipWs();
                if (idx >= len) break;

                char c = s.charAt(idx);

                // Check if the character is a command letter
                if (isCmd(c)) {
                    cmd = c;
                    idx++;
                    // Reset arg counter for new command
                    nargs = 0;
                    rargs = getArgsPerCommand(cmd);
                    if (rargs == -1) {
                        // Unknown command, stop or skip
                        cmd = 0;
                        continue;
                    }
                } else if (cmd == 0) {
                    // No command active and not a command letter? bail.
                    break;
                }

                // Parse arguments for the command
                // If we haven't filled the required arguments yet, keep parsing numbers
                if (rargs > 0) {
                    // We need to parse rargs numbers
                    // However, SVG allows multiple sets of arguments for one command
                    // e.g., L 10 10 20 20 is implicitly L 10 10 L 20 20.
                    // The loop below handles collecting arguments and executing.
                }

                // Logic port from nsvg__parsePath loop
                while (true) {
                    // If we have enough arguments for the current command, execute it
                    if (nargs >= rargs && rargs > 0) {
                        executeCommand(cmd, args, tol);

                        // Handle Implicit Commands
                        // If M/m is followed by more numbers, it becomes L/l
                        if (cmd == 'M') { cmd = 'L'; rargs = 2; }
                        else if (cmd == 'm') { cmd = 'l'; rargs = 2; }

                        // Prepare for next set of arguments (e.g. polyline L x y x y...)
                        // For M/L/C/etc, we just reset the count.
                        // However, for smooth curves, the "last control point" logic relies on
                        // the previous execution.
                        nargs = 0;
                    }

                    // Break if we are at a command letter or EOF
                    skipCommaWs();
                    if (idx >= len) break;
                    if (isCmd(s.charAt(idx))) break;

                    // Parse next number
                    if (nargs < args.length) {
                        args[nargs++] = nextFloat();
                    } else {
                        // Too many arguments or overflow, consume to prevent stuck loop
                        nextFloat();
                    }
                }

                // Handle Z (Close Path) specifically as it has 0 args
                if (cmd == 'Z' || cmd == 'z') {
                    executeCommand(cmd, args, tol);
                    // Z resets the "last command" context for control points
                    cmd = 0;
                }
            }

            flushContour();
            return new VectorPath(out);
        }

        private void executeCommand(char cmd, float[] args, float tol) {
            boolean rel = (cmd >= 'a' && cmd <= 'z');

            switch (cmd) {
                case 'M', 'm' -> {
                    float x = args[0];
                    float y = args[1];
                    if (rel) { x += cpx; y += cpy; }
                    moveTo(x, y);
                    // M command resets the secondary control point to the point itself
                    cpx2 = x; cpy2 = y;
                }
                case 'L', 'l' -> {
                    float x = args[0];
                    float y = args[1];
                    if (rel) { x += cpx; y += cpy; }
                    lineTo(x, y);
                    cpx2 = x; cpy2 = y;
                }
                case 'H', 'h' -> {
                    float x = args[0];
                    if (rel) { x += cpx; }
                    lineTo(x, cpy);
                    cpx2 = x; cpy2 = cpy;
                }
                case 'V', 'v' -> {
                    float y = args[0];
                    if (rel) { y += cpy; }
                    lineTo(cpx, y);
                    cpx2 = cpx; cpy2 = y;
                }
                case 'C', 'c' -> {
                    float x1 = args[0], y1 = args[1];
                    float x2 = args[2], y2 = args[3];
                    float x  = args[4], y  = args[5];
                    if (rel) {
                        x1 += cpx; y1 += cpy;
                        x2 += cpx; y2 += cpy;
                        x  += cpx; y  += cpy;
                    }
                    cubicTo(x1, y1, x2, y2, x, y, tol);
                    cpx2 = x2; cpy2 = y2;
                }
                case 'S', 's' -> {
                    // Logic from nsvg__pathCubicBezShortTo
                    // Reflection of the *previous* control point
                    float x2 = args[0], y2 = args[1];
                    float x  = args[2], y  = args[3];
                    if (rel) {
                        x2 += cpx; y2 += cpy;
                        x  += cpx; y  += cpy;
                    }
                    // Reflect control point: current + (current - last_control)
                    float x1 = 2 * cpx - cpx2;
                    float y1 = 2 * cpy - cpy2;

                    cubicTo(x1, y1, x2, y2, x, y, tol);
                    cpx2 = x2; cpy2 = y2;
                }
                case 'Q', 'q' -> {
                    float x1 = args[0], y1 = args[1];
                    float x  = args[2], y  = args[3];
                    if (rel) {
                        x1 += cpx; y1 += cpy;
                        x  += cpx; y  += cpy;
                    }
                    quadTo(x1, y1, x, y, tol);
                    cpx2 = x1; cpy2 = y1;
                }
                case 'T', 't' -> {
                    // Logic from nsvg__pathQuadBezShortTo
                    float x = args[0];
                    float y = args[1];
                    if (rel) { x += cpx; y += cpy; }

                    // Reflect control point
                    float x1 = 2 * cpx - cpx2;
                    float y1 = 2 * cpy - cpy2;

                    quadTo(x1, y1, x, y, tol);
                    cpx2 = x1; cpy2 = y1;
                }
                case 'A', 'a' -> {
                    // args: rx, ry, rot, large, sweep, x, y
                    float rx = args[0], ry = args[1];
                    float rot = args[2];
                    boolean large = (args[3] != 0.0f);
                    boolean sweep = (args[4] != 0.0f);
                    float x = args[5], y = args[6];
                    if (rel) { x += cpx; y += cpy; }

                    arcTo(rx, ry, rot, large, sweep, x, y, tol);
                    cpx2 = x; cpy2 = y;
                }
                case 'Z', 'z' -> {
                    closePath();
                    cpx2 = cpx; cpy2 = cpy;
                }
            }
        }

        // --- Drawing Actions ---

        private void moveTo(float x, float y) {
            // If we have an open contour, flush it
            flushContour();
            isSubpathStarted = true;
            startX = x;
            startY = y;
            cpx = x;
            cpy = y;
            cur.add(x);
            cur.add(y);
            curClosed = false;
        }

        private void lineTo(float x, float y) {
            if (!isSubpathStarted) moveTo(x, y); // Implicit move if no previous M
            cur.add(x);
            cur.add(y);
            cpx = x;
            cpy = y;
        }

        private void closePath() {
            if (cur.size() > 0) {
                curClosed = true;
                // Visually move pen to start, but no need to add point if it duplicates
                cpx = startX;
                cpy = startY;
                flushContour();
                isSubpathStarted = false;
            }
        }

        private void quadTo(float x1, float y1, float x, float y, float tol) {
            flattenQuad(cpx, cpy, x1, y1, x, y, tol, 0);
            cpx = x;
            cpy = y;
        }

        private void cubicTo(float x1, float y1, float x2, float y2, float x, float y, float tol) {
            flattenCubic(cpx, cpy, x1, y1, x2, y2, x, y, tol, 0);
            cpx = x;
            cpy = y;
        }

        private void arcTo(float rx, float ry, float rot, boolean large, boolean sweep, float x, float y, float tol) {
            // This logic discretizes the arc into line segments directly.
            // Based on common SVG arc implementation (similar to NanoSVG but flattened immediately).

            float x0 = cpx;
            float y0 = cpy;

            // Degenerate cases
            if (rx == 0 || ry == 0) {
                lineTo(x, y);
                return;
            }
            if (Math.abs(x0 - x) < 1e-6f && Math.abs(y0 - y) < 1e-6f) {
                return;
            }

            rx = Math.abs(rx);
            ry = Math.abs(ry);

            double phi = Math.toRadians(rot % 360.0);
            double cosPhi = Math.cos(phi);
            double sinPhi = Math.sin(phi);

            // Compute center parameterization
            double dx2 = (x0 - x) * 0.5;
            double dy2 = (y0 - y) * 0.5;
            double x1p = cosPhi * dx2 + sinPhi * dy2;
            double y1p = -sinPhi * dx2 + cosPhi * dy2;

            double rx2 = rx * rx;
            double ry2 = ry * ry;
            double x1p2 = x1p * x1p;
            double y1p2 = y1p * y1p;

            // Scale radii if not large enough to bridge points
            double lambda = x1p2 / rx2 + y1p2 / ry2;
            if (lambda > 1.0) {
                double s = Math.sqrt(lambda);
                rx *= s;
                ry *= s;
                rx2 = rx * rx;
                ry2 = ry * ry;
            }

            double sign = (large == sweep) ? -1.0 : 1.0;
            double num = Math.max(0.0, rx2 * ry2 - rx2 * y1p2 - ry2 * x1p2);
            double den = rx2 * y1p2 + ry2 * x1p2;
            double coef = sign * Math.sqrt(num / den);

            double cxp = coef * (rx * y1p) / ry;
            double cyp = coef * (-ry * x1p) / rx;

            double mx = (x0 + x) * 0.5;
            double my = (y0 + y) * 0.5;
            double cx = cosPhi * cxp - sinPhi * cyp + mx;
            double cy = sinPhi * cxp + cosPhi * cyp + my;

            double ux = (x1p - cxp) / rx;
            double uy = (y1p - cyp) / ry;
            double vx = (-x1p - cxp) / rx;
            double vy = (-y1p - cyp) / ry;

            double theta1 = Math.atan2(uy, ux);
            double dTheta = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);

            if (!sweep && dTheta > 0) dTheta -= Math.PI * 2;
            else if (sweep && dTheta < 0) dTheta += Math.PI * 2;

            // Estimation of segments based on tolerance
            int segments = (int) Math.ceil(Math.abs(dTheta * Math.max(rx, ry)) / (tol * 2));
            segments = Math.max(2, Math.min(segments, 100)); // clamp

            for (int i = 1; i <= segments; i++) {
                double t = (double) i / segments;
                double angle = theta1 + dTheta * t;
                double cA = Math.cos(angle);
                double sA = Math.sin(angle);

                double px = rx * cA;
                double py = ry * sA;

                double ptx = cosPhi * px - sinPhi * py + cx;
                double pty = sinPhi * px + cosPhi * py + cy;
                lineTo((float) ptx, (float) pty);
            }
        }

        // --- Recursive Flattening (De Casteljau) ---

        private void flattenQuad(float x0, float y0, float x1, float y1, float x2, float y2, float tol, int depth) {
            // Stop if flat enough or too deep
            if (depth > 10 || quadFlatEnough(x0, y0, x1, y1, x2, y2, tol)) {
                lineTo(x2, y2);
                return;
            }
            float x01 = (x0 + x1) * 0.5f;
            float y01 = (y0 + y1) * 0.5f;
            float x12 = (x1 + x2) * 0.5f;
            float y12 = (y1 + y2) * 0.5f;
            float x012 = (x01 + x12) * 0.5f;
            float y012 = (y01 + y12) * 0.5f;
            flattenQuad(x0, y0, x01, y01, x012, y012, tol, depth + 1);
            flattenQuad(x012, y012, x12, y12, x2, y2, tol, depth + 1);
        }

        private void flattenCubic(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float tol, int depth) {
            if (depth > 10 || cubicFlatEnough(x0, y0, x1, y1, x2, y2, x3, y3, tol)) {
                lineTo(x3, y3);
                return;
            }
            float x01 = (x0 + x1) * 0.5f;
            float y01 = (y0 + y1) * 0.5f;
            float x12 = (x1 + x2) * 0.5f;
            float y12 = (y1 + y2) * 0.5f;
            float x23 = (x2 + x3) * 0.5f;
            float y23 = (y2 + y3) * 0.5f;

            float x012 = (x01 + x12) * 0.5f;
            float y012 = (y01 + y12) * 0.5f;
            float x123 = (x12 + x23) * 0.5f;
            float y123 = (y12 + y23) * 0.5f;

            float x0123 = (x012 + x123) * 0.5f;
            float y0123 = (y012 + y123) * 0.5f;

            flattenCubic(x0, y0, x01, y01, x012, y012, x0123, y0123, tol, depth + 1);
            flattenCubic(x0123, y0123, x123, y123, x23, y23, x3, y3, tol, depth + 1);
        }

        private static boolean quadFlatEnough(float x0, float y0, float x1, float y1, float x2, float y2, float tol) {
            // Distance from control point to the line segment start-end
            return ptSegDistSq(x1, y1, x0, y0, x2, y2) <= tol * tol;
        }

        private static boolean cubicFlatEnough(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float tol) {
            float tolSq = tol * tol;
            return ptSegDistSq(x1, y1, x0, y0, x3, y3) <= tolSq &&
                    ptSegDistSq(x2, y2, x0, y0, x3, y3) <= tolSq;
        }

        private static float ptSegDistSq(float px, float py, float ax, float ay, float bx, float by) {
            float dx = bx - ax;
            float dy = by - ay;
            if (dx == 0 && dy == 0) {
                float lx = px - ax;
                float ly = py - ay;
                return lx * lx + ly * ly;
            }
            float t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
            t = Math.max(0, Math.min(1, t));
            float cx = ax + t * dx;
            float cy = ay + t * dy;
            float lx = px - cx;
            float ly = py - cy;
            return lx * lx + ly * ly;
        }

        // --- Utils ---

        private void flushContour() {
            if (cur.size() >= 4) {
                out.add(new VectorPath.Contour(cur.toArray(), curClosed));
            }
            cur.clear();
            curClosed = false;
        }

        private static boolean isCmd(char c) {
            switch (c) {
                case 'M': case 'm': case 'L': case 'l': case 'H': case 'h': case 'V': case 'v':
                case 'C': case 'c': case 'S': case 's': case 'Q': case 'q': case 'T': case 't':
                case 'A': case 'a': case 'Z': case 'z':
                    return true;
                default:
                    return false;
            }
        }

        private static int getArgsPerCommand(char c) {
            switch (c) {
                case 'H': case 'h': case 'V': case 'v': return 1;
                case 'M': case 'm': case 'L': case 'l': case 'T': case 't': return 2;
                case 'Q': case 'q': case 'S': case 's': return 4;
                case 'C': case 'c': return 6;
                case 'A': case 'a': return 7;
                case 'Z': case 'z': return 0;
                default: return -1;
            }
        }

        // --- Robust Parsing Helpers (Porting scanNumber/parseNumber logic) ---

        private void skipWs() {
            while (idx < len) {
                char c = s.charAt(idx);
                if (!Character.isWhitespace(c) && c != ',') break;
                idx++;
            }
        }

        private void skipCommaWs() {
            while (idx < len) {
                char c = s.charAt(idx);
                if (!Character.isWhitespace(c) && c != ',') break;
                idx++;
            }
        }

        /**
         * Scans the string for the next valid float, strictly following SVG number rules.
         * Handles cases like "10-20" -> "10", "-20" and ".5.5" -> "0.5", "0.5".
         */
        private float nextFloat() {
            skipCommaWs();
            if (idx >= len) return 0f;

            int start = idx;
            char c = s.charAt(idx);

            // Optional sign
            if (c == '+' || c == '-') {
                idx++;
            }

            // Integer part
            while (idx < len && Character.isDigit(s.charAt(idx))) {
                idx++;
            }

            // Fraction part
            if (idx < len && s.charAt(idx) == '.') {
                idx++;
                while (idx < len && Character.isDigit(s.charAt(idx))) {
                    idx++;
                }
            }

            // Exponent part
            if (idx < len && (s.charAt(idx) == 'e' || s.charAt(idx) == 'E')) {
                int ePos = idx;
                idx++;
                if (idx < len && (s.charAt(idx) == '+' || s.charAt(idx) == '-')) {
                    idx++;
                }
                if (idx < len && Character.isDigit(s.charAt(idx))) {
                    while (idx < len && Character.isDigit(s.charAt(idx))) {
                        idx++;
                    }
                } else {
                    // Malformed exponent, backtrack
                    idx = ePos;
                }
            }

            // Parsing
            if (idx > start) {
                try {
                    return Float.parseFloat(s.substring(start, idx));
                } catch (NumberFormatException e) {
                    return 0f;
                }
            }
            return 0f;
        }
    }

    // Simple auto-growing float list
    private static final class FloatList {
        private float[] data = new float[64];
        private int size;

        int size() {
            return size;
        }

        void add(float v) {
            if (size >= data.length) {
                float[] n = new float[data.length * 2];
                System.arraycopy(data, 0, n, 0, data.length);
                data = n;
            }
            data[size++] = v;
        }

        void clear() {
            size = 0;
        }

        float[] toArray() {
            float[] n = new float[size];
            System.arraycopy(data, 0, n, 0, size);
            return n;
        }
    }
}