package com.miry.ui.gizmo;

import org.joml.Vector3f;

/**
 * Axis identifiers for 3D gizmos.
 */
public enum GizmoAxis {
    NONE(new Vector3f(0, 0, 0)),
    X(new Vector3f(1, 0, 0)),
    Y(new Vector3f(0, 1, 0)),
    Z(new Vector3f(0, 0, 1));

    private final Vector3f axis;

    GizmoAxis(Vector3f axis) {
        this.axis = axis;
    }

    /**
     * Unit direction in world space.
     */
    public Vector3f dir() {
        return axis;
    }
}

