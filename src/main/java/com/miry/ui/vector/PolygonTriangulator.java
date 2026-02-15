package com.miry.ui.vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Very small ear-clipping triangulator for simple polygons.
 * <p>
 * Input polygon must be non-self-intersecting and without holes.
 */
public final class PolygonTriangulator {
    private PolygonTriangulator() {}

    /**
     * Triangulates the polygon defined by {@code pts} (x0,y0,x1,y1,...).
     *
     * @return triangle vertex indices (triples).
     */
    public static int[] triangulate(float[] pts) {
        if (pts == null) return new int[0];
        int n = pts.length / 2;
        if (n < 3) return new int[0];

        int[] V = new int[n];
        for (int i = 0; i < n; i++) V[i] = i;

        // Ensure CCW winding.
        if (signedArea2(pts) < 0.0f) {
            for (int i = 0; i < n / 2; i++) {
                int tmp = V[i];
                V[i] = V[n - 1 - i];
                V[n - 1 - i] = tmp;
            }
        }

        List<Integer> out = new ArrayList<>(Math.max(0, (n - 2) * 3));
        int nv = n;
        int guard = 0;
        while (nv > 3 && guard++ < 10_000) {
            boolean cut = false;
            for (int i = 0; i < nv; i++) {
                int i0 = V[(i + nv - 1) % nv];
                int i1 = V[i];
                int i2 = V[(i + 1) % nv];

                if (!isConvex(pts, i0, i1, i2)) {
                    continue;
                }
                if (containsAnyPoint(pts, V, nv, i0, i1, i2)) {
                    continue;
                }

                out.add(i0);
                out.add(i1);
                out.add(i2);

                // Remove ear tip i1 from V.
                for (int k = i; k < nv - 1; k++) {
                    V[k] = V[k + 1];
                }
                nv--;
                cut = true;
                break;
            }
            if (!cut) {
                // Fallback: triangle fan (better than nothing if we hit degeneracy).
                out.clear();
                for (int i = 1; i < nv - 1; i++) {
                    out.add(V[0]);
                    out.add(V[i]);
                    out.add(V[i + 1]);
                }
                return toIntArray(out);
            }
        }

        if (nv == 3) {
            out.add(V[0]);
            out.add(V[1]);
            out.add(V[2]);
        }

        return toIntArray(out);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private static float signedArea2(float[] pts) {
        int n = pts.length / 2;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double xi = pts[i * 2];
            double yi = pts[i * 2 + 1];
            double xj = pts[j * 2];
            double yj = pts[j * 2 + 1];
            sum += (xi * yj - xj * yi);
        }
        return (float) sum;
    }

    private static boolean isConvex(float[] pts, int i0, int i1, int i2) {
        float ax = pts[i0 * 2];
        float ay = pts[i0 * 2 + 1];
        float bx = pts[i1 * 2];
        float by = pts[i1 * 2 + 1];
        float cx = pts[i2 * 2];
        float cy = pts[i2 * 2 + 1];
        float abx = bx - ax;
        float aby = by - ay;
        float bcx = cx - bx;
        float bcy = cy - by;
        float cross = abx * bcy - aby * bcx;
        return cross > 1e-6f;
    }

    private static boolean containsAnyPoint(float[] pts, int[] V, int nv, int i0, int i1, int i2) {
        float ax = pts[i0 * 2];
        float ay = pts[i0 * 2 + 1];
        float bx = pts[i1 * 2];
        float by = pts[i1 * 2 + 1];
        float cx = pts[i2 * 2];
        float cy = pts[i2 * 2 + 1];
        for (int k = 0; k < nv; k++) {
            int p = V[k];
            if (p == i0 || p == i1 || p == i2) continue;
            float px = pts[p * 2];
            float py = pts[p * 2 + 1];
            if (pointInTri(px, py, ax, ay, bx, by, cx, cy)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointInTri(float px, float py,
                                      float ax, float ay,
                                      float bx, float by,
                                      float cx, float cy) {
        // Barycentric technique.
        float v0x = cx - ax;
        float v0y = cy - ay;
        float v1x = bx - ax;
        float v1y = by - ay;
        float v2x = px - ax;
        float v2y = py - ay;

        float dot00 = v0x * v0x + v0y * v0y;
        float dot01 = v0x * v1x + v0y * v1y;
        float dot02 = v0x * v2x + v0y * v2y;
        float dot11 = v1x * v1x + v1y * v1y;
        float dot12 = v1x * v2x + v1y * v2y;

        float denom = dot00 * dot11 - dot01 * dot01;
        if (Math.abs(denom) < 1e-8f) return false;
        float inv = 1.0f / denom;
        float u = (dot11 * dot02 - dot01 * dot12) * inv;
        float v = (dot00 * dot12 - dot01 * dot02) * inv;
        return u >= -1e-4f && v >= -1e-4f && (u + v) <= 1.0001f;
    }
}

