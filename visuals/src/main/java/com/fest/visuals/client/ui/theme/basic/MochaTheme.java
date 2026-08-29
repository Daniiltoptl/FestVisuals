package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class MochaTheme extends ADefaultTheme {
    public MochaTheme() { super("Mocha"); }
    @Override public Color setPrimary() { return new Color(214, 170, 120, 255); }
    @Override public Color setSecondary() { return new Color(168, 120, 80, 255); }
    @Override public Color setBlur() { return new Color(28, 22, 18, 255); }
    @Override public Color setWidgetBlur() { return new Color(42, 34, 28, 255); }
    @Override public Color setBackgroundBlur() { return new Color(36, 28, 22, 255); }
    @Override public Color setText() { return new Color(248, 236, 220, 255); }
    @Override public Color setInactiveText() { return new Color(180, 160, 145, 255); }
    @Override public Color setKnob() { return new Color(230, 190, 140, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
