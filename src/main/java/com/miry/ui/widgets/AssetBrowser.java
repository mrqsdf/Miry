package com.miry.ui.widgets;

import com.miry.graphics.Texture;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Asset browser with grid/list view and thumbnails.
 */
public final class AssetBrowser<T> extends BaseWidget {
    public enum ViewMode { GRID, LIST }

    public static final class Style {
        public boolean drawContainer = true;
        public int containerBg = 0;

        public int rowHoverBg = 0;
        public int rowSelectedBg = 0;

        public int gridItemHoverBg = 0;
        public int gridItemSelectedBg = 0;
        public int gridThumbBg = 0;

        public int textColor = 0;
        public int mutedColor = 0;
    }

    public static final class AssetItem<T> {
        public final T data;
        public String name;
        public Icon icon;
        public Texture thumbnail;
        public String type;

        public AssetItem(T data, String name) {
            this.data = data;
            this.name = name != null ? name : "Unnamed";
        }
    }

    private final List<AssetItem<T>> items = new ArrayList<>();
    private ViewMode viewMode = ViewMode.GRID;
    private int selectedIndex = -1;
    private int thumbnailSize = 64;
    private float scrollY = 0.0f;
    private Style style;

    public void clear() { items.clear(); selectedIndex = -1; }
    public void addItem(AssetItem<T> item) { items.add(item); }
    public void setViewMode(ViewMode mode) { this.viewMode = mode; }
    public ViewMode viewMode() { return viewMode; }
    public void setThumbnailSize(int size) { this.thumbnailSize = Math.max(32, Math.min(256, size)); }
    public void setScrollY(float scrollY) { this.scrollY = Math.max(0.0f, scrollY); }
    public float scrollY() { return scrollY; }
    public AssetItem<T> selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Style style() {
        return style;
    }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        Style s = style;
        if (s == null || s.drawContainer) {
            int bg = (s != null && s.containerBg != 0) ? s.containerBg : Theme.toArgb(theme.panelBg);
            r.drawRect(x, y, width, height, bg);
        }

        boolean canInteract = enabled() && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;
        boolean mousePressed = canInteract && input.mousePressed();

        int pad = theme.design.space_sm;
        boolean hovered = canInteract && mx >= x && my >= y && mx < x + width && my < y + height;

        // Scroll (mouse wheel) when hovered.
        if (hovered) {
            float wheel = input.consumeMouseScrollDelta();
            if (wheel != 0.0f) {
                scrollY -= wheel * 34.0f;
            }
        }

        if (viewMode == ViewMode.GRID) {
            int itemSize = thumbnailSize + pad * 4;
            int cols = Math.max(1, (width - pad) / (itemSize + pad));
            int colsUsed = Math.min(cols, Math.max(1, items.size()));
            int gridW = colsUsed * itemSize + (colsUsed - 1) * pad;
            int startX = x + Math.max(pad, (width - gridW) / 2);
            int rows = (items.size() + colsUsed - 1) / colsUsed;
            int contentH = pad + rows * (itemSize + pad) + pad;
            scrollY = clampScroll(scrollY, contentH, height);

            r.pushClipRect(x, y, width, height);
            int baseY = y + pad - Math.round(scrollY);

            for (int i = 0; i < items.size(); i++) {
                AssetItem<T> item = items.get(i);
                int col = i % colsUsed;
                int row = i / colsUsed;
                int itemX = startX + col * (itemSize + pad);
                int itemY = baseY + row * (itemSize + pad);

                if (itemY > y + height) break;
                if (itemY + itemSize < y) continue;

                boolean itemHovered = hovered && mx >= itemX && mx < itemX + itemSize && my >= itemY && my < itemY + itemSize;
                boolean selected = i == selectedIndex;

                int itemBg = selected ? Theme.toArgb(theme.widgetActive)
                    : (itemHovered ? Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.6f) : 0);
                if (s != null) {
                    if (selected && s.gridItemSelectedBg != 0) {
                        itemBg = s.gridItemSelectedBg;
                    } else if (itemHovered && s.gridItemHoverBg != 0) {
                        itemBg = s.gridItemHoverBg;
                    }
                }
                if (itemBg != 0) {
                    r.drawRoundedRect(itemX, itemY, itemSize, itemSize, theme.design.radius_sm, itemBg);
                }

                int thumbX = itemX + pad;
                int thumbY = itemY + pad;
                int thumbBg = Theme.darkenArgb(Theme.toArgb(theme.panelBg), 0.15f);
                if (s != null && s.gridThumbBg != 0) {
                    thumbBg = s.gridThumbBg;
                }
                r.drawRoundedRect(thumbX, thumbY, thumbnailSize, thumbnailSize, theme.design.radius_sm, thumbBg);

                if (item.thumbnail != null) {
                    r.drawTexture(item.thumbnail, thumbX, thumbY, thumbnailSize, thumbnailSize, 0xFFFFFFFF);
                } else if (item.icon != null && theme.icons != null) {
                    int iconSize = thumbnailSize / 2;
                    theme.icons.draw(r, item.icon,
                        thumbX + (thumbnailSize - iconSize) / 2.0f,
                        thumbY + (thumbnailSize - iconSize) / 2.0f,
                        iconSize,
                        s != null && s.mutedColor != 0 ? s.mutedColor : Theme.toArgb(theme.textMuted));
                }

                String clipped = r.clipText(item.name, itemSize - pad * 2);
                float tw = r.measureText(clipped);
                float tx = itemX + (itemSize - tw) * 0.5f;
                float ty = r.baselineForBox(itemY + pad + thumbnailSize + pad, r.lineHeight() + pad);
                r.drawText(clipped, tx, ty, s != null && s.textColor != 0 ? s.textColor : Theme.toArgb(theme.text));

                if (itemHovered && mousePressed) selectedIndex = i;
            }
            r.popClipRect();
        } else {
            int rowH = Math.max(28, Math.round(theme.design.widget_height_md));
            int contentH = pad + items.size() * rowH + pad;
            scrollY = clampScroll(scrollY, contentH, height);

            r.pushClipRect(x, y, width, height);
            int cursorY = y + pad - Math.round(scrollY);
            for (int i = 0; i < items.size(); i++) {
                AssetItem<T> item = items.get(i);
                int rowY = cursorY + i * rowH;
                if (rowY > y + height) break;
                if (rowY + rowH < y) continue;

                boolean rowHovered = hovered && mx >= x && mx < x + width && my >= rowY && my < rowY + rowH;
                boolean selected = i == selectedIndex;

                int bg = selected ? Theme.lerpArgb(theme.widgetHover, theme.widgetActive, 0.18f)
                    : (rowHovered ? Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f) : 0);
                if (s != null) {
                    if (selected && s.rowSelectedBg != 0) {
                        bg = s.rowSelectedBg;
                    } else if (rowHovered && s.rowHoverBg != 0) {
                        bg = s.rowHoverBg;
                    }
                }
                if (bg != 0) {
                    r.drawRect(x + 1, rowY, width - 2, rowH, bg);
                }

                int iconSize = Math.min(thumbnailSize, rowH - pad);
                int iconX = x + pad;
                int iconY = rowY + (rowH - iconSize) / 2;
                if (item.thumbnail != null) {
                    r.drawTexture(item.thumbnail, iconX, iconY, iconSize, iconSize, 0xFFFFFFFF);
                } else if (item.icon != null && theme.icons != null) {
                    theme.icons.draw(r, item.icon, iconX, iconY, iconSize, s != null && s.mutedColor != 0 ? s.mutedColor : Theme.toArgb(theme.textMuted));
                }

                int textX = iconX + iconSize + pad;
                int textW = Math.max(1, width - (textX - x) - pad);
                String name = r.clipText(item.name, textW);
                r.drawText(name, textX, r.baselineForBox(rowY, rowH), s != null && s.textColor != 0 ? s.textColor : Theme.toArgb(theme.text));

                if (rowHovered && mousePressed) selectedIndex = i;
            }
            r.popClipRect();
        }
    }

    private static float clampScroll(float scrollY, int contentHeight, int viewHeight) {
        float max = Math.max(0.0f, contentHeight - viewHeight);
        if (scrollY < 0.0f) return 0.0f;
        if (scrollY > max) return max;
        return scrollY;
    }
}
