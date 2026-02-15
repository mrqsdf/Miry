package com.miry.ui.util;

import org.joml.Vector3f;

/**
 * Math utility functions used throughout the UI library.
 */
public final class MathUtils {
    private MathUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Clamps a value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between min and max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between 0.0 and 1.0.
     */
    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * Clamps an integer value between 0 and 255 (for color channels).
     */
    public static int clamp255(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Calculates squared distance between two 2D points.
     * Avoids expensive sqrt operation when only comparing distances.
     */
    public static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    /**
     * Calculates distance between two 2D points.
     */
    public static float dist(float ax, float ay, float bx, float by) {
        return (float) Math.sqrt(dist2(ax, ay, bx, by));
    }

    /**
     * Linear interpolation between a and b.
     *
     * @param a start value
     * @param b end value
     * @param t interpolation factor (0.0 to 1.0)
     * @return interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Normalizes a vector with safety check for zero-length vectors.
     * Returns the original vector if its length is too small.
     */
    public static Vector3f safeNormalize(Vector3f v) {
        float lsq = v.lengthSquared();
        if (lsq < 1e-8f) {
            return v;
        }
        return v.mul((float) (1.0 / Math.sqrt(lsq)));
    }

    /**
     * Normalizes a vector with safety check for zero-length vectors.
     * Returns a new vector instead of modifying the input.
     */
    public static Vector3f safeNormalizeCopy(Vector3f v) {
        float lsq = v.lengthSquared();
        if (lsq < 1e-8f) {
            return new Vector3f(v);
        }
        return new Vector3f(v).mul((float) (1.0 / Math.sqrt(lsq)));
    }
}
