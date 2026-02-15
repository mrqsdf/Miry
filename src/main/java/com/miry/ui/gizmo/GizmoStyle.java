package com.miry.ui.gizmo;

import org.joml.Vector3f;

/**
 * Styling parameters for gizmo rendering and hit testing.
 */
public final class GizmoStyle {
    /** Axis length on screen, in pixels. */
    public float axisLengthPx = 84.0f;

    /** Axis ribbon width, in pixels. */
    public float axisWidthPx = 6.0f;

    /** Arrow head length, in pixels. */
    public float headLengthPx = 14.0f;

    /** Arrow head width, in pixels. */
    public float headWidthPx = 12.0f;

    /** Center handle size (half extent), in pixels. */
    public float centerSizePx = 6.0f;

    /** Plane handle size (square side length), in pixels. */
    public float planeHandleSizePx = 14.0f;

    /** Plane handle offset from origin along each axis, in pixels. */
    public float planeHandleOffsetPx = 28.0f;

    /** Hit test threshold from the axis segment, in pixels. */
    public float hitThresholdPx = 16.0f;

    /** Scale handle size (square), in pixels. */
    public float scaleHandleSizePx = 12.0f;

    /** Scale drag sensitivity (scale units per pixel). */
    public float scaleDragPerPx = 0.01f;

    /** Rotation ring radius on screen, in pixels. */
    public float rotateRadiusPx = 72.0f;

    /** Rotation ring ribbon width, in pixels. */
    public float rotateBandWidthPx = 6.0f;

    /** Hit test threshold from the rotation ring, in pixels. */
    public float rotateHitThresholdPx = 12.0f;

    /** Segment count used to approximate the rotation ring. */
    public int rotateSegments = 64;

    /** Black outline thickness around handles, in pixels. */
    public float outlineWidthPx = 1.5f;

    /** Subtle glow strength applied to active handle (rendered as a second pass). */
    public float glowIntensity = 0.2f;

    /** Enables base→tip gradients on axis handles. */
    public boolean useGradients = true;

    // More saturated "Unity/Blender-ish" defaults.
    public Vector3f xColor = new Vector3f(0.95f, 0.25f, 0.25f);
    public Vector3f yColor = new Vector3f(0.38f, 0.95f, 0.38f);
    public Vector3f zColor = new Vector3f(0.35f, 0.60f, 0.99f);
    public Vector3f planeColor = new Vector3f(0.95f, 0.95f, 0.20f);
    public Vector3f centerColor = new Vector3f(1.0f, 1.0f, 1.0f);

    /** Enables snap by default (hosts can also toggle per-frame via modifiers). */
    public boolean snapEnabled = false;
    public float snapTranslate = 1.0f;
    public float snapRotate = 15.0f; // degrees
    public float snapScale = 0.1f;

    /** Brightness multiplier when hovered. */
    public float hoverBoost = 1.18f;

    /** Brightness multiplier when active (dragging). */
    public float activeBoost = 1.30f;
}
