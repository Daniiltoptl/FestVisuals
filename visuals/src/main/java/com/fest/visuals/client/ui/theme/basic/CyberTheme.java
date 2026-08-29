package com.fest.visuals.client.ui.theme.basic;
import java.awt.Color;
public class CyberTheme extends ADefaultTheme {
    public CyberTheme() { super("Cyber"); }
    @Override public Color setPrimary() { return new Color(0, 240, 200, 255); }
    @Override public Color setSecondary() { return new Color(230, 60, 200, 255); }
    @Override public Color setBlur() { return new Color(10, 12, 22, 255); }
    @Override public Color setWidgetBlur() { return new Color(18, 22, 36, 255); }
    @Override public Color setBackgroundBlur() { return new Color(14, 16, 28, 255); }
    @Override public Color setText() { return new Color(230, 245, 255, 255); }
    @Override public Color setInactiveText() { return new Color(140, 155, 180, 255); }
    @Override public Color setKnob() { return new Color(80, 255, 220, 255); }
    @Override public Color setInactiveKnob() { return new Color(255, 255, 255, 255); }
}
