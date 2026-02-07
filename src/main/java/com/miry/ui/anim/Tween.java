package com.miry.ui.anim;

/**
 * Tween (time-based interpolation) between two float values.
 * <p>
 * {@link #update(float)} advances time and {@link #value()} returns the eased value.
 */
public final class Tween {
    private float startValue;
    private float endValue;
    private float duration;
    private float elapsed;
    private Easing easing;
    private boolean finished;

    public Tween(float from, float to, float duration, Easing easing) {
        this.startValue = from;
        this.endValue = to;
        this.duration = Math.max(0.001f, duration);
        this.easing = easing != null ? easing : Easing.LINEAR;
        this.elapsed = 0.0f;
        this.finished = false;
    }

    public void update(float dt) {
        if (finished) return;
        elapsed += dt;
        if (elapsed >= duration) {
            elapsed = duration;
            finished = true;
        }
    }

    public float value() {
        if (duration <= 0.0f) return endValue;
        float t = Math.min(1.0f, elapsed / duration);
        t = easing.apply(t);
        return startValue + (endValue - startValue) * t;
    }

    public boolean isFinished() {
        return finished;
    }

    public void reset(float from, float to) {
        this.startValue = from;
        this.endValue = to;
        this.elapsed = 0.0f;
        this.finished = false;
    }
}
