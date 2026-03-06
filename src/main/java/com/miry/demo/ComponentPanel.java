package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.component.*;
import com.miry.ui.component.graphic.GraphicComponent;
import com.miry.ui.component.graphic.GraphicDataSeries;
import com.miry.ui.component.graphic.GraphicType;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.widgets.ComboBox;

public final class ComponentPanel extends Panel {

    private boolean scrollAreaToggle = false;
    private boolean toggle = false;
    private float sliderValue1 = 0.5f;
    private float sliderValue2 = 0.5f;
    private float sliderValue3 = 0.5f;

    private String textFiledValue = "Editable text";
    private final ComboBox<String> combo;
    private TextFieldComponent textFieldComponent;
    private ComboBoxComponent comboComponent;
    private ScrollAreaComponent scrollArea;

    ToggleComponent scrollAreaToggleComp;

    TextComponent text;
    TextComponent textRed;
    TextComponent toggleText;
    ToggleComponent toggleComp;
    TextComponent buttonText1;
    ButtonComponent button1;
    TextComponent buttonText2;
    ButtonComponent button2;
    TextComponent sliderText1;
    SliderComponent slider1;
    TextComponent sliderText2;
    SliderComponent slider2;

    Component component;
    TextComponent grid1;
    ButtonComponent grid2;
    ButtonComponent grid3;
    TextComponent grid4;
    ToggleComponent grid5;
    GridComponent grid;
    GroupedComponent groupedComponent;
    ButtonComponent buttonGraph;
    GraphicComponent graphic;
    ToggleComponent groupedToggle;

    public ComponentPanel() {
        super("Component Panel");

        // Initialize components

        textFieldComponent = new TextFieldComponent("textFieldComponent", textFiledValue);

        combo = new ComboBox<>();

        comboComponent = new ComboBoxComponent("comboComponent", combo);

        scrollArea = new ScrollAreaComponent("scrollArea");

        combo.addItem("Option A");
        combo.addItem("Option B");
        combo.addItem("Option C");
        combo.addItem("Very Long Option Name…");
        combo.setSelectedIndex(0);

        scrollAreaToggleComp = new ToggleComponent(new TextComponent("Toggle Scroll Area").setColor(new Color(java.awt.Color.WHITE)), scrollAreaToggle)
                .setToggleColor(scrollAreaToggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY))
                .setOnChange(value -> {
                    scrollAreaToggle = value;
                    scrollAreaToggleComp.setToggleColor(value ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));
                    System.out.println("Scroll area toggle: " + value);
                });

        text = new TextComponent(textFiledValue)
                .setColor(new Color(java.awt.Color.BLUE));
        textRed = new TextComponent("This is a red text.")
                .setColor(new Color(java.awt.Color.RED));
        text.addChild(textRed);

        toggleText = new TextComponent("Toggle me!")
                .setColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));

        toggleComp = new ToggleComponent(toggleText, toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));


        buttonText1 = new TextComponent("Click me 1!")
                .setColor(new Color(java.awt.Color.WHITE));

        button1 = new ButtonComponent(buttonText1)
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button clicked!"));

        buttonText2 = new TextComponent("Click me 2!")
                .setColor(new Color(java.awt.Color.WHITE));

        button2 = new ButtonComponent(buttonText2)
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY));

        sliderText1 = new TextComponent("Slider 1: " + String.format("%.2f", sliderValue1))
                .setColor(new Color(java.awt.Color.magenta));

        slider1 = new SliderComponent("slider1", sliderText1, sliderValue1, 0f, 1f, 0.01f)
                .setBackgroundColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnChange(value -> {
                    sliderValue1 = value;
                    System.out.println("Slider 1 value: " + sliderValue1);
                });

        sliderText2 = new TextComponent("slider2", "Slider 2: " + String.format("%.2f", sliderValue2))
                .setColor(new Color(java.awt.Color.magenta));

        slider2 = new SliderComponent(sliderText2, sliderValue2, 0f, 1f, 0.01f)
                .setBackgroundColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY));

        component = new Component("customComponent");
        component.addChild(new TextComponent("This is a custom component!").setColor(new Color(java.awt.Color.ORANGE)));
        component.addChild(new ButtonComponent(new TextComponent("Click me in custom component!").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.BLUE))
                .setHoverColor(new Color(java.awt.Color.CYAN))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button in custom component clicked!")));

        grid1 = new TextComponent("Grid Cell 1").setColor(new Color(java.awt.Color.PINK));
        grid2 = new ButtonComponent(new TextComponent("Grid Cell 2").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.MAGENTA))
                .setHoverColor(new Color(java.awt.Color.PINK))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button 2 in grid cell clicked!"));
        grid3 = new ButtonComponent(new TextComponent("Grid Cell 3").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.GREEN))
                .setHoverColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setActiveColor(new Color(java.awt.Color.GRAY))
                .setOnClick(() -> System.out.println("Button 3 in grid cell clicked!"));

        grid4 = new TextComponent("Grid Cell 4").setColor(new Color(java.awt.Color.YELLOW));

        grid5 = new ToggleComponent(new TextComponent("Grid Cell 5").setColor(new Color(java.awt.Color.WHITE)), toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY))
                .setOnChange(value -> {
                    toggle = value;
                    grid5.setToggleColor(value ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));
                    System.out.println("Toggle in grid cell toggled: " + value);
                });

        grid = new GridComponent("grid", 2, 5)
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

        groupedComponent = new GroupedComponent("groupedComponent");
        groupedComponent.addChild(new TextComponent("This is a grouped component!").setColor(new Color(java.awt.Color.CYAN)));
        groupedComponent.addChild(new ButtonComponent(new TextComponent("Click me in grouped component!").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.BLUE))
                .setHoverColor(new Color(java.awt.Color.CYAN))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> System.out.println("Button in grouped component clicked!")));
        groupedToggle = new ToggleComponent(new TextComponent("Grouped Toggle").setColor(new Color(java.awt.Color.WHITE)), toggle)
                .setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY))
                .setOnChange(value -> {
                    toggle = value;
                    groupedToggle.setToggleColor(value ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));
                    System.out.println("Grouped toggle toggled: " + value);
                });
        groupedComponent.addChild(groupedToggle);

        graphic = new GraphicComponent("graphic", GraphicType.CLOUD, 400, 300);

        buttonGraph = new ButtonComponent(new TextComponent("Add Data Point").setColor(new Color(java.awt.Color.WHITE)))
                .setBgColor(new Color(java.awt.Color.DARK_GRAY))
                .setHoverColor(new Color(java.awt.Color.GRAY))
                .setActiveColor(new Color(java.awt.Color.LIGHT_GRAY))
                .setOnClick(() -> {
                    GraphicDataSeries dataPoint = new GraphicDataSeries(
                            (float) (Math.random() * 10),
                            (float) (Math.random() * 10),
                            "Point " + (this.graphic.getDataSeries().size() + 1),
                            new Color(new java.awt.Color((int) (Math.random() * 0x1000000)))
                    );
                    this.graphic.addDataSeries(dataPoint);
                });


        // Build hierarchy in scoll area

        scrollArea.addChild(scrollAreaToggleComp);

        scrollArea.addChild(textFieldComponent);
        scrollArea.addChild(comboComponent);

        scrollArea.addChild(text);

        scrollArea.addChild(toggleComp);

        scrollArea.addChild(button1);
        scrollArea.addChild(button2);

        scrollArea.addChild(slider1);
        scrollArea.addChild(slider2);

        scrollArea.addChild(component);

        scrollArea.addChild(grid);

        scrollArea.addChild(groupedComponent);

        scrollArea.addChild(buttonGraph);
        scrollArea.addChild(graphic);
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        UiRenderer r = ctx.renderer();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        if (scrollAreaToggle){
            ui.scrollArea(r, scrollArea);


            textFiledValue = textFieldComponent.text();
            text.setText(textFiledValue);

        } else {

            ui.toggle(r, scrollAreaToggleComp);

            ui.textField(r, textFieldComponent);
            textFiledValue = textFieldComponent.text();
            text.setText(textFiledValue);

            ui.comboBox(r, comboComponent);

            ui.label(r, text);

            toggle = ui.toggle(r, toggleComp);
            toggleComp.setToggleColor(toggle ? new Color(java.awt.Color.GREEN) : new Color(java.awt.Color.GRAY));

            ui.button(r, button1);

            if (ui.button(r, button2)) System.out.println("Button 2 clicked!");

            ui.sliderFloat(r, slider1);

            sliderValue2 = ui.sliderFloat(r, slider2);

            sliderValue3 = ui.sliderFloat(r, "Slider 3: " + String.format("%.2f", sliderValue3), sliderValue3, 0f, 1f);

            ui.renderComponent(r, component);

            ui.grid(r, grid);


            ui.group(r, groupedComponent);


            ui.button(r, buttonGraph);
            ui.graphic(r, graphic);
        }

        ui.endPanel();
    }


    @Override
    public void handleKey(UiContext ctx, KeyEvent e) {
        textFieldComponent.handleKey(ctx, e);
        comboComponent.handleKey(ctx, e);
    }

    @Override
    public void handleTextInput(UiContext ctx, TextInputEvent e) {
        textFieldComponent.handleTextInput(ctx, e);
        comboComponent.handleTextInput(ctx, e);
    }
}
