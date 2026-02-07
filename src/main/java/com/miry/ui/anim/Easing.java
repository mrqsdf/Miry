package com.miry.ui.anim;

/**
 * Common easing curves for {@link Tween}.
 */
public enum Easing {
    LINEAR,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_IN_BACK,
    EASE_OUT_BACK,
    EASE_IN_OUT_BACK;

    /**
     * Applies the easing curve to a normalized value in [0,1].
     */
    public float apply(float t) {
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> t * (2.0f - t);
            case EASE_IN_OUT_QUAD -> t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> { float v = t - 1.0f; yield v * v * v + 1.0f; }
            case EASE_IN_OUT_CUBIC -> t < 0.5f ? 4.0f * t * t * t : (t - 1.0f) * (2.0f * t - 2.0f) * (2.0f * t - 2.0f) + 1.0f;
            case EASE_IN_BACK -> { float c = 1.70158f; yield t * t * ((c + 1.0f) * t - c); }
            case EASE_OUT_BACK -> { float c = 1.70158f; float v = t - 1.0f; yield v * v * ((c + 1.0f) * v + c) + 1.0f; }
            case EASE_IN_OUT_BACK -> {
                float c = 1.70158f * 1.525f;
                if (t < 0.5f) {
                    yield (2.0f * t) * (2.0f * t) * ((c + 1.0f) * 2.0f * t - c) / 2.0f;
                }
                float v = 2.0f * t - 2.0f;
                yield (v * v * ((c + 1.0f) * v + c) + 2.0f) / 2.0f;
            }
        };
    }
}
