package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class SunsetTheme extends ADefaultTheme {
    public SunsetTheme() { super("Sunset"); }
    @Override public Color setPrimary() { return new Color(230, 126, 34, 255); }
    @Override public Color setSecondary() { return new Color(211, 84, 0, 255); }
    @Override public Color setBlur() { return new Color(15, 10, 10, 255); }
    @Override public Color setWidgetBlur() { return new Color(25, 20, 20, 255); }
    @Override public Color setBackgroundBlur() { return new Color(20, 15, 15, 255); }
    @Override public Color setText() { return new Color(255, 255, 255, 255); }
    @Override public Color setInactiveText() { return new Color(220, 200, 200, 255); }
    @Override public Color setKnob() { return new Color(255, 150, 100, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}