package com.miry.ui.anim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Small lifetime manager for active {@link Tween}s.
 */
public final class AnimationManager {
    private final List<Tween> activeTweens = new ArrayList<>();

    /**
     * Creates and registers a new tween.
     */
    public Tween createTween(float from, float to, float duration, Easing easing) {
        Tween tween = new Tween(from, to, duration, easing);
        activeTweens.add(tween);
        return tween;
    }

    public void update(float dt) {
        Iterator<Tween> it = activeTweens.iterator();
        while (it.hasNext()) {
            Tween tween = it.next();
            tween.update(dt);
            if (tween.isFinished()) {
                it.remove();
            }
        }
    }

    public void clear() {
        activeTweens.clear();
    }

    public int activeCount() {
        return activeTweens.size();
    }
}
