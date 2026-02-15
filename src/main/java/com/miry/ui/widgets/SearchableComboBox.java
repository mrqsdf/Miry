package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Combo box that opens into a popup with an immediately focused filter TextField (Blender-ish).
 */
public final class SearchableComboBox<T> extends BaseWidget {
    private final List<T> items = new ArrayList<>();
    private int selectedIndex = -1;
    private int highlightedIndex = -1;
    private boolean open;

    private final TextField filterField = new TextField();
    private boolean ignorePopupPressThisFrame;

    public SearchableComboBox() {
        filterField.setMaxLength(128);
    }

    public void addItem(T item) {
        items.add(item);
        if (selectedIndex < 0) {
            setSelectedIndex(0);
        }
    }

    public List<T> items() {
        return items;
    }

    public void clear() {
        items.clear();
        selectedIndex = -1;
        highlightedIndex = -1;
        open = false;
        filterField.setText("");
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index >= 0 && index < items.size() ? index : -1;
        highlightedIndex = selectedIndex;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public T selected() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(UiContext ctx, boolean open) {
        if (!enabled()) {
            this.open = false;
            return;
        }
        this.open = open;
        ignorePopupPressThisFrame = true;
        if (!open) {
            filterField.setText("");
            highlightedIndex = selectedIndex;
            if (ctx != null && ctx.focus().focused() == filterField.id()) {
                ctx.focus().setFocus(id());
            }
        } else {
            filterField.setText("");
            if (ctx != null) {
                ctx.focus().setFocus(filterField.id());
            }
            if (highlightedIndex < 0) {
                highlightedIndex = selectedIndex >= 0 ? selectedIndex : (items.isEmpty() ? -1 : 0);
            }
        }
    }

    public boolean handleKey(UiContext ctx, KeyEvent event, Clipboard clipboard) {
        if (!enabled() || ctx == null || event == null || !event.isPressOrRepeat()) {
            return false;
        }
        if (!open) {
            if (ctx.focus().focused() != id()) {
                return false;
            }
            if (event.key() == InputConstants.KEY_ENTER || event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_DOWN) {
                setOpen(ctx, true);
                return true;
            }
            return false;
        }

        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            setOpen(ctx, false);
            return true;
        }

        List<Integer> filtered = filteredIndices();
        if (filtered.isEmpty()) {
            highlightedIndex = -1;
        }

        if (key == InputConstants.KEY_DOWN && !filtered.isEmpty()) {
            int pos = Math.max(0, filtered.indexOf(highlightedIndex));
            pos = Math.min(filtered.size() - 1, pos + 1);
            highlightedIndex = filtered.get(pos);
            return true;
        }
        if (key == InputConstants.KEY_UP && !filtered.isEmpty()) {
            int pos = Math.max(0, filtered.indexOf(highlightedIndex));
            pos = Math.max(0, pos - 1);
            highlightedIndex = filtered.get(pos);
            return true;
        }
        if (key == InputConstants.KEY_HOME && !filtered.isEmpty()) {
            highlightedIndex = filtered.get(0);
            return true;
        }
        if (key == InputConstants.KEY_END && !filtered.isEmpty()) {
            highlightedIndex = filtered.get(filtered.size() - 1);
            return true;
        }
        if (key == InputConstants.KEY_ENTER) {
            if (highlightedIndex >= 0 && highlightedIndex < items.size()) {
                selectedIndex = highlightedIndex;
            }
            setOpen(ctx, false);
            return true;
        }

        // Pass through to filter text field (typing, backspace, etc).
        if (ctx.focus().focused() == filterField.id()) {
            filterField.handleKey(event, clipboard);
            // Clamp highlight to first filtered element when filter changes.
            List<Integer> f = filteredIndices();
            if (!f.isEmpty()) {
                highlightedIndex = f.get(0);
            } else {
                highlightedIndex = -1;
            }
            return true;
        }

        return false;
    }

    public boolean handleTextInput(UiContext ctx, TextInputEvent event) {
        if (!enabled() || ctx == null || event == null || !open) {
            return false;
        }
        if (ctx.focus().focused() != filterField.id()) {
            return false;
        }
        filterField.handleTextInput(event);
        List<Integer> f = filteredIndices();
        highlightedIndex = f.isEmpty() ? -1 : f.get(0);
        return true;
    }

    private List<Integer> filteredIndices() {
        String filter = filterField.text().trim().toLowerCase(Locale.ROOT);
        List<Integer> out = new ArrayList<>();
        if (filter.isEmpty()) {
            for (int i = 0; i < items.size(); i++) out.add(i);
            return out;
        }
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (item != null && item.toString().toLowerCase(Locale.ROOT).contains(filter)) {
                out.add(i);
            }
        }
        return out;
    }

    public boolean render(UiRenderer r,
                          UiContext ctx,
                          UiInput input,
                          Theme theme,
                          int x,
                          int y,
                          int w,
                          int h,
                          int popupMaxHeight,
                          int itemHeight,
                          boolean interactive) {
        if (r == null || theme == null) return false;

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        boolean hovered = canInteract && pxInside(mx, my, x, y, w, h);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        ignorePopupPressThisFrame = false;
        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            setOpen(ctx, !open);
            ignorePopupPressThisFrame = true;
        }

        int bg = enabled() ? computeStateColor(theme) : Theme.toArgb(theme.disabledBg);
        int outline = enabled() ? computeBorderColor(theme) : Theme.toArgb(theme.widgetOutline);
        if (enabled() && open) {
            outline = Theme.toArgb(theme.widgetActive);
        }
        int textColor = enabled() ? computeTextColor(theme) : Theme.toArgb(theme.disabledFg);

        int t = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        r.drawRoundedRect(x, y, w, h, radius, top, top, bottom, bottom, t, outline);
        if (enabled() && !pressed) {
            int hl = Theme.lightenArgb(bg, 0.12f);
            int a = (int) (((hl >>> 24) & 0xFF) * 0.20f);
            r.drawRect(x + t, y + t, w - t * 2, t, (a << 24) | (hl & 0x00FFFFFF));
        }
        drawFocusRing(r, theme, x, y, w, h);

        String label = selected() != null ? selected().toString() : "<None>";
        float baselineY = r.baselineForBox(y, h);
        int pad = theme.design.space_sm;
        r.drawText(label, x + pad, baselineY, textColor);
        float iconSize = Math.min(theme.design.icon_sm, h - theme.design.space_sm);
        Icon icon = open ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN;
        theme.icons.draw(r, icon, x + w - iconSize - theme.design.space_sm, y + (h - iconSize) * 0.5f, iconSize, textColor);

        boolean changed = false;
        if (open) {
            int popupX = x;
            int popupY = y + h;
            int maxH = Math.max(60, popupMaxHeight);
            int filterH = Math.min(34, itemHeight);
            int listTop = popupY + filterH + 6;
            List<Integer> filtered = filteredIndices();
            int listH = Math.max(0, maxH - (filterH + 6));
            int shown = itemHeight > 0 ? Math.min(filtered.size(), listH / Math.max(1, itemHeight)) : 0;
            int popupH = Math.min(maxH, filterH + 6 + shown * itemHeight);
            if (filtered.size() > shown) {
                popupH = maxH;
            }

            int popupBg = Theme.toArgb(theme.panelBg);
            int popupHover = Theme.toArgb(theme.widgetHover);
            int popupOutline = Theme.toArgb(theme.widgetOutline);
            int shadow = Theme.toArgb(theme.shadow);
            drawDropShadow(r, popupX, popupY, w, popupH, shadow, 0.0f, theme.design.space_xs, theme.design.shadow_md, theme.design.radius_sm, 1.0f);
            r.drawRoundedRect(popupX, popupY, w, popupH, radius, popupBg, t, popupOutline);

            // Filter field (immediately focused).
            filterField.render(r, ctx, input, theme, popupX + t, popupY + t, w - t * 2, filterH, interactive);

            // List.
            int hoverFiltered = -1;
            if (canInteract) {
                if (mx >= popupX && my >= listTop && mx < popupX + w && my < popupY + popupH) {
                    int idx = (int) ((my - listTop) / Math.max(1, itemHeight));
                    if (idx >= 0 && idx < filtered.size()) {
                        hoverFiltered = idx;
                    }
                }
            }

            if (canInteract && input.mousePressed() && !ignorePopupPressThisFrame) {
                boolean insidePopup = mx >= popupX && my >= popupY && mx < popupX + w && my < popupY + popupH;
                if (!insidePopup) {
                    setOpen(ctx, false);
                } else if (hoverFiltered >= 0) {
                    int before = selectedIndex;
                    selectedIndex = filtered.get(hoverFiltered);
                    highlightedIndex = selectedIndex;
                    setOpen(ctx, false);
                    changed = before != selectedIndex;
                }
            }

            int maxVisible = itemHeight > 0 ? Math.max(0, (popupY + popupH - listTop) / Math.max(1, itemHeight)) : 0;
            int end = Math.min(filtered.size(), maxVisible);
            for (int i = 0; i < end; i++) {
                int iy = listTop + i * itemHeight;
                int idx = filtered.get(i);
                boolean hi = i == hoverFiltered || idx == highlightedIndex;
                if (hi) {
                    r.drawRect(popupX + t, iy, w - t * 2, itemHeight, popupHover);
                }
                float ib = r.baselineForBox(iy, itemHeight);
                r.drawText(items.get(idx).toString(), popupX + theme.design.space_sm, ib, Theme.toArgb(theme.text));
            }

        }

        return changed;
    }
}
