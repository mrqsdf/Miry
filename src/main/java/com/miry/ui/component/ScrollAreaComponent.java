// ===== ScrollAreaComponent additions (English doc inside) =====
package com.miry.ui.component;



/**
 * Component wrapper for an IMUI scroll area.
 *
 * <p>This component is meant to be rendered by {@code Ui.scrollArea(...)}.</p>
 *
 * Key concepts
 * <ul>
 *   <li><b>height</b>: visible viewport height used by the rect-less overload {@code scrollArea(r, sc)}.</li>
 *   <li><b>yOffset</b>: shifts the scroll area viewport down inside its allocated region.
 *       Useful when you draw non-component elements before (headers, separators, etc.).</li>
 *   <li><b>contentHeight</b>: cached total content height. It is automatically updated by {@code Ui.scrollArea}
 *       after rendering children (computed from layout consumption).</li>
 *   <li><b>key</b>: stable identifier for the scroll state. If not set, {@code id} is used.</li>
 * </ul>
 */
public class ScrollAreaComponent extends Component {

    private String key;
    private int height;        // viewport height for rect-less rendering
    private int yOffset;       // additional top space inside the allocated rect
    private int contentHeight; // cached computed content height

    public ScrollAreaComponent(String id) {
        super(id);
        this.key = id;
        this.height = 0;      // sensible default
        this.yOffset = 0;
        this.contentHeight = 0;
    }

    /** Optional: stable key for scroll state. If null, uses {@link #getId()}. */
    public String getKey() {
        return key;
    }

    public ScrollAreaComponent setKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Viewport height (in pixels) used by {@code Ui.scrollArea(r, sc)}.
     * If you render via {@code Ui.scrollArea(r, sc, rect)} this is ignored.
     */
    public int getHeight() {
        return height;
    }

    public ScrollAreaComponent setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
     * Extra vertical padding/offset applied inside the scroll area viewport.
     * The visible viewport becomes {@code rect.h - yOffset} and its y becomes {@code rect.y + yOffset}.
     */
    public int getYOffset() {
        return yOffset;
    }

    public ScrollAreaComponent setYOffset(int yOffset) {
        this.yOffset = yOffset;
        return this;
    }

    /**
     * Cached total content height.
     * Updated automatically by {@code Ui.scrollArea} after rendering children.
     */
    public int getContentHeight() {
        return contentHeight;
    }

    public ScrollAreaComponent setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        return this;
    }
}