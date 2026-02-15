package com.miry.ui.gizmo;

import com.miry.ui.input.UiInput;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.Objects;

/**
 * Viewport-independent 3D rotation gizmo (X/Y/Z) using screen-space ring dragging.
 * <p>
 * Rotation is expressed as Euler degrees (XYZ) and mutates {@code inOutEulerDeg} directly.
 * Geometry output format matches {@link TranslateGizmo3D}: {@code pos.xyz, color.rgb}.
 */
public final class RotateGizmo3D {
    private final GizmoStyle style;
    private GizmoSpace space = GizmoSpace.WORLD;
    private final Matrix3f localAxes = new Matrix3f().identity();

    private GizmoAxis hovered = GizmoAxis.NONE;
    private GizmoAxis active = GizmoAxis.NONE;

    private final Vector3f dragStartEuler = new Vector3f();
    private float dragStartAngleRad;
    private float dragCurrentAngleRad;
    private String dragLabel;

    public RotateGizmo3D() {
        this(new GizmoStyle());
    }

    public RotateGizmo3D(GizmoStyle style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    public GizmoStyle style() {
        return style;
    }

    public GizmoSpace space() {
        return space;
    }

    public void setSpace(GizmoSpace space) {
        this.space = space == null ? GizmoSpace.WORLD : space;
    }

    /**
     * Sets the local-space axes (rotation-only) used when {@link #space()} is {@link GizmoSpace#LOCAL}.
     */
    public void setLocalAxes(Matrix3f axes) {
        if (axes == null) {
            localAxes.identity();
        } else {
            localAxes.set(axes);
        }
    }

    public GizmoAxis hoveredAxis() {
        return hovered;
    }

    public GizmoAxis activeAxis() {
        return active;
    }

    /**
     * @return a short human-readable label for the current drag (e.g. {@code "Y: 45°"}), or null when not dragging
     */
    public String dragLabel() {
        return dragLabel;
    }

    public boolean update(UiInput input,
                          Matrix4f viewProj,
                          Vector3f cameraPos,
                          int viewportX,
                          int viewportY,
                          int viewportW,
                          int viewportH,
                          Vector3f origin,
                          Vector3f inOutEulerDeg) {
        if (input == null || viewProj == null || cameraPos == null || origin == null || inOutEulerDeg == null) {
            return false;
        }
        if (viewportW <= 0 || viewportH <= 0) {
            return false;
        }

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean inside = mx >= viewportX && my >= viewportY && mx < viewportX + viewportW && my < viewportY + viewportH;

        Vector2f center = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, origin);
        if (center == null) {
            hovered = GizmoAxis.NONE;
            active = GizmoAxis.NONE;
            return false;
        }

        if (active == GizmoAxis.NONE) {
            hovered = inside ? hitTestAxis(mx, my, viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, origin) : GizmoAxis.NONE;
        }

        if (input.mousePressed() && inside) {
            if (hovered != GizmoAxis.NONE) {
                active = hovered;
                dragStartEuler.set(inOutEulerDeg);
                Vector3f camDir = new Vector3f(cameraPos).sub(origin);
                if (camDir.lengthSquared() < 1e-8f) {
                    camDir.set(0, 0, 1);
                } else {
                    camDir.normalize();
                }
                Vector3f axisDir = axisWorld(active, new Vector3f());
                dragStartAngleRad = angleOnRing(viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, origin, axisDir, camDir, mx, my,
                    (float) Math.atan2(my - center.y, mx - center.x));
                dragCurrentAngleRad = dragStartAngleRad;
            } else {
                active = GizmoAxis.NONE;
            }
        }

        if (active != GizmoAxis.NONE && input.mouseDown()) {
            Vector3f camDir = new Vector3f(cameraPos).sub(origin);
            if (camDir.lengthSquared() < 1e-8f) {
                camDir.set(0, 0, 1);
            } else {
                camDir.normalize();
            }
            Vector3f axisDir = axisWorld(active, new Vector3f());
            float now = angleOnRing(viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, origin, axisDir, camDir, mx, my,
                (float) Math.atan2(my - center.y, mx - center.x));
            dragCurrentAngleRad = now;
            float delta = normalizeAngleRad(now - dragStartAngleRad);
            float deltaDeg = (float) Math.toDegrees(delta);
            if (style.snapRotate > 0.0f && (style.snapEnabled || input.ctrlDown())) {
                deltaDeg = Math.round(deltaDeg / style.snapRotate) * style.snapRotate;
            }
            inOutEulerDeg.set(dragStartEuler);
            if (active == GizmoAxis.X) {
                inOutEulerDeg.x = dragStartEuler.x + deltaDeg;
            } else if (active == GizmoAxis.Y) {
                inOutEulerDeg.y = dragStartEuler.y + deltaDeg;
            } else if (active == GizmoAxis.Z) {
                inOutEulerDeg.z = dragStartEuler.z + deltaDeg;
            }
            dragLabel = axisName(active) + ": " + formatAngleDeg(deltaDeg);
        } else if (active == GizmoAxis.NONE) {
            dragLabel = null;
        }

        if (input.mouseReleased()) {
            active = GizmoAxis.NONE;
            dragLabel = null;
        }

        return hovered != GizmoAxis.NONE || active != GizmoAxis.NONE;
    }

    private static String axisName(GizmoAxis axis) {
        return switch (axis) {
            case X -> "X";
            case Y -> "Y";
            case Z -> "Z";
            default -> "";
        };
    }

    private static String formatAngleDeg(float deg) {
        float v = Float.isFinite(deg) ? deg : 0.0f;
        return String.format(java.util.Locale.ROOT, "%.1f°", v);
    }

    public void writeTriangles(FloatBuffer out,
                               Matrix4f viewProj,
                               Vector3f cameraPos,
                               int viewportX,
                               int viewportY,
                               int viewportW,
                               int viewportH,
                               Vector3f origin) {
        if (out == null || viewProj == null || cameraPos == null || origin == null) {
            return;
        }

        Vector3f camDir = new Vector3f(cameraPos).sub(origin);
        if (camDir.lengthSquared() < 1e-8f) {
            camDir.set(0, 0, 1);
        } else {
            camDir.normalize();
        }

        float worldPerPx = worldUnitsPerPixel(viewProj, viewportW, viewportH, origin);
        float rWorld = Math.max(0.05f, style.rotateRadiusPx * worldPerPx);

        int seg = Math.max(12, style.rotateSegments);
        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            writeRing(out, axis, camDir, origin, rWorld, seg, worldPerPx);
        }
    }

    private void writeRing(FloatBuffer out,
                           GizmoAxis axis,
                           Vector3f camDir,
                           Vector3f origin,
                           float radiusWorld,
                           int segments,
                           float worldPerPx) {
        Vector3f axisDir = axisWorld(axis, new Vector3f());
        Vector3f u = new Vector3f(axisDir).cross(camDir);
        if (u.lengthSquared() < 1e-8f) {
            u.set(0, 1, 0).cross(axisDir);
        }
        safeNormalize(u);
        Vector3f v = safeNormalize(new Vector3f(axisDir).cross(u));

        float halfW = Math.max(0.0005f, (style.rotateBandWidthPx * 0.5f) * worldPerPx);
        float boost = (axis == active) ? (style.activeBoost + style.glowIntensity) : (axis == hovered ? style.hoverBoost : 1.0f);
        Vector3f c = baseColor(axis).mul(boost, new Vector3f());
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;

        if (outline > 0.0f) {
            writeRingBand(out, origin, axisDir, u, v, radiusWorld, segments, halfW + outline, new Vector3f(0, 0, 0));
        }
        writeRingBand(out, origin, axisDir, u, v, radiusWorld, segments, halfW, c);

        // Tick marks (15° by default, Unity-style).
        float tickStepDeg = 15.0f;
        int tickCount = Math.max(1, Math.round(360.0f / tickStepDeg));
        float tickLen = Math.max(0.001f, 6.0f * worldPerPx);
        float tickHalfW = Math.max(0.0005f, 1.25f * worldPerPx);
        for (int i = 0; i < tickCount; i++) {
            float a = (float) Math.toRadians(i * tickStepDeg);
            Vector3f dir = new Vector3f(u).mul((float) Math.cos(a)).fma((float) Math.sin(a), v);
            Vector3f tan = new Vector3f(u).mul((float) -Math.sin(a)).fma((float) Math.cos(a), v);
            Vector3f p0 = new Vector3f(origin).fma(radiusWorld, dir);
            Vector3f p1 = new Vector3f(origin).fma(radiusWorld + tickLen, dir);
            Vector3f s = safeNormalize(tan).mul(tickHalfW, new Vector3f());
            if (outline > 0.0f) {
                Vector3f so = safeNormalize(tan).mul(tickHalfW + outline, new Vector3f());
                putQuad(out,
                    new Vector3f(p0).add(so),
                    new Vector3f(p0).sub(so),
                    new Vector3f(p1).sub(so),
                    new Vector3f(p1).add(so),
                    new Vector3f(0, 0, 0)
                );
            }
            putQuad(out,
                new Vector3f(p0).add(s),
                new Vector3f(p0).sub(s),
                new Vector3f(p1).sub(s),
                new Vector3f(p1).add(s),
                c
            );
        }

        // Active arc preview.
        if (axis == active) {
            float delta = normalizeAngleRad(dragCurrentAngleRad - dragStartAngleRad);
            float abs = Math.abs(delta);
            if (abs > 1e-4f) {
                int arcSeg = Math.max(3, Math.min(segments, (int) Math.ceil(abs / (Math.PI * 2.0) * segments)));
                float start = dragStartAngleRad;
                float step = delta / arcSeg;
                float arcHalfW = halfW * 1.35f;
                if (outline > 0.0f) {
                    writeArcBand(out, origin, axisDir, u, v, radiusWorld, start, step, arcSeg, arcHalfW + outline, new Vector3f(0, 0, 0));
                }
                writeArcBand(out, origin, axisDir, u, v, radiusWorld, start, step, arcSeg, arcHalfW, c);
            }
        }
    }

    private static void writeRingBand(FloatBuffer out,
                                      Vector3f origin,
                                      Vector3f axisDir,
                                      Vector3f u,
                                      Vector3f v,
                                      float radiusWorld,
                                      int segments,
                                      float halfW,
                                      Vector3f rgb) {
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (i * (Math.PI * 2.0) / segments);
            float a1 = (float) ((i + 1) * (Math.PI * 2.0) / segments);
            Vector3f p0 = new Vector3f(origin).fma((float) Math.cos(a0) * radiusWorld, u).fma((float) Math.sin(a0) * radiusWorld, v);
            Vector3f p1 = new Vector3f(origin).fma((float) Math.cos(a1) * radiusWorld, u).fma((float) Math.sin(a1) * radiusWorld, v);

            Vector3f dir = new Vector3f(p1).sub(p0);
            if (dir.lengthSquared() < 1e-10f) {
                continue;
            }
            dir.normalize();
            Vector3f side = safeNormalize(new Vector3f(axisDir).cross(dir));
            Vector3f s = new Vector3f(side).mul(halfW);

            putQuad(out,
                new Vector3f(p0).add(s),
                new Vector3f(p0).sub(s),
                new Vector3f(p1).sub(s),
                new Vector3f(p1).add(s),
                rgb
            );
        }
    }

    private static void writeArcBand(FloatBuffer out,
                                     Vector3f origin,
                                     Vector3f axisDir,
                                     Vector3f u,
                                     Vector3f v,
                                     float radiusWorld,
                                     float start,
                                     float step,
                                     int segments,
                                     float halfW,
                                     Vector3f rgb) {
        for (int i = 0; i < segments; i++) {
            float a0 = start + step * i;
            float a1 = start + step * (i + 1);
            Vector3f p0 = new Vector3f(origin).fma((float) Math.cos(a0) * radiusWorld, u).fma((float) Math.sin(a0) * radiusWorld, v);
            Vector3f p1 = new Vector3f(origin).fma((float) Math.cos(a1) * radiusWorld, u).fma((float) Math.sin(a1) * radiusWorld, v);

            Vector3f dir = new Vector3f(p1).sub(p0);
            if (dir.lengthSquared() < 1e-10f) {
                continue;
            }
            dir.normalize();
            Vector3f side = safeNormalize(new Vector3f(axisDir).cross(dir));
            Vector3f s = new Vector3f(side).mul(halfW);
            putQuad(out,
                new Vector3f(p0).add(s),
                new Vector3f(p0).sub(s),
                new Vector3f(p1).sub(s),
                new Vector3f(p1).add(s),
                rgb
            );
        }
    }

    private GizmoAxis hitTestAxis(float mx,
                                  float my,
                                  Matrix4f viewProj,
                                  Vector3f cameraPos,
                                  int vx,
                                  int vy,
                                  int vw,
                                  int vh,
                                  Vector3f origin) {
        float thr2 = style.rotateHitThresholdPx * style.rotateHitThresholdPx;
        float worldPerPx = worldUnitsPerPixel(viewProj, vw, vh, origin);
        float rWorld = Math.max(0.05f, style.rotateRadiusPx * worldPerPx);

        Vector3f camDir = new Vector3f(cameraPos).sub(origin);
        if (camDir.lengthSquared() < 1e-8f) {
            camDir.set(0, 0, 1);
        } else {
            camDir.normalize();
        }

        int seg = Math.max(12, style.rotateSegments);
        GizmoAxis best = GizmoAxis.NONE;
        float bestScore = Float.MAX_VALUE;

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            Vector3f axisDir = axisWorld(axis, new Vector3f());
            Vector3f u = new Vector3f(axisDir).cross(camDir);
            if (u.lengthSquared() < 1e-8f) {
                u.set(0, 1, 0).cross(axisDir);
            }
            safeNormalize(u);
            Vector3f v = safeNormalize(new Vector3f(axisDir).cross(u));

            Vector2f prev = null;
            float bestD = Float.MAX_VALUE;
            for (int i = 0; i <= seg; i++) {
                float a = (float) (i * (Math.PI * 2.0) / seg);
                Vector3f p = new Vector3f(origin).fma((float) Math.cos(a) * rWorld, u).fma((float) Math.sin(a) * rWorld, v);
                Vector2f s = projectVisible(viewProj, vx, vy, vw, vh, p);
                if (s == null) {
                    prev = null;
                    continue;
                }
                if (prev != null) {
                    float d = distToSegment2(mx, my, prev.x, prev.y, s.x, s.y);
                    if (d < bestD) {
                        bestD = d;
                    }
                }
                prev = s;
            }

            if (bestD <= thr2) {
                // Prefer rings that are more visible (axis not aligned to view direction).
                float sin = axisSinToView(axisDir, camDir);
                float score = bestD / Math.max(0.35f, sin);
                if (score < bestScore) {
                    bestScore = score;
                    best = axis;
                }
            }
        }

        return best;
    }

    private Vector3f axisWorld(GizmoAxis axis, Vector3f dest) {
        dest.set(axis.dir());
        if (space == GizmoSpace.LOCAL) {
            localAxes.transform(dest);
        }
        if (dest.lengthSquared() < 1e-8f) {
            dest.set(axis.dir());
        }
        return safeNormalize(dest);
    }

    private static float angleOnRing(Matrix4f viewProj,
                                     Vector3f cameraPos,
                                     int vx,
                                     int vy,
                                     int vw,
                                     int vh,
                                     Vector3f origin,
                                     Vector3f axisDir,
                                     Vector3f camDir,
                                     float mouseX,
                                     float mouseY,
                                     float fallback) {
        Matrix4f invViewProj = new Matrix4f(viewProj).invert();
        Vector3f rayDir = rayDirection(invViewProj, vx, vy, vw, vh, mouseX, mouseY);
        float denom = rayDir.dot(axisDir);
        if (Math.abs(denom) < 1e-6f) {
            return fallback;
        }
        float t = new Vector3f(origin).sub(cameraPos).dot(axisDir) / denom;
        if (!Float.isFinite(t)) {
            return fallback;
        }
        Vector3f hit = new Vector3f(cameraPos).fma(t, rayDir);

        Vector3f u = new Vector3f(axisDir).cross(camDir);
        if (u.lengthSquared() < 1e-8f) {
            u.set(0, 1, 0).cross(axisDir);
        }
        safeNormalize(u);
        Vector3f v = safeNormalize(new Vector3f(axisDir).cross(u));

        Vector3f r = hit.sub(origin, new Vector3f());
        float x = r.dot(u);
        float y = r.dot(v);
        if (!Float.isFinite(x) || !Float.isFinite(y) || (x * x + y * y) < 1e-10f) {
            return fallback;
        }
        return (float) Math.atan2(y, x);
    }

    private static Vector3f rayDirection(Matrix4f invViewProj,
                                         int vx,
                                         int vy,
                                         int vw,
                                         int vh,
                                         float mouseX,
                                         float mouseY) {
        float ndcX = ((mouseX - vx) / (float) Math.max(1, vw)) * 2.0f - 1.0f;
        float ndcY = 1.0f - ((mouseY - vy) / (float) Math.max(1, vh)) * 2.0f;

        Vector3f p0 = unproject(invViewProj, ndcX, ndcY, -1.0f);
        Vector3f p1 = unproject(invViewProj, ndcX, ndcY, 1.0f);
        return safeNormalize(p1.sub(p0));
    }

    private static Vector3f unproject(Matrix4f invViewProj, float ndcX, float ndcY, float ndcZ) {
        Vector4f v = new Vector4f(ndcX, ndcY, ndcZ, 1.0f);
        invViewProj.transform(v);
        if (Math.abs(v.w) < 1e-6f) {
            return new Vector3f();
        }
        return new Vector3f(v.x / v.w, v.y / v.w, v.z / v.w);
    }

    private Vector3f baseColor(GizmoAxis axis) {
        return switch (axis) {
            case X -> style.xColor;
            case Y -> style.yColor;
            case Z -> style.zColor;
            default -> new Vector3f(0.9f, 0.9f, 0.9f);
        };
    }

    private static float normalizeAngleRad(float a) {
        float twoPi = (float) (Math.PI * 2.0);
        a = (a + (float) Math.PI) % twoPi;
        if (a < 0) {
            a += twoPi;
        }
        return a - (float) Math.PI;
    }

    private static void putQuad(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f rgb) {
        putTri(out, a, b, c, rgb);
        putTri(out, a, c, d, rgb);
    }

    private static void putTri(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f rgb) {
        out.put(a.x).put(a.y).put(a.z).put(rgb.x).put(rgb.y).put(rgb.z);
        out.put(b.x).put(b.y).put(b.z).put(rgb.x).put(rgb.y).put(rgb.z);
        out.put(c.x).put(c.y).put(c.z).put(rgb.x).put(rgb.y).put(rgb.z);
    }

    private static Vector2f projectVisible(Matrix4f viewProj,
                                           int vx,
                                           int vy,
                                           int vw,
                                           int vh,
                                           Vector3f p) {
        Vector4f clip = new Vector4f(p.x, p.y, p.z, 1.0f).mul(viewProj);
        if (clip.w <= 1e-6f) {
            return null;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;
        if (ndcZ < -1.2f || ndcZ > 1.2f) {
            return null;
        }
        float sx = vx + (ndcX * 0.5f + 0.5f) * vw;
        float sy = vy + (1.0f - (ndcY * 0.5f + 0.5f)) * vh;
        return new Vector2f(sx, sy);
    }

    private static float worldUnitsPerPixel(Matrix4f viewProj, int viewportW, int viewportH, Vector3f origin) {
        Vector2f o2 = projectVisible(viewProj, 0, 0, viewportW, viewportH, origin);
        if (o2 == null) {
            return 0.01f;
        }
        Vector3f p1 = new Vector3f(origin).add(1.0f, 0.0f, 0.0f);
        Vector2f p2 = projectVisible(viewProj, 0, 0, viewportW, viewportH, p1);
        if (p2 == null) {
            return 0.01f;
        }
        float dx = p2.x - o2.x;
        float dy = p2.y - o2.y;
        float distPx = (float) Math.sqrt(dx * dx + dy * dy);
        if (distPx < 1e-3f) {
            return 0.01f;
        }
        return 1.0f / distPx;
    }

    private static float axisSinToView(Vector3f axisDir, Vector3f camDir) {
        float d = clamp(Math.abs(axisDir.dot(camDir)), 0.0f, 1.0f);
        return (float) Math.sqrt(Math.max(0.0f, 1.0f - d * d));
    }

    private static Vector3f safeNormalize(Vector3f v) {
        float len2 = v.lengthSquared();
        if (len2 < 1e-12f) {
            return v;
        }
        return v.mul(1.0f / (float) Math.sqrt(len2));
    }

    private static float distToSegment2(float px, float py, float ax, float ay, float bx, float by) {
        float abx = bx - ax;
        float aby = by - ay;
        float apx = px - ax;
        float apy = py - ay;
        float abLen2 = abx * abx + aby * aby;
        float t = abLen2 <= 1e-6f ? 0.0f : (apx * abx + apy * aby) / abLen2;
        t = clamp(t, 0.0f, 1.0f);
        float cx = ax + abx * t;
        float cy = ay + aby * t;
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
