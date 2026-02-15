package com.miry.ui.layout.constraints;

/**
 * One item in a 1D constraint solve.
 *
 * @param min minimum size (px)
 * @param pref preferred size (px)
 * @param max maximum size (px)
 * @param weight distribution weight when expanding/shrinking
 */
public record ConstraintItem(int min, int pref, int max, float weight) {
    public ConstraintItem {
        min = Math.max(0, min);
        max = Math.max(min, max);
        pref = clamp(pref, min, max);
        weight = Math.max(0.0f, weight);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

