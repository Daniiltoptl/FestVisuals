package com.fest.visuals.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import com.fest.visuals.client.ui.clickgui.module.settings.ColorComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Theme {
    private final String name;
    private final List<ElementColor> elementColors = new ArrayList<>();

    public Theme(String name) {
        this.name = name;

        elementColors.add(new ElementColor("Primary", new Color(255, 180, 50)));
        elementColors.add(new ElementColor("Secondary", new Color(255, 175, 0)));
        elementColors.add(new ElementColor("Blur", new Color(46, 38, 33)));
        elementColors.add(new ElementColor("Widget blur", new Color(32, 28, 23, 255)));
        elementColors.add(new ElementColor("Background blur", new Color(47, 39, 29, 255)));
        elementColors.add(new ElementColor("Text", new Color(255, 235, 220, 255)));
        elementColors.add(new ElementColor("Inactive text", new Color(152, 152, 152)));
        elementColors.add(new ElementColor("Knob", new Color(255, 180, 50)));
        elementColors.add(new ElementColor("Inactive knob", new Color(255, 255, 255)));
        elementColors.add(new ElementColor("Positive", new Color(130, 255, 130)));
        elementColors.add(new ElementColor("Middle", new Color(255, 200, 95)));
        elementColors.add(new ElementColor("Negative", new Color(255, 80, 80)));
    }

    public Color getPrimaryColor() { return getElementColor("Primary"); }
    public Color getSecondaryColor() { return getElementColor("Secondary"); }
    public Color getBlurColor() { return getElementColor("Blur"); }
    public Color getWidgetBlurColor() { return getElementColor("Widget blur"); }
    public Color getBackgroundBlurColor() { return getElementColor("Background blur"); }
    public Color getTextColor() { return getElementColor("Text"); }
    public Color getInactiveTextColor() { return getElementColor("Inactive text"); }
    public Color getKnobColor() { return getElementColor("Knob"); }
    public Color getInactiveKnobColor() { return getElementColor("Inactive knob"); }
    public Color getPositiveColor() { return getElementColor("Positive"); }
    public Color getMiddleColor() { return getElementColor("Middle"); }
    public Color getNegativeColor() { return getElementColor("Negative"); }

    public Color getElementColor(String elementName) {
        for (ElementColor element : elementColors) {
            if (element.getName().equalsIgnoreCase(elementName)) {
                return element.getColor();
            }
        }
        return new Color(-1);
    }

    @Getter
    public static class ElementColor {
        private final String name;
        @Setter private Color color;
        private final ColorComponent colorComponent;

        public ElementColor(String name, Color color) {
            this.name = name;
            this.color = color;
            this.colorComponent = new ColorComponent(this);
        }
    }
}
