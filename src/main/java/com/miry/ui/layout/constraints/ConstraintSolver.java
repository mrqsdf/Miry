package com.miry.ui.layout.constraints;

import java.util.Arrays;

/**
 * Tiny 1D constraint solver for UI row sizing.
 * <p>
 * Solves sizes for items with min/pref/max and weights, similar to a simplified flex layout but
 * with an explicit preferred size.
 */
public final class ConstraintSolver {
    private ConstraintSolver() {}

    /**
     * Solves widths that sum to {@code total}.
     *
     * @return widths array (same length as items).
     */
    public static int[] solve(int total, ConstraintItem[] items) {
        if (items == null || items.length == 0) {
            return new int[0];
        }
        int n = items.length;
        int[] out = new int[n];

        int target = Math.max(0, total);
        int sum = 0;
        for (int i = 0; i < n; i++) {
            ConstraintItem it = items[i];
            out[i] = it.pref();
            sum += out[i];
        }

        if (sum == target) {
            return out;
        }

        // Expand or shrink iteratively while respecting max/min.
        int guard = 0;
        while (sum != target && guard++ < 200) {
            int delta = target - sum;
            if (delta == 0) break;

            float totalWeight = 0.0f;
            for (int i = 0; i < n; i++) {
                ConstraintItem it = items[i];
                if (delta > 0) {
                    if (out[i] < it.max()) totalWeight += Math.max(1e-6f, it.weight());
                } else {
                    if (out[i] > it.min()) totalWeight += Math.max(1e-6f, it.weight());
                }
            }

            if (totalWeight <= 1e-6f) {
                // No weights available; distribute uniformly among eligible items.
                int eligible = 0;
                for (int i = 0; i < n; i++) {
                    ConstraintItem it = items[i];
                    if (delta > 0) {
                        if (out[i] < it.max()) eligible++;
                    } else {
                        if (out[i] > it.min()) eligible++;
                    }
                }
                if (eligible == 0) break;
                int step = delta / eligible;
                if (step == 0) step = delta > 0 ? 1 : -1;
                boolean progressed = false;
                for (int i = 0; i < n && sum != target; i++) {
                    ConstraintItem it = items[i];
                    if (delta > 0 && out[i] >= it.max()) continue;
                    if (delta < 0 && out[i] <= it.min()) continue;
                    int want = out[i] + step;
                    int clamped = clamp(want, it.min(), it.max());
                    if (clamped != out[i]) {
                        sum += (clamped - out[i]);
                        out[i] = clamped;
                        progressed = true;
                    }
                }
                if (!progressed) break;
                continue;
            }

            boolean progressed = false;
            for (int i = 0; i < n; i++) {
                if (sum == target) break;
                ConstraintItem it = items[i];
                int min = it.min();
                int max = it.max();
                if (delta > 0 && out[i] >= max) continue;
                if (delta < 0 && out[i] <= min) continue;

                float w = Math.max(1e-6f, it.weight());
                int share = Math.round(delta * (w / totalWeight));
                if (share == 0) share = delta > 0 ? 1 : -1;
                int want = out[i] + share;
                int clamped = clamp(want, min, max);
                if (clamped != out[i]) {
                    sum += (clamped - out[i]);
                    out[i] = clamped;
                    progressed = true;
                }
            }
            if (!progressed) break;
        }

        // Final correction if rounding left us off by a few px.
        int remain = target - Arrays.stream(out).sum();
        if (remain != 0) {
            int step = remain > 0 ? 1 : -1;
            int abs = Math.abs(remain);
            for (int iter = 0; iter < abs; iter++) {
                boolean applied = false;
                for (int i = 0; i < n; i++) {
                    ConstraintItem it = items[i];
                    int want = out[i] + step;
                    if (want < it.min() || want > it.max()) continue;
                    out[i] = want;
                    applied = true;
                    break;
                }
                if (!applied) break;
            }
        }

        return out;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

