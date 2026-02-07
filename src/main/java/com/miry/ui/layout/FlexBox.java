package com.miry.ui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal "flex" size distribution helper for one-dimensional layouts.
 * <p>
 * This is used to compute pixel sizes/offsets for a list of {@link FlexItem}s given an available
 * size (width or height).
 */
public final class FlexBox {
    private final FlexDirection direction;
    private final int gap;
    private final List<FlexItem> items = new ArrayList<>();

    public FlexBox(FlexDirection direction, int gap) {
        this.direction = direction;
        this.gap = Math.max(0, gap);
    }

    public void addItem(FlexItem item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
    }

    public void layout(int availableSize) {
        if (items.isEmpty()) return;

        int totalGap = gap * (items.size() - 1);
        int workingSize = Math.max(0, availableSize - totalGap);

        int totalMin = 0;
        int totalMax = 0;
        float totalGrow = 0.0f;
        float totalShrink = 0.0f;

        for (FlexItem item : items) {
            totalMin += item.minSize();
            totalMax += Math.min(item.maxSize(), workingSize);
            totalGrow += item.grow();
            totalShrink += item.shrink();
        }

        if (workingSize >= totalMax) {
            distributeGrow(workingSize, totalMax, totalGrow);
        } else if (workingSize < totalMin) {
            distributeShrink(workingSize, totalMin, totalShrink);
        } else {
            for (FlexItem item : items) {
                item.setComputedSize(clamp(workingSize / items.size(), item.minSize(), item.maxSize()));
            }
        }

        int offset = 0;
        for (FlexItem item : items) {
            item.setComputedOffset(offset);
            offset += item.computedSize() + gap;
        }
    }

    private void distributeGrow(int availableSize, int totalMax, float totalGrow) {
        int remaining = availableSize - totalMax;
        for (FlexItem item : items) {
            int base = Math.min(item.maxSize(), availableSize);
            int extra = (totalGrow > 0.0f) ? (int) (remaining * (item.grow() / totalGrow)) : 0;
            item.setComputedSize(base + extra);
        }
    }

    private void distributeShrink(int availableSize, int totalMin, float totalShrink) {
        int deficit = totalMin - availableSize;
        for (FlexItem item : items) {
            int base = item.minSize();
            int reduce = (totalShrink > 0.0f) ? (int) (deficit * (item.shrink() / totalShrink)) : 0;
            item.setComputedSize(Math.max(0, base - reduce));
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public List<FlexItem> items() {
        return items;
    }

    public FlexDirection direction() {
        return direction;
    }
}
