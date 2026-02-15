package com.miry.ui.util;

/**
 * Rectangle and bounds testing utilities.
 */
public final class RectUtils {
    private RectUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Tests if a point is inside a rectangle.
     *
     * @param px point x coordinate
     * @param py point y coordinate
     * @param x rectangle x coordinate
     * @param y rectangle y coordinate
     * @param w rectangle width
     * @param h rectangle height
     * @return true if point is inside rectangle
     */
    public static boolean contains(float px, float py, float x, float y, float w, float h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /**
     * Tests if a point is inside a rectangle (integer version).
     */
    public static boolean contains(float px, float py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /**
     * Tests if a point is inside a rectangle (integer version).
     */
    public static boolean contains(int px, int py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /**
     * Tests if a point hits a circle.
     *
     * @param px point x coordinate
     * @param py point y coordinate
     * @param cx circle center x
     * @param cy circle center y
     * @param radius circle radius
     * @return true if point is inside circle
     */
    public static boolean hitCircle(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        float rr = radius * radius;
        return dx * dx + dy * dy <= rr;
    }

    /**
     * Tests if a point hits a circle (integer version).
     */
    public static boolean hitCircle(float px, float py, int cx, int cy, int radius) {
        float dx = px - cx;
        float dy = py - cy;
        float rr = radius * (float) radius;
        return dx * dx + dy * dy <= rr;
    }

    /**
     * Tests if two rectangles intersect.
     *
     * @param ax first rectangle x
     * @param ay first rectangle y
     * @param aw first rectangle width
     * @param ah first rectangle height
     * @param bx second rectangle x
     * @param by second rectangle y
     * @param bw second rectangle width
     * @param bh second rectangle height
     * @return true if rectangles intersect
     */
    public static boolean rectsIntersect(float ax, float ay, float aw, float ah,
                                         float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    /**
     * Tests if two rectangles intersect (integer version).
     */
    public static boolean rectsIntersect(int ax, int ay, int aw, int ah,
                                         int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
