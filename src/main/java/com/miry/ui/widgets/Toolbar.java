package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal toolbar with icon buttons and toggle groups.
 */
public final class Toolbar extends BaseWidget {
    public enum ItemType { BUTTON, TOGGLE, SEPARATOR, SPACER }

    public static final class ToolbarItem {
        public final ItemType type;
        public Icon icon;
        public String tooltip;
        public Runnable action;
        public boolean toggled;
        public int toggleGroup = -1;

        public ToolbarItem(ItemType type) {
            this.type = type;
        }

        public static ToolbarItem button(Icon icon, String tooltip, Runnable action) {
            ToolbarItem item = new ToolbarItem(ItemType.BUTTON);
            item.icon = icon;
            item.tooltip = tooltip;
            item.action = action;
            return item;
        }

        public static ToolbarItem toggle(Icon icon, String tooltip, boolean initial) {
            ToolbarItem item = new ToolbarItem(ItemType.TOGGLE);
            item.icon = icon;
            item.tooltip = tooltip;
            item.toggled = initial;
            return item;
        }
    }

    private final List<ToolbarItem> items = new ArrayList<>();
    private int hoverIndex = -1;
    private int pressedIndex = -1;
    private String hoveredTooltip = null;
    private float[] hoverT = new float[0];

    public void addItem(ToolbarItem item) { items.add(item); }
    public void addButton(Icon icon, String tooltip, Runnable action) { items.add(ToolbarItem.button(icon, tooltip, action)); }
    public void addToggle(Icon icon, String tooltip, boolean initial) { items.add(ToolbarItem.toggle(icon, tooltip, initial)); }
    public void addToggleGroup(Icon icon, String tooltip, int groupId, boolean initial) {
        ToolbarItem item = ToolbarItem.toggle(icon, tooltip, initial);
        item.toggleGroup = groupId;
        items.add(item);
    }
    public void addSeparator() { items.add(new ToolbarItem(ItemType.SEPARATOR)); }
    public void addSpacer() { items.add(new ToolbarItem(ItemType.SPACER)); }
    public List<ToolbarItem> items() { return items; }
    public String hoveredTooltip() { return hoveredTooltip; }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        render(r, null, input, theme, x, y, width, height, true);
    }

    public void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        int bg = Theme.toArgb(theme.headerBg);
        r.drawRect(x, y, width, height, bg);
        r.drawRect(x, y + height - 1, width, 1, Theme.toArgb(theme.headerLine));

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;
        boolean mousePressed = canInteract && input.mousePressed();
        boolean mouseReleased = canInteract && input.mouseReleased();

        int pad = theme.design.space_sm;
        int buttonSize = height - pad * 2;
        int cursorX = x + pad;
        hoverIndex = -1;
        hoveredTooltip = null;
        if (hoverT.length != items.size()) hoverT = new float[items.size()];

        float dt = ctx != null ? ctx.lastDt() : 0.0f;
        float speed = theme.design.animSpeed_fast;

        for (int i = 0; i < items.size(); i++) {
            ToolbarItem item = items.get(i);

            if (item.type == ItemType.SEPARATOR) {
                int sepX = cursorX + pad;
                r.drawRect(sepX, y + pad, 1, height - pad * 2, Theme.toArgb(theme.headerLine));
                cursorX = sepX + 1 + pad;
            } else if (item.type == ItemType.SPACER) {
                cursorX = x + width - pad;
            } else {
                int btnX = cursorX;
                int btnY = y + pad;
                int btnW = buttonSize;
                int btnH = buttonSize;

                boolean hovered = canInteract && mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
                if (hovered) hoverIndex = i;
                if (hovered && item.tooltip != null && !item.tooltip.isEmpty()) {
                    hoveredTooltip = item.tooltip;
                }
                hoverT[i] = approachExp(hoverT[i], hovered ? 1.0f : 0.0f, speed, dt);

                boolean pressed = pressedIndex == i;
                boolean toggled = item.toggled;

                int btnBg = 0;
                if (toggled || pressed) {
                    btnBg = Theme.mulAlpha(Theme.toArgb(theme.accent), toggled ? 0.3f : 0.5f);
                } else if (hovered) {
                    btnBg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f * hoverT[i]);
                }
                if (btnBg != 0) {
                    r.drawRoundedRect(btnX, btnY, btnW, btnH, theme.design.radius_sm, btnBg);
                }

                if (item.icon != null && theme.icons != null) {
                    int iconColor = toggled ? Theme.toArgb(theme.accent) : Theme.toArgb(theme.text);
                    int iconSize = buttonSize - pad * 2;
                    theme.icons.draw(r, item.icon, btnX + (btnW - iconSize) / 2, btnY + (btnH - iconSize) / 2, iconSize, iconColor);
                }

                if (hovered && mousePressed) pressedIndex = i;
                if (hovered && mouseReleased && pressedIndex == i) {
                    if (item.type == ItemType.TOGGLE) {
                        if (item.toggleGroup >= 0) {
                            for (ToolbarItem other : items) {
                                if (other.toggleGroup == item.toggleGroup) other.toggled = false;
                            }
                            item.toggled = true;
                        } else {
                            item.toggled = !item.toggled;
                        }
                    }
                    if (item.action != null) item.action.run();
                }

                cursorX += btnW + pad;
            }
        }

        if (mouseReleased) pressedIndex = -1;
    }
}
