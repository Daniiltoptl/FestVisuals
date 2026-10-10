package com.fest.visuals.client.ui.theme.basic;

import java.awt.*;

public class CrimsonTheme extends ADefaultTheme {
    public CrimsonTheme() {
        super("Crimson");
    }

    @Override
    public Color setPrimary() {
        return new Color(214, 118, 132, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(168, 86, 102, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(18, 18, 20, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(28, 28, 30, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(38, 38, 40, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 255, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(154, 154, 154, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 255, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
