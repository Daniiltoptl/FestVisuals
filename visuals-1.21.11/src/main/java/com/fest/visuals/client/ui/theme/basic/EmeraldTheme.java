package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class EmeraldTheme extends ADefaultTheme {
    public EmeraldTheme() { super("Emerald"); }
    @Override public Color setPrimary() { return new Color(46, 204, 113, 255); }
    @Override public Color setSecondary() { return new Color(39, 174, 96, 255); }
    @Override public Color setBlur() { return new Color(10, 15, 10, 255); }
    @Override public Color setWidgetBlur() { return new Color(20, 25, 20, 255); }
    @Override public Color setBackgroundBlur() { return new Color(15, 20, 15, 255); }
    @Override public Color setText() { return new Color(255, 255, 255, 255); }
    @Override public Color setInactiveText() { return new Color(200, 200, 200, 255); }
    @Override public Color setKnob() { return new Color(100, 255, 150, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}