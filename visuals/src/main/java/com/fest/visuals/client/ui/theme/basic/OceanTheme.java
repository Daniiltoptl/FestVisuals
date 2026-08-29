package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class OceanTheme extends ADefaultTheme {
    public OceanTheme() { super("Ocean"); }
    @Override public Color setPrimary() { return new Color(64, 190, 220, 255); }
    @Override public Color setSecondary() { return new Color(40, 130, 200, 255); }
    @Override public Color setBlur() { return new Color(12, 22, 34, 255); }
    @Override public Color setWidgetBlur() { return new Color(20, 34, 52, 255); }
    @Override public Color setBackgroundBlur() { return new Color(16, 28, 44, 255); }
    @Override public Color setText() { return new Color(230, 244, 255, 255); }
    @Override public Color setInactiveText() { return new Color(150, 170, 190, 255); }
    @Override public Color setKnob() { return new Color(120, 210, 240, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
