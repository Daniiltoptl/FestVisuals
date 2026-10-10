package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class GrapeTheme extends ADefaultTheme {
    public GrapeTheme() { super("Grape"); }
    @Override public Color setPrimary() { return new Color(180, 120, 255, 255); }
    @Override public Color setSecondary() { return new Color(130, 80, 210, 255); }
    @Override public Color setBlur() { return new Color(22, 16, 30, 255); }
    @Override public Color setWidgetBlur() { return new Color(34, 26, 46, 255); }
    @Override public Color setBackgroundBlur() { return new Color(28, 20, 38, 255); }
    @Override public Color setText() { return new Color(240, 234, 255, 255); }
    @Override public Color setInactiveText() { return new Color(170, 160, 190, 255); }
    @Override public Color setKnob() { return new Color(200, 150, 255, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
