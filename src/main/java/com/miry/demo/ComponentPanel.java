package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.component.*;
import com.miry.ui.component.graphic.GraphicComponent;
import com.miry.ui.component.graphic.GraphicDataSeries;
import com.miry.ui.component.graphic.GraphicType;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;

public final class ComponentPanel extends Panel {

    private boolean toggle = false;
    private float sliderValue1 = 0.5f;
    private float sliderValue2 = 0.5f;
    private float sliderValue3 = 0.5f;

    GraphicComponent graphic = new GraphicComponent("graphic", GraphicType.CLOUD, 400, 300);

    public ComponentPanel() {
        super("Component Panel");
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        UiRenderer r = ctx.renderer();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());

        TextComponent text = new TextComponent("Hello, Miry UI!")
                .setColor(new Color(java.awt.Color.BLUE));
        TextComponent textRed = new TextComponent("This is a red text.")
                .setColor(new Color(java.awt.Color.RED));
        text.addChild(textRed);

        ui.label(r, text);

        TextComponent toggleText = new TextComponent("Toggle me!")
                .setColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));

        ToggleComponent toggleComp = new ToggleComponent(toggleText, toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));

        toggle = ui.toggle(r, toggleComp);

        TextComponent buttonText1 = new TextComponent("Click me 1!")
                .setColor(new Color(java.awt.Color.WHITE));
        ButtonComponent button1 = new ButtonComponent(buttonText1)
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button clicked!"));

        ui.button(r, button1);

        TextComponent buttonText2 = new TextComponent("Click me 2!")
                .setColor(new Color(java.awt.Color.WHITE));
        ButtonComponent button2 = new ButtonComponent(buttonText2)
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY));

        if (ui.button(r, button2)) {
            System.out.println("Button 2 clicked!");
        }

        TextComponent sliderText1 = new TextComponent("Slider 1: " + String.format("%.2f", sliderValue1))
                .setColor(new Color(java.awt.Color.magenta));

        SliderComponent slider1 = new SliderComponent("slider1", sliderText1, sliderValue1, 0f, 1f, 0.01f)
                .setBackgroundColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnChange(value -> {
                    sliderValue1 = value;
                    System.out.println("Slider 1 value: " + sliderValue1);
                });

        ui.sliderFloat(r, slider1);

        TextComponent sliderText2 = new TextComponent("slider2", "Slider 2: " + String.format("%.2f", sliderValue2))
                .setColor(new Color(java.awt.Color.magenta));

        SliderComponent slider2 = new SliderComponent(sliderText2, sliderValue2, 0f, 1f, 0.01f)
                .setBackgroundColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY));

        sliderValue2 = ui.sliderFloat(r, slider2);

        sliderValue3 = ui.sliderFloat(r, "Slider 3: " + String.format("%.2f", sliderValue3), sliderValue3, 0f, 1f);

        Component component = new Component("customComponent");
        component.addChild(new TextComponent("This is a custom component!").setColor(new Color(java.awt.Color.ORANGE)));
        component.addChild(new ButtonComponent(new TextComponent("Click me in custom component!").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.BLUE))
                .setHoverColor(new Color(java.awt.Color.CYAN))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button in custom component clicked!")));

        ui.renderComponent(r, component);

        TextComponent grid1 = new TextComponent("Grid Cell 1").setColor(new Color(java.awt.Color.PINK));
        ButtonComponent grid2 = new ButtonComponent(new TextComponent("Grid Cell 2").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.MAGENTA))
                .setHoverColor(new Color(java.awt.Color.PINK))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button 2 in grid cell clicked!"));
        ButtonComponent grid3 = new ButtonComponent(new TextComponent("Grid Cell 3").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.GREEN))
                .setHoverColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setActiveColor(new Color(java.awt.Color.GRAY))
                .setOnClick(() -> System.out.println("Button 3 in grid cell clicked!"));

        TextComponent grid4 = new TextComponent("Grid Cell 4").setColor(new Color(java.awt.Color.YELLOW));

        ToggleComponent grid5 = new ToggleComponent(new TextComponent("Grid Cell 5").setColor(new Color(java.awt.Color.WHITE)), toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY))
                .setOnChange(value -> {
                    toggle = value;
                    System.out.println("Toggle in grid cell toggled: " + value);
                });

        GridComponent grid = new GridComponent("grid", 2, 5)
                .setCell(0, 0, grid1)
                .setCell(1, 0, grid2)
                .setCell(0, 1, grid3)
                .setCell(1, 1, grid3)
                .setCell(0, 2, grid4)
                .setCell(1, 2, grid4)
                .setCell(0, 3, grid5)
                .setCell(1, 3, grid5)
                .setCell(0, 4, grid5)
                .setCell(1, 4, grid5);

        //ui.grid(r, grid);


        GroupedComponent groupedComponent = new GroupedComponent("groupedComponent");
        groupedComponent.addChild(new TextComponent("This is a grouped component!").setColor(new Color(java.awt.Color.CYAN)));
        groupedComponent.addChild(new ButtonComponent(new TextComponent("Click me in grouped component!").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.BLUE))
                .setHoverColor(new Color(java.awt.Color.CYAN))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button in grouped component clicked!")));
        groupedComponent.addChild(new ToggleComponent(new TextComponent("Toggle me in grouped component!").setColor(new Color(java.awt.Color.WHITE)), toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY))
                .setOnChange(value -> {
                    toggle = value;
                    System.out.println("Toggle in grouped component toggled: " + value);
                }));

        //ui.group(r, groupedComponent);

        ButtonComponent buttonGraph= new ButtonComponent(new TextComponent("Add Data Point").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> {
                    GraphicDataSeries dataPoint = new GraphicDataSeries(
                            (float) (Math.random() * 10),
                            (float) (Math.random() * 10),
                            "Point " + (graphic.getDataSeries().size() + 1),
                            new Color(new java.awt.Color((int)(Math.random() * 0x1000000)))
                    );
                    graphic.addDataSeries(dataPoint);
                });

        ui.button(r, buttonGraph);
        ui.graphic(r, graphic);



        ui.endPanel();
    }
}
