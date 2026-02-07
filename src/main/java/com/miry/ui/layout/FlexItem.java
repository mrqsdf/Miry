package com.miry.ui.layout;

/**
 * A single item participating in a {@link FlexBox} layout.
 * <p>
 * Items have min/max constraints and grow/shrink weights; computed size and offset are written by
 * {@link FlexBox#layout(int)}.
 */
public final class FlexItem {
    private final int minSize;
    private final int maxSize;
    private final float grow;
    private final float shrink;

    private int computedSize;
    private int computedOffset;

    public FlexItem(int minSize, int maxSize, float grow, float shrink) {
        this.minSize = Math.max(0, minSize);
        this.maxSize = maxSize > 0 ? maxSize : Integer.MAX_VALUE;
        this.grow = Math.max(0.0f, grow);
        this.shrink = Math.max(0.0f, shrink);
    }

    public static FlexItem fixed(int size) {
        return new FlexItem(size, size, 0.0f, 0.0f);
    }

    public static FlexItem grow(int minSize, float grow) {
        return new FlexItem(minSize, Integer.MAX_VALUE, grow, 1.0f);
    }

    public static FlexItem shrink(int maxSize, float shrink) {
        return new FlexItem(0, maxSize, 0.0f, shrink);
    }

    public int minSize() { return minSize; }
    public int maxSize() { return maxSize; }
    public float grow() { return grow; }
    public float shrink() { return shrink; }
    public int computedSize() { return computedSize; }
    public int computedOffset() { return computedOffset; }

    void setComputedSize(int size) { this.computedSize = size; }
    void setComputedOffset(int offset) { this.computedOffset = offset; }
}
