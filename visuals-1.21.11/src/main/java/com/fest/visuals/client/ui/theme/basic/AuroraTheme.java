package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class AuroraTheme extends ADefaultTheme {
    public AuroraTheme() { super("Aurora"); }
    @Override public Color setPrimary() { return new Color(100, 220, 180, 255); }
    @Override public Color setSecondary() { return new Color(140, 130, 240, 255); }
    @Override public Color setBlur() { return new Color(14, 18, 28, 255); }
    @Override public Color setWidgetBlur() { return new Color(22, 28, 42, 255); }
    @Override public Color setBackgroundBlur() { return new Color(18, 22, 34, 255); }
    @Override public Color setText() { return new Color(232, 244, 255, 255); }
    @Override public Color setInactiveText() { return new Color(150, 170, 195, 255); }
    @Override public Color setKnob() { return new Color(130, 230, 200, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
