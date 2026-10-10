package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.render.fonts.Icons;
import com.fest.visuals.client.features.modules.hud.StatsHudModule;
import com.fest.visuals.client.ui.widget.InformationWidget;

public class FPSWidget extends InformationWidget {
    private float animFps;

    @Override
    public String getName() {
        return "FPS";
    }

    public FPSWidget() {
        super(50f, 100f);
    }

    @Override
    public String getValue() {
        animFps = MathUtil.interpolate((int) animFps, mc.getFps(), 0.2f);
        return String.valueOf((int) animFps);
    }


    @Override
    public Icons getIcon() {
        return null;
    }

    @Override
    public float fontMul() { return StatsHudModule.getInstance().fontScale.getValue(); }
}
