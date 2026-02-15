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
 * Viewport-independent 3D scale gizmo (X/Y/Z).
 * <p>
 * Dragging an axis adjusts the corresponding component of {@code inOutScale}.
 * Geometry output format matches {@link TranslateGizmo3D}: {@code pos.xyz, color.rgb}.
 */
public final class ScaleGizmo3D {
    private final GizmoStyle style;
    private GizmoSpace space = GizmoSpace.WORLD;
    private final Matrix3f localAxes = new Matrix3f().identity();

    private ScaleHandle hovered = ScaleHandle.NONE;
    private ScaleHandle active = ScaleHandle.NONE;

    private final Vector3f dragStartScale = new Vector3f(1, 1, 1);
    private float dragStartMouseX;
    private float dragStartMouseY;
    private final Vector2f dragAxisScreenDir = new Vector2f();
    private float dragStartUniformDistPx;
    private String dragLabel;

    public ScaleGizmo3D() {
        this(new GizmoStyle());
    }

    public ScaleGizmo3D(GizmoStyle style) {
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
        return toAxis(hovered);
    }

    public GizmoAxis activeAxis() {
        return toAxis(active);
    }

    public ScaleHandle hoveredHandle() {
        return hovered;
    }

    public ScaleHandle activeHandle() {
        return active;
    }

    /**
     * @return a short human-readable label for the current drag (e.g. {@code "Scale: 1.25x"}), or null when not dragging
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
                          Vector3f inOutScale) {
        if (input == null || viewProj == null || cameraPos == null || origin == null || inOutScale == null) {
            return false;
        }
        if (viewportW <= 0 || viewportH <= 0) {
            return false;
        }

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean inside = mx >= viewportX && my >= viewportY && mx < viewportX + viewportW && my < viewportY + viewportH;

        if (active == ScaleHandle.NONE) {
            hovered = inside ? hitTestHandle(mx, my, viewProj, viewportX, viewportY, viewportW, viewportH, cameraPos, origin) : ScaleHandle.NONE;
        }

        if (input.mousePressed() && inside) {
            if (hovered != ScaleHandle.NONE) {
                active = hovered;
                dragStartScale.set(inOutScale);
                dragStartMouseX = mx;
                dragStartMouseY = my;

                Vector2f o2 = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, origin);
                if (active == ScaleHandle.UNIFORM) {
                    if (o2 != null) {
                        float dx = mx - o2.x;
                        float dy = my - o2.y;
                        dragStartUniformDistPx = (float) Math.sqrt(dx * dx + dy * dy);
                    } else {
                        dragStartUniformDistPx = 0.0f;
                    }
                } else {
                    Vector3f axisDir = axisDirWorld(active, new Vector3f());
                    Vector2f e2 = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, new Vector3f(origin).add(axisDir));
                    if (o2 != null && e2 != null) {
                        dragAxisScreenDir.set(e2).sub(o2);
                        if (dragAxisScreenDir.lengthSquared() >= 1e-6f) {
                            dragAxisScreenDir.normalize();
                        } else {
                            dragAxisScreenDir.set(1, 0);
                        }
                    } else {
                        dragAxisScreenDir.set(1, 0);
                    }
                }
            } else {
                active = ScaleHandle.NONE;
            }
        }

        if (active != ScaleHandle.NONE && input.mouseDown()) {
            float min = 0.01f;
            if (active == ScaleHandle.UNIFORM) {
                Vector2f o2 = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, origin);
                float nowDist = dragStartUniformDistPx;
                if (o2 != null) {
                    float dx = mx - o2.x;
                    float dy = my - o2.y;
                    nowDist = (float) Math.sqrt(dx * dx + dy * dy);
                }
                float deltaPx = nowDist - dragStartUniformDistPx;
                float factor = 1.0f + deltaPx * style.scaleDragPerPx;
                if (!Float.isFinite(factor)) {
                    factor = 1.0f;
                }
                factor = Math.max(0.01f, factor);
                inOutScale.set(dragStartScale).mul(factor);
                inOutScale.x = Math.max(min, inOutScale.x);
                inOutScale.y = Math.max(min, inOutScale.y);
                inOutScale.z = Math.max(min, inOutScale.z);
            } else {
                float dx = mx - dragStartMouseX;
                float dy = my - dragStartMouseY;
                float alongPx = dx * dragAxisScreenDir.x + dy * dragAxisScreenDir.y;
                float delta = alongPx * style.scaleDragPerPx;

                inOutScale.set(dragStartScale);
                if (active == ScaleHandle.X) {
                    inOutScale.x = Math.max(min, dragStartScale.x + delta);
                } else if (active == ScaleHandle.Y) {
                    inOutScale.y = Math.max(min, dragStartScale.y + delta);
                } else if (active == ScaleHandle.Z) {
                    inOutScale.z = Math.max(min, dragStartScale.z + delta);
                }
            }

            if (style.snapScale > 0.0f && (style.snapEnabled || input.ctrlDown())) {
                snapScale(inOutScale, style.snapScale);
            }

            dragLabel = switch (active) {
                case X -> "X: " + formatScaleFactor(inOutScale.x, dragStartScale.x);
                case Y -> "Y: " + formatScaleFactor(inOutScale.y, dragStartScale.y);
                case Z -> "Z: " + formatScaleFactor(inOutScale.z, dragStartScale.z);
                case UNIFORM -> "Scale: " + formatScaleFactor(avg(inOutScale), avg(dragStartScale));
                case NONE -> null;
            };
        }

        if (input.mouseReleased()) {
            active = ScaleHandle.NONE;
            dragLabel = null;
        }

        return hovered != ScaleHandle.NONE || active != ScaleHandle.NONE;
    }

    private static float avg(Vector3f v) {
        return (v.x + v.y + v.z) / 3.0f;
    }

    private static String formatScaleFactor(float now, float start) {
        float factor = (Math.abs(start) > 1e-8f) ? (now / start) : 1.0f;
        if (!Float.isFinite(factor)) {
            factor = 1.0f;
        }
        return String.format(java.util.Locale.ROOT, "%.3fx", factor);
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

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            writeAxis(out, axis, camDir, origin, worldPerPx);
        }

        writeUniformHandle(out, camDir, origin, worldPerPx);
    }

    private void writeAxis(FloatBuffer out, GizmoAxis axis, Vector3f camDir, Vector3f origin, float worldPerPx) {
        Vector3f axisDir = axisWorld(axis, new Vector3f());
        float sin = axisSinToView(axisDir, camDir);
        float lenWorld = clamp(style.axisLengthPx * worldPerPx / Math.max(0.45f, sin), 0.05f, 25.0f);

        Vector3f side = safeNormalize(new Vector3f(axisDir).cross(camDir));
        if (side.lengthSquared() < 1e-8f) {
            side.set(0, 1, 0).cross(axisDir);
            safeNormalize(side);
        }

        float halfW = Math.max(0.0005f, (style.axisWidthPx * 0.5f) * worldPerPx);
        ScaleHandle handle = toHandle(axis);
        float boost = (handle == active) ? (style.activeBoost + style.glowIntensity) : (handle == hovered ? style.hoverBoost : 1.0f);
        Vector3f c = baseColor(axis).mul(boost, new Vector3f());
        Vector3f cBase = style.useGradients ? new Vector3f(c).mul(0.85f) : c;
        Vector3f cTip = style.useGradients ? new Vector3f(c).mul(1.05f) : c;

        Vector3f a = new Vector3f(origin);
        Vector3f b = new Vector3f(origin).fma(lenWorld, axisDir);

        Vector3f s = new Vector3f(side).mul(halfW);
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;
        if (outline > 0.0f) {
            Vector3f so = new Vector3f(side).mul(halfW + outline);
            putQuad(out,
                    new Vector3f(a).add(so),
                    new Vector3f(a).sub(so),
                    new Vector3f(b).sub(so),
                    new Vector3f(b).add(so),
                    new Vector3f(0, 0, 0));
        }
        putQuad(out,
                new Vector3f(a).add(s),
                new Vector3f(a).sub(s),
                new Vector3f(b).sub(s),
                new Vector3f(b).add(s),
                cBase, cBase, cTip, cTip);

        // Handle square (billboarded to camera) at the end.
        float handleHalf = Math.max(0.0008f, (style.scaleHandleSizePx * 0.5f) * worldPerPx);
        Vector3f upRef = new Vector3f(0, 1, 0);
        Vector3f right = safeNormalize(new Vector3f(upRef).cross(camDir));
        if (right.lengthSquared() < 1e-8f) {
            right.set(1, 0, 0);
        }
        Vector3f up = safeNormalize(new Vector3f(camDir).cross(right));
        Vector3f rr = new Vector3f(right).mul(handleHalf);
        Vector3f uu = new Vector3f(up).mul(handleHalf);
        if (outline > 0.0f) {
            Vector3f rrO = new Vector3f(right).mul(handleHalf + outline);
            Vector3f uuO = new Vector3f(up).mul(handleHalf + outline);
            putQuad(out,
                    new Vector3f(b).add(rrO).add(uuO),
                    new Vector3f(b).sub(rrO).add(uuO),
                    new Vector3f(b).sub(rrO).sub(uuO),
                    new Vector3f(b).add(rrO).sub(uuO),
                    new Vector3f(0, 0, 0));
        }
        putQuad(out,
                new Vector3f(b).add(rr).add(uu),
                new Vector3f(b).sub(rr).add(uu),
                new Vector3f(b).sub(rr).sub(uu),
                new Vector3f(b).add(rr).sub(uu),
                c);
    }

    private void writeUniformHandle(FloatBuffer out, Vector3f camDir, Vector3f origin, float worldPerPx) {
        Vector3f upRef = new Vector3f(0, 1, 0);
        Vector3f right = safeNormalize(new Vector3f(upRef).cross(camDir));
        if (right.lengthSquared() < 1e-8f) {
            right.set(1, 0, 0);
        }
        Vector3f up = safeNormalize(new Vector3f(camDir).cross(right));

        float boost = (active == ScaleHandle.UNIFORM) ? (style.activeBoost + style.glowIntensity) : (hovered == ScaleHandle.UNIFORM ? style.hoverBoost : 1.0f);
        Vector3f c = new Vector3f(style.centerColor).mul(boost);

        float half = Math.max(0.001f, style.centerSizePx * worldPerPx);
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;

        Vector3f rr = new Vector3f(right).mul(half);
        Vector3f uu = new Vector3f(up).mul(half);

        if (outline > 0.0f) {
            Vector3f rrO = new Vector3f(right).mul(half + outline);
            Vector3f uuO = new Vector3f(up).mul(half + outline);
            putQuad(out,
                    new Vector3f(origin).add(rrO).add(uuO),
                    new Vector3f(origin).sub(rrO).add(uuO),
                    new Vector3f(origin).sub(rrO).sub(uuO),
                    new Vector3f(origin).add(rrO).sub(uuO),
                    new Vector3f(0, 0, 0));
        }
        putQuad(out,
                new Vector3f(origin).add(rr).add(uu),
                new Vector3f(origin).sub(rr).add(uu),
                new Vector3f(origin).sub(rr).sub(uu),
                new Vector3f(origin).add(rr).sub(uu),
                c);
    }

    private ScaleHandle hitTestHandle(float mx,
                                      float my,
                                      Matrix4f viewProj,
                                      int vx,
                                      int vy,
                                      int vw,
                                      int vh,
                                      Vector3f cameraPos,
                                      Vector3f origin) {
        float worldPerPx = worldUnitsPerPixel(viewProj, vw, vh, origin);
        Vector2f o2 = projectVisible(viewProj, vx, vy, vw, vh, origin);
        if (o2 == null) {
            return ScaleHandle.NONE;
        }

        Vector3f camDir = new Vector3f(cameraPos).sub(origin);
        if (camDir.lengthSquared() < 1e-8f) {
            camDir.set(0, 0, 1);
        } else {
            camDir.normalize();
        }

        ScaleHandle best = ScaleHandle.NONE;
        float bestScore = Float.MAX_VALUE;

        // Uniform handle (center square).
        {
            Vector3f upRef = new Vector3f(0, 1, 0);
            Vector3f right = safeNormalize(new Vector3f(upRef).cross(camDir));
            if (right.lengthSquared() < 1e-8f) {
                right.set(1, 0, 0);
            }
            Vector3f up = safeNormalize(new Vector3f(camDir).cross(right));
            float half = Math.max(0.001f, style.centerSizePx * worldPerPx);
            Vector3f rr = new Vector3f(right).mul(half);
            Vector3f uu = new Vector3f(up).mul(half);
            Vector2f p0 = projectVisible(viewProj, vx, vy, vw, vh, new Vector3f(origin).add(rr).add(uu));
            Vector2f p1 = projectVisible(viewProj, vx, vy, vw, vh, new Vector3f(origin).sub(rr).add(uu));
            Vector2f p2 = projectVisible(viewProj, vx, vy, vw, vh, new Vector3f(origin).sub(rr).sub(uu));
            Vector2f p3 = projectVisible(viewProj, vx, vy, vw, vh, new Vector3f(origin).add(rr).sub(uu));
            if (p0 != null && p1 != null && p2 != null && p3 != null && pointInQuad(mx, my, p0, p1, p2, p3)) {
                best = ScaleHandle.UNIFORM;
                bestScore = dist2(mx, my, o2.x, o2.y);
            }
        }

        float thr2 = style.hitThresholdPx * style.hitThresholdPx;

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            Vector3f axisDir = axisWorld(axis, new Vector3f());
            float sin = axisSinToView(axisDir, camDir);
            float lenWorld = clamp(style.axisLengthPx * worldPerPx / Math.max(0.45f, sin), 0.05f, 25.0f);
            Vector3f end3 = new Vector3f(origin).fma(lenWorld, axisDir);
            Vector2f e2 = projectVisible(viewProj, vx, vy, vw, vh, end3);
            if (e2 == null) {
                continue;
            }
            float d = distToSegment2(mx, my, o2.x, o2.y, e2.x, e2.y);
            if (d > thr2) {
                continue;
            }
            float score = d / Math.max(0.35f, sin);
            if (score < bestScore) {
                bestScore = score;
                best = toHandle(axis);
            }
        }

        return best;
    }

    private Vector3f baseColor(GizmoAxis axis) {
        return switch (axis) {
            case X -> style.xColor;
            case Y -> style.yColor;
            case Z -> style.zColor;
            default -> new Vector3f(0.9f, 0.9f, 0.9f);
        };
    }

    private static void putQuad(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f rgb) {
        putTri(out, a, b, c, rgb);
        putTri(out, a, c, d, rgb);
    }

    private static void putQuad(FloatBuffer out,
                                Vector3f a,
                                Vector3f b,
                                Vector3f c,
                                Vector3f d,
                                Vector3f ca,
                                Vector3f cb,
                                Vector3f cc,
                                Vector3f cd) {
        putTri(out, a, ca, b, cb, c, cc);
        putTri(out, a, ca, c, cc, d, cd);
    }

    private static void putTri(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f rgb) {
        putVertex(out, a, rgb);
        putVertex(out, b, rgb);
        putVertex(out, c, rgb);
    }

    private static void putTri(FloatBuffer out,
                               Vector3f a,
                               Vector3f ca,
                               Vector3f b,
                               Vector3f cb,
                               Vector3f c,
                               Vector3f cc) {
        putVertex(out, a, ca);
        putVertex(out, b, cb);
        putVertex(out, c, cc);
    }

    private static void putVertex(FloatBuffer out, Vector3f p, Vector3f rgb) {
        out.put(p.x).put(p.y).put(p.z).put(rgb.x).put(rgb.y).put(rgb.z);
    }

    private static boolean pointInQuad(float px, float py, Vector2f a, Vector2f b, Vector2f c, Vector2f d) {
        return pointInTri(px, py, a, b, c) || pointInTri(px, py, a, c, d);
    }

    private static boolean pointInTri(float px, float py, Vector2f a, Vector2f b, Vector2f c) {
        float v0x = c.x - a.x;
        float v0y = c.y - a.y;
        float v1x = b.x - a.x;
        float v1y = b.y - a.y;
        float v2x = px - a.x;
        float v2y = py - a.y;

        float dot00 = v0x * v0x + v0y * v0y;
        float dot01 = v0x * v1x + v0y * v1y;
        float dot02 = v0x * v2x + v0y * v2y;
        float dot11 = v1x * v1x + v1y * v1y;
        float dot12 = v1x * v2x + v1y * v2y;

        float denom = (dot00 * dot11 - dot01 * dot01);
        if (Math.abs(denom) < 1e-8f) {
            return false;
        }
        float inv = 1.0f / denom;
        float u = (dot11 * dot02 - dot01 * dot12) * inv;
        float v = (dot00 * dot12 - dot01 * dot02) * inv;
        return u >= 0.0f && v >= 0.0f && (u + v) <= 1.0f;
    }

    private static void snapScale(Vector3f scale, float step) {
        if (!(step > 0.0f) || !Float.isFinite(step)) {
            return;
        }
        scale.x = Math.max(0.01f, Math.round(scale.x / step) * step);
        scale.y = Math.max(0.01f, Math.round(scale.y / step) * step);
        scale.z = Math.max(0.01f, Math.round(scale.z / step) * step);
    }

    private static ScaleHandle toHandle(GizmoAxis axis) {
        return switch (axis) {
            case X -> ScaleHandle.X;
            case Y -> ScaleHandle.Y;
            case Z -> ScaleHandle.Z;
            default -> ScaleHandle.NONE;
        };
    }

    private static GizmoAxis toAxis(ScaleHandle handle) {
        return switch (handle) {
            case X -> GizmoAxis.X;
            case Y -> GizmoAxis.Y;
            case Z -> GizmoAxis.Z;
            default -> GizmoAxis.NONE;
        };
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

    private Vector3f axisDirWorld(ScaleHandle handle, Vector3f dest) {
        return axisWorld(toAxis(handle), dest);
    }

    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
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
