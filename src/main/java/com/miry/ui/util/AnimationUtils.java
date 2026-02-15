package com.miry.ui.util;

/**
 * Animation and interpolation utilities.
 */
public final class AnimationUtils {
    private AnimationUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Exponential approach for smooth animations.
     * Uses exponential decay: 1 - e^(-speed * dt)
     * This creates natural-feeling animations with quick initial response
     * and smooth deceleration.
     *
     * @param value current value
     * @param target target value
     * @param speed approach speed (higher = faster, typically 5-20)
     * @param dt delta time in seconds
     * @return new value approaching target
     */
    public static float approachExp(float value, float target, float speed, float dt) {
        if (dt <= 0.0f) {
            return target;
        }
        float k = 1.0f - (float) Math.exp(-speed * dt);
        return value + (target - value) * k;
    }

    /**
     * Exponential approach for integer values.
     */
    public static int approachExp(int value, int target, float speed, float dt) {
        return Math.round(approachExp((float) value, (float) target, speed, dt));
    }

    /**
     * Linear approach for animations.
     * Moves value toward target by a fixed amount per frame.
     *
     * @param value current value
     * @param target target value
     * @param maxDelta maximum change per step
     * @return new value approaching target
     */
    public static float approachLinear(float value, float target, float maxDelta) {
        float diff = target - value;
        if (Math.abs(diff) <= maxDelta) {
            return target;
        }
        return value + Math.signum(diff) * maxDelta;
    }

    /**
     * Smooth step interpolation (cubic Hermite).
     * Provides smooth acceleration and deceleration at the edges.
     *
     * @param t interpolation factor (0.0 to 1.0)
     * @return smoothed value (0.0 to 1.0)
     */
    public static float smoothStep(float t) {
        float tt = MathUtils.clamp01(t);
        return tt * tt * (3.0f - 2.0f * tt);
    }

    /**
     * Smoother step interpolation (quintic Hermite).
     * Even smoother than smoothStep.
     *
     * @param t interpolation factor (0.0 to 1.0)
     * @return smoothed value (0.0 to 1.0)
     */
    public static float smootherStep(float t) {
        float tt = MathUtils.clamp01(t);
        return tt * tt * tt * (tt * (tt * 6.0f - 15.0f) + 10.0f);
    }

    /**
     * Ease-out cubic function.
     * Fast initial change that slows down.
     *
     * @param t interpolation factor (0.0 to 1.0)
     * @return eased value (0.0 to 1.0)
     */
    public static float easeOutCubic(float t) {
        float tt = MathUtils.clamp01(t);
        float t1 = 1.0f - tt;
        return 1.0f - t1 * t1 * t1;
    }

    /**
     * Ease-in cubic function.
     * Slow initial change that speeds up.
     *
     * @param t interpolation factor (0.0 to 1.0)
     * @return eased value (0.0 to 1.0)
     */
    public static float easeInCubic(float t) {
        float tt = MathUtils.clamp01(t);
        return tt * tt * tt;
    }

    /**
     * Ease-in-out cubic function.
     * Slow at the start and end, fast in the middle.
     *
     * @param t interpolation factor (0.0 to 1.0)
     * @return eased value (0.0 to 1.0)
     */
    public static float easeInOutCubic(float t) {
        float tt = MathUtils.clamp01(t);
        if (tt < 0.5f) {
            return 4.0f * tt * tt * tt;
        } else {
            float t1 = 2.0f * tt - 2.0f;
            return 1.0f + t1 * t1 * t1 / 2.0f;
        }
    }
}
