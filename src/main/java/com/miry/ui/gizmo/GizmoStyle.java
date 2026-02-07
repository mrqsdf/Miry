package com.miry.ui.gizmo;

import org.joml.Vector3f;

/**
 * Styling parameters for gizmo rendering and hit testing.
 */
public final class GizmoStyle {
    /** Axis length on screen, in pixels. */
    public float axisLengthPx = 84.0f;

    /** Axis ribbon width, in pixels. */
    public float axisWidthPx = 4.0f;

    /** Arrow head length, in pixels. */
    public float headLengthPx = 14.0f;

    /** Arrow head width, in pixels. */
    public float headWidthPx = 12.0f;

    /** Center handle size (half extent), in pixels. */
    public float centerSizePx = 6.0f;

    /** Hit test threshold from the axis segment, in pixels. */
    public float hitThresholdPx = 10.0f;

    public Vector3f xColor = new Vector3f(0.92f, 0.32f, 0.28f);
    public Vector3f yColor = new Vector3f(0.30f, 0.86f, 0.36f);
    public Vector3f zColor = new Vector3f(0.30f, 0.55f, 0.96f);

    /** Brightness multiplier when hovered. */
    public float hoverBoost = 1.18f;

    /** Brightness multiplier when active (dragging). */
    public float activeBoost = 1.30f;
}

