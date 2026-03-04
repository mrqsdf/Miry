package com.miry.ui.component;

import com.miry.ui.Ui;
import com.miry.ui.theme.ColorPalette;

public class SeparatorComponent extends Component{

    private int thickness;
    private Color color;
    private String themeId;
    public SeparatorComponent(int thickness, Color color) {
        super(null);
        this.thickness = thickness;
        this.color = color;
    }

    public SeparatorComponent(int thickness) {
        this(thickness, null); // Default color
    }
    public SeparatorComponent() {
        this(1, null); // Default thickness and color
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public Color getColor(Ui ui) {
        if (ui == null && color == null) {
            throw new NullPointerException("You must provide a color");
        }
        return color != null ? color : themeId != null ? ui.theme().getColor(themeId) : ui.theme().widgetOutline;
    }

    public void setColor(Color color) {
        this.color = color;
    }

}
