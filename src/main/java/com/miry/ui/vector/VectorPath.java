package com.miry.ui.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Flattened vector path composed of polylines (contours) in icon space.
 * <p>
 * This is a minimal building block for stroke-based icon rendering (using capsules).
 */
public final class VectorPath {
    public static final class Contour {
        private final float[] points; // x0,y0,x1,y1,...
        private final boolean closed;

        Contour(float[] points, boolean closed) {
            this.points = points;
            this.closed = closed;
        }

        public float[] points() {
            return points;
        }

        public boolean closed() {
            return closed;
        }
    }

    private final List<Contour> contours;

    VectorPath(List<Contour> contours) {
        this.contours = Collections.unmodifiableList(new ArrayList<>(contours));
    }

    public List<Contour> contours() {
        return contours;
    }
}

