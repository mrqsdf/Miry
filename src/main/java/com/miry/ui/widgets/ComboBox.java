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
            return;
        }
        this.open = open;
        if (!open) {
            filterText = "";
            highlightedIndex = -1;
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
        List<T> filtered = filteredItems();
        int popupHeight = Math.min(filtered.size() * itemHeight, maxHeight);
        r.drawRect(x, y, width, popupHeight, bgColor);

        for (int i = 0; i < filtered.size(); i++) {
            int itemY = y + i * itemHeight;
            if (i == hoverIndex) {
                r.drawRect(x, itemY, width, itemHeight, hoverColor);
            }
            float baselineY = r.baselineForBox(itemY, itemHeight);
            r.drawText(filtered.get(i).toString(), x + 8, baselineY, textColor);
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

        int bg = enabled()
            ? Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT())
            : Theme.toArgb(theme.disabledBg);
        if (enabled() && pressT() > 0.001f) {
            bg = Theme.lerpArgb(theme.widgetHover, theme.widgetActive, pressT() * 0.18f);
        }
        int outline = Theme.toArgb((isFocused(ctx) || isOpen()) ? theme.widgetActive : theme.widgetOutline);
        int textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);

        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, x, y, width, height, bg, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bg);
            drawOutline(r, x, y, width, height, 1, outline);
        }
        if (enabled() && !pressed) {
            r.drawRect(x + 1, y + 1, width - 2, 1, 0x22000000);
        }
        drawFocusRing(r, theme, x, y, width, height);

        String label = selected() != null ? selected().toString() : "<None>";
        float baselineY = r.baselineForBox(y, height);
        r.drawText(label, x + 8, baselineY, textColor);
        float iconSize = Math.min(18.0f, height - 8.0f);
        theme.icons.draw(r, Icon.CHEVRON_DOWN, x + width - iconSize - 6.0f, y + (height - iconSize) * 0.5f, iconSize, textColor);

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

            renderPopup(
                r,
                popupX,
                popupY,
                width,
                popupMaxHeight,
                itemHeight,
                Theme.toArgb(theme.panelBg),
                Theme.toArgb(theme.widgetHover),
                textColor,
                hoverIndex
            );
        }

        return changed;
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
