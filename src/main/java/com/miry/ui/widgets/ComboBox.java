package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple combo box (dropdown) widget with optional keyboard navigation.
 */
public final class ComboBox<T> extends BaseWidget {
    private final List<T> items = new ArrayList<>();
    private int selectedIndex = -1;
    private int highlightedIndex = -1;
    private boolean open;
    private String filterText = "";
    private boolean ignorePopupPressThisFrame;

    private boolean hasDeferredPopup;
    private int deferredX;
    private int deferredY;
    private int deferredWidth;
    private int deferredPopupMaxHeight;
    private int deferredItemHeight;
    private int deferredBgColor;
    private int deferredHoverColor;
    private int deferredTextColor;
    private int deferredHoverIndex;

    public void addItem(T item) {
        items.add(item);
    }

    public void removeItem(T item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
        selectedIndex = -1;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index >= 0 && index < items.size() ? index : -1;
        if (selectedIndex != -1) {
            highlightedIndex = selectedIndex;
        }
    }

    public T selected() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public void setOpen(boolean open) {
        if (!enabled()) {
            this.open = false;
            hasDeferredPopup = false;
            return;
        }
        this.open = open;
        if (!open) {
            filterText = "";
            highlightedIndex = -1;
            hasDeferredPopup = false;
        } else {
            if (highlightedIndex < 0 || highlightedIndex >= items.size()) {
                highlightedIndex = selectedIndex >= 0 ? selectedIndex : (items.isEmpty() ? -1 : 0);
            }
        }
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Updates the current filter string when the combo is open.
     * <p>
     * The host application must forward {@link TextInputEvent}s to this method.
     */
    public boolean handleTextInput(TextInputEvent event) {
        if (!enabled() || event == null || !open) {
            return false;
        }
        int cp = event.codepoint();
        if (cp < 32 || cp == 127) {
            return false;
        }
        filterText += Character.toString((char) cp);
        clampHighlightToFiltered();
        return true;
    }

    public void setFilter(String filter) {
        this.filterText = filter != null ? filter : "";
    }

    public List<T> filteredItems() {
        if (filterText.isEmpty()) {
            return items;
        }
        List<T> filtered = new ArrayList<>();
        String lower = filterText.toLowerCase();
        for (T item : items) {
            if (item.toString().toLowerCase().contains(lower)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public void renderButton(UiRenderer r, int x, int y, int width, int height, int bgColor, int textColor) {
        r.drawRect(x, y, width, height, bgColor);
        String label = selected() != null ? selected().toString() : "<None>";
        float baselineY = r.baselineForBox(y, height);
        r.drawText(label, x + 8, baselineY, textColor);
        r.drawText("v", x + width - 20, baselineY, textColor);
    }

    public void renderPopup(UiRenderer r, int x, int y, int width, int maxHeight, int itemHeight, int bgColor, int hoverColor, int textColor, int hoverIndex) {
        renderPopup(r, null, x, y, width, maxHeight, itemHeight, bgColor, hoverColor, textColor, hoverIndex);
    }

    private void renderPopup(UiRenderer r,
                             Theme theme,
                             int x,
                             int y,
                             int width,
                             int maxHeight,
                             int itemHeight,
                             int bgColor,
                             int hoverColor,
                             int textColor,
                             int hoverIndex) {
        List<T> filtered = filteredItems();
        int popupHeight = Math.min(filtered.size() * itemHeight, maxHeight);
        if (theme != null) {
            int shadow = Theme.toArgb(theme.shadow);
            drawDropShadow(r, x, y, width, popupHeight, shadow, 0.0f, theme.design.space_xs, theme.design.shadow_md, theme.design.radius_sm, 1.0f);
            int outline = Theme.toArgb(theme.widgetOutline);
            if (theme.skins.popup != null) {
                theme.skins.popup.drawWithOutline(r, x, y, width, popupHeight, bgColor, outline, theme.design.border_thin);
            } else {
                int t = theme.design.border_thin;
                float radius = theme.design.radius_sm;
                int top = Theme.lightenArgb(bgColor, 0.02f);
                int bottom = Theme.darkenArgb(bgColor, 0.02f);
                r.drawRoundedRect(x, y, width, popupHeight, radius, top, top, bottom, bottom, t, outline);
            }
        } else {
            r.drawRect(x, y, width, popupHeight, bgColor);
        }

        for (int i = 0; i < filtered.size(); i++) {
            int itemY = y + i * itemHeight;
            if (i == hoverIndex) {
                r.drawRect(x, itemY, width, itemHeight, hoverColor);
            }
            float baselineY = r.baselineForBox(itemY, itemHeight);
            int pad = theme != null ? theme.design.space_sm : 8;
            r.drawText(filtered.get(i).toString(), x + pad, baselineY, textColor);
        }
    }

    public int hoverIndex(int mx, int my, int x, int y, int width, int maxHeight, int itemHeight) {
        if (!open) {
            return -1;
        }
        List<T> filtered = filteredItems();
        int popupHeight = Math.min(filtered.size() * itemHeight, maxHeight);
        if (mx < x || my < y || mx >= x + width || my >= y + popupHeight) {
            return -1;
        }
        int idx = (my - y) / itemHeight;
        if (idx < 0 || idx >= filtered.size()) {
            return -1;
        }
        return idx;
    }

    /**
     * Returns true if the click was inside the popup (consumed), false otherwise.
     */
    public boolean handlePopupClick(int mx, int my, int x, int y, int width, int maxHeight, int itemHeight) {
        if (!open) {
            return false;
        }

        int idx = hoverIndex(mx, my, x, y, width, maxHeight, itemHeight);
        if (idx >= 0) {
            List<T> filtered = filteredItems();
            T item = filtered.get(idx);
            int originalIndex = items.indexOf(item);
            setSelectedIndex(originalIndex);
            setOpen(false);
            return true;
        }

        List<T> filtered = filteredItems();
        int popupHeight = Math.min(filtered.size() * itemHeight, maxHeight);
        if (mx < x || my < y || mx >= x + width || my >= y + popupHeight) {
            // Outside click closes the popup; caller decides whether to treat it as consumed.
            setOpen(false);
            return false;
        }

        return true;
    }

    public List<T> items() {
        return items;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public int highlightedIndex() {
        return highlightedIndex;
    }

    public boolean handleKey(KeyEvent event) {
        if (!enabled() || event == null || !event.isPressOrRepeat()) {
            return false;
        }

        if (!open) {
            if (event.key() == InputConstants.KEY_ENTER || event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_DOWN) {
                setOpen(true);
                return true;
            }
            return false;
        }

        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            setOpen(false);
            return true;
        }
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!filterText.isEmpty()) {
                filterText = filterText.substring(0, filterText.length() - 1);
                clampHighlightToFiltered();
                return true;
            }
        }
        if (items.isEmpty()) {
            return false;
        }

        List<T> filtered = filteredItems();
        if (filtered.isEmpty()) {
            highlightedIndex = -1;
            return false;
        }

        int filteredHighlight = filteredIndexForHighlighted(filtered);
        if (key == InputConstants.KEY_DOWN) {
            filteredHighlight = Math.min(filtered.size() - 1, filteredHighlight + 1);
            highlightedIndex = items.indexOf(filtered.get(filteredHighlight));
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            filteredHighlight = Math.max(0, filteredHighlight - 1);
            highlightedIndex = items.indexOf(filtered.get(filteredHighlight));
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            highlightedIndex = items.indexOf(filtered.get(0));
            return true;
        }
        if (key == InputConstants.KEY_END) {
            highlightedIndex = items.indexOf(filtered.get(filtered.size() - 1));
            return true;
        }
        if (key == InputConstants.KEY_ENTER) {
            if (highlightedIndex >= 0 && highlightedIndex < items.size()) {
                setSelectedIndex(highlightedIndex);
            }
            setOpen(false);
            return true;
        }

        return false;
    }

    /**
     * Convenience render: draws button + popup and handles mouse interaction.
     * Returns true if the selected item changed during this call.
     */
    public boolean render(UiRenderer r,
                          UiContext ctx,
                          UiInput input,
                          Theme theme,
                          int x,
                          int y,
                          int width,
                          int height,
                          int popupMaxHeight,
                          int itemHeight,
                          boolean interactive) {
        return render(r, ctx, input, theme, x, y, width, height, popupMaxHeight, itemHeight, interactive, false);
    }

    /**
     * Render overload that can defer popup drawing to an overlay pass.
     * <p>
     * If {@code deferPopup} is true, this call renders the combo button and updates interaction
     * state but does not draw the popup. If a {@link UiContext} is provided, the popup is queued
     * into {@link UiContext#overlay()} automatically. If no context is available, the host can call
     * {@link #renderDeferredPopup(UiRenderer)} later in the frame to draw the popup above other UI.
     */
    public boolean render(UiRenderer r,
                          UiContext ctx,
                          UiInput input,
                          Theme theme,
                          int x,
                          int y,
                          int width,
                          int height,
                          int popupMaxHeight,
                          int itemHeight,
                          boolean interactive,
                          boolean deferPopup) {
        if (r == null || theme == null) {
            return false;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        boolean hovered = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        ignorePopupPressThisFrame = false;
        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            setOpen(!isOpen());
            ignorePopupPressThisFrame = true;
        }

        int bg = enabled() ? computeStateColor(theme) : Theme.toArgb(theme.disabledBg);
        int outline = enabled() ? computeBorderColor(theme) : Theme.toArgb(theme.widgetOutline);
        if (enabled() && isOpen()) {
            outline = Theme.toArgb(theme.widgetActive);
        }
        int textColor = enabled() ? computeTextColor(theme) : Theme.toArgb(theme.disabledFg);

        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, x, y, width, height, bg, outline, theme.design.border_thin);
        } else {
            int t = theme.design.border_thin;
            float radius = theme.design.radius_sm;
            int top = Theme.lightenArgb(bg, 0.06f);
            int bottom = Theme.darkenArgb(bg, 0.06f);
            r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
        }
        if (enabled() && !pressed) {
            int t = theme.design.border_thin;
            int hl = Theme.lightenArgb(bg, 0.12f);
            int a = (int) (((hl >>> 24) & 0xFF) * 0.20f);
            r.drawRect(x + t, y + t, width - t * 2, t, (a << 24) | (hl & 0x00FFFFFF));
        }
        drawFocusRing(r, theme, x, y, width, height);

        String label = selected() != null ? selected().toString() : "<None>";
        float baselineY = r.baselineForBox(y, height);
        int pad = theme.design.space_sm;
        r.drawText(label, x + pad, baselineY, textColor);
        float iconSize = Math.min(theme.design.icon_sm, height - theme.design.space_sm);
        Icon icon = isOpen() ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN;
        theme.icons.draw(r, icon, x + width - iconSize - theme.design.space_sm, y + (height - iconSize) * 0.5f, iconSize, textColor);

        boolean changed = false;
        if (isOpen()) {
            int popupX = x;
            int popupY = y + height;
            List<T> filtered = filteredItems();
            int hoverIndex = hoverIndex((int) mx, (int) my, popupX, popupY, width, popupMaxHeight, itemHeight);
            if (hoverIndex < 0) {
                hoverIndex = filteredIndexForHighlighted(filtered);
            }

            if (canInteract && input.mousePressed() && !ignorePopupPressThisFrame) {
                int before = selectedIndex;
                boolean consumed = handlePopupClick((int) mx, (int) my, popupX, popupY, width, popupMaxHeight, itemHeight);
                changed = before != selectedIndex;
                if (!consumed && !hovered) {
                    setOpen(false);
                }
            }

            int popupBg = Theme.toArgb(theme.panelBg);
            int popupHover = Theme.toArgb(theme.widgetHover);
            if (deferPopup && ctx != null) {
                // Prefer the central overlay queue so the host only needs one end-of-frame render call.
                hasDeferredPopup = false;
                int fHoverIndex = hoverIndex;
                int fTextColor = textColor;
                ctx.overlay().add(rr -> renderPopup(
                    rr,
                    theme,
                    popupX,
                    popupY,
                    width,
                    popupMaxHeight,
                    itemHeight,
                    popupBg,
                    popupHover,
                    fTextColor,
                    fHoverIndex
                ));
            } else if (deferPopup) {
                // Back-compat when no UiContext is available: caller can invoke renderDeferredPopup().
                hasDeferredPopup = true;
                deferredX = popupX;
                deferredY = popupY;
                deferredWidth = width;
                deferredPopupMaxHeight = popupMaxHeight;
                deferredItemHeight = itemHeight;
                deferredBgColor = popupBg;
                deferredHoverColor = popupHover;
                deferredTextColor = textColor;
                deferredHoverIndex = hoverIndex;
            } else {
                hasDeferredPopup = false;
                renderPopup(
                    r,
                    theme,
                    popupX,
                    popupY,
                    width,
                    popupMaxHeight,
                    itemHeight,
                    popupBg,
                    popupHover,
                    textColor,
                    hoverIndex
                );
            }
        } else {
            hasDeferredPopup = false;
        }

        return changed;
    }

    /**
     * Renders a previously deferred popup (see {@link #render(UiRenderer, UiContext, UiInput, Theme, int, int, int, int, int, int, boolean, boolean)}).
     */
    public void renderDeferredPopup(UiRenderer r) {
        if (!open || !hasDeferredPopup || r == null) {
            return;
        }
        renderPopup(
            r,
            deferredX,
            deferredY,
            deferredWidth,
            deferredPopupMaxHeight,
            deferredItemHeight,
            deferredBgColor,
            deferredHoverColor,
            deferredTextColor,
            deferredHoverIndex
        );
    }

    private int filteredIndexForHighlighted(List<T> filtered) {
        if (filtered == null || filtered.isEmpty()) {
            return -1;
        }
        if (highlightedIndex >= 0 && highlightedIndex < items.size()) {
            int idx = filtered.indexOf(items.get(highlightedIndex));
            if (idx >= 0) {
                return idx;
            }
        }
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            int idx = filtered.indexOf(items.get(selectedIndex));
            if (idx >= 0) {
                highlightedIndex = selectedIndex;
                return idx;
            }
        }
        highlightedIndex = items.indexOf(filtered.get(0));
        return 0;
    }

    private void clampHighlightToFiltered() {
        List<T> filtered = filteredItems();
        if (filtered.isEmpty()) {
            highlightedIndex = -1;
            return;
        }
        filteredIndexForHighlighted(filtered);
    }
}
