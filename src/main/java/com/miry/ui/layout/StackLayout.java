package com.miry.ui.layout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simple z-ordered stacking layout (useful for overlays/popups).
 */
public final class StackLayout {
    private final List<Layer> layers = new ArrayList<>();

    public void addLayer(int zIndex, int x, int y, int width, int height) {
        layers.add(new Layer(zIndex, x, y, width, height));
        layers.sort(Comparator.comparingInt(Layer::zIndex));
    }

    public List<Layer> layers() {
        return layers;
    }

    public void clear() {
        layers.clear();
    }

    /**
     * A single layer rectangle with an ordering key.
     */
    public record Layer(int zIndex, int x, int y, int width, int height) {}
}
