package com.miry.ui.gizmo;

import com.miry.ui.input.UiInput;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.Objects;

/**
 * Viewport-independent 3D translate gizmo (X/Y/Z).
 * <p>
 * This gizmo does not depend on an OpenGL renderer. It relies on:
 * <ul>
 *   <li>a view-projection matrix</li>
 *   <li>a viewport rectangle in the same pixel coordinate space as the mouse</li>
 *   <li>a camera position (for billboard ribbon orientation)</li>
 * </ul>
 * and can write triangle geometry (pos + rgb color) into a buffer for the host to render.
 */
public final class TranslateGizmo3D {
    private final GizmoStyle style;

    private GizmoAxis hovered = GizmoAxis.NONE;
    private GizmoAxis active = GizmoAxis.NONE;

    private final Vector3f dragStartPos = new Vector3f();
    private float dragStartMouseX;
    private float dragStartMouseY;
    private float axisWorldPerPx;

    public TranslateGizmo3D() {
        this(new GizmoStyle());
    }

    public TranslateGizmo3D(GizmoStyle style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    public GizmoStyle style() {
        return style;
    }

    public GizmoAxis hoveredAxis() {
        return hovered;
    }

    public GizmoAxis activeAxis() {
        return active;
    }

    /**
     * Updates hover/drag state and mutates {@code inOutPos} while dragging.
     *
     * @return true if the gizmo is currently active (dragging) or hovered
     */
    public boolean update(UiInput input,
                          Matrix4f viewProj,
                          Vector3f cameraPos,
                          int viewportX,
                          int viewportY,
                          int viewportW,
                          int viewportH,
                          Vector3f inOutPos) {
        if (input == null || viewProj == null || cameraPos == null || inOutPos == null) {
            return false;
        }
        if (viewportW <= 0 || viewportH <= 0) {
            return false;
        }

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean inside = mx >= viewportX && my >= viewportY && mx < viewportX + viewportW && my < viewportY + viewportH;

        if (active == GizmoAxis.NONE) {
            hovered = inside ? hitTestAxis(mx, my, viewProj, viewportX, viewportY, viewportW, viewportH, cameraPos, inOutPos) : GizmoAxis.NONE;
        }

        if (input.mousePressed() && inside) {
            if (hovered != GizmoAxis.NONE) {
                active = hovered;
                dragStartPos.set(inOutPos);
                dragStartMouseX = mx;
                dragStartMouseY = my;
                axisWorldPerPx = worldPerPixelAlongDir(viewProj, viewportX, viewportY, viewportW, viewportH, inOutPos, active.dir());
            } else {
                active = GizmoAxis.NONE;
            }
        }

        if (active != GizmoAxis.NONE && input.mouseDown()) {
            Vector2f axisDir2 = axisScreenDir(viewProj, viewportX, viewportY, viewportW, viewportH, inOutPos, active.dir());
            float dx = mx - dragStartMouseX;
            float dy = my - dragStartMouseY;
            float pixelDelta = dx * axisDir2.x + dy * axisDir2.y;
            float worldDelta = pixelDelta * axisWorldPerPx;
            inOutPos.set(dragStartPos).fma(worldDelta, active.dir());
        }

        if (input.mouseReleased()) {
            active = GizmoAxis.NONE;
        }

        return hovered != GizmoAxis.NONE || active != GizmoAxis.NONE;
    }

    /**
     * Writes triangle geometry for the gizmo into {@code out}.
     * <p>
     * Vertex format is: {@code pos.xyz, color.rgb} (6 floats per vertex).
     */
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

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            writeAxis(out, axis, viewProj, camDir, viewportX, viewportY, viewportW, viewportH, origin);
        }

        writeCenter(out, viewProj, camDir, viewportX, viewportY, viewportW, viewportH, origin);
    }

    private void writeAxis(FloatBuffer out,
                           GizmoAxis axis,
                           Matrix4f viewProj,
                           Vector3f camDir,
                           int vx,
                           int vy,
                           int vw,
                           int vh,
                           Vector3f origin) {
        Vector3f axisDir = axis.dir();
        float axisWorldPerPx = worldPerPixelAlongDir(viewProj, vx, vy, vw, vh, origin, axisDir);
        float lenWorld = clamp(style.axisLengthPx * axisWorldPerPx, 0.05f, 10.0f);

        Vector3f side = safeNormalize(new Vector3f(axisDir).cross(camDir));
        if (side.lengthSquared() < 1e-8f) {
            side.set(0, 1, 0).cross(axisDir);
            safeNormalize(side);
        }

        float sideWorldPerPx = worldPerPixelAlongDir(viewProj, vx, vy, vw, vh, origin, side);
        float halfW = Math.max(0.0005f, (style.axisWidthPx * 0.5f) * sideWorldPerPx);

        float boost = (axis == active) ? style.activeBoost : (axis == hovered ? style.hoverBoost : 1.0f);
        Vector3f c = baseColor(axis).mul(boost, new Vector3f());

        Vector3f a = new Vector3f(origin);
        Vector3f b = new Vector3f(origin).fma(lenWorld, axisDir);

        // Ribbon quad (two triangles), billboarded using 'side'.
        Vector3f s = new Vector3f(side).mul(halfW);
        putQuad(out,
            new Vector3f(a).add(s),
            new Vector3f(a).sub(s),
            new Vector3f(b).sub(s),
            new Vector3f(b).add(s),
            c
        );

        // Arrow head (triangle), also billboarded in the side plane.
        float headLenWorld = clamp(style.headLengthPx * axisWorldPerPx, 0.01f, lenWorld * 0.75f);
        float headHalfW = Math.max(0.0005f, (style.headWidthPx * 0.5f) * sideWorldPerPx);
        Vector3f baseCenter = new Vector3f(b).fma(-headLenWorld, axisDir);
        Vector3f left = new Vector3f(baseCenter).fma(headHalfW, side);
        Vector3f right = new Vector3f(baseCenter).fma(-headHalfW, side);
        putTri(out, b, left, right, c);
    }

    private void writeCenter(FloatBuffer out,
                             Matrix4f viewProj,
                             Vector3f camDir,
                             int vx,
                             int vy,
                             int vw,
                             int vh,
                             Vector3f origin) {
        Vector3f upRef = new Vector3f(0, 1, 0);
        Vector3f right = safeNormalize(new Vector3f(upRef).cross(camDir));
        if (right.lengthSquared() < 1e-8f) {
            right.set(1, 0, 0);
        }
        Vector3f up = safeNormalize(new Vector3f(camDir).cross(right));

        float rightWorldPerPx = worldPerPixelAlongDir(viewProj, vx, vy, vw, vh, origin, right);
        float half = Math.max(0.001f, style.centerSizePx * rightWorldPerPx);

        Vector3f rr = new Vector3f(right).mul(half);
        Vector3f uu = new Vector3f(up).mul(half);

        Vector3f c = new Vector3f(0.85f, 0.85f, 0.90f);
        putQuad(out,
            new Vector3f(origin).add(rr).add(uu),
            new Vector3f(origin).sub(rr).add(uu),
            new Vector3f(origin).sub(rr).sub(uu),
            new Vector3f(origin).add(rr).sub(uu),
            c
        );
    }

    private GizmoAxis hitTestAxis(float mx,
                                  float my,
                                  Matrix4f viewProj,
                                  int vx,
                                  int vy,
                                  int vw,
                                  int vh,
                                  Vector3f cameraPos,
                                  Vector3f origin) {
        float thr2 = style.hitThresholdPx * style.hitThresholdPx;
        Vector2f o2 = project(viewProj, vx, vy, vw, vh, origin);

        Vector3f camDir = new Vector3f(cameraPos).sub(origin);
        if (camDir.lengthSquared() < 1e-8f) {
            camDir.set(0, 0, 1);
        } else {
            camDir.normalize();
        }

        GizmoAxis best = GizmoAxis.NONE;
        float bestD = Float.MAX_VALUE;

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            float axisWorldPerPx = worldPerPixelAlongDir(viewProj, vx, vy, vw, vh, origin, axis.dir());
            float lenWorld = clamp(style.axisLengthPx * axisWorldPerPx, 0.05f, 10.0f);

            Vector3f end3 = new Vector3f(origin).fma(lenWorld, axis.dir());
            Vector2f e2 = project(viewProj, vx, vy, vw, vh, end3);
            float d = distToSegment2(mx, my, o2.x, o2.y, e2.x, e2.y);
            if (d < bestD) {
                bestD = d;
                best = axis;
            }
        }

        return bestD <= thr2 ? best : GizmoAxis.NONE;
    }

    private Vector2f axisScreenDir(Matrix4f viewProj,
                                   int vx,
                                   int vy,
                                   int vw,
                                   int vh,
                                   Vector3f origin,
                                   Vector3f axisDir) {
        Vector2f o = project(viewProj, vx, vy, vw, vh, origin);
        Vector3f end3 = new Vector3f(origin).fma(1.0f, axisDir);
        Vector2f e = project(viewProj, vx, vy, vw, vh, end3);
        float dx = e.x - o.x;
        float dy = e.y - o.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 1e-6f) {
            return new Vector2f(1, 0);
        }
        return new Vector2f(dx / len, dy / len);
    }

    private float worldPerPixelAlongDir(Matrix4f viewProj,
                                        int vx,
                                        int vy,
                                        int vw,
                                        int vh,
                                        Vector3f origin,
                                        Vector3f dir) {
        Vector2f o = project(viewProj, vx, vy, vw, vh, origin);
        Vector3f end3 = new Vector3f(origin).fma(1.0f, dir);
        Vector2f e = project(viewProj, vx, vy, vw, vh, end3);
        float lenPx = (float) Math.sqrt(dist2(o.x, o.y, e.x, e.y));
        if (lenPx < 1.0f) {
            return 0.01f;
        }
        return 1.0f / lenPx;
    }

    private Vector2f project(Matrix4f viewProj, int vx, int vy, int vw, int vh, Vector3f p) {
        Vector4f clip = new Vector4f(p, 1.0f);
        viewProj.transform(clip);
        if (Math.abs(clip.w) < 1e-6f) {
            return new Vector2f(vx, vy);
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        float sx = vx + (ndcX * 0.5f + 0.5f) * vw;
        float sy = vy + (1.0f - (ndcY * 0.5f + 0.5f)) * vh;
        return new Vector2f(sx, sy);
    }

    private static void putQuad(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f color) {
        putTri(out, a, b, c, color);
        putTri(out, a, c, d, color);
    }

    private static void putTri(FloatBuffer out, Vector3f a, Vector3f b, Vector3f c, Vector3f color) {
        out.put(a.x).put(a.y).put(a.z).put(color.x).put(color.y).put(color.z);
        out.put(b.x).put(b.y).put(b.z).put(color.x).put(color.y).put(color.z);
        out.put(c.x).put(c.y).put(c.z).put(color.x).put(color.y).put(color.z);
    }

    private Vector3f baseColor(GizmoAxis axis) {
        return switch (axis) {
            case X -> style.xColor;
            case Y -> style.yColor;
            case Z -> style.zColor;
            default -> new Vector3f(0.8f, 0.8f, 0.85f);
        };
    }

    private static Vector3f safeNormalize(Vector3f v) {
        float lsq = v.lengthSquared();
        if (lsq < 1e-8f) {
            return v;
        }
        return v.mul((float) (1.0 / Math.sqrt(lsq)));
    }

    private static float distToSegment2(float px, float py, float ax, float ay, float bx, float by) {
        float abx = bx - ax;
        float aby = by - ay;
        float apx = px - ax;
        float apy = py - ay;
        float ab2 = abx * abx + aby * aby;
        float t = ab2 > 1e-8f ? ((apx * abx + apy * aby) / ab2) : 0.0f;
        t = clamp(t, 0.0f, 1.0f);
        float cx = ax + abx * t;
        float cy = ay + aby * t;
        return dist2(px, py, cx, cy);
    }

    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

