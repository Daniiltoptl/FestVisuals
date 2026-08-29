package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class MidnightTheme extends ADefaultTheme {
    public MidnightTheme() { super("Midnight"); }
    @Override public Color setPrimary() { return new Color(155, 89, 182, 255); }
    @Override public Color setSecondary() { return new Color(142, 68, 173, 255); }
    @Override public Color setBlur() { return new Color(5, 5, 10, 255); }
    @Override public Color setWidgetBlur() { return new Color(15, 15, 20, 255); }
    @Override public Color setBackgroundBlur() { return new Color(10, 10, 15, 255); }
    @Override public Color setText() { return new Color(255, 255, 255, 255); }
    @Override public Color setInactiveText() { return new Color(200, 200, 220, 255); }
    @Override public Color setKnob() { return new Color(200, 150, 255, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}