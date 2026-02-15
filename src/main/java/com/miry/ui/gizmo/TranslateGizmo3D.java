package com.miry.ui.gizmo;

import com.miry.ui.input.UiInput;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.List;

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
    private GizmoSpace space = GizmoSpace.WORLD;
    private final Matrix3f localAxes = new Matrix3f().identity();

    private TranslateHandle hovered = TranslateHandle.NONE;
    private TranslateHandle active = TranslateHandle.NONE;

    private final Vector3f dragStartPos = new Vector3f();
    private final Vector3f dragDelta = new Vector3f();
    private float dragStartAxisT;
    private float dragStartMouseX;
    private float dragStartMouseY;
    private boolean dragUseScreenProjection;
    private float dragWorldPerPx;
    private float dragAxisSin;
    private final Vector2f dragAxisScreenDir = new Vector2f();
    private final Vector3f dragPlaneNormal = new Vector3f();
    private final Vector3f dragStartPlaneHit = new Vector3f();
    private boolean dragUsePlaneIntersection;
    private String dragLabel;

    public TranslateGizmo3D() {
        this(new GizmoStyle());
    }

    public TranslateGizmo3D(GizmoStyle style) {
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

    public TranslateHandle hoveredHandle() {
        return hovered;
    }

    public TranslateHandle activeHandle() {
        return active;
    }

    /**
     * @return a short human-readable label for the current drag (e.g. {@code "X: +2.5"}), or null when not dragging
     */
    public String dragLabel() {
        return dragLabel;
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

        if (active == TranslateHandle.NONE) {
            hovered = inside ? hitTestHandle(mx, my, viewProj, viewportX, viewportY, viewportW, viewportH, cameraPos, inOutPos) : TranslateHandle.NONE;
        }

        if (input.mousePressed() && inside) {
            if (hovered != TranslateHandle.NONE) {
                active = hovered;
                dragStartPos.set(inOutPos);
                dragStartMouseX = mx;
                dragStartMouseY = my;

                // Prefer a stable screen-projected drag mapping (avoids "inverted" feeling under perspective).
                dragUseScreenProjection = false;
                dragUsePlaneIntersection = false;
                Vector3f camDir = new Vector3f(cameraPos).sub(dragStartPos);
                if (camDir.lengthSquared() >= 1e-8f) {
                    camDir.normalize();
                } else {
                    camDir.set(0, 0, 1);
                }
                dragWorldPerPx = worldUnitsPerPixel(viewProj, viewportW, viewportH, dragStartPos);

                if (isAxisHandle(active)) {
                    Vector3f axisDir = axisDirWorld(active, new Vector3f());
                    dragAxisSin = axisSinToView(axisDir, camDir);

                    Vector2f o2 = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, dragStartPos);
                    Vector2f e2 = projectVisible(viewProj, viewportX, viewportY, viewportW, viewportH, new Vector3f(dragStartPos).add(axisDir));
                    if (o2 != null && e2 != null) {
                        dragAxisScreenDir.set(e2).sub(o2);
                        if (dragAxisScreenDir.lengthSquared() >= 1e-6f) {
                            dragAxisScreenDir.normalize();
                            dragUseScreenProjection = true;
                        }
                    }

                    dragStartAxisT = axisParamAtMouse(viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, mx, my, dragStartPos, axisDir, 0.0f);
                } else {
                    Matrix4f invViewProj = new Matrix4f(viewProj).invert();
                    Vector3f rayDir = rayDirection(invViewProj, viewportX, viewportY, viewportW, viewportH, mx, my);
                    Vector3f n = planeNormalWorld(active, camDir, new Vector3f());
                    Vector3f hit = rayPlaneIntersection(cameraPos, rayDir, dragStartPos, n, new Vector3f());
                    if (hit != null) {
                        dragPlaneNormal.set(n);
                        dragStartPlaneHit.set(hit);
                        dragUsePlaneIntersection = true;
                    }
                }
            } else {
                active = TranslateHandle.NONE;
            }
        }

        if (active != TranslateHandle.NONE && input.mouseDown()) {
            if (isAxisHandle(active)) {
                Vector3f axisDir = axisDirWorld(active, new Vector3f());
                if (dragUseScreenProjection) {
                    float dx = mx - dragStartMouseX;
                    float dy = my - dragStartMouseY;
                    float alongPx = dx * dragAxisScreenDir.x + dy * dragAxisScreenDir.y;
                    float deltaWorld = alongPx * dragWorldPerPx / Math.max(0.45f, dragAxisSin);
                    inOutPos.set(dragStartPos).fma(deltaWorld, axisDir);
                } else {
                    float t = axisParamAtMouse(viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, mx, my, dragStartPos, axisDir, dragStartAxisT);
                    float delta = t - dragStartAxisT;
                    inOutPos.set(dragStartPos).fma(delta, axisDir);
                }
            } else if (dragUsePlaneIntersection) {
                Matrix4f invViewProj = new Matrix4f(viewProj).invert();
                Vector3f rayDir = rayDirection(invViewProj, viewportX, viewportY, viewportW, viewportH, mx, my);
                Vector3f hit = rayPlaneIntersection(cameraPos, rayDir, dragStartPos, dragPlaneNormal, new Vector3f());
                if (hit != null) {
                    Vector3f delta = hit.sub(dragStartPlaneHit, new Vector3f());
                    float alongN = delta.dot(dragPlaneNormal);
                    delta.fma(-alongN, dragPlaneNormal);
                    inOutPos.set(dragStartPos).add(delta);
                }
            }

            if (style.snapTranslate > 0.0f && (style.snapEnabled || input.ctrlDown())) {
                snapPosition(inOutPos, style.snapTranslate);
            }

            dragDelta.set(inOutPos).sub(dragStartPos);
            dragLabel = switch (active) {
                case X -> "X: " + formatSigned(dragDelta.x);
                case Y -> "Y: " + formatSigned(dragDelta.y);
                case Z -> "Z: " + formatSigned(dragDelta.z);
                case XY -> "X: " + formatSigned(dragDelta.x) + "  Y: " + formatSigned(dragDelta.y);
                case YZ -> "Y: " + formatSigned(dragDelta.y) + "  Z: " + formatSigned(dragDelta.z);
                case XZ -> "X: " + formatSigned(dragDelta.x) + "  Z: " + formatSigned(dragDelta.z);
                case CENTER -> "X: " + formatSigned(dragDelta.x) + "  Y: " + formatSigned(dragDelta.y) + "  Z: " + formatSigned(dragDelta.z);
                case NONE -> null;
            };
        }

        if (input.mouseReleased()) {
            active = TranslateHandle.NONE;
            dragLabel = null;
        }

        return hovered != TranslateHandle.NONE || active != TranslateHandle.NONE;
    }

    /**
     * Multi-object translation using the average position as a shared pivot.
     * <p>
     * Mutates every element of {@code inOutPositions} by the same delta.
     */
    public boolean updateMultiple(UiInput input,
                                  Matrix4f viewProj,
                                  Vector3f cameraPos,
                                  int viewportX,
                                  int viewportY,
                                  int viewportW,
                                  int viewportH,
                                  List<Vector3f> inOutPositions) {
        return updateMultiple(input, viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, inOutPositions, null);
    }

    /**
     * Multi-object translation using a shared pivot.
     *
     * @param pivotWorld when null, uses the average position
     */
    public boolean updateMultiple(UiInput input,
                                  Matrix4f viewProj,
                                  Vector3f cameraPos,
                                  int viewportX,
                                  int viewportY,
                                  int viewportW,
                                  int viewportH,
                                  List<Vector3f> inOutPositions,
                                  Vector3f pivotWorld) {
        if (inOutPositions == null || inOutPositions.isEmpty()) {
            return false;
        }
        Vector3f pivot = pivotWorld != null ? new Vector3f(pivotWorld) : average(inOutPositions, new Vector3f());
        Vector3f pivotMut = new Vector3f(pivot);
        boolean interacted = update(input, viewProj, cameraPos, viewportX, viewportY, viewportW, viewportH, pivotMut);
        Vector3f delta = pivotMut.sub(pivot, new Vector3f());
        if (delta.lengthSquared() > 1e-16f) {
            for (Vector3f p : inOutPositions) {
                if (p != null) {
                    p.add(delta);
                }
            }
        }
        return interacted;
    }

    private static Vector3f average(List<Vector3f> pts, Vector3f dest) {
        float x = 0, y = 0, z = 0;
        int n = 0;
        for (Vector3f p : pts) {
            if (p == null) continue;
            x += p.x;
            y += p.y;
            z += p.z;
            n++;
        }
        if (n <= 0) {
            return dest.set(0, 0, 0);
        }
        return dest.set(x / n, y / n, z / n);
    }

    private static String formatSigned(float v) {
        float x = Float.isFinite(v) ? v : 0.0f;
        return String.format(java.util.Locale.ROOT, "%+.3f", x);
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

        float worldPerPx = worldUnitsPerPixel(viewProj, viewportW, viewportH, origin);

        for (GizmoAxis axis : new GizmoAxis[]{GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z}) {
            writeAxis(out, axis, viewProj, camDir, viewportX, viewportY, viewportW, viewportH, origin, worldPerPx);
        }

        writePlaneHandles(out, camDir, origin, worldPerPx);
        writeCenter(out, viewProj, camDir, viewportX, viewportY, viewportW, viewportH, origin, worldPerPx);
    }

    private void writeAxis(FloatBuffer out,
                           GizmoAxis axis,
                           Matrix4f viewProj,
                           Vector3f camDir,
                           int vx,
                           int vy,
                           int vw,
                           int vh,
                           Vector3f origin,
                           float worldPerPx) {
        Vector3f axisDir = axisWorld(axis, new Vector3f());
        // Keep the on-screen axis length stable even when the axis points toward/away from the camera
        // (perspective foreshortening). Approximate by scaling by 1/sin(theta).
        float sin = axisSinToView(axisDir, camDir);
        float lenWorld = clamp(style.axisLengthPx * worldPerPx / Math.max(0.45f, sin), 0.05f, 25.0f);

        Vector3f side = safeNormalize(new Vector3f(axisDir).cross(camDir));
        if (side.lengthSquared() < 1e-8f) {
            Vector3f upRef = new Vector3f(0, 1, 0);
            Vector3f camRight = safeNormalize(new Vector3f(upRef).cross(camDir));
            Vector3f camUp = safeNormalize(new Vector3f(camDir).cross(camRight));
            side.set(axisDir).cross(camUp);
            safeNormalize(side);
            if (side.lengthSquared() < 1e-8f) {
                side.set(axisDir).cross(camRight);
                safeNormalize(side);
            }
        }

        float halfW = Math.max(0.0005f, (style.axisWidthPx * 0.5f) * worldPerPx);

        TranslateHandle handle = toHandle(axis);
        float boost = (handle == active) ? (style.activeBoost + style.glowIntensity) : (handle == hovered ? style.hoverBoost : 1.0f);
        Vector3f c = baseColor(axis).mul(boost, new Vector3f());
        Vector3f cBase = style.useGradients ? new Vector3f(c).mul(0.85f) : c;
        Vector3f cTip = style.useGradients ? new Vector3f(c).mul(1.05f) : c;

        Vector3f a = new Vector3f(origin);
        Vector3f b = new Vector3f(origin).fma(lenWorld, axisDir);

        // Ribbon quad (two triangles), billboarded using 'side'.
        Vector3f s = new Vector3f(side).mul(halfW);
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;
        if (outline > 0.0f) {
            Vector3f so = new Vector3f(side).mul(halfW + outline);
            Vector3f black = new Vector3f(0, 0, 0);
            putQuad(out,
                new Vector3f(a).add(so),
                new Vector3f(a).sub(so),
                new Vector3f(b).sub(so),
                new Vector3f(b).add(so),
                black
            );
        }

        putQuad(out,
            new Vector3f(a).add(s),
            new Vector3f(a).sub(s),
            new Vector3f(b).sub(s),
            new Vector3f(b).add(s),
            cBase, cBase, cTip, cTip
        );

        // Arrow head: 3D cone with optional gradient.
        float headLenWorld = clamp(style.headLengthPx * worldPerPx / Math.max(0.45f, sin), 0.01f, lenWorld * 0.85f);
        float headRadiusWorld = Math.max(0.0006f, (style.headWidthPx * 0.5f) * worldPerPx);
        Vector3f baseCenter = new Vector3f(b).fma(-headLenWorld, axisDir);

        Vector3f u = perpendicularUnit(axisDir, new Vector3f());
        Vector3f v = safeNormalize(new Vector3f(axisDir).cross(u));

        int seg = 12;
        if (outline > 0.0f) {
            writeCone(out, b, baseCenter, u, v, headRadiusWorld + outline, seg, new Vector3f(0, 0, 0));
        }
        writeCone(out, b, baseCenter, u, v, headRadiusWorld, seg, cTip, cBase);
    }

    private void writeCenter(FloatBuffer out,
                             Matrix4f viewProj,
                             Vector3f camDir,
                             int vx,
                             int vy,
                             int vw,
                             int vh,
                             Vector3f origin,
                             float worldPerPx) {
        Vector3f upRef = new Vector3f(0, 1, 0);
        Vector3f right = safeNormalize(new Vector3f(upRef).cross(camDir));
        if (right.lengthSquared() < 1e-8f) {
            right.set(1, 0, 0);
        }
        Vector3f up = safeNormalize(new Vector3f(camDir).cross(right));

        float half = Math.max(0.001f, style.centerSizePx * worldPerPx);

        Vector3f rr = new Vector3f(right).mul(half);
        Vector3f uu = new Vector3f(up).mul(half);

        float boost = (active == TranslateHandle.CENTER) ? (style.activeBoost + style.glowIntensity) : (hovered == TranslateHandle.CENTER ? style.hoverBoost : 1.0f);
        Vector3f c = new Vector3f(style.centerColor).mul(boost);
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;
        if (outline > 0.0f) {
            Vector3f rrO = new Vector3f(right).mul(half + outline);
            Vector3f uuO = new Vector3f(up).mul(half + outline);
            Vector3f black = new Vector3f(0, 0, 0);
            putQuad(out,
                new Vector3f(origin).add(rrO).add(uuO),
                new Vector3f(origin).sub(rrO).add(uuO),
                new Vector3f(origin).sub(rrO).sub(uuO),
                new Vector3f(origin).add(rrO).sub(uuO),
                black
            );
        }
        putQuad(out,
            new Vector3f(origin).add(rr).add(uu),
            new Vector3f(origin).sub(rr).add(uu),
            new Vector3f(origin).sub(rr).sub(uu),
            new Vector3f(origin).add(rr).sub(uu),
            c
        );
    }

    private void writePlaneHandles(FloatBuffer out, Vector3f camDir, Vector3f origin, float worldPerPx) {
        float sizeHalf = Math.max(0.001f, style.planeHandleSizePx * 0.5f * worldPerPx);
        float outline = Math.max(0.0f, style.outlineWidthPx) * worldPerPx;

        Vector3f xDir = axisWorld(GizmoAxis.X, new Vector3f());
        Vector3f yDir = axisWorld(GizmoAxis.Y, new Vector3f());
        Vector3f zDir = axisWorld(GizmoAxis.Z, new Vector3f());

        // Keep offsets relatively stable on-screen by applying the same foreshortening correction used for axis length.
        float offX = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(xDir, camDir));
        float offY = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(yDir, camDir));
        float offZ = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(zDir, camDir));

        writePlaneHandle(out, TranslateHandle.XY, origin, xDir, yDir, offX, offY, sizeHalf, outline);
        writePlaneHandle(out, TranslateHandle.YZ, origin, yDir, zDir, offY, offZ, sizeHalf, outline);
        writePlaneHandle(out, TranslateHandle.XZ, origin, xDir, zDir, offX, offZ, sizeHalf, outline);
    }

    private void writePlaneHandle(FloatBuffer out,
                                  TranslateHandle handle,
                                  Vector3f origin,
                                  Vector3f axA,
                                  Vector3f axB,
                                  float offsetA,
                                  float offsetB,
                                  float half,
                                  float outline) {
        float boost = (active == handle) ? (style.activeBoost + style.glowIntensity) : (hovered == handle ? style.hoverBoost : 1.0f);
        Vector3f c = new Vector3f(style.planeColor).mul(boost);

        Vector3f center = new Vector3f(origin).fma(offsetA, axA).fma(offsetB, axB);
        Vector3f a = new Vector3f(center).fma(half, axA).fma(half, axB);
        Vector3f b = new Vector3f(center).fma(-half, axA).fma(half, axB);
        Vector3f c2 = new Vector3f(center).fma(-half, axA).fma(-half, axB);
        Vector3f d = new Vector3f(center).fma(half, axA).fma(-half, axB);

        if (outline > 0.0f) {
            float ho = half + outline;
            Vector3f ao = new Vector3f(center).fma(ho, axA).fma(ho, axB);
            Vector3f bo = new Vector3f(center).fma(-ho, axA).fma(ho, axB);
            Vector3f co = new Vector3f(center).fma(-ho, axA).fma(-ho, axB);
            Vector3f do2 = new Vector3f(center).fma(ho, axA).fma(-ho, axB);
            putQuad(out, ao, bo, co, do2, new Vector3f(0, 0, 0));
        }
        putQuad(out, a, b, c2, d, c);
    }

    private TranslateHandle hitTestHandle(float mx,
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
            return TranslateHandle.NONE;
        }

        Vector3f camDir = new Vector3f(cameraPos).sub(origin);
        if (camDir.lengthSquared() < 1e-8f) {
            camDir.set(0, 0, 1);
        } else {
            camDir.normalize();
        }

        TranslateHandle best = TranslateHandle.NONE;
        float[] bestScore = new float[]{Float.MAX_VALUE};

        // Center handle (billboard quad).
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
                float score = dist2(mx, my, o2.x, o2.y);
                bestScore[0] = score;
                best = TranslateHandle.CENTER;
            }
        }

        // Plane handles (XY/YZ/XZ).
        Vector3f xDir = axisWorld(GizmoAxis.X, new Vector3f());
        Vector3f yDir = axisWorld(GizmoAxis.Y, new Vector3f());
        Vector3f zDir = axisWorld(GizmoAxis.Z, new Vector3f());
        float offX = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(xDir, camDir));
        float offY = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(yDir, camDir));
        float offZ = (style.planeHandleOffsetPx * worldPerPx) / Math.max(0.45f, axisSinToView(zDir, camDir));
        float half = Math.max(0.001f, style.planeHandleSizePx * 0.5f * worldPerPx);
        best = hitTestPlane(mx, my, viewProj, vx, vy, vw, vh, origin, TranslateHandle.XY, xDir, yDir, offX, offY, half, best, bestScore);
        best = hitTestPlane(mx, my, viewProj, vx, vy, vw, vh, origin, TranslateHandle.YZ, yDir, zDir, offY, offZ, half, best, bestScore);
        best = hitTestPlane(mx, my, viewProj, vx, vy, vw, vh, origin, TranslateHandle.XZ, xDir, zDir, offX, offZ, half, best, bestScore);

        // Axis handles.
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
            if (score < bestScore[0]) {
                bestScore[0] = score;
                best = toHandle(axis);
            }
        }

        return best;
    }

    private TranslateHandle hitTestPlane(float mx,
                                         float my,
                                         Matrix4f viewProj,
                                         int vx,
                                         int vy,
                                         int vw,
                                         int vh,
                                         Vector3f origin,
                                         TranslateHandle handle,
                                         Vector3f axA,
                                         Vector3f axB,
                                         float offA,
                                         float offB,
                                         float half,
                                         TranslateHandle currentBest,
                                         float[] currentBestScore) {
        Vector3f center3 = new Vector3f(origin).fma(offA, axA).fma(offB, axB);
        Vector3f a = new Vector3f(center3).fma(half, axA).fma(half, axB);
        Vector3f b = new Vector3f(center3).fma(-half, axA).fma(half, axB);
        Vector3f c = new Vector3f(center3).fma(-half, axA).fma(-half, axB);
        Vector3f d = new Vector3f(center3).fma(half, axA).fma(-half, axB);
        Vector2f p0 = projectVisible(viewProj, vx, vy, vw, vh, a);
        Vector2f p1 = projectVisible(viewProj, vx, vy, vw, vh, b);
        Vector2f p2 = projectVisible(viewProj, vx, vy, vw, vh, c);
        Vector2f p3 = projectVisible(viewProj, vx, vy, vw, vh, d);
        if (p0 == null || p1 == null || p2 == null || p3 == null) {
            return currentBest;
        }
        if (!pointInQuad(mx, my, p0, p1, p2, p3)) {
            return currentBest;
        }
        float cx = (p0.x + p1.x + p2.x + p3.x) * 0.25f;
        float cy = (p0.y + p1.y + p2.y + p3.y) * 0.25f;
        float score = dist2(mx, my, cx, cy);
        if (score < currentBestScore[0]) {
            currentBestScore[0] = score;
            return handle;
        }
        return currentBest;
    }

    private static float axisParamAtMouse(Matrix4f viewProj,
                                          Vector3f cameraPos,
                                          int vx,
                                          int vy,
                                          int vw,
                                          int vh,
                                          float mouseX,
                                          float mouseY,
                                          Vector3f lineOrigin,
                                          Vector3f axisDir,
                                          float fallbackT) {
        Matrix4f invViewProj = new Matrix4f(viewProj).invert();
        Vector3f rayDir = rayDirection(invViewProj, vx, vy, vw, vh, mouseX, mouseY);

        Vector3f a = safeNormalize(new Vector3f(axisDir));
        Vector3f b = safeNormalize(new Vector3f(rayDir));

        Vector3f w0 = new Vector3f(lineOrigin).sub(cameraPos);
        float B = a.dot(b);
        float D = a.dot(w0);
        float E = b.dot(w0);
        float denom = 1.0f - B * B;
        if (Math.abs(denom) < 1e-6f) {
            return fallbackT;
        }
        return (B * E - D) / denom;
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

    private static float worldUnitsPerPixel(Matrix4f viewProj, int viewportW, int viewportH, Vector3f origin) {
        if (viewportW <= 0 || viewportH <= 0) {
            return 0.01f;
        }
        Vector4f clip = new Vector4f(origin, 1.0f);
        viewProj.transform(clip);
        if (clip.w <= 1e-6f) {
            return 0.01f;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;

        Matrix4f invViewProj = new Matrix4f(viewProj).invert();
        Vector3f p0 = unproject(invViewProj, ndcX, ndcY, ndcZ);

        float dx = 2.0f / viewportW;
        float dy = 2.0f / viewportH;
        Vector3f pRight = unproject(invViewProj, ndcX + dx, ndcY, ndcZ);
        Vector3f pDown = unproject(invViewProj, ndcX, ndcY - dy, ndcZ);

        float ux = pRight.distance(p0);
        float uy = pDown.distance(p0);
        float out = (ux + uy) * 0.5f;
        if (!Float.isFinite(out) || out <= 1e-8f) {
            return 0.01f;
        }
        return out;
    }

    private static Vector2f projectVisible(Matrix4f viewProj, int vx, int vy, int vw, int vh, Vector3f p) {
        Vector4f clip = new Vector4f(p, 1.0f);
        viewProj.transform(clip);
        if (clip.w <= 1e-6f) {
            return null;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        float sx = vx + (ndcX * 0.5f + 0.5f) * vw;
        float sy = vy + (1.0f - (ndcY * 0.5f + 0.5f)) * vh;
        return new Vector2f(sx, sy);
    }

    private static Vector3f unproject(Matrix4f invViewProj, float ndcX, float ndcY, float ndcZ) {
        Vector4f v = new Vector4f(ndcX, ndcY, ndcZ, 1.0f);
        invViewProj.transform(v);
        if (Math.abs(v.w) < 1e-6f) {
            return new Vector3f();
        }
        return new Vector3f(v.x / v.w, v.y / v.w, v.z / v.w);
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

    private static void writeCone(FloatBuffer out,
                                  Vector3f tip,
                                  Vector3f baseCenter,
                                  Vector3f u,
                                  Vector3f v,
                                  float radiusWorld,
                                  int segments,
                                  Vector3f rgb) {
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (i * (Math.PI * 2.0) / segments);
            float a1 = (float) ((i + 1) * (Math.PI * 2.0) / segments);
            Vector3f p0 = new Vector3f(baseCenter).fma((float) Math.cos(a0) * radiusWorld, u).fma((float) Math.sin(a0) * radiusWorld, v);
            Vector3f p1 = new Vector3f(baseCenter).fma((float) Math.cos(a1) * radiusWorld, u).fma((float) Math.sin(a1) * radiusWorld, v);
            putTri(out, tip, p0, p1, rgb);
        }
    }

    private static void writeCone(FloatBuffer out,
                                  Vector3f tip,
                                  Vector3f baseCenter,
                                  Vector3f u,
                                  Vector3f v,
                                  float radiusWorld,
                                  int segments,
                                  Vector3f tipColor,
                                  Vector3f baseColor) {
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (i * (Math.PI * 2.0) / segments);
            float a1 = (float) ((i + 1) * (Math.PI * 2.0) / segments);
            Vector3f p0 = new Vector3f(baseCenter).fma((float) Math.cos(a0) * radiusWorld, u).fma((float) Math.sin(a0) * radiusWorld, v);
            Vector3f p1 = new Vector3f(baseCenter).fma((float) Math.cos(a1) * radiusWorld, u).fma((float) Math.sin(a1) * radiusWorld, v);
            putTri(out, tip, tipColor, p0, baseColor, p1, baseColor);
        }
    }

    private static Vector3f perpendicularUnit(Vector3f axisDir, Vector3f dest) {
        if (Math.abs(axisDir.y) < 0.85f) {
            dest.set(0, 1, 0);
        } else {
            dest.set(1, 0, 0);
        }
        dest.cross(axisDir);
        return safeNormalize(dest);
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

    private static void snapPosition(Vector3f pos, float step) {
        if (!(step > 0.0f) || !Float.isFinite(step)) {
            return;
        }
        pos.x = Math.round(pos.x / step) * step;
        pos.y = Math.round(pos.y / step) * step;
        pos.z = Math.round(pos.z / step) * step;
    }

    private static boolean isAxisHandle(TranslateHandle handle) {
        return handle == TranslateHandle.X || handle == TranslateHandle.Y || handle == TranslateHandle.Z;
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

    private Vector3f axisDirWorld(TranslateHandle handle, Vector3f dest) {
        return axisWorld(toAxis(handle), dest);
    }

    private static TranslateHandle toHandle(GizmoAxis axis) {
        return switch (axis) {
            case X -> TranslateHandle.X;
            case Y -> TranslateHandle.Y;
            case Z -> TranslateHandle.Z;
            default -> TranslateHandle.NONE;
        };
    }

    private static GizmoAxis toAxis(TranslateHandle handle) {
        return switch (handle) {
            case X -> GizmoAxis.X;
            case Y -> GizmoAxis.Y;
            case Z -> GizmoAxis.Z;
            default -> GizmoAxis.NONE;
        };
    }

    private Vector3f planeNormalWorld(TranslateHandle handle, Vector3f camDir, Vector3f dest) {
        return switch (handle) {
            case XY -> axisWorld(GizmoAxis.Z, dest);
            case YZ -> axisWorld(GizmoAxis.X, dest);
            case XZ -> axisWorld(GizmoAxis.Y, dest);
            case CENTER -> {
                dest.set(camDir);
                if (dest.lengthSquared() < 1e-8f) {
                    dest.set(0, 0, 1);
                }
                yield safeNormalize(dest);
            }
            default -> axisWorld(GizmoAxis.Z, dest);
        };
    }

    private static Vector3f rayPlaneIntersection(Vector3f rayOrigin,
                                                 Vector3f rayDir,
                                                 Vector3f planePoint,
                                                 Vector3f planeNormal,
                                                 Vector3f dest) {
        float denom = rayDir.dot(planeNormal);
        if (Math.abs(denom) < 1e-6f) {
            return null;
        }
        float t = new Vector3f(planePoint).sub(rayOrigin).dot(planeNormal) / denom;
        if (!Float.isFinite(t)) {
            return null;
        }
        return dest.set(rayOrigin).fma(t, rayDir);
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

    private static float axisSinToView(Vector3f axisDir, Vector3f camDir) {
        // Both are expected normalized.
        float d = Math.abs(axisDir.dot(camDir));
        float s2 = Math.max(0.0f, 1.0f - d * d);
        return (float) Math.sqrt(s2);
    }
}
