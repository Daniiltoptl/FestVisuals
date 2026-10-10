package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class SlateTheme extends ADefaultTheme {
    public SlateTheme() { super("Slate"); }
    @Override public Color setPrimary() { return new Color(200, 210, 225, 255); }
    @Override public Color setSecondary() { return new Color(140, 155, 175, 255); }
    @Override public Color setBlur() { return new Color(22, 24, 28, 255); }
    @Override public Color setWidgetBlur() { return new Color(34, 38, 44, 255); }
    @Override public Color setBackgroundBlur() { return new Color(28, 32, 38, 255); }
    @Override public Color setText() { return new Color(240, 244, 250, 255); }
    @Override public Color setInactiveText() { return new Color(155, 165, 180, 255); }
    @Override public Color setKnob() { return new Color(220, 228, 240, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
