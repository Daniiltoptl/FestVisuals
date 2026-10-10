package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class ForestTheme extends ADefaultTheme {
    public ForestTheme() { super("Forest"); }
    @Override public Color setPrimary() { return new Color(126, 200, 90, 255); }
    @Override public Color setSecondary() { return new Color(80, 140, 60, 255); }
    @Override public Color setBlur() { return new Color(18, 24, 18, 255); }
    @Override public Color setWidgetBlur() { return new Color(28, 38, 28, 255); }
    @Override public Color setBackgroundBlur() { return new Color(22, 30, 22, 255); }
    @Override public Color setText() { return new Color(235, 245, 225, 255); }
    @Override public Color setInactiveText() { return new Color(160, 180, 155, 255); }
    @Override public Color setKnob() { return new Color(160, 220, 130, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
