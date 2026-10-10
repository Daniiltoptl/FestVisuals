package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class RoseTheme extends ADefaultTheme {
    public RoseTheme() { super("Rose"); }
    @Override public Color setPrimary() { return new Color(255, 130, 175, 255); }
    @Override public Color setSecondary() { return new Color(220, 90, 140, 255); }
    @Override public Color setBlur() { return new Color(30, 18, 24, 255); }
    @Override public Color setWidgetBlur() { return new Color(46, 28, 38, 255); }
    @Override public Color setBackgroundBlur() { return new Color(38, 22, 30, 255); }
    @Override public Color setText() { return new Color(255, 235, 244, 255); }
    @Override public Color setInactiveText() { return new Color(190, 155, 170, 255); }
    @Override public Color setKnob() { return new Color(255, 160, 200, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
